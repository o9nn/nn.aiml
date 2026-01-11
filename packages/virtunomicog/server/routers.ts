import { COOKIE_NAME } from "@shared/const";
import { z } from "zod";
import { getSessionCookieOptions } from "./_core/cookies";
import { systemRouter } from "./_core/systemRouter";
import { publicProcedure, protectedProcedure, router } from "./_core/trpc";
import {
  createCompany,
  getCompanyByUserId,
  getCompanyById,
  getAllCompanies,
  updateCompanyCash,
  createBusinessUnit,
  getBusinessUnitsByCompany,
  getBusinessUnitById,
  updateBusinessUnit,
  getAllCities,
  getCityById,
  getAllResourceTypes,
  getResourceTypeById,
  createEmployees,
  getEmployeesByUnit,
  updateEmployees,
  getInventoryByUnit,
  upsertInventory,
  createMarketListing,
  getMarketListings,
  createTransaction,
  getTransactionsByCompany,
  createNotification,
  getNotificationsByUser,
  markNotificationRead,
  getGameState,
  initializeGameData,
  getProductionRecipes,
  getProductionRecipeById,
  addToProductionQueue,
  getProductionQueue,
  purchaseFromMarket,
  getMarketListingById,
  cancelMarketListing,
  seedProductionRecipes,
} from "./db";

// Initialize game data on server start
initializeGameData().then(() => seedProductionRecipes()).catch(console.error);

export const appRouter = router({
  system: systemRouter,
  
  auth: router({
    me: publicProcedure.query((opts) => opts.ctx.user),
    logout: publicProcedure.mutation(({ ctx }) => {
      const cookieOptions = getSessionCookieOptions(ctx.req);
      ctx.res.clearCookie(COOKIE_NAME, { ...cookieOptions, maxAge: -1 });
      return { success: true } as const;
    }),
  }),

  // ============================================================================
  // COMPANY ROUTES
  // ============================================================================
  company: router({
    // Get current user's company
    mine: protectedProcedure.query(async ({ ctx }) => {
      return await getCompanyByUserId(ctx.user.id);
    }),

    // Create a new company
    create: protectedProcedure
      .input(
        z.object({
          name: z.string().min(2).max(128),
          description: z.string().optional(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        // Check if user already has a company
        const existing = await getCompanyByUserId(ctx.user.id);
        if (existing) {
          throw new Error("You already have a company");
        }

        const company = await createCompany({
          userId: ctx.user.id,
          name: input.name,
          description: input.description,
          cash: "1000000.00", // Starting capital
        });

        if (company) {
          // Create notification
          await createNotification({
            userId: ctx.user.id,
            type: "success",
            title: "Company Founded!",
            message: `Congratulations! ${input.name} has been established with $1,000,000 starting capital.`,
          });
        }

        return company;
      }),

    // Get company by ID
    byId: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        return await getCompanyById(input.id);
      }),

    // Get all companies (for leaderboard)
    all: protectedProcedure.query(async () => {
      return await getAllCompanies();
    }),
  }),

  // ============================================================================
  // BUSINESS UNIT ROUTES
  // ============================================================================
  businessUnit: router({
    // Get all units for current company
    list: protectedProcedure.query(async ({ ctx }) => {
      const company = await getCompanyByUserId(ctx.user.id);
      if (!company) return [];
      return await getBusinessUnitsByCompany(company.id);
    }),

    // Get single unit by ID
    byId: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        return await getBusinessUnitById(input.id);
      }),

    // Create a new business unit
    create: protectedProcedure
      .input(
        z.object({
          type: z.enum(["office", "store", "factory", "mine", "farm", "laboratory"]),
          name: z.string().min(2).max(128),
          cityId: z.number(),
          size: z.number().min(50).max(10000).optional(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) {
          throw new Error("You need to create a company first");
        }

        // Calculate construction cost based on type and size
        const baseCosts: Record<string, number> = {
          office: 50000,
          store: 100000,
          factory: 500000,
          mine: 1000000,
          farm: 200000,
          laboratory: 750000,
        };

        const size = input.size || 100;
        const cost = baseCosts[input.type] * (size / 100);

        if (parseFloat(company.cash) < cost) {
          throw new Error(`Insufficient funds. Need $${cost.toLocaleString()}`);
        }

        // Deduct cost
        const newCash = (parseFloat(company.cash) - cost).toFixed(2);
        await updateCompanyCash(company.id, newCash);

        // Create the unit
        const unit = await createBusinessUnit({
          companyId: company.id,
          type: input.type,
          name: input.name,
          cityId: input.cityId,
          size: size,
        });

        if (unit) {
          // Create initial employees record
          await createEmployees({
            businessUnitId: unit.id,
            count: 0,
            salary: "1000.00",
          });

          // Record transaction
          await createTransaction({
            type: "construction",
            companyId: company.id,
            amount: (-cost).toFixed(2),
            description: `Built ${input.type}: ${input.name}`,
            relatedUnitId: unit.id,
          });

          // Notification
          await createNotification({
            userId: ctx.user.id,
            type: "success",
            title: "Construction Complete",
            message: `${input.name} (${input.type}) has been built in the selected city.`,
          });
        }

        return unit;
      }),

    // Update business unit
    update: protectedProcedure
      .input(
        z.object({
          id: z.number(),
          name: z.string().min(2).max(128).optional(),
          isActive: z.boolean().optional(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const unit = await getBusinessUnitById(input.id);
        if (!unit) throw new Error("Unit not found");

        const company = await getCompanyByUserId(ctx.user.id);
        if (!company || unit.companyId !== company.id) {
          throw new Error("Not authorized");
        }

        await updateBusinessUnit(input.id, {
          name: input.name,
          isActive: input.isActive,
        });

        return await getBusinessUnitById(input.id);
      }),

    // Get employees for a unit
    employees: protectedProcedure
      .input(z.object({ unitId: z.number() }))
      .query(async ({ input }) => {
        return await getEmployeesByUnit(input.unitId);
      }),

    // Hire/update employees
    updateEmployees: protectedProcedure
      .input(
        z.object({
          unitId: z.number(),
          count: z.number().min(0),
          salary: z.number().min(100),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const unit = await getBusinessUnitById(input.unitId);
        if (!unit) throw new Error("Unit not found");

        const company = await getCompanyByUserId(ctx.user.id);
        if (!company || unit.companyId !== company.id) {
          throw new Error("Not authorized");
        }

        await updateEmployees(input.unitId, {
          count: input.count,
          salary: input.salary.toFixed(2),
        });

        return await getEmployeesByUnit(input.unitId);
      }),

    // Get inventory for a unit
    inventory: protectedProcedure
      .input(z.object({ unitId: z.number() }))
      .query(async ({ input }) => {
        return await getInventoryByUnit(input.unitId);
      }),
  }),

  // ============================================================================
  // CITY ROUTES
  // ============================================================================
  city: router({
    list: protectedProcedure.query(async () => {
      return await getAllCities();
    }),

    byId: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        return await getCityById(input.id);
      }),
  }),

  // ============================================================================
  // RESOURCE ROUTES
  // ============================================================================
  resource: router({
    list: protectedProcedure.query(async () => {
      return await getAllResourceTypes();
    }),

    byId: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        return await getResourceTypeById(input.id);
      }),
  }),

  // ============================================================================
  // MARKET ROUTES
  // ============================================================================
  market: router({
    listings: protectedProcedure
      .input(
        z.object({
          resourceTypeId: z.number().optional(),
          cityId: z.number().optional(),
        }).optional()
      )
      .query(async ({ input }) => {
        return await getMarketListings(input?.resourceTypeId, input?.cityId);
      }),

    createListing: protectedProcedure
      .input(
        z.object({
          businessUnitId: z.number(),
          resourceTypeId: z.number(),
          quantity: z.number().positive(),
          pricePerUnit: z.number().positive(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const unit = await getBusinessUnitById(input.businessUnitId);
        if (!unit || unit.companyId !== company.id) {
          throw new Error("Not authorized");
        }

        await createMarketListing({
          companyId: company.id,
          businessUnitId: input.businessUnitId,
          resourceTypeId: input.resourceTypeId,
          quantity: input.quantity.toFixed(4),
          pricePerUnit: input.pricePerUnit.toFixed(2),
          cityId: unit.cityId,
        });

        return { success: true };
      }),

    // Get a specific listing
    byId: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        return await getMarketListingById(input.id);
      }),

    // Purchase from market
    purchase: protectedProcedure
      .input(
        z.object({
          listingId: z.number(),
          quantity: z.number().positive(),
          destinationUnitId: z.number(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        // Verify the destination unit belongs to the buyer
        const unit = await getBusinessUnitById(input.destinationUnitId);
        if (!unit || unit.companyId !== company.id) {
          throw new Error("Invalid destination unit");
        }

        const result = await purchaseFromMarket(
          company.id,
          input.listingId,
          input.quantity,
          input.destinationUnitId
        );

        if (!result.success) {
          throw new Error(result.message);
        }

        return result;
      }),

    // Cancel a listing
    cancel: protectedProcedure
      .input(z.object({ listingId: z.number() }))
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const result = await cancelMarketListing(input.listingId, company.id);
        if (!result.success) {
          throw new Error(result.message);
        }

        return result;
      }),
  }),

  // ============================================================================
  // PRODUCTION ROUTES
  // ============================================================================
  production: router({
    // Get available recipes for a unit type
    recipes: protectedProcedure
      .input(
        z.object({
          unitType: z.enum(["factory", "farm", "mine", "laboratory"]).optional(),
        }).optional()
      )
      .query(async ({ input }) => {
        return await getProductionRecipes(input?.unitType);
      }),

    // Get a specific recipe
    recipeById: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        return await getProductionRecipeById(input.id);
      }),

    // Get production queue for a unit
    queue: protectedProcedure
      .input(z.object({ unitId: z.number() }))
      .query(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) return [];

        const unit = await getBusinessUnitById(input.unitId);
        if (!unit || unit.companyId !== company.id) {
          throw new Error("Not authorized");
        }

        return await getProductionQueue(input.unitId);
      }),

    // Start production
    start: protectedProcedure
      .input(
        z.object({
          unitId: z.number(),
          recipeId: z.number(),
          quantity: z.number().positive(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const unit = await getBusinessUnitById(input.unitId);
        if (!unit || unit.companyId !== company.id) {
          throw new Error("Not authorized");
        }

        // Verify recipe is valid for this unit type
        const recipe = await getProductionRecipeById(input.recipeId);
        if (!recipe || recipe.unitType !== unit.type) {
          throw new Error("Invalid recipe for this unit type");
        }

        // Add to production queue
        await addToProductionQueue({
          businessUnitId: input.unitId,
          recipeId: input.recipeId,
          quantity: input.quantity,
        });

        await createNotification({
          userId: ctx.user.id,
          type: "production",
          title: "Production Started",
          message: `Started producing ${input.quantity} units at ${unit.name}`,
        });

        return { success: true };
      }),
  }),

  // ============================================================================
  // TRANSACTION ROUTES
  // ============================================================================
  transaction: router({
    list: protectedProcedure
      .input(z.object({ limit: z.number().min(1).max(100).optional() }).optional())
      .query(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) return [];
        return await getTransactionsByCompany(company.id, input?.limit || 50);
      }),
  }),

  // ============================================================================
  // NOTIFICATION ROUTES
  // ============================================================================
  notification: router({
    list: protectedProcedure.query(async ({ ctx }) => {
      return await getNotificationsByUser(ctx.user.id);
    }),

    markRead: protectedProcedure
      .input(z.object({ id: z.number() }))
      .mutation(async ({ input }) => {
        await markNotificationRead(input.id);
        return { success: true };
      }),
  }),

   // ============================================================================
  // GAME STATE ROUTES
  // ============================================================================
  game: router({
    state: protectedProcedure.query(async () => {
      return await getGameState();
    }),
  }),

  // ============================================================================
  // AGENT ROUTES (DreamCog Integration)
  // ============================================================================
  agent: router({
    // Create a new agent
    create: protectedProcedure
      .input(
        z.object({
          name: z.string().min(2).max(128),
          type: z.enum(["customer", "supplier", "employee", "partner", "investor", "competitor"]),
          bio: z.string().optional(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const { agentBrain } = await import("./services/agentBrain");
        const company = await getCompanyByUserId(ctx.user.id);
        return await agentBrain.createAgent({
          name: input.name,
          type: input.type,
          companyId: company?.id,
          bio: input.bio,
        });
      }),

    // Get agent by ID
    get: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        const { drizzle } = await import("drizzle-orm/mysql2");
        const { agents, agentEmotionalStates, agentMotivations } = await import("../drizzle/schema");
        const { eq } = await import("drizzle-orm");
        const db = drizzle(process.env.DATABASE_URL!);
        
        const [agent] = await db.select().from(agents).where(eq(agents.id, input.id)).limit(1);
        if (!agent) return null;
        
        const [emotional] = await db.select().from(agentEmotionalStates).where(eq(agentEmotionalStates.agentId, input.id)).limit(1);
        const motivations = await db.select().from(agentMotivations).where(eq(agentMotivations.agentId, input.id));
        
        return { ...agent, emotionalState: emotional, motivations };
      }),

    // List agents for company
    list: protectedProcedure.query(async ({ ctx }) => {
      const { drizzle } = await import("drizzle-orm/mysql2");
      const { agents } = await import("../drizzle/schema");
      const { eq, isNull, or } = await import("drizzle-orm");
      const db = drizzle(process.env.DATABASE_URL!);
      
      const company = await getCompanyByUserId(ctx.user.id);
      if (!company) {
        return db.select().from(agents).where(isNull(agents.companyId));
      }
      return db.select().from(agents).where(
        or(eq(agents.companyId, company.id), isNull(agents.companyId))
      );
    }),

    // Make agent decision
    decide: protectedProcedure
      .input(
        z.object({
          agentId: z.number(),
          context: z.object({
            type: z.enum(["trade", "negotiation", "investment", "hiring", "partnership", "conflict", "cooperation"]),
            situation: z.string(),
            options: z.array(
              z.object({
                id: z.string(),
                description: z.string(),
                expectedOutcome: z.string(),
                riskLevel: z.number().min(0).max(100),
                potentialReward: z.number().min(0).max(100),
                requiresCooperation: z.boolean(),
                requiresConflict: z.boolean(),
              })
            ),
            relatedAgentId: z.number().optional(),
            relatedCompanyId: z.number().optional(),
          }),
        })
      )
      .mutation(async ({ input }) => {
        const { agentBrain } = await import("./services/agentBrain");
        return await agentBrain.makeDecision(input.agentId, input.context);
      }),

    // Get agent decisions history
    decisions: protectedProcedure
      .input(z.object({ agentId: z.number(), limit: z.number().min(1).max(50).optional() }))
      .query(async ({ input }) => {
        const { drizzle } = await import("drizzle-orm/mysql2");
        const { agentDecisions } = await import("../drizzle/schema");
        const { eq, desc } = await import("drizzle-orm");
        const db = drizzle(process.env.DATABASE_URL!);
        
        return db.select()
          .from(agentDecisions)
          .where(eq(agentDecisions.agentId, input.agentId))
          .orderBy(desc(agentDecisions.createdAt))
          .limit(input.limit || 10);
      }),
  }),

  // ============================================================================
  // WORLD ROUTES (DreamCog Integration)
  // ============================================================================
  world: router({
    // Create a new world
    create: protectedProcedure
      .input(
        z.object({
          name: z.string().min(2).max(128),
          genre: z.enum(["modern", "historical", "futuristic", "fantasy", "steampunk", "cyberpunk"]).optional(),
          description: z.string().optional(),
          timePeriod: z.string().optional(),
          technologyLevel: z.number().min(1).max(100).optional(),
          economicSystem: z.enum(["capitalism", "socialism", "mixed", "feudal", "post_scarcity"]).optional(),
        })
      )
      .mutation(async ({ input }) => {
        const { worldEconomy } = await import("./services/worldEconomy");
        return await worldEconomy.createWorld(input);
      }),

    // Get world state
    state: protectedProcedure
      .input(z.object({ worldId: z.number() }))
      .query(async ({ input }) => {
        const { worldEconomy } = await import("./services/worldEconomy");
        return await worldEconomy.getWorldState(input.worldId);
      }),

    // Get world lore
    lore: protectedProcedure
      .input(z.object({ worldId: z.number(), category: z.string().optional() }))
      .query(async ({ input }) => {
        const { worldEconomy } = await import("./services/worldEconomy");
        return await worldEconomy.getWorldLore(input.worldId, input.category);
      }),

    // Get location effects
    locationEffects: protectedProcedure
      .input(z.object({ cityId: z.number(), worldId: z.number() }))
      .query(async ({ input }) => {
        const { worldEconomy } = await import("./services/worldEconomy");
        return await worldEconomy.getLocationEffects(input.cityId, input.worldId);
      }),

    // List all worlds
    list: protectedProcedure.query(async () => {
      const { drizzle } = await import("drizzle-orm/mysql2");
      const { worlds } = await import("../drizzle/schema");
      const { eq } = await import("drizzle-orm");
      const db = drizzle(process.env.DATABASE_URL!);
      
      return db.select().from(worlds).where(eq(worlds.isActive, true));
    }),

    // Get active world events
    events: protectedProcedure
      .input(z.object({ worldId: z.number() }))
      .query(async ({ input }) => {
        const { drizzle } = await import("drizzle-orm/mysql2");
        const { worldEvents } = await import("../drizzle/schema");
        const { eq, and } = await import("drizzle-orm");
        const db = drizzle(process.env.DATABASE_URL!);
        
        return db.select()
          .from(worldEvents)
          .where(and(
            eq(worldEvents.worldId, input.worldId),
            eq(worldEvents.isActive, true)
          ));
      }),
  }),

  // ============================================================================
  // SUPPLY CHAIN ROUTES (Phase 9)
  // ============================================================================
  supplyChain: router({
    // Get available routes
    routes: protectedProcedure
      .input(
        z.object({
          fromCityId: z.number().optional(),
          toCityId: z.number().optional(),
        }).optional()
      )
      .query(async ({ input }) => {
        const { supplyChainService } = await import("./services/phase9");
        return await supplyChainService.getRoutes(input?.fromCityId, input?.toCityId);
      }),

    // Calculate shipping cost
    calculateShipping: protectedProcedure
      .input(
        z.object({
          fromUnitId: z.number(),
          toUnitId: z.number(),
          quantity: z.number().positive(),
        })
      )
      .query(async ({ input }) => {
        const { supplyChainService } = await import("./services/phase9");
        return await supplyChainService.calculateShippingCost(
          input.fromUnitId,
          input.toUnitId,
          input.quantity
        );
      }),

    // Ship goods between units
    ship: protectedProcedure
      .input(
        z.object({
          fromUnitId: z.number(),
          toUnitId: z.number(),
          resourceTypeId: z.number(),
          quantity: z.number().positive(),
          contractId: z.number().optional(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const { supplyChainService } = await import("./services/phase9");
        const state = await getGameState();

        return await supplyChainService.shipGoods(
          company.id,
          input.fromUnitId,
          input.toUnitId,
          input.resourceTypeId,
          input.quantity,
          state?.currentTurn || 1,
          input.contractId
        );
      }),

    // Get company shipments
    shipments: protectedProcedure
      .input(
        z.object({
          status: z.enum(["pending", "in_transit", "delivered", "delayed", "lost"]).optional(),
        }).optional()
      )
      .query(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) return [];

        const { supplyChainService } = await import("./services/phase9");
        return await supplyChainService.getShipments(company.id, input?.status);
      }),

    // Get incoming shipments for a unit
    incoming: protectedProcedure
      .input(z.object({ unitId: z.number() }))
      .query(async ({ input }) => {
        const { supplyChainService } = await import("./services/phase9");
        return await supplyChainService.getIncomingShipments(input.unitId);
      }),

    // Get warehouse info
    warehouse: protectedProcedure
      .input(z.object({ unitId: z.number() }))
      .query(async ({ input }) => {
        const { supplyChainService } = await import("./services/phase9");
        return await supplyChainService.getWarehouse(input.unitId);
      }),
  }),

  // ============================================================================
  // CONTRACT ROUTES (Phase 9)
  // ============================================================================
  contracts: router({
    // Create a new contract
    create: protectedProcedure
      .input(
        z.object({
          buyerCompanyId: z.number(),
          title: z.string().min(2).max(256),
          description: z.string().optional(),
          startTurn: z.number(),
          endTurn: z.number(),
          deliveryFrequency: z.enum(["per_turn", "weekly", "monthly", "quarterly", "one_time"]).optional(),
          latePenaltyPercent: z.number().min(0).max(100).optional(),
          qualityPenaltyPercent: z.number().min(0).max(100).optional(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const { contractService } = await import("./services/phase9");
        return await contractService.createContract(company.id, input.buyerCompanyId, input);
      }),

    // Add item to contract
    addItem: protectedProcedure
      .input(
        z.object({
          contractId: z.number(),
          resourceTypeId: z.number(),
          quantityPerDelivery: z.number().positive(),
          totalQuantity: z.number().positive(),
          pricePerUnit: z.number().positive(),
          minQuality: z.number().min(0).max(1).optional(),
          fromUnitId: z.number().optional(),
          toUnitId: z.number().optional(),
        })
      )
      .mutation(async ({ input }) => {
        const { contractService } = await import("./services/phase9");
        return await contractService.addContractItem(input.contractId, {
          resourceTypeId: input.resourceTypeId,
          quantityPerDelivery: input.quantityPerDelivery.toFixed(4),
          totalQuantity: input.totalQuantity.toFixed(4),
          pricePerUnit: input.pricePerUnit.toFixed(2),
          minQuality: input.minQuality?.toFixed(2),
          fromUnitId: input.fromUnitId,
          toUnitId: input.toUnitId,
        });
      }),

    // Get contracts for current company
    list: protectedProcedure
      .input(
        z.object({
          status: z.enum(["draft", "proposed", "negotiating", "active", "completed", "cancelled", "breached"]).optional(),
        }).optional()
      )
      .query(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) return [];

        const { contractService } = await import("./services/phase9");
        return await contractService.getContracts(company.id, input?.status);
      }),

    // Get contract with items
    byId: protectedProcedure
      .input(z.object({ id: z.number() }))
      .query(async ({ input }) => {
        const { contractService } = await import("./services/phase9");
        return await contractService.getContractWithItems(input.id);
      }),

    // Propose a contract
    propose: protectedProcedure
      .input(z.object({ contractId: z.number() }))
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const { contractService } = await import("./services/phase9");
        return await contractService.proposeContract(input.contractId, company.id);
      }),

    // Accept a contract
    accept: protectedProcedure
      .input(z.object({ contractId: z.number() }))
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const { contractService } = await import("./services/phase9");
        return await contractService.acceptContract(input.contractId, company.id);
      }),

    // Reject a contract
    reject: protectedProcedure
      .input(z.object({ contractId: z.number() }))
      .mutation(async ({ input }) => {
        const { contractService } = await import("./services/phase9");
        return await contractService.rejectContract(input.contractId);
      }),
  }),

  // ============================================================================
  // QUALITY CONTROL ROUTES (Phase 9)
  // ============================================================================
  quality: router({
    // Get quality standards
    standards: protectedProcedure.query(async ({ ctx }) => {
      const company = await getCompanyByUserId(ctx.user.id);
      if (!company) throw new Error("No company found");

      const { qualityService } = await import("./services/phase9");
      return await qualityService.getQualityStandards(company.id);
    }),

    // Update quality standards
    updateStandards: protectedProcedure
      .input(
        z.object({
          inputQualityThreshold: z.number().min(0).max(1).optional(),
          outputQualityTarget: z.number().min(0).max(1).optional(),
          inspectionFrequency: z.enum(["none", "random", "periodic", "every_batch"]).optional(),
          inspectionRigor: z.number().min(1).max(100).optional(),
          qualityBonusEnabled: z.boolean().optional(),
        })
      )
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const { qualityService } = await import("./services/phase9");
        return await qualityService.updateQualityStandards(company.id, {
          inputQualityThreshold: input.inputQualityThreshold?.toFixed(2),
          outputQualityTarget: input.outputQualityTarget?.toFixed(2),
          inspectionFrequency: input.inspectionFrequency,
          inspectionRigor: input.inspectionRigor,
          qualityBonusEnabled: input.qualityBonusEnabled,
        });
      }),

    // Perform quality inspection
    inspect: protectedProcedure
      .input(
        z.object({
          businessUnitId: z.number(),
          resourceTypeId: z.number(),
          batchSize: z.number().positive(),
        })
      )
      .mutation(async ({ input }) => {
        const { qualityService } = await import("./services/phase9");
        const state = await getGameState();

        return await qualityService.performInspection(
          input.businessUnitId,
          input.resourceTypeId,
          input.batchSize,
          state?.currentTurn || 1
        );
      }),

    // Get inspection history
    inspections: protectedProcedure
      .input(z.object({ unitId: z.number(), limit: z.number().min(1).max(100).optional() }))
      .query(async ({ input }) => {
        const { qualityService } = await import("./services/phase9");
        return await qualityService.getInspectionHistory(input.unitId, input.limit);
      }),
  }),

  // ============================================================================
  // TECHNOLOGY ROUTES (Phase 9)
  // ============================================================================
  technology: router({
    // Get all technologies
    all: protectedProcedure.query(async () => {
      const { technologyService } = await import("./services/phase9");
      return await technologyService.getAllTechnologies();
    }),

    // Get company's researched technologies
    mine: protectedProcedure.query(async ({ ctx }) => {
      const company = await getCompanyByUserId(ctx.user.id);
      if (!company) return [];

      const { technologyService } = await import("./services/phase9");
      return await technologyService.getCompanyTechnologies(company.id);
    }),

    // Get available technologies for research
    available: protectedProcedure.query(async ({ ctx }) => {
      const company = await getCompanyByUserId(ctx.user.id);
      if (!company) return [];

      const { technologyService } = await import("./services/phase9");
      return await technologyService.getAvailableTechnologies(company.id);
    }),

    // Start researching a technology
    startResearch: protectedProcedure
      .input(z.object({ technologyId: z.number() }))
      .mutation(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) throw new Error("No company found");

        const { technologyService } = await import("./services/phase9");
        return await technologyService.startResearch(company.id, input.technologyId);
      }),

    // Check if company has technology
    hasTech: protectedProcedure
      .input(z.object({ technologyId: z.number() }))
      .query(async ({ ctx, input }) => {
        const company = await getCompanyByUserId(ctx.user.id);
        if (!company) return false;

        const { technologyService } = await import("./services/phase9");
        return await technologyService.hasTechnology(company.id, input.technologyId);
      }),
  }),

  // ============================================================================
  // TURN PROCESSING ROUTES (Phase 9)
  // ============================================================================
  turn: router({
    // Process the current turn (admin only or automated)
    process: protectedProcedure.mutation(async ({ ctx }) => {
      // In a real game, this would be automated or admin-only
      const { turnProcessingService } = await import("./services/phase9");
      const result = await turnProcessingService.processTurn();

      if (result.success) {
        await createNotification({
          userId: ctx.user.id,
          type: "info",
          title: "Turn Processed",
          message: `Turn ${(result.details.turn as number) || 0} has been completed.`,
        });
      }

      return result;
    }),

    // Get turn processing history
    history: protectedProcedure
      .input(z.object({ limit: z.number().min(1).max(50).optional() }).optional())
      .query(async ({ input }) => {
        const { turnProcessingService } = await import("./services/phase9");
        return await turnProcessingService.getTurnHistory(input?.limit);
      }),
  }),

  // ============================================================================
  // EVENT BRIDGE ROUTES
  // ============================================================================
  events: router({
    // Propagate a business event to narrative
    propagateBusiness: protectedProcedure
      .input(
        z.object({
          type: z.enum(["bankruptcy", "merger", "market_crash", "expansion", "layoff", "innovation", "scandal", "success"]),
          companyId: z.number(),
          magnitude: z.number().min(1).max(100),
          description: z.string(),
          affectedResources: z.array(z.number()).optional(),
          affectedCities: z.array(z.number()).optional(),
        })
      )
      .mutation(async ({ input }) => {
        const { eventBridge } = await import("./services/eventBridge");
        return await eventBridge.propagateBusinessEvent(input);
      }),

    // Schedule a future event
    schedule: protectedProcedure
      .input(
        z.object({
          worldId: z.number().optional(),
          type: z.enum(["world_event", "agent_action", "market_change", "narrative_trigger"]),
          triggerType: z.enum(["time_based", "condition_based", "recurring"]),
          triggerTurn: z.number().optional(),
          eventData: z.record(z.string(), z.unknown()),
          priority: z.number().min(1).max(100).optional(),
        })
      )
      .mutation(async ({ input }) => {
        const { eventBridge } = await import("./services/eventBridge");
        return await eventBridge.scheduleEvent(input);
      }),

    // Get event propagation history
    history: protectedProcedure
      .input(z.object({ limit: z.number().min(1).max(50).optional() }))
      .query(async ({ input }) => {
        const { drizzle } = await import("drizzle-orm/mysql2");
        const { eventPropagation } = await import("../drizzle/schema");
        const { desc } = await import("drizzle-orm");
        const db = drizzle(process.env.DATABASE_URL!);
        
        return db.select()
          .from(eventPropagation)
          .orderBy(desc(eventPropagation.createdAt))
          .limit(input.limit || 20);
      }),
  }),
});
export type AppRouter = typeof appRouter;
