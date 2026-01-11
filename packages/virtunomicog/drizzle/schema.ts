import { relations } from "drizzle-orm";
import {
  int,
  mysqlEnum,
  mysqlTable,
  text,
  timestamp,
  varchar,
  decimal,
  boolean,
  json,
} from "drizzle-orm/mysql-core";

// ============================================================================
// USER TABLE (existing, extended)
// ============================================================================
export const users = mysqlTable("users", {
  id: int("id").autoincrement().primaryKey(),
  openId: varchar("openId", { length: 64 }).notNull().unique(),
  name: text("name"),
  email: varchar("email", { length: 320 }),
  loginMethod: varchar("loginMethod", { length: 64 }),
  role: mysqlEnum("role", ["user", "admin"]).default("user").notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  lastSignedIn: timestamp("lastSignedIn").defaultNow().notNull(),
});

export type User = typeof users.$inferSelect;
export type InsertUser = typeof users.$inferInsert;

// ============================================================================
// COMPANIES - Player's business entity
// ============================================================================
export const companies = mysqlTable("companies", {
  id: int("id").autoincrement().primaryKey(),
  userId: int("userId").notNull(),
  name: varchar("name", { length: 128 }).notNull(),
  logo: varchar("logo", { length: 512 }),
  description: text("description"),
  cash: decimal("cash", { precision: 20, scale: 2 }).default("1000000.00").notNull(),
  reputation: int("reputation").default(50).notNull(),
  founded: timestamp("founded").defaultNow().notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type Company = typeof companies.$inferSelect;
export type InsertCompany = typeof companies.$inferInsert;

// ============================================================================
// RESOURCE TYPES - Definition of all resources in the game
// ============================================================================
export const resourceTypes = mysqlTable("resource_types", {
  id: int("id").autoincrement().primaryKey(),
  code: varchar("code", { length: 64 }).notNull().unique(),
  name: varchar("name", { length: 128 }).notNull(),
  category: mysqlEnum("category", [
    "raw_material",
    "intermediate",
    "finished_good",
    "equipment",
    "consumable",
  ]).notNull(),
  basePrice: decimal("basePrice", { precision: 12, scale: 2 }).notNull(),
  unit: varchar("unit", { length: 32 }).default("units").notNull(),
  icon: varchar("icon", { length: 128 }),
  description: text("description"),
});

export type ResourceType = typeof resourceTypes.$inferSelect;
export type InsertResourceType = typeof resourceTypes.$inferInsert;

// ============================================================================
// BUSINESS UNITS - Factories, stores, mines, farms, labs, offices
// ============================================================================
export const businessUnits = mysqlTable("business_units", {
  id: int("id").autoincrement().primaryKey(),
  companyId: int("companyId").notNull(),
  type: mysqlEnum("type", [
    "office",
    "store",
    "factory",
    "mine",
    "farm",
    "laboratory",
  ]).notNull(),
  name: varchar("name", { length: 128 }).notNull(),
  cityId: int("cityId").notNull(),
  level: int("level").default(1).notNull(),
  size: int("size").default(100).notNull(), // Square meters or capacity
  condition: int("condition").default(100).notNull(), // 0-100%
  efficiency: decimal("efficiency", { precision: 5, scale: 2 }).default("1.00").notNull(),
  isActive: boolean("isActive").default(true).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type BusinessUnit = typeof businessUnits.$inferSelect;
export type InsertBusinessUnit = typeof businessUnits.$inferInsert;

// ============================================================================
// CITIES - Locations where business units can be placed
// ============================================================================
export const cities = mysqlTable("cities", {
  id: int("id").autoincrement().primaryKey(),
  name: varchar("name", { length: 128 }).notNull(),
  country: varchar("country", { length: 64 }).notNull(),
  population: int("population").notNull(),
  wealthIndex: decimal("wealthIndex", { precision: 5, scale: 2 }).default("1.00").notNull(),
  taxRate: decimal("taxRate", { precision: 5, scale: 4 }).default("0.2000").notNull(),
  latitude: decimal("latitude", { precision: 10, scale: 7 }),
  longitude: decimal("longitude", { precision: 10, scale: 7 }),
});

export type City = typeof cities.$inferSelect;
export type InsertCity = typeof cities.$inferInsert;

// ============================================================================
// EMPLOYEES - Workers at business units
// ============================================================================
export const employees = mysqlTable("employees", {
  id: int("id").autoincrement().primaryKey(),
  businessUnitId: int("businessUnitId").notNull(),
  count: int("count").default(0).notNull(),
  qualification: decimal("qualification", { precision: 5, scale: 2 }).default("1.00").notNull(),
  salary: decimal("salary", { precision: 12, scale: 2 }).default("1000.00").notNull(),
  morale: int("morale").default(70).notNull(), // 0-100
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type Employee = typeof employees.$inferSelect;
export type InsertEmployee = typeof employees.$inferInsert;

// ============================================================================
// INVENTORY - Resources stored at business units
// ============================================================================
export const inventory = mysqlTable("inventory", {
  id: int("id").autoincrement().primaryKey(),
  businessUnitId: int("businessUnitId").notNull(),
  resourceTypeId: int("resourceTypeId").notNull(),
  quantity: decimal("quantity", { precision: 20, scale: 4 }).default("0").notNull(),
  quality: decimal("quality", { precision: 5, scale: 2 }).default("1.00").notNull(),
  averageCost: decimal("averageCost", { precision: 12, scale: 2 }).default("0").notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type Inventory = typeof inventory.$inferSelect;
export type InsertInventory = typeof inventory.$inferInsert;

// ============================================================================
// PRODUCTION RECIPES - What factories/farms can produce
// ============================================================================
export const productionRecipes = mysqlTable("production_recipes", {
  id: int("id").autoincrement().primaryKey(),
  unitType: mysqlEnum("unitType", ["factory", "farm", "mine", "laboratory"]).notNull(),
  outputResourceId: int("outputResourceId").notNull(),
  outputQuantity: decimal("outputQuantity", { precision: 12, scale: 4 }).default("1").notNull(),
  inputResources: json("inputResources").$type<{ resourceId: number; quantity: number }[]>(),
  laborRequired: int("laborRequired").default(1).notNull(),
  timeRequired: int("timeRequired").default(1).notNull(), // In game turns
  technologyRequired: int("technologyRequired").default(0),
  description: text("description"),
});

export type ProductionRecipe = typeof productionRecipes.$inferSelect;
export type InsertProductionRecipe = typeof productionRecipes.$inferInsert;

// ============================================================================
// PRODUCTION QUEUE - Active production at units
// ============================================================================
export const productionQueue = mysqlTable("production_queue", {
  id: int("id").autoincrement().primaryKey(),
  businessUnitId: int("businessUnitId").notNull(),
  recipeId: int("recipeId").notNull(),
  quantity: decimal("quantity", { precision: 12, scale: 4 }).notNull(),
  progress: decimal("progress", { precision: 5, scale: 2 }).default("0").notNull(),
  isActive: boolean("isActive").default(true).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type ProductionQueue = typeof productionQueue.$inferSelect;
export type InsertProductionQueue = typeof productionQueue.$inferInsert;

// ============================================================================
// MARKET LISTINGS - Items for sale on the market
// ============================================================================
export const marketListings = mysqlTable("market_listings", {
  id: int("id").autoincrement().primaryKey(),
  companyId: int("companyId").notNull(),
  businessUnitId: int("businessUnitId").notNull(),
  resourceTypeId: int("resourceTypeId").notNull(),
  quantity: decimal("quantity", { precision: 20, scale: 4 }).notNull(),
  quality: decimal("quality", { precision: 5, scale: 2 }).default("1.00").notNull(),
  pricePerUnit: decimal("pricePerUnit", { precision: 12, scale: 2 }).notNull(),
  cityId: int("cityId").notNull(),
  isActive: boolean("isActive").default(true).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  expiresAt: timestamp("expiresAt"),
});

export type MarketListing = typeof marketListings.$inferSelect;
export type InsertMarketListing = typeof marketListings.$inferInsert;

// ============================================================================
// TRANSACTIONS - Purchase/sale history
// ============================================================================
export const transactions = mysqlTable("transactions", {
  id: int("id").autoincrement().primaryKey(),
  type: mysqlEnum("type", [
    "purchase",
    "sale",
    "salary",
    "tax",
    "construction",
    "maintenance",
    "other",
  ]).notNull(),
  companyId: int("companyId").notNull(),
  amount: decimal("amount", { precision: 20, scale: 2 }).notNull(),
  description: text("description"),
  relatedUnitId: int("relatedUnitId"),
  relatedResourceId: int("relatedResourceId"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type Transaction = typeof transactions.$inferSelect;
export type InsertTransaction = typeof transactions.$inferInsert;

// ============================================================================
// TECHNOLOGIES - Research tree
// ============================================================================
export const technologies = mysqlTable("technologies", {
  id: int("id").autoincrement().primaryKey(),
  code: varchar("code", { length: 64 }).notNull().unique(),
  name: varchar("name", { length: 128 }).notNull(),
  category: mysqlEnum("category", [
    "production",
    "commerce",
    "management",
    "science",
  ]).notNull(),
  description: text("description"),
  researchCost: int("researchCost").notNull(),
  prerequisites: json("prerequisites").$type<number[]>(),
  effects: json("effects").$type<{ type: string; value: number }[]>(),
});

export type Technology = typeof technologies.$inferSelect;
export type InsertTechnology = typeof technologies.$inferInsert;

// ============================================================================
// COMPANY TECHNOLOGIES - Technologies researched by companies
// ============================================================================
export const companyTechnologies = mysqlTable("company_technologies", {
  id: int("id").autoincrement().primaryKey(),
  companyId: int("companyId").notNull(),
  technologyId: int("technologyId").notNull(),
  researchProgress: int("researchProgress").default(0).notNull(),
  isCompleted: boolean("isCompleted").default(false).notNull(),
  completedAt: timestamp("completedAt"),
});

export type CompanyTechnology = typeof companyTechnologies.$inferSelect;
export type InsertCompanyTechnology = typeof companyTechnologies.$inferInsert;

// ============================================================================
// GAME STATE - Global game settings and turn info
// ============================================================================
export const gameState = mysqlTable("game_state", {
  id: int("id").autoincrement().primaryKey(),
  currentTurn: int("currentTurn").default(1).notNull(),
  turnDuration: int("turnDuration").default(3600).notNull(), // Seconds
  lastTurnProcessed: timestamp("lastTurnProcessed"),
  settings: json("settings").$type<Record<string, unknown>>(),
});

export type GameState = typeof gameState.$inferSelect;
export type InsertGameState = typeof gameState.$inferInsert;

// ============================================================================
// NOTIFICATIONS - Player notifications
// ============================================================================
export const notifications = mysqlTable("notifications", {
  id: int("id").autoincrement().primaryKey(),
  userId: int("userId").notNull(),
  type: mysqlEnum("type", [
    "info",
    "success",
    "warning",
    "error",
    "trade",
    "production",
  ]).notNull(),
  title: varchar("title", { length: 256 }).notNull(),
  message: text("message"),
  isRead: boolean("isRead").default(false).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type Notification = typeof notifications.$inferSelect;
export type InsertNotification = typeof notifications.$inferInsert;

// ============================================================================
// RELATIONS
// ============================================================================
export const usersRelations = relations(users, ({ many }) => ({
  companies: many(companies),
  notifications: many(notifications),
}));

export const companiesRelations = relations(companies, ({ one, many }) => ({
  user: one(users, {
    fields: [companies.userId],
    references: [users.id],
  }),
  businessUnits: many(businessUnits),
  marketListings: many(marketListings),
  transactions: many(transactions),
  technologies: many(companyTechnologies),
}));

export const businessUnitsRelations = relations(businessUnits, ({ one, many }) => ({
  company: one(companies, {
    fields: [businessUnits.companyId],
    references: [companies.id],
  }),
  city: one(cities, {
    fields: [businessUnits.cityId],
    references: [cities.id],
  }),
  employees: one(employees),
  inventory: many(inventory),
  productionQueue: many(productionQueue),
}));

export const inventoryRelations = relations(inventory, ({ one }) => ({
  businessUnit: one(businessUnits, {
    fields: [inventory.businessUnitId],
    references: [businessUnits.id],
  }),
  resourceType: one(resourceTypes, {
    fields: [inventory.resourceTypeId],
    references: [resourceTypes.id],
  }),
}));


// ============================================================================
// DREAMCOG INTEGRATION - Advanced Agentic Simulation
// ============================================================================

// ============================================================================
// AGENTS - AI-driven entities with personality and behavior
// ============================================================================
export const agents = mysqlTable("agents", {
  id: int("id").autoincrement().primaryKey(),
  companyId: int("companyId"), // Optional: link to player company
  name: varchar("name", { length: 128 }).notNull(),
  type: mysqlEnum("type", [
    "customer",
    "supplier",
    "employee",
    "partner",
    "investor",
    "competitor",
  ]).notNull(),
  avatar: varchar("avatar", { length: 512 }),
  bio: text("bio"),
  // Big Five Personality Traits (0-100 scale)
  openness: int("openness").default(50).notNull(),
  conscientiousness: int("conscientiousness").default(50).notNull(),
  extraversion: int("extraversion").default(50).notNull(),
  agreeableness: int("agreeableness").default(50).notNull(),
  neuroticism: int("neuroticism").default(50).notNull(),
  // Behavioral tendencies
  impulsiveness: int("impulsiveness").default(50).notNull(),
  riskTolerance: int("riskTolerance").default(50).notNull(),
  empathy: int("empathy").default(50).notNull(),
  leadership: int("leadership").default(50).notNull(),
  independence: int("independence").default(50).notNull(),
  // Autonomy settings
  autonomyLevel: int("autonomyLevel").default(50).notNull(), // How much independent action
  isActive: boolean("isActive").default(true).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type Agent = typeof agents.$inferSelect;
export type InsertAgent = typeof agents.$inferInsert;

// ============================================================================
// AGENT EMOTIONAL STATES - Current emotional state of agents
// ============================================================================
export const agentEmotionalStates = mysqlTable("agent_emotional_states", {
  id: int("id").autoincrement().primaryKey(),
  agentId: int("agentId").notNull(),
  // Core emotions (0-100 scale)
  happiness: int("happiness").default(50).notNull(),
  satisfaction: int("satisfaction").default(50).notNull(),
  stress: int("stress").default(30).notNull(),
  anger: int("anger").default(10).notNull(),
  fear: int("fear").default(10).notNull(),
  trust: int("trust").default(50).notNull(),
  // Psychological needs (0-100 fulfillment)
  needFinancialSecurity: int("needFinancialSecurity").default(50).notNull(),
  needRecognition: int("needRecognition").default(50).notNull(),
  needAutonomy: int("needAutonomy").default(50).notNull(),
  needBelonging: int("needBelonging").default(50).notNull(),
  needGrowth: int("needGrowth").default(50).notNull(),
  // Overall state
  overallMood: int("overallMood").default(50).notNull(),
  stressLevel: int("stressLevel").default(30).notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type AgentEmotionalState = typeof agentEmotionalStates.$inferSelect;
export type InsertAgentEmotionalState = typeof agentEmotionalStates.$inferInsert;

// ============================================================================
// AGENT MOTIVATIONS - Goals and aspirations
// ============================================================================
export const agentMotivations = mysqlTable("agent_motivations", {
  id: int("id").autoincrement().primaryKey(),
  agentId: int("agentId").notNull(),
  type: mysqlEnum("type", [
    "short_term",
    "long_term",
    "core_value",
  ]).notNull(),
  description: text("description").notNull(),
  priority: int("priority").default(50).notNull(), // 1-100
  progress: int("progress").default(0).notNull(), // 0-100
  isActive: boolean("isActive").default(true).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  completedAt: timestamp("completedAt"),
});

export type AgentMotivation = typeof agentMotivations.$inferSelect;
export type InsertAgentMotivation = typeof agentMotivations.$inferInsert;

// ============================================================================
// AGENT MEMORIES - Experience tracking
// ============================================================================
export const agentMemories = mysqlTable("agent_memories", {
  id: int("id").autoincrement().primaryKey(),
  agentId: int("agentId").notNull(),
  type: mysqlEnum("type", [
    "experience",
    "knowledge",
    "trauma",
    "achievement",
    "relationship",
  ]).notNull(),
  content: text("content").notNull(),
  emotionalImpact: int("emotionalImpact").default(0).notNull(), // -100 to 100
  importance: int("importance").default(50).notNull(), // 1-100
  relatedAgentId: int("relatedAgentId"),
  relatedCompanyId: int("relatedCompanyId"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  expiresAt: timestamp("expiresAt"), // Some memories fade
});

export type AgentMemory = typeof agentMemories.$inferSelect;
export type InsertAgentMemory = typeof agentMemories.$inferInsert;

// ============================================================================
// AGENT RELATIONSHIPS - Connections between agents
// ============================================================================
export const agentRelationships = mysqlTable("agent_relationships", {
  id: int("id").autoincrement().primaryKey(),
  agentId1: int("agentId1").notNull(),
  agentId2: int("agentId2").notNull(),
  type: mysqlEnum("type", [
    "business",
    "personal",
    "professional",
    "familial",
    "competitive",
    "mentor",
    "ally",
    "neutral",
    "hostile",
  ]).notNull(),
  // Relationship metrics (0-100)
  trust: int("trust").default(50).notNull(),
  affection: int("affection").default(50).notNull(),
  respect: int("respect").default(50).notNull(),
  loyalty: int("loyalty").default(50).notNull(),
  dependency: int("dependency").default(0).notNull(),
  tension: int("tension").default(0).notNull(),
  // History
  interactionCount: int("interactionCount").default(0).notNull(),
  lastInteraction: timestamp("lastInteraction"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type AgentRelationship = typeof agentRelationships.$inferSelect;
export type InsertAgentRelationship = typeof agentRelationships.$inferInsert;

// ============================================================================
// WORLDS - Narrative world settings
// ============================================================================
export const worlds = mysqlTable("worlds", {
  id: int("id").autoincrement().primaryKey(),
  name: varchar("name", { length: 128 }).notNull(),
  genre: mysqlEnum("genre", [
    "modern",
    "historical",
    "futuristic",
    "fantasy",
    "steampunk",
    "cyberpunk",
  ]).default("modern").notNull(),
  description: text("description"),
  timePeriod: varchar("timePeriod", { length: 64 }),
  technologyLevel: int("technologyLevel").default(50).notNull(), // 1-100
  economicSystem: mysqlEnum("economicSystem", [
    "capitalism",
    "socialism",
    "mixed",
    "feudal",
    "post_scarcity",
  ]).default("capitalism").notNull(),
  worldRules: json("worldRules").$type<Record<string, unknown>>(),
  isActive: boolean("isActive").default(true).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type World = typeof worlds.$inferSelect;
export type InsertWorld = typeof worlds.$inferInsert;

// ============================================================================
// WORLD EVENTS - Narrative events that affect the game world
// ============================================================================
export const worldEvents = mysqlTable("world_events", {
  id: int("id").autoincrement().primaryKey(),
  worldId: int("worldId").notNull(),
  type: mysqlEnum("type", [
    "economic",
    "political",
    "natural",
    "technological",
    "social",
    "conflict",
    "discovery",
  ]).notNull(),
  name: varchar("name", { length: 256 }).notNull(),
  description: text("description"),
  importance: int("importance").default(50).notNull(), // 1-100
  // Effects on game mechanics
  effects: json("effects").$type<{
    marketPriceModifier?: Record<string, number>;
    taxRateModifier?: number;
    demandModifier?: Record<string, number>;
    supplyModifier?: Record<string, number>;
    reputationModifier?: number;
  }>(),
  // Affected areas
  affectedCityIds: json("affectedCityIds").$type<number[]>(),
  affectedResourceIds: json("affectedResourceIds").$type<number[]>(),
  // Timing
  startTurn: int("startTurn").notNull(),
  endTurn: int("endTurn"),
  isActive: boolean("isActive").default(true).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type WorldEvent = typeof worldEvents.$inferSelect;
export type InsertWorldEvent = typeof worldEvents.$inferInsert;

// ============================================================================
// SCHEDULED EVENTS - Future events to be triggered
// ============================================================================
export const scheduledEvents = mysqlTable("scheduled_events", {
  id: int("id").autoincrement().primaryKey(),
  worldId: int("worldId"),
  type: mysqlEnum("type", [
    "world_event",
    "agent_action",
    "market_change",
    "narrative_trigger",
  ]).notNull(),
  triggerType: mysqlEnum("triggerType", [
    "time_based",
    "condition_based",
    "recurring",
  ]).notNull(),
  triggerTurn: int("triggerTurn"),
  triggerCondition: json("triggerCondition").$type<{
    type: string;
    params: Record<string, unknown>;
  }>(),
  eventData: json("eventData").$type<Record<string, unknown>>().notNull(),
  priority: int("priority").default(50).notNull(),
  isProcessed: boolean("isProcessed").default(false).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type ScheduledEvent = typeof scheduledEvents.$inferSelect;
export type InsertScheduledEvent = typeof scheduledEvents.$inferInsert;

// ============================================================================
// AGENT DECISIONS - Log of agent decision-making with reasoning
// ============================================================================
export const agentDecisions = mysqlTable("agent_decisions", {
  id: int("id").autoincrement().primaryKey(),
  agentId: int("agentId").notNull(),
  decisionType: mysqlEnum("decisionType", [
    "trade",
    "negotiation",
    "investment",
    "hiring",
    "partnership",
    "conflict",
    "cooperation",
  ]).notNull(),
  context: json("context").$type<Record<string, unknown>>().notNull(),
  options: json("options").$type<{ id: string; description: string; score: number }[]>().notNull(),
  chosenOption: varchar("chosenOption", { length: 64 }).notNull(),
  reasoning: text("reasoning").notNull(),
  // Personality influence breakdown
  personalityFactors: json("personalityFactors").$type<{
    openness: number;
    conscientiousness: number;
    extraversion: number;
    agreeableness: number;
    neuroticism: number;
  }>(),
  emotionalFactors: json("emotionalFactors").$type<{
    mood: number;
    stress: number;
    trust: number;
  }>(),
  outcome: mysqlEnum("outcome", ["pending", "success", "failure", "neutral"]).default("pending").notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type AgentDecision = typeof agentDecisions.$inferSelect;
export type InsertAgentDecision = typeof agentDecisions.$inferInsert;

// ============================================================================
// EVENT PROPAGATION - Cross-system event triggers
// ============================================================================
export const eventPropagation = mysqlTable("event_propagation", {
  id: int("id").autoincrement().primaryKey(),
  sourceType: mysqlEnum("sourceType", ["business", "narrative", "agent"]).notNull(),
  sourceEventId: int("sourceEventId").notNull(),
  sourceEventType: varchar("sourceEventType", { length: 64 }).notNull(),
  targetType: mysqlEnum("targetType", ["business", "narrative", "agent"]).notNull(),
  targetEventType: varchar("targetEventType", { length: 64 }).notNull(),
  propagationData: json("propagationData").$type<Record<string, unknown>>(),
  isProcessed: boolean("isProcessed").default(false).notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  processedAt: timestamp("processedAt"),
});

export type EventPropagation = typeof eventPropagation.$inferSelect;
export type InsertEventPropagation = typeof eventPropagation.$inferInsert;

// ============================================================================
// LORE ENTRIES - World knowledge database
// ============================================================================
export const loreEntries = mysqlTable("lore_entries", {
  id: int("id").autoincrement().primaryKey(),
  worldId: int("worldId").notNull(),
  category: mysqlEnum("category", [
    "history",
    "legend",
    "culture",
    "religion",
    "politics",
    "science",
    "economics",
    "geography",
  ]).notNull(),
  title: varchar("title", { length: 256 }).notNull(),
  content: text("content").notNull(),
  isPublic: boolean("isPublic").default(true).notNull(),
  tags: json("tags").$type<string[]>(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type LoreEntry = typeof loreEntries.$inferSelect;
export type InsertLoreEntry = typeof loreEntries.$inferInsert;

// ============================================================================
// DREAMCOG RELATIONS
// ============================================================================
export const agentsRelations = relations(agents, ({ one, many }) => ({
  company: one(companies, {
    fields: [agents.companyId],
    references: [companies.id],
  }),
  emotionalState: one(agentEmotionalStates),
  motivations: many(agentMotivations),
  memories: many(agentMemories),
  decisions: many(agentDecisions),
}));

export const agentEmotionalStatesRelations = relations(agentEmotionalStates, ({ one }) => ({
  agent: one(agents, {
    fields: [agentEmotionalStates.agentId],
    references: [agents.id],
  }),
}));

export const agentMotivationsRelations = relations(agentMotivations, ({ one }) => ({
  agent: one(agents, {
    fields: [agentMotivations.agentId],
    references: [agents.id],
  }),
}));

export const agentMemoriesRelations = relations(agentMemories, ({ one }) => ({
  agent: one(agents, {
    fields: [agentMemories.agentId],
    references: [agents.id],
  }),
  relatedAgent: one(agents, {
    fields: [agentMemories.relatedAgentId],
    references: [agents.id],
  }),
}));

export const worldEventsRelations = relations(worldEvents, ({ one }) => ({
  world: one(worlds, {
    fields: [worldEvents.worldId],
    references: [worlds.id],
  }),
}));

export const loreEntriesRelations = relations(loreEntries, ({ one }) => ({
  world: one(worlds, {
    fields: [loreEntries.worldId],
    references: [worlds.id],
  }),
}));

// ============================================================================
// PHASE 9: ADVANCED BUSINESS MECHANICS
// ============================================================================

// ============================================================================
// SUPPLY ROUTES - Logistics routes between cities
// ============================================================================
export const supplyRoutes = mysqlTable("supply_routes", {
  id: int("id").autoincrement().primaryKey(),
  fromCityId: int("fromCityId").notNull(),
  toCityId: int("toCityId").notNull(),
  distance: decimal("distance", { precision: 10, scale: 2 }).notNull(), // km
  baseShippingCost: decimal("baseShippingCost", { precision: 12, scale: 2 }).notNull(), // per unit weight
  transitTime: int("transitTime").default(1).notNull(), // in game turns
  routeType: mysqlEnum("routeType", [
    "road",
    "rail",
    "sea",
    "air",
  ]).default("road").notNull(),
  reliability: decimal("reliability", { precision: 5, scale: 2 }).default("0.95").notNull(), // 0-1
  maxCapacityPerTurn: decimal("maxCapacityPerTurn", { precision: 12, scale: 2 }).default("10000").notNull(),
  isActive: boolean("isActive").default(true).notNull(),
});

export type SupplyRoute = typeof supplyRoutes.$inferSelect;
export type InsertSupplyRoute = typeof supplyRoutes.$inferInsert;

// ============================================================================
// SHIPMENTS - Goods in transit between business units
// ============================================================================
export const shipments = mysqlTable("shipments", {
  id: int("id").autoincrement().primaryKey(),
  companyId: int("companyId").notNull(),
  fromUnitId: int("fromUnitId").notNull(),
  toUnitId: int("toUnitId").notNull(),
  routeId: int("routeId").notNull(),
  resourceTypeId: int("resourceTypeId").notNull(),
  quantity: decimal("quantity", { precision: 20, scale: 4 }).notNull(),
  quality: decimal("quality", { precision: 5, scale: 2 }).default("1.00").notNull(),
  shippingCost: decimal("shippingCost", { precision: 12, scale: 2 }).notNull(),
  status: mysqlEnum("status", [
    "pending",
    "in_transit",
    "delivered",
    "delayed",
    "lost",
  ]).default("pending").notNull(),
  departureTurn: int("departureTurn").notNull(),
  expectedArrivalTurn: int("expectedArrivalTurn").notNull(),
  actualArrivalTurn: int("actualArrivalTurn"),
  contractId: int("contractId"), // Optional: linked to a contract
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type Shipment = typeof shipments.$inferSelect;
export type InsertShipment = typeof shipments.$inferInsert;

// ============================================================================
// WAREHOUSES - Dedicated storage facilities with special features
// ============================================================================
export const warehouses = mysqlTable("warehouses", {
  id: int("id").autoincrement().primaryKey(),
  businessUnitId: int("businessUnitId").notNull().unique(),
  storageCapacity: decimal("storageCapacity", { precision: 12, scale: 2 }).default("1000").notNull(),
  usedCapacity: decimal("usedCapacity", { precision: 12, scale: 2 }).default("0").notNull(),
  temperatureControlled: boolean("temperatureControlled").default(false).notNull(),
  securityLevel: int("securityLevel").default(1).notNull(), // 1-5
  handlingEfficiency: decimal("handlingEfficiency", { precision: 5, scale: 2 }).default("1.00").notNull(),
  operatingCostPerTurn: decimal("operatingCostPerTurn", { precision: 12, scale: 2 }).default("500").notNull(),
  preservationBonus: decimal("preservationBonus", { precision: 5, scale: 2 }).default("0.00").notNull(), // Quality loss reduction
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type Warehouse = typeof warehouses.$inferSelect;
export type InsertWarehouse = typeof warehouses.$inferInsert;

// ============================================================================
// CONTRACTS - Long-term supply agreements between players
// ============================================================================
export const contracts = mysqlTable("contracts", {
  id: int("id").autoincrement().primaryKey(),
  sellerCompanyId: int("sellerCompanyId").notNull(),
  buyerCompanyId: int("buyerCompanyId").notNull(),
  title: varchar("title", { length: 256 }).notNull(),
  description: text("description"),
  status: mysqlEnum("status", [
    "draft",
    "proposed",
    "negotiating",
    "active",
    "completed",
    "cancelled",
    "breached",
  ]).default("draft").notNull(),
  startTurn: int("startTurn").notNull(),
  endTurn: int("endTurn").notNull(),
  deliveryFrequency: mysqlEnum("deliveryFrequency", [
    "per_turn",
    "weekly",
    "monthly",
    "quarterly",
    "one_time",
  ]).default("per_turn").notNull(),
  // Penalty terms
  latePenaltyPercent: decimal("latePenaltyPercent", { precision: 5, scale: 2 }).default("5.00").notNull(),
  qualityPenaltyPercent: decimal("qualityPenaltyPercent", { precision: 5, scale: 2 }).default("10.00").notNull(),
  breachPenalty: decimal("breachPenalty", { precision: 20, scale: 2 }).default("0").notNull(),
  // Totals
  totalValue: decimal("totalValue", { precision: 20, scale: 2 }).default("0").notNull(),
  totalDelivered: decimal("totalDelivered", { precision: 20, scale: 2 }).default("0").notNull(),
  // Metadata
  proposedBy: int("proposedBy").notNull(), // Company that initiated
  signedAt: timestamp("signedAt"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type Contract = typeof contracts.$inferSelect;
export type InsertContract = typeof contracts.$inferInsert;

// ============================================================================
// CONTRACT ITEMS - Specific goods and terms within a contract
// ============================================================================
export const contractItems = mysqlTable("contract_items", {
  id: int("id").autoincrement().primaryKey(),
  contractId: int("contractId").notNull(),
  resourceTypeId: int("resourceTypeId").notNull(),
  quantityPerDelivery: decimal("quantityPerDelivery", { precision: 20, scale: 4 }).notNull(),
  totalQuantity: decimal("totalQuantity", { precision: 20, scale: 4 }).notNull(),
  deliveredQuantity: decimal("deliveredQuantity", { precision: 20, scale: 4 }).default("0").notNull(),
  pricePerUnit: decimal("pricePerUnit", { precision: 12, scale: 2 }).notNull(),
  minQuality: decimal("minQuality", { precision: 5, scale: 2 }).default("0.80").notNull(),
  fromUnitId: int("fromUnitId"), // Seller's source unit
  toUnitId: int("toUnitId"), // Buyer's destination unit
});

export type ContractItem = typeof contractItems.$inferSelect;
export type InsertContractItem = typeof contractItems.$inferInsert;

// ============================================================================
// CONTRACT DELIVERIES - Record of actual deliveries against contracts
// ============================================================================
export const contractDeliveries = mysqlTable("contract_deliveries", {
  id: int("id").autoincrement().primaryKey(),
  contractId: int("contractId").notNull(),
  contractItemId: int("contractItemId").notNull(),
  shipmentId: int("shipmentId"),
  quantity: decimal("quantity", { precision: 20, scale: 4 }).notNull(),
  quality: decimal("quality", { precision: 5, scale: 2 }).notNull(),
  deliveryTurn: int("deliveryTurn").notNull(),
  status: mysqlEnum("status", [
    "pending",
    "delivered",
    "late",
    "rejected",
  ]).default("pending").notNull(),
  penaltyApplied: decimal("penaltyApplied", { precision: 12, scale: 2 }).default("0").notNull(),
  notes: text("notes"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type ContractDelivery = typeof contractDeliveries.$inferSelect;
export type InsertContractDelivery = typeof contractDeliveries.$inferInsert;

// ============================================================================
// QUALITY INSPECTIONS - Quality control records
// ============================================================================
export const qualityInspections = mysqlTable("quality_inspections", {
  id: int("id").autoincrement().primaryKey(),
  businessUnitId: int("businessUnitId").notNull(),
  resourceTypeId: int("resourceTypeId").notNull(),
  batchSize: decimal("batchSize", { precision: 20, scale: 4 }).notNull(),
  sampleSize: decimal("sampleSize", { precision: 20, scale: 4 }).notNull(),
  measuredQuality: decimal("measuredQuality", { precision: 5, scale: 2 }).notNull(),
  passThreshold: decimal("passThreshold", { precision: 5, scale: 2 }).default("0.80").notNull(),
  passed: boolean("passed").notNull(),
  defectsFound: int("defectsFound").default(0).notNull(),
  defectTypes: json("defectTypes").$type<string[]>(),
  inspectorQualification: decimal("inspectorQualification", { precision: 5, scale: 2 }).default("1.00").notNull(),
  notes: text("notes"),
  turn: int("turn").notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type QualityInspection = typeof qualityInspections.$inferSelect;
export type InsertQualityInspection = typeof qualityInspections.$inferInsert;

// ============================================================================
// QUALITY STANDARDS - Company quality policies
// ============================================================================
export const qualityStandards = mysqlTable("quality_standards", {
  id: int("id").autoincrement().primaryKey(),
  companyId: int("companyId").notNull().unique(),
  inputQualityThreshold: decimal("inputQualityThreshold", { precision: 5, scale: 2 }).default("0.70").notNull(),
  outputQualityTarget: decimal("outputQualityTarget", { precision: 5, scale: 2 }).default("0.90").notNull(),
  inspectionFrequency: mysqlEnum("inspectionFrequency", [
    "none",
    "random",
    "periodic",
    "every_batch",
  ]).default("periodic").notNull(),
  inspectionRigor: int("inspectionRigor").default(50).notNull(), // 1-100, affects cost and accuracy
  qualityBonusEnabled: boolean("qualityBonusEnabled").default(true).notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type QualityStandard = typeof qualityStandards.$inferSelect;
export type InsertQualityStandard = typeof qualityStandards.$inferInsert;

// ============================================================================
// TURN PROCESSING LOG - Track game turn processing
// ============================================================================
export const turnProcessingLog = mysqlTable("turn_processing_log", {
  id: int("id").autoincrement().primaryKey(),
  turn: int("turn").notNull(),
  phase: mysqlEnum("phase", [
    "start",
    "production",
    "shipments",
    "contracts",
    "salaries",
    "maintenance",
    "taxes",
    "events",
    "complete",
  ]).notNull(),
  status: mysqlEnum("status", [
    "pending",
    "processing",
    "completed",
    "failed",
  ]).default("pending").notNull(),
  details: json("details").$type<Record<string, unknown>>(),
  error: text("error"),
  startedAt: timestamp("startedAt"),
  completedAt: timestamp("completedAt"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
});

export type TurnProcessingLog = typeof turnProcessingLog.$inferSelect;
export type InsertTurnProcessingLog = typeof turnProcessingLog.$inferInsert;

// ============================================================================
// PHASE 9 RELATIONS
// ============================================================================
export const supplyRoutesRelations = relations(supplyRoutes, ({ one }) => ({
  fromCity: one(cities, {
    fields: [supplyRoutes.fromCityId],
    references: [cities.id],
  }),
  toCity: one(cities, {
    fields: [supplyRoutes.toCityId],
    references: [cities.id],
  }),
}));

export const shipmentsRelations = relations(shipments, ({ one }) => ({
  company: one(companies, {
    fields: [shipments.companyId],
    references: [companies.id],
  }),
  fromUnit: one(businessUnits, {
    fields: [shipments.fromUnitId],
    references: [businessUnits.id],
  }),
  toUnit: one(businessUnits, {
    fields: [shipments.toUnitId],
    references: [businessUnits.id],
  }),
  route: one(supplyRoutes, {
    fields: [shipments.routeId],
    references: [supplyRoutes.id],
  }),
  resourceType: one(resourceTypes, {
    fields: [shipments.resourceTypeId],
    references: [resourceTypes.id],
  }),
  contract: one(contracts, {
    fields: [shipments.contractId],
    references: [contracts.id],
  }),
}));

export const warehousesRelations = relations(warehouses, ({ one }) => ({
  businessUnit: one(businessUnits, {
    fields: [warehouses.businessUnitId],
    references: [businessUnits.id],
  }),
}));

export const contractsRelations = relations(contracts, ({ one, many }) => ({
  seller: one(companies, {
    fields: [contracts.sellerCompanyId],
    references: [companies.id],
  }),
  buyer: one(companies, {
    fields: [contracts.buyerCompanyId],
    references: [companies.id],
  }),
  items: many(contractItems),
  deliveries: many(contractDeliveries),
}));

export const contractItemsRelations = relations(contractItems, ({ one }) => ({
  contract: one(contracts, {
    fields: [contractItems.contractId],
    references: [contracts.id],
  }),
  resourceType: one(resourceTypes, {
    fields: [contractItems.resourceTypeId],
    references: [resourceTypes.id],
  }),
}));

export const contractDeliveriesRelations = relations(contractDeliveries, ({ one }) => ({
  contract: one(contracts, {
    fields: [contractDeliveries.contractId],
    references: [contracts.id],
  }),
  contractItem: one(contractItems, {
    fields: [contractDeliveries.contractItemId],
    references: [contractItems.id],
  }),
  shipment: one(shipments, {
    fields: [contractDeliveries.shipmentId],
    references: [shipments.id],
  }),
}));

export const qualityInspectionsRelations = relations(qualityInspections, ({ one }) => ({
  businessUnit: one(businessUnits, {
    fields: [qualityInspections.businessUnitId],
    references: [businessUnits.id],
  }),
  resourceType: one(resourceTypes, {
    fields: [qualityInspections.resourceTypeId],
    references: [resourceTypes.id],
  }),
}));

export const qualityStandardsRelations = relations(qualityStandards, ({ one }) => ({
  company: one(companies, {
    fields: [qualityStandards.companyId],
    references: [companies.id],
  }),
}));
