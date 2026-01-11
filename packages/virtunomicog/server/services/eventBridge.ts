/**
 * Event Bridge Service - Bidirectional Narrative-Business Event Propagation
 * 
 * This service creates the feedback loop between DreamCog narrative events
 * and Vorticog business events, enabling emergent storytelling where
 * gameplay generates narrative and narrative affects gameplay.
 */

import { eq, and, lte, isNull } from "drizzle-orm";
import { drizzle } from "drizzle-orm/mysql2";
import {
  worldEvents,
  WorldEvent,
  InsertWorldEvent,
  scheduledEvents,
  ScheduledEvent,
  InsertScheduledEvent,
  eventPropagation,
  EventPropagation,
  InsertEventPropagation,
  companies,
  transactions,
  agents,
  agentEmotionalStates,
  agentMemories,
  notifications,
  marketListings,
  gameState,
} from "../../drizzle/schema";
import { agentBrain } from "./agentBrain";

// ============================================================================
// Types
// ============================================================================

export interface BusinessEvent {
  type: "bankruptcy" | "merger" | "market_crash" | "expansion" | "layoff" | "innovation" | "scandal" | "success";
  companyId: number;
  magnitude: number; // 1-100
  description: string;
  affectedResources?: number[];
  affectedCities?: number[];
}

export interface NarrativeEvent {
  type: "conflict" | "alliance" | "betrayal" | "discovery" | "crisis" | "celebration" | "tragedy";
  worldId: number;
  importance: number; // 1-100
  description: string;
  affectedAgentIds?: number[];
}

export interface PropagationResult {
  sourceType: "business" | "narrative";
  targetEvents: Array<{
    type: string;
    description: string;
    effects: Record<string, unknown>;
  }>;
}

// ============================================================================
// Database Helper
// ============================================================================

let _db: ReturnType<typeof drizzle> | null = null;

function getDb() {
  if (!_db && process.env.DATABASE_URL) {
    _db = drizzle(process.env.DATABASE_URL);
  }
  return _db;
}

// ============================================================================
// Event Bridge Service
// ============================================================================

export class EventBridgeService {
  /**
   * Process a business event and propagate to narrative system
   */
  async propagateBusinessEvent(event: BusinessEvent): Promise<PropagationResult> {
    const db = getDb();
    if (!db) throw new Error("Database not available");
    
    const targetEvents: PropagationResult["targetEvents"] = [];
    
    // Get current game state for turn number
    const [state] = await db.select().from(gameState).limit(1);
    const currentTurn = state?.currentTurn || 1;
    
    // Create world event based on business event type
    const worldEventData = this.businessToNarrativeMapping(event, currentTurn);
    
    if (worldEventData && worldEventData.type && worldEventData.name) {
      // Get or create default world
      let worldId = 1;
      const [world] = await db.select().from(worldEvents).limit(1);
      if (world) worldId = world.worldId;
      
      await db.insert(worldEvents).values({
        worldId,
        type: worldEventData.type,
        name: worldEventData.name,
        description: worldEventData.description || "",
        importance: worldEventData.importance || 50,
        effects: worldEventData.effects,
        affectedCityIds: worldEventData.affectedCityIds,
        affectedResourceIds: worldEventData.affectedResourceIds,
        startTurn: worldEventData.startTurn || currentTurn,
        endTurn: worldEventData.endTurn,
      });
      
      targetEvents.push({
        type: worldEventData.type || "economic",
        description: worldEventData.description || "",
        effects: worldEventData.effects || {},
      });
    }
    
    // Affect agents emotionally based on business event
    await this.affectAgentsFromBusinessEvent(event);
    
    // Log the propagation
    await db.insert(eventPropagation).values({
      sourceType: "business",
      sourceEventId: event.companyId,
      sourceEventType: event.type,
      targetType: "narrative",
      targetEventType: worldEventData?.type || "unknown",
      propagationData: { event, targetEvents },
    });
    
    return { sourceType: "business", targetEvents };
  }
  
  /**
   * Process a narrative event and propagate to business system
   */
  async propagateNarrativeEvent(event: NarrativeEvent): Promise<PropagationResult> {
    const db = getDb();
    if (!db) throw new Error("Database not available");
    
    const targetEvents: PropagationResult["targetEvents"] = [];
    
    // Map narrative event to business effects
    const businessEffects = this.narrativeToBusinessMapping(event);
    
    // Apply market effects
    if (businessEffects.marketPriceModifier) {
      for (const [resourceId, modifier] of Object.entries(businessEffects.marketPriceModifier)) {
        // Update market listings prices
        const listings = await db.select().from(marketListings).where(eq(marketListings.resourceTypeId, parseInt(resourceId)));
        for (const listing of listings) {
          const newPrice = parseFloat(listing.pricePerUnit) * (1 + modifier);
          await db.update(marketListings)
            .set({ pricePerUnit: newPrice.toFixed(2) })
            .where(eq(marketListings.id, listing.id));
        }
        
        targetEvents.push({
          type: "market_adjustment",
          description: `Market prices adjusted by ${(modifier * 100).toFixed(1)}% due to ${event.type}`,
          effects: { resourceId, modifier },
        });
      }
    }
    
    // Affect company reputations
    if (businessEffects.reputationModifier && event.affectedAgentIds) {
      for (const agentId of event.affectedAgentIds) {
        const [agent] = await db.select().from(agents).where(eq(agents.id, agentId)).limit(1);
        if (agent?.companyId) {
          const [company] = await db.select().from(companies).where(eq(companies.id, agent.companyId)).limit(1);
          if (company) {
            const newReputation = Math.max(0, Math.min(100, company.reputation + businessEffects.reputationModifier));
            await db.update(companies)
              .set({ reputation: newReputation })
              .where(eq(companies.id, company.id));
            
            targetEvents.push({
              type: "reputation_change",
              description: `Company reputation changed by ${businessEffects.reputationModifier} due to ${event.type}`,
              effects: { companyId: company.id, change: businessEffects.reputationModifier },
            });
          }
        }
      }
    }
    
    // Create notifications for affected players
    if (event.affectedAgentIds) {
      for (const agentId of event.affectedAgentIds) {
        const [agent] = await db.select().from(agents).where(eq(agents.id, agentId)).limit(1);
        if (agent?.companyId) {
          const [company] = await db.select().from(companies).where(eq(companies.id, agent.companyId)).limit(1);
          if (company) {
            await db.insert(notifications).values({
              userId: company.userId,
              type: event.type === "crisis" || event.type === "tragedy" ? "warning" : "info",
              title: `World Event: ${event.type.charAt(0).toUpperCase() + event.type.slice(1)}`,
              message: event.description,
            });
          }
        }
      }
    }
    
    // Log the propagation
    await db.insert(eventPropagation).values({
      sourceType: "narrative",
      sourceEventId: event.worldId,
      sourceEventType: event.type,
      targetType: "business",
      targetEventType: "market_effects",
      propagationData: { event, targetEvents, businessEffects },
    });
    
    return { sourceType: "narrative", targetEvents };
  }
  
  /**
   * Process scheduled events that are due
   */
  async processScheduledEvents(currentTurn: number): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    // Get all unprocessed scheduled events that are due
    const dueEvents = await db.select()
      .from(scheduledEvents)
      .where(and(
        eq(scheduledEvents.isProcessed, false),
        lte(scheduledEvents.triggerTurn, currentTurn)
      ));
    
    for (const event of dueEvents) {
      try {
        if (event.type === "world_event") {
          await this.triggerWorldEvent(event);
        } else if (event.type === "agent_action") {
          await this.triggerAgentAction(event);
        } else if (event.type === "market_change") {
          await this.triggerMarketChange(event);
        }
        
        // Mark as processed
        await db.update(scheduledEvents)
          .set({ isProcessed: true })
          .where(eq(scheduledEvents.id, event.id));
        
        // If recurring, create next occurrence
        if (event.triggerType === "recurring" && event.triggerTurn) {
          const eventData = event.eventData as { interval?: number };
          const interval = eventData?.interval || 10;
          await db.insert(scheduledEvents).values({
            worldId: event.worldId,
            type: event.type,
            triggerType: "recurring",
            triggerTurn: currentTurn + interval,
            eventData: event.eventData,
            priority: event.priority,
          });
        }
      } catch (error) {
        console.error(`Failed to process scheduled event ${event.id}:`, error);
      }
    }
  }
  
  /**
   * Schedule a future event
   */
  async scheduleEvent(data: InsertScheduledEvent): Promise<ScheduledEvent> {
    const db = getDb();
    if (!db) throw new Error("Database not available");
    
    const [result] = await db.insert(scheduledEvents).values(data).$returningId();
    const [created] = await db.select().from(scheduledEvents).where(eq(scheduledEvents.id, result.id)).limit(1);
    return created;
  }
  
  // ============================================================================
  // Private Helper Methods
  // ============================================================================
  
  private businessToNarrativeMapping(event: BusinessEvent, currentTurn: number): Partial<InsertWorldEvent> | null {
    const mappings: Record<BusinessEvent["type"], Partial<InsertWorldEvent>> = {
      bankruptcy: {
        type: "economic",
        name: "Corporate Collapse",
        description: `A major company has declared bankruptcy, sending shockwaves through the market.`,
        importance: event.magnitude,
        effects: {
          marketPriceModifier: { all: -0.05 },
          demandModifier: { all: -0.1 },
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 5,
      },
      merger: {
        type: "economic",
        name: "Corporate Merger",
        description: `Two companies have merged, reshaping the competitive landscape.`,
        importance: event.magnitude,
        effects: {
          marketPriceModifier: {},
          supplyModifier: { all: 0.1 },
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 10,
      },
      market_crash: {
        type: "economic",
        name: "Market Crash",
        description: `A sudden market crash has devastated investor confidence.`,
        importance: Math.min(100, event.magnitude + 20),
        effects: {
          marketPriceModifier: { all: -0.2 },
          demandModifier: { all: -0.3 },
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 15,
      },
      expansion: {
        type: "economic",
        name: "Business Expansion",
        description: `A company is expanding operations, creating new opportunities.`,
        importance: event.magnitude,
        effects: {
          demandModifier: { all: 0.05 },
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 8,
      },
      layoff: {
        type: "social",
        name: "Mass Layoffs",
        description: `A company has announced significant layoffs, affecting the workforce.`,
        importance: event.magnitude,
        effects: {
          demandModifier: { all: -0.05 },
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 3,
      },
      innovation: {
        type: "technological",
        name: "Technological Breakthrough",
        description: `A company has achieved a major innovation, disrupting the industry.`,
        importance: event.magnitude,
        effects: {
          supplyModifier: { all: 0.1 },
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 20,
      },
      scandal: {
        type: "social",
        name: "Corporate Scandal",
        description: `A scandal has rocked a major company, damaging trust in the industry.`,
        importance: event.magnitude,
        effects: {
          reputationModifier: -10,
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 7,
      },
      success: {
        type: "economic",
        name: "Business Success Story",
        description: `A company has achieved remarkable success, inspiring the market.`,
        importance: event.magnitude,
        effects: {
          marketPriceModifier: { all: 0.02 },
          demandModifier: { all: 0.05 },
        },
        startTurn: currentTurn,
        endTurn: currentTurn + 5,
      },
    };
    
    return mappings[event.type] || null;
  }
  
  private narrativeToBusinessMapping(event: NarrativeEvent): {
    marketPriceModifier?: Record<string, number>;
    taxRateModifier?: number;
    demandModifier?: Record<string, number>;
    supplyModifier?: Record<string, number>;
    reputationModifier?: number;
  } {
    const mappings: Record<NarrativeEvent["type"], ReturnType<typeof this.narrativeToBusinessMapping>> = {
      conflict: {
        marketPriceModifier: { all: 0.15 },
        supplyModifier: { all: -0.2 },
        reputationModifier: -5,
      },
      alliance: {
        marketPriceModifier: { all: -0.05 },
        demandModifier: { all: 0.1 },
        reputationModifier: 5,
      },
      betrayal: {
        reputationModifier: -15,
        demandModifier: { all: -0.1 },
      },
      discovery: {
        supplyModifier: { all: 0.2 },
        marketPriceModifier: { all: -0.1 },
      },
      crisis: {
        marketPriceModifier: { all: 0.25 },
        demandModifier: { all: -0.15 },
        supplyModifier: { all: -0.15 },
      },
      celebration: {
        demandModifier: { all: 0.15 },
        reputationModifier: 3,
      },
      tragedy: {
        demandModifier: { all: -0.2 },
        reputationModifier: -8,
      },
    };
    
    return mappings[event.type] || {};
  }
  
  private async affectAgentsFromBusinessEvent(event: BusinessEvent): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    // Get agents associated with the company
    const companyAgents = await db.select()
      .from(agents)
      .where(eq(agents.companyId, event.companyId));
    
    for (const agent of companyAgents) {
      // Update emotional state based on event type
      const emotionalUpdate = this.getEmotionalImpact(event.type, event.magnitude);
      
      await db.update(agentEmotionalStates)
        .set(emotionalUpdate)
        .where(eq(agentEmotionalStates.agentId, agent.id));
      
      // Create a memory of the event
      await db.insert(agentMemories).values({
        agentId: agent.id,
        type: event.type === "success" || event.type === "innovation" ? "achievement" : 
              event.type === "bankruptcy" || event.type === "scandal" ? "trauma" : "experience",
        content: `Witnessed ${event.type}: ${event.description}`,
        emotionalImpact: emotionalUpdate.overallMood ? emotionalUpdate.overallMood - 50 : 0,
        importance: event.magnitude,
        relatedCompanyId: event.companyId,
      });
    }
  }
  
  private getEmotionalImpact(eventType: BusinessEvent["type"], magnitude: number): Partial<{
    happiness: number;
    satisfaction: number;
    stress: number;
    fear: number;
    trust: number;
    overallMood: number;
    stressLevel: number;
  }> {
    const scale = magnitude / 100;
    
    const impacts: Record<BusinessEvent["type"], ReturnType<typeof this.getEmotionalImpact>> = {
      bankruptcy: { happiness: 20, satisfaction: 15, stress: 80, fear: 70, overallMood: 25, stressLevel: 75 },
      merger: { happiness: 45, satisfaction: 40, stress: 55, overallMood: 45, stressLevel: 50 },
      market_crash: { happiness: 25, satisfaction: 20, stress: 75, fear: 65, overallMood: 30, stressLevel: 70 },
      expansion: { happiness: 65, satisfaction: 70, stress: 40, overallMood: 65, stressLevel: 35 },
      layoff: { happiness: 30, satisfaction: 25, stress: 70, fear: 60, overallMood: 30, stressLevel: 65 },
      innovation: { happiness: 75, satisfaction: 80, stress: 30, overallMood: 75, stressLevel: 25 },
      scandal: { happiness: 25, satisfaction: 20, stress: 65, trust: 30, overallMood: 30, stressLevel: 60 },
      success: { happiness: 80, satisfaction: 85, stress: 20, overallMood: 80, stressLevel: 15 },
    };
    
    const base = impacts[eventType] || {};
    // Scale the impact by magnitude
    const scaled: typeof base = {};
    for (const [key, value] of Object.entries(base)) {
      if (value !== undefined) {
        const deviation = value - 50;
        scaled[key as keyof typeof base] = Math.round(50 + deviation * scale);
      }
    }
    return scaled;
  }
  
  private async triggerWorldEvent(event: ScheduledEvent): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    const eventData = event.eventData as Partial<InsertWorldEvent>;
    if (eventData && event.worldId) {
      const [state] = await db.select().from(gameState).limit(1);
      await db.insert(worldEvents).values({
        worldId: event.worldId,
        type: eventData.type || "economic",
        name: eventData.name || "Scheduled Event",
        description: eventData.description || "A scheduled event has occurred",
        importance: eventData.importance || 50,
        effects: eventData.effects,
        startTurn: state?.currentTurn || 1,
        endTurn: eventData.endTurn,
      });
    }
  }
  
  private async triggerAgentAction(event: ScheduledEvent): Promise<void> {
    const eventData = event.eventData as { agentId?: number; action?: string };
    if (eventData?.agentId && eventData?.action) {
      // Trigger agent to make a decision
      await agentBrain.makeDecision(eventData.agentId, {
        type: "trade",
        situation: eventData.action,
        options: [
          { id: "proceed", description: "Proceed with planned action", expectedOutcome: "Execute plan", riskLevel: 40, potentialReward: 60, requiresCooperation: false, requiresConflict: false },
          { id: "delay", description: "Delay action", expectedOutcome: "Wait for better conditions", riskLevel: 20, potentialReward: 30, requiresCooperation: false, requiresConflict: false },
          { id: "cancel", description: "Cancel action", expectedOutcome: "Abort plan", riskLevel: 10, potentialReward: 10, requiresCooperation: false, requiresConflict: false },
        ],
      });
    }
  }
  
  private async triggerMarketChange(event: ScheduledEvent): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    const eventData = event.eventData as { resourceId?: number; priceChange?: number };
    if (eventData?.resourceId && eventData?.priceChange) {
      const listings = await db.select()
        .from(marketListings)
        .where(eq(marketListings.resourceTypeId, eventData.resourceId));
      
      for (const listing of listings) {
        const newPrice = parseFloat(listing.pricePerUnit) * (1 + eventData.priceChange);
        await db.update(marketListings)
          .set({ pricePerUnit: Math.max(0.01, newPrice).toFixed(2) })
          .where(eq(marketListings.id, listing.id));
      }
    }
  }
}

// Export singleton instance
export const eventBridge = new EventBridgeService();
