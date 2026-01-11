/**
 * World-Economy Synchronization Service
 * 
 * This service connects world-building elements to market dynamics,
 * ensuring that narrative world state affects the business simulation
 * and vice versa.
 */

import { eq, and, gte, lte, sql } from "drizzle-orm";
import { drizzle } from "drizzle-orm/mysql2";
import {
  worlds,
  World,
  InsertWorld,
  worldEvents,
  WorldEvent,
  cities,
  City,
  resourceTypes,
  ResourceType,
  marketListings,
  gameState,
  loreEntries,
  InsertLoreEntry,
  companies,
  notifications,
} from "../../drizzle/schema";

// ============================================================================
// Types
// ============================================================================

export interface WorldState {
  world: World;
  activeEvents: WorldEvent[];
  marketModifiers: MarketModifiers;
  economicIndicators: EconomicIndicators;
}

export interface MarketModifiers {
  globalPriceMultiplier: number;
  globalDemandMultiplier: number;
  globalSupplyMultiplier: number;
  resourceModifiers: Record<number, { price: number; demand: number; supply: number }>;
  cityModifiers: Record<number, { taxRate: number; dangerLevel: number; shippingCost: number }>;
}

export interface EconomicIndicators {
  marketHealth: number; // 0-100
  inflation: number; // percentage
  unemployment: number; // percentage
  consumerConfidence: number; // 0-100
  tradeVolume: number;
}

export interface LocationEffect {
  cityId: number;
  dangerLevel: number; // 0-100
  shippingCostMultiplier: number;
  productionEfficiencyModifier: number;
  description: string;
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
// World Economy Service
// ============================================================================

export class WorldEconomyService {
  /**
   * Get the current world state including all active modifiers
   */
  async getWorldState(worldId: number): Promise<WorldState | null> {
    const db = getDb();
    if (!db) return null;
    
    const [world] = await db.select().from(worlds).where(eq(worlds.id, worldId)).limit(1);
    if (!world) return null;
    
    const [state] = await db.select().from(gameState).limit(1);
    const currentTurn = state?.currentTurn || 1;
    
    // Get active world events
    const activeEvents = await db.select()
      .from(worldEvents)
      .where(and(
        eq(worldEvents.worldId, worldId),
        eq(worldEvents.isActive, true),
        lte(worldEvents.startTurn, currentTurn)
      ));
    
    // Filter events that haven't ended yet
    const currentActiveEvents = activeEvents.filter(e => 
      !e.endTurn || e.endTurn >= currentTurn
    );
    
    // Calculate market modifiers from active events
    const marketModifiers = this.calculateMarketModifiers(currentActiveEvents);
    
    // Calculate economic indicators
    const economicIndicators = await this.calculateEconomicIndicators(worldId);
    
    return {
      world,
      activeEvents: currentActiveEvents,
      marketModifiers,
      economicIndicators,
    };
  }
  
  /**
   * Create or update a world
   */
  async createWorld(data: InsertWorld): Promise<World> {
    const db = getDb();
    if (!db) throw new Error("Database not available");
    
    const [result] = await db.insert(worlds).values(data).$returningId();
    const [created] = await db.select().from(worlds).where(eq(worlds.id, result.id)).limit(1);
    
    // Create initial lore entries
    await this.generateInitialLore(result.id, data);
    
    return created;
  }
  
  /**
   * Apply world event effects to the economy
   */
  async applyWorldEventEffects(eventId: number): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    const [event] = await db.select().from(worldEvents).where(eq(worldEvents.id, eventId)).limit(1);
    if (!event || !event.effects) return;
    
    const effects = event.effects as {
      marketPriceModifier?: Record<string, number>;
      taxRateModifier?: number;
      demandModifier?: Record<string, number>;
      supplyModifier?: Record<string, number>;
      reputationModifier?: number;
    };
    
    // Apply tax rate changes to affected cities
    if (effects.taxRateModifier && event.affectedCityIds) {
      for (const cityId of event.affectedCityIds) {
        const [city] = await db.select().from(cities).where(eq(cities.id, cityId)).limit(1);
        if (city) {
          const newTaxRate = Math.max(0, Math.min(1, parseFloat(city.taxRate) + effects.taxRateModifier));
          await db.update(cities)
            .set({ taxRate: newTaxRate.toFixed(4) })
            .where(eq(cities.id, cityId));
        }
      }
    }
    
    // Apply price modifiers to market listings
    if (effects.marketPriceModifier) {
      for (const [resourceKey, modifier] of Object.entries(effects.marketPriceModifier)) {
        if (resourceKey === "all") {
          // Apply to all listings
          const listings = await db.select().from(marketListings).where(eq(marketListings.isActive, true));
          for (const listing of listings) {
            const newPrice = parseFloat(listing.pricePerUnit) * (1 + modifier);
            await db.update(marketListings)
              .set({ pricePerUnit: Math.max(0.01, newPrice).toFixed(2) })
              .where(eq(marketListings.id, listing.id));
          }
        } else if (event.affectedResourceIds?.includes(parseInt(resourceKey))) {
          const listings = await db.select()
            .from(marketListings)
            .where(and(
              eq(marketListings.resourceTypeId, parseInt(resourceKey)),
              eq(marketListings.isActive, true)
            ));
          for (const listing of listings) {
            const newPrice = parseFloat(listing.pricePerUnit) * (1 + modifier);
            await db.update(marketListings)
              .set({ pricePerUnit: Math.max(0.01, newPrice).toFixed(2) })
              .where(eq(marketListings.id, listing.id));
          }
        }
      }
    }
    
    // Notify affected companies
    await this.notifyAffectedCompanies(event);
  }
  
  /**
   * Get location effects for a city based on world state
   */
  async getLocationEffects(cityId: number, worldId: number): Promise<LocationEffect> {
    const db = getDb();
    if (!db) {
      return {
        cityId,
        dangerLevel: 0,
        shippingCostMultiplier: 1,
        productionEfficiencyModifier: 1,
        description: "Normal conditions",
      };
    }
    
    const [state] = await db.select().from(gameState).limit(1);
    const currentTurn = state?.currentTurn || 1;
    
    // Get events affecting this city
    const affectingEvents = await db.select()
      .from(worldEvents)
      .where(and(
        eq(worldEvents.worldId, worldId),
        eq(worldEvents.isActive, true),
        lte(worldEvents.startTurn, currentTurn)
      ));
    
    let dangerLevel = 0;
    let shippingCostMultiplier = 1;
    let productionEfficiencyModifier = 1;
    const descriptions: string[] = [];
    
    for (const event of affectingEvents) {
      if (!event.affectedCityIds?.includes(cityId)) continue;
      if (event.endTurn && event.endTurn < currentTurn) continue;
      
      // Apply effects based on event type
      switch (event.type) {
        case "conflict":
          dangerLevel += 30;
          shippingCostMultiplier *= 1.5;
          productionEfficiencyModifier *= 0.7;
          descriptions.push(`Conflict: ${event.name}`);
          break;
        case "natural":
          dangerLevel += 20;
          shippingCostMultiplier *= 1.3;
          productionEfficiencyModifier *= 0.8;
          descriptions.push(`Natural disaster: ${event.name}`);
          break;
        case "political":
          shippingCostMultiplier *= 1.2;
          descriptions.push(`Political instability: ${event.name}`);
          break;
        case "economic":
          productionEfficiencyModifier *= 0.9;
          descriptions.push(`Economic event: ${event.name}`);
          break;
        case "technological":
          productionEfficiencyModifier *= 1.1;
          descriptions.push(`Tech advancement: ${event.name}`);
          break;
        case "social":
          // Minor effects
          descriptions.push(`Social event: ${event.name}`);
          break;
        case "discovery":
          productionEfficiencyModifier *= 1.15;
          descriptions.push(`Discovery: ${event.name}`);
          break;
      }
    }
    
    return {
      cityId,
      dangerLevel: Math.min(100, dangerLevel),
      shippingCostMultiplier,
      productionEfficiencyModifier,
      description: descriptions.length > 0 ? descriptions.join("; ") : "Normal conditions",
    };
  }
  
  /**
   * Synchronize world state with economy at turn end
   */
  async synchronizeAtTurnEnd(worldId: number): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    const [state] = await db.select().from(gameState).limit(1);
    const currentTurn = state?.currentTurn || 1;
    
    // Deactivate expired events
    await db.update(worldEvents)
      .set({ isActive: false })
      .where(and(
        eq(worldEvents.worldId, worldId),
        lte(worldEvents.endTurn, currentTurn)
      ));
    
    // Apply ongoing event effects
    const activeEvents = await db.select()
      .from(worldEvents)
      .where(and(
        eq(worldEvents.worldId, worldId),
        eq(worldEvents.isActive, true)
      ));
    
    for (const event of activeEvents) {
      await this.applyWorldEventEffects(event.id);
    }
    
    // Calculate and store economic indicators
    const indicators = await this.calculateEconomicIndicators(worldId);
    
    // Create lore entry for significant economic changes
    if (indicators.inflation > 10 || indicators.inflation < -5) {
      await db.insert(loreEntries).values({
        worldId,
        category: "economics",
        title: indicators.inflation > 10 ? "Period of High Inflation" : "Deflationary Period",
        content: `Turn ${currentTurn}: The economy experienced ${indicators.inflation > 10 ? "significant inflation" : "deflation"} with prices ${indicators.inflation > 0 ? "rising" : "falling"} by ${Math.abs(indicators.inflation).toFixed(1)}%.`,
        isPublic: true,
        tags: ["economy", "inflation", `turn-${currentTurn}`],
      });
    }
  }
  
  /**
   * Get all lore entries for a world
   */
  async getWorldLore(worldId: number, category?: string): Promise<typeof loreEntries.$inferSelect[]> {
    const db = getDb();
    if (!db) return [];
    
    if (category) {
      return db.select()
        .from(loreEntries)
        .where(and(
          eq(loreEntries.worldId, worldId),
          eq(loreEntries.category, category as typeof loreEntries.$inferSelect["category"])
        ));
    }
    
    return db.select().from(loreEntries).where(eq(loreEntries.worldId, worldId));
  }
  
  // ============================================================================
  // Private Helper Methods
  // ============================================================================
  
  private calculateMarketModifiers(events: WorldEvent[]): MarketModifiers {
    const modifiers: MarketModifiers = {
      globalPriceMultiplier: 1,
      globalDemandMultiplier: 1,
      globalSupplyMultiplier: 1,
      resourceModifiers: {},
      cityModifiers: {},
    };
    
    for (const event of events) {
      if (!event.effects) continue;
      
      const effects = event.effects as {
        marketPriceModifier?: Record<string, number>;
        demandModifier?: Record<string, number>;
        supplyModifier?: Record<string, number>;
        taxRateModifier?: number;
      };
      
      // Apply global modifiers
      if (effects.marketPriceModifier?.all) {
        modifiers.globalPriceMultiplier *= (1 + effects.marketPriceModifier.all);
      }
      if (effects.demandModifier?.all) {
        modifiers.globalDemandMultiplier *= (1 + effects.demandModifier.all);
      }
      if (effects.supplyModifier?.all) {
        modifiers.globalSupplyMultiplier *= (1 + effects.supplyModifier.all);
      }
      
      // Apply resource-specific modifiers
      if (event.affectedResourceIds) {
        for (const resourceId of event.affectedResourceIds) {
          if (!modifiers.resourceModifiers[resourceId]) {
            modifiers.resourceModifiers[resourceId] = { price: 1, demand: 1, supply: 1 };
          }
          
          const resKey = resourceId.toString();
          if (effects.marketPriceModifier?.[resKey]) {
            modifiers.resourceModifiers[resourceId].price *= (1 + effects.marketPriceModifier[resKey]);
          }
          if (effects.demandModifier?.[resKey]) {
            modifiers.resourceModifiers[resourceId].demand *= (1 + effects.demandModifier[resKey]);
          }
          if (effects.supplyModifier?.[resKey]) {
            modifiers.resourceModifiers[resourceId].supply *= (1 + effects.supplyModifier[resKey]);
          }
        }
      }
      
      // Apply city-specific modifiers
      if (event.affectedCityIds && effects.taxRateModifier) {
        for (const cityId of event.affectedCityIds) {
          if (!modifiers.cityModifiers[cityId]) {
            modifiers.cityModifiers[cityId] = { taxRate: 0, dangerLevel: 0, shippingCost: 1 };
          }
          modifiers.cityModifiers[cityId].taxRate += effects.taxRateModifier;
          
          // Conflict events increase danger and shipping costs
          if (event.type === "conflict") {
            modifiers.cityModifiers[cityId].dangerLevel += 20;
            modifiers.cityModifiers[cityId].shippingCost *= 1.3;
          }
        }
      }
    }
    
    return modifiers;
  }
  
  private async calculateEconomicIndicators(worldId: number): Promise<EconomicIndicators> {
    const db = getDb();
    if (!db) {
      return {
        marketHealth: 50,
        inflation: 0,
        unemployment: 5,
        consumerConfidence: 50,
        tradeVolume: 0,
      };
    }
    
    // Get active listings count as proxy for market health
    const listings = await db.select().from(marketListings).where(eq(marketListings.isActive, true));
    const marketHealth = Math.min(100, listings.length * 5);
    
    // Calculate average price deviation from base prices as inflation proxy
    const allResources = await db.select().from(resourceTypes);
    let totalDeviation = 0;
    let resourceCount = 0;
    
    for (const resource of allResources) {
      const resourceListings = listings.filter(l => l.resourceTypeId === resource.id);
      if (resourceListings.length > 0) {
        const avgPrice = resourceListings.reduce((sum, l) => sum + parseFloat(l.pricePerUnit), 0) / resourceListings.length;
        const basePrice = parseFloat(resource.basePrice);
        totalDeviation += (avgPrice - basePrice) / basePrice;
        resourceCount++;
      }
    }
    
    const inflation = resourceCount > 0 ? (totalDeviation / resourceCount) * 100 : 0;
    
    // Trade volume is total quantity listed
    const tradeVolume = listings.reduce((sum, l) => sum + parseFloat(l.quantity), 0);
    
    // Consumer confidence based on market health and inflation
    const consumerConfidence = Math.max(0, Math.min(100, 
      50 + (marketHealth - 50) * 0.3 - Math.abs(inflation) * 2
    ));
    
    return {
      marketHealth,
      inflation: Math.round(inflation * 10) / 10,
      unemployment: 5, // Placeholder - would need employee data
      consumerConfidence: Math.round(consumerConfidence),
      tradeVolume: Math.round(tradeVolume),
    };
  }
  
  private async generateInitialLore(worldId: number, worldData: InsertWorld): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    const loreEntriesToCreate: InsertLoreEntry[] = [
      {
        worldId,
        category: "history",
        title: "The Founding Era",
        content: `The world of ${worldData.name} emerged during the ${worldData.timePeriod || "modern"} era. Its ${worldData.economicSystem || "capitalist"} economic system has shaped the development of commerce and industry.`,
        isPublic: true,
        tags: ["founding", "history", "origin"],
      },
      {
        worldId,
        category: "economics",
        title: "Economic Foundations",
        content: `The economy operates under a ${worldData.economicSystem || "capitalist"} system with a technology level of ${worldData.technologyLevel || 50}/100. Trade and commerce form the backbone of society.`,
        isPublic: true,
        tags: ["economy", "trade", "foundation"],
      },
    ];
    
    if (worldData.genre === "cyberpunk" || worldData.genre === "futuristic") {
      loreEntriesToCreate.push({
        worldId,
        category: "science",
        title: "Technological Advancement",
        content: "Advanced technology permeates every aspect of life, from automated production to AI-assisted decision making.",
        isPublic: true,
        tags: ["technology", "science", "future"],
      });
    }
    
    for (const entry of loreEntriesToCreate) {
      await db.insert(loreEntries).values(entry);
    }
  }
  
  private async notifyAffectedCompanies(event: WorldEvent): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    // Get all companies (in a real system, filter by affected cities)
    const allCompanies = await db.select().from(companies);
    
    for (const company of allCompanies) {
      await db.insert(notifications).values({
        userId: company.userId,
        type: event.type === "conflict" || event.type === "natural" ? "warning" : "info",
        title: `World Event: ${event.name}`,
        message: event.description || `A ${event.type} event is affecting the market.`,
      });
    }
  }
}

// Export singleton instance
export const worldEconomy = new WorldEconomyService();
