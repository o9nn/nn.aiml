/**
 * Agent Brain Service - Personality-Weighted Decision Making
 */

import { eq, and, desc } from "drizzle-orm";
import { drizzle } from "drizzle-orm/mysql2";
import {
  agents,
  Agent,
  agentEmotionalStates,
  AgentEmotionalState,
  agentMotivations,
  InsertAgentMotivation,
  agentMemories,
  AgentMemory,
  agentRelationships,
  AgentRelationship,
  agentDecisions,
} from "../../drizzle/schema";

// Types
export interface DecisionContext {
  type: "trade" | "negotiation" | "investment" | "hiring" | "partnership" | "conflict" | "cooperation";
  situation: string;
  options: DecisionOption[];
  relatedAgentId?: number;
  relatedCompanyId?: number;
  financialStakes?: number;
  riskLevel?: number;
}

export interface DecisionOption {
  id: string;
  description: string;
  expectedOutcome: string;
  riskLevel: number;
  potentialReward: number;
  requiresCooperation: boolean;
  requiresConflict: boolean;
}

export interface DecisionResult {
  chosenOption: DecisionOption;
  reasoning: string;
  confidence: number;
  personalityInfluence: PersonalityInfluence;
  emotionalInfluence: EmotionalInfluence;
}

export interface PersonalityInfluence {
  openness: number;
  conscientiousness: number;
  extraversion: number;
  agreeableness: number;
  neuroticism: number;
}

export interface EmotionalInfluence {
  mood: number;
  stress: number;
  trust: number;
}

interface ScoredOption {
  option: DecisionOption;
  score: {
    total: number;
    personalityInfluence: PersonalityInfluence;
    emotionalInfluence: EmotionalInfluence;
  };
}

let _db: ReturnType<typeof drizzle> | null = null;

function getDb() {
  if (!_db && process.env.DATABASE_URL) {
    _db = drizzle(process.env.DATABASE_URL);
  }
  return _db;
}

export class AgentBrainService {
  async makeDecision(agentId: number, context: DecisionContext): Promise<DecisionResult> {
    const db = getDb();
    if (!db) throw new Error("Database not available");
    
    const [agent] = await db.select().from(agents).where(eq(agents.id, agentId)).limit(1);
    if (!agent) throw new Error(`Agent ${agentId} not found`);
    
    const [emotionalState] = await db.select().from(agentEmotionalStates).where(eq(agentEmotionalStates.agentId, agentId)).limit(1);
    const memories = await db.select().from(agentMemories).where(eq(agentMemories.agentId, agentId)).orderBy(desc(agentMemories.importance)).limit(10);
    
    let relationship: AgentRelationship | undefined;
    if (context.relatedAgentId) {
      const [rel] = await db.select().from(agentRelationships).where(and(eq(agentRelationships.agentId1, agentId), eq(agentRelationships.agentId2, context.relatedAgentId))).limit(1);
      relationship = rel;
    }
    
    const scoredOptions: ScoredOption[] = context.options.map(option => ({
      option,
      score: this.calculateOptionScore(agent, emotionalState, memories, relationship, option),
    }));
    
    scoredOptions.sort((a, b) => b.score.total - a.score.total);
    const chosen = scoredOptions[0];
    const reasoning = this.generateReasoning(agent, chosen);
    const confidence = this.calculateConfidence(scoredOptions);
    
    await db.insert(agentDecisions).values({
      agentId,
      decisionType: context.type,
      context: context as unknown as Record<string, unknown>,
      options: context.options.map(o => ({ id: o.id, description: o.description, score: scoredOptions.find(s => s.option.id === o.id)?.score.total || 0 })),
      chosenOption: chosen.option.id,
      reasoning,
      personalityFactors: chosen.score.personalityInfluence,
      emotionalFactors: chosen.score.emotionalInfluence,
      outcome: "pending",
    });
    
    return { chosenOption: chosen.option, reasoning, confidence, personalityInfluence: chosen.score.personalityInfluence, emotionalInfluence: chosen.score.emotionalInfluence };
  }
  
  private calculateOptionScore(agent: Agent, emotionalState: AgentEmotionalState | undefined, memories: AgentMemory[], relationship: AgentRelationship | undefined, option: DecisionOption) {
    let score = 50;
    const personalityInfluence: PersonalityInfluence = { openness: 0, conscientiousness: 0, extraversion: 0, agreeableness: 0, neuroticism: 0 };
    
    const opennessEffect = (agent.openness - 50) / 50;
    if (option.riskLevel > 50) { personalityInfluence.openness = opennessEffect * 15; score += personalityInfluence.openness; }
    
    const conscientiousnessEffect = (agent.conscientiousness - 50) / 50;
    personalityInfluence.conscientiousness = conscientiousnessEffect * (100 - option.riskLevel) / 5;
    score += personalityInfluence.conscientiousness;
    
    const extraversionEffect = (agent.extraversion - 50) / 50;
    if (option.requiresCooperation) { personalityInfluence.extraversion = extraversionEffect * 10; score += personalityInfluence.extraversion; }
    
    const agreeablenessEffect = (agent.agreeableness - 50) / 50;
    if (option.requiresConflict) { personalityInfluence.agreeableness = -agreeablenessEffect * 15; }
    else if (option.requiresCooperation) { personalityInfluence.agreeableness = agreeablenessEffect * 10; }
    score += personalityInfluence.agreeableness;
    
    const neuroticismEffect = (agent.neuroticism - 50) / 50;
    personalityInfluence.neuroticism = -neuroticismEffect * option.riskLevel / 5;
    score += personalityInfluence.neuroticism;
    
    const emotionalInfluence: EmotionalInfluence = { mood: 0, stress: 0, trust: 0 };
    
    if (emotionalState) {
      const moodEffect = (emotionalState.overallMood - 50) / 50;
      emotionalInfluence.mood = moodEffect * option.potentialReward / 10;
      score += emotionalInfluence.mood;
      
      const stressEffect = emotionalState.stressLevel / 100;
      emotionalInfluence.stress = -stressEffect * option.riskLevel / 5;
      score += emotionalInfluence.stress;
      
      if (relationship) {
        const trustEffect = (relationship.trust - 50) / 50;
        emotionalInfluence.trust = trustEffect * 10;
        score += emotionalInfluence.trust;
      }
    }
    
    for (const memory of memories) {
      if (memory.type === "trauma" && option.riskLevel > 70) score -= 10;
      if (memory.type === "achievement" && option.potentialReward > 70) score += 5;
    }
    
    const riskToleranceEffect = (agent.riskTolerance - 50) / 50;
    score += riskToleranceEffect * option.riskLevel / 5;
    
    const impulsivenessEffect = (agent.impulsiveness - 50) / 50;
    score += impulsivenessEffect * option.potentialReward / 10;
    
    return { total: Math.max(0, Math.min(100, score)), personalityInfluence, emotionalInfluence };
  }
  
  private generateReasoning(agent: Agent, chosen: ScoredOption): string {
    const reasons: string[] = [];
    if (agent.conscientiousness > 60 && chosen.option.riskLevel < 50) reasons.push("As a careful planner, I prefer the more measured approach.");
    if (agent.openness > 60 && chosen.option.riskLevel > 50) reasons.push("I'm drawn to the innovative potential of this option.");
    if (agent.agreeableness > 60 && chosen.option.requiresCooperation) reasons.push("Working together seems like the best path forward.");
    if (agent.agreeableness < 40 && chosen.option.requiresConflict) reasons.push("Sometimes you have to be willing to fight for what you want.");
    if (agent.neuroticism > 60 && chosen.option.riskLevel < 40) reasons.push("I'd rather play it safe than risk everything.");
    if (chosen.score.emotionalInfluence.stress < -5) reasons.push("Given the current pressure, I need to be conservative.");
    if (chosen.score.emotionalInfluence.trust > 5) reasons.push("I trust this partner based on our history together.");
    if (chosen.score.emotionalInfluence.mood > 5) reasons.push("I'm feeling optimistic about the potential here.");
    if (reasons.length === 0) reasons.push(`After weighing all factors, "${chosen.option.description}" offers the best balance of risk and reward.`);
    return reasons.join(" ");
  }
  
  private calculateConfidence(scoredOptions: ScoredOption[]): number {
    if (scoredOptions.length < 2) return 100;
    const topScore = scoredOptions[0].score.total;
    const secondScore = scoredOptions[1].score.total;
    return Math.min(100, 50 + (topScore - secondScore) * 2);
  }
  
  async processDecisionOutcome(decisionId: number, outcome: "success" | "failure" | "neutral"): Promise<void> {
    const db = getDb();
    if (!db) return;
    
    const [decision] = await db.select().from(agentDecisions).where(eq(agentDecisions.id, decisionId)).limit(1);
    if (!decision) return;
    
    await db.update(agentDecisions).set({ outcome }).where(eq(agentDecisions.id, decisionId));
    
    if (outcome === "success") {
      await db.update(agentEmotionalStates).set({ happiness: 60, satisfaction: 65, overallMood: 60, stressLevel: 25 }).where(eq(agentEmotionalStates.agentId, decision.agentId));
    } else if (outcome === "failure") {
      await db.update(agentEmotionalStates).set({ happiness: 35, satisfaction: 30, stress: 60, overallMood: 35 }).where(eq(agentEmotionalStates.agentId, decision.agentId));
    }
    
    await db.insert(agentMemories).values({
      agentId: decision.agentId,
      type: outcome === "success" ? "achievement" : outcome === "failure" ? "trauma" : "experience",
      content: `Made a ${decision.decisionType} decision: ${decision.reasoning}. Outcome: ${outcome}`,
      emotionalImpact: outcome === "success" ? 30 : outcome === "failure" ? -30 : 0,
      importance: outcome === "failure" ? 70 : 50,
    });
  }
  
  async createAgent(data: { name: string; type: Agent["type"]; companyId?: number; bio?: string }): Promise<Agent> {
    const db = getDb();
    if (!db) throw new Error("Database not available");
    
    const personality = {
      openness: this.randomTrait(), conscientiousness: this.randomTrait(), extraversion: this.randomTrait(),
      agreeableness: this.randomTrait(), neuroticism: this.randomTrait(), impulsiveness: this.randomTrait(),
      riskTolerance: this.randomTrait(), empathy: this.randomTrait(), leadership: this.randomTrait(), independence: this.randomTrait(),
    };
    
    const [result] = await db.insert(agents).values({ name: data.name, type: data.type, companyId: data.companyId, bio: data.bio, ...personality }).$returningId();
    await db.insert(agentEmotionalStates).values({ agentId: result.id });
    
    const motivations = this.getDefaultMotivations(data.type);
    for (const m of motivations) {
      await db.insert(agentMotivations).values({ agentId: result.id, type: m.type!, description: m.description!, priority: m.priority });
    }
    
    const [createdAgent] = await db.select().from(agents).where(eq(agents.id, result.id)).limit(1);
    return createdAgent;
  }
  
  private randomTrait(): number {
    const u1 = Math.random(), u2 = Math.random();
    const normal = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    return Math.max(0, Math.min(100, Math.round(50 + normal * 15)));
  }
  
  private getDefaultMotivations(type: Agent["type"]): Partial<InsertAgentMotivation>[] {
    const motivations: Record<string, Partial<InsertAgentMotivation>[]> = {
      customer: [{ type: "short_term", description: "Find quality products at good prices", priority: 80 }, { type: "long_term", description: "Build relationships with reliable suppliers", priority: 60 }],
      supplier: [{ type: "short_term", description: "Maximize sales volume", priority: 80 }, { type: "long_term", description: "Establish market dominance", priority: 70 }, { type: "core_value", description: "Maintain product quality reputation", priority: 90 }],
      employee: [{ type: "short_term", description: "Perform well in current role", priority: 70 }, { type: "long_term", description: "Advance career and increase income", priority: 80 }, { type: "core_value", description: "Work-life balance", priority: 60 }],
      partner: [{ type: "short_term", description: "Successful joint ventures", priority: 75 }, { type: "long_term", description: "Build lasting business alliance", priority: 85 }],
      investor: [{ type: "short_term", description: "Positive quarterly returns", priority: 85 }, { type: "long_term", description: "Portfolio growth and diversification", priority: 90 }, { type: "core_value", description: "Risk-adjusted returns", priority: 80 }],
      competitor: [{ type: "short_term", description: "Gain market share", priority: 90 }, { type: "long_term", description: "Industry leadership", priority: 85 }, { type: "core_value", description: "Innovation and differentiation", priority: 75 }],
    };
    return motivations[type] || [];
  }
}

export const agentBrain = new AgentBrainService();
