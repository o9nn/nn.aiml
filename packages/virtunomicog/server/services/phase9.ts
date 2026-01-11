/**
 * Phase 9: Advanced Business Mechanics
 * - Supply Chain & Logistics
 * - Contract System
 * - Quality Control
 * - Technology Research
 * - Turn Processing
 */

import { and, eq, desc, sql, isNull, or, gte, lte, inArray } from "drizzle-orm";
import { drizzle } from "drizzle-orm/mysql2";
import {
  supplyRoutes,
  shipments,
  warehouses,
  contracts,
  contractItems,
  contractDeliveries,
  qualityInspections,
  qualityStandards,
  turnProcessingLog,
  technologies,
  companyTechnologies,
  companies,
  businessUnits,
  inventory,
  employees,
  cities,
  resourceTypes,
  gameState,
  transactions,
  notifications,
  productionQueue,
  productionRecipes,
  InsertSupplyRoute,
  InsertShipment,
  InsertWarehouse,
  InsertContract,
  InsertContractItem,
  InsertContractDelivery,
  InsertQualityInspection,
  InsertQualityStandard,
  InsertTurnProcessingLog,
  SupplyRoute,
  Shipment,
  Warehouse,
  Contract,
  ContractItem,
  QualityInspection,
  Technology,
} from "../../drizzle/schema";

function getDb() {
  if (!process.env.DATABASE_URL) {
    throw new Error("DATABASE_URL is required");
  }
  return drizzle(process.env.DATABASE_URL);
}

// ============================================================================
// SUPPLY CHAIN SERVICE
// ============================================================================

export const supplyChainService = {
  // Get all routes between cities
  async getRoutes(fromCityId?: number, toCityId?: number): Promise<SupplyRoute[]> {
    const db = getDb();
    let conditions = [eq(supplyRoutes.isActive, true)];

    if (fromCityId) conditions.push(eq(supplyRoutes.fromCityId, fromCityId));
    if (toCityId) conditions.push(eq(supplyRoutes.toCityId, toCityId));

    return db.select().from(supplyRoutes).where(and(...conditions));
  },

  // Get best route between two cities
  async getBestRoute(fromCityId: number, toCityId: number): Promise<SupplyRoute | null> {
    const db = getDb();
    const routes = await db.select()
      .from(supplyRoutes)
      .where(and(
        eq(supplyRoutes.fromCityId, fromCityId),
        eq(supplyRoutes.toCityId, toCityId),
        eq(supplyRoutes.isActive, true)
      ))
      .orderBy(supplyRoutes.transitTime)
      .limit(1);

    return routes[0] || null;
  },

  // Create a new supply route
  async createRoute(data: InsertSupplyRoute): Promise<SupplyRoute> {
    const db = getDb();
    const result = await db.insert(supplyRoutes).values(data);
    const [created] = await db.select().from(supplyRoutes).where(eq(supplyRoutes.id, result[0].insertId));
    return created;
  },

  // Create a shipment
  async createShipment(data: InsertShipment): Promise<Shipment> {
    const db = getDb();
    const result = await db.insert(shipments).values(data);
    const [created] = await db.select().from(shipments).where(eq(shipments.id, result[0].insertId));
    return created;
  },

  // Get shipments for a company
  async getShipments(companyId: number, status?: string): Promise<Shipment[]> {
    const db = getDb();
    let conditions = [eq(shipments.companyId, companyId)];

    if (status) {
      conditions.push(eq(shipments.status, status as "pending" | "in_transit" | "delivered" | "delayed" | "lost"));
    }

    return db.select().from(shipments).where(and(...conditions)).orderBy(desc(shipments.createdAt));
  },

  // Get shipments in transit for a business unit
  async getIncomingShipments(unitId: number): Promise<Shipment[]> {
    const db = getDb();
    return db.select()
      .from(shipments)
      .where(and(
        eq(shipments.toUnitId, unitId),
        eq(shipments.status, "in_transit")
      ))
      .orderBy(shipments.expectedArrivalTurn);
  },

  // Calculate shipping cost
  async calculateShippingCost(
    fromUnitId: number,
    toUnitId: number,
    quantity: number
  ): Promise<{ cost: number; transitTime: number; routeId: number } | null> {
    const db = getDb();

    // Get the cities for both units
    const [fromUnit] = await db.select().from(businessUnits).where(eq(businessUnits.id, fromUnitId));
    const [toUnit] = await db.select().from(businessUnits).where(eq(businessUnits.id, toUnitId));

    if (!fromUnit || !toUnit) return null;

    // Same city = no shipping
    if (fromUnit.cityId === toUnit.cityId) {
      return { cost: 0, transitTime: 0, routeId: 0 };
    }

    // Find route
    const route = await this.getBestRoute(fromUnit.cityId, toUnit.cityId);
    if (!route) return null;

    const cost = parseFloat(route.baseShippingCost) * quantity;
    return { cost, transitTime: route.transitTime, routeId: route.id };
  },

  // Ship goods between units
  async shipGoods(
    companyId: number,
    fromUnitId: number,
    toUnitId: number,
    resourceTypeId: number,
    quantity: number,
    currentTurn: number,
    contractId?: number
  ): Promise<{ success: boolean; message: string; shipment?: Shipment }> {
    const db = getDb();

    // Verify ownership
    const [fromUnit] = await db.select().from(businessUnits).where(eq(businessUnits.id, fromUnitId));
    if (!fromUnit || fromUnit.companyId !== companyId) {
      return { success: false, message: "You don't own the source unit" };
    }

    // Check inventory
    const [inv] = await db.select()
      .from(inventory)
      .where(and(
        eq(inventory.businessUnitId, fromUnitId),
        eq(inventory.resourceTypeId, resourceTypeId)
      ));

    if (!inv || parseFloat(inv.quantity) < quantity) {
      return { success: false, message: "Insufficient inventory" };
    }

    // Calculate shipping
    const shippingInfo = await this.calculateShippingCost(fromUnitId, toUnitId, quantity);
    if (!shippingInfo && fromUnit.cityId !== (await db.select().from(businessUnits).where(eq(businessUnits.id, toUnitId)))[0]?.cityId) {
      return { success: false, message: "No shipping route available" };
    }

    const cost = shippingInfo?.cost || 0;
    const transitTime = shippingInfo?.transitTime || 0;
    const routeId = shippingInfo?.routeId || 0;

    // Check company funds
    const [company] = await db.select().from(companies).where(eq(companies.id, companyId));
    if (!company || parseFloat(company.cash) < cost) {
      return { success: false, message: "Insufficient funds for shipping" };
    }

    // Deduct inventory
    await db.update(inventory)
      .set({ quantity: (parseFloat(inv.quantity) - quantity).toFixed(4) })
      .where(eq(inventory.id, inv.id));

    // Deduct shipping cost
    if (cost > 0) {
      await db.update(companies)
        .set({ cash: (parseFloat(company.cash) - cost).toFixed(2) })
        .where(eq(companies.id, companyId));

      await db.insert(transactions).values({
        type: "other",
        companyId,
        amount: (-cost).toFixed(2),
        description: `Shipping cost for ${quantity} units`,
        relatedUnitId: fromUnitId,
        relatedResourceId: resourceTypeId,
      });
    }

    // Create shipment
    const shipment = await this.createShipment({
      companyId,
      fromUnitId,
      toUnitId,
      routeId: routeId || 1, // Default route if same city
      resourceTypeId,
      quantity: quantity.toFixed(4),
      quality: inv.quality,
      shippingCost: cost.toFixed(2),
      status: transitTime === 0 ? "delivered" : "in_transit",
      departureTurn: currentTurn,
      expectedArrivalTurn: currentTurn + transitTime,
      actualArrivalTurn: transitTime === 0 ? currentTurn : undefined,
      contractId,
    });

    // If same city, deliver immediately
    if (transitTime === 0) {
      await this.deliverShipment(shipment.id, currentTurn);
    }

    return { success: true, message: "Shipment created", shipment };
  },

  // Deliver a shipment
  async deliverShipment(shipmentId: number, currentTurn: number): Promise<boolean> {
    const db = getDb();

    const [shipment] = await db.select().from(shipments).where(eq(shipments.id, shipmentId));
    if (!shipment || shipment.status === "delivered") return false;

    // Add to destination inventory
    const [existingInv] = await db.select()
      .from(inventory)
      .where(and(
        eq(inventory.businessUnitId, shipment.toUnitId),
        eq(inventory.resourceTypeId, shipment.resourceTypeId)
      ));

    if (existingInv) {
      // Average quality with existing inventory
      const existingQty = parseFloat(existingInv.quantity);
      const newQty = parseFloat(shipment.quantity);
      const existingQual = parseFloat(existingInv.quality);
      const newQual = parseFloat(shipment.quality);
      const avgQuality = ((existingQty * existingQual) + (newQty * newQual)) / (existingQty + newQty);

      await db.update(inventory)
        .set({
          quantity: (existingQty + newQty).toFixed(4),
          quality: avgQuality.toFixed(2),
        })
        .where(eq(inventory.id, existingInv.id));
    } else {
      await db.insert(inventory).values({
        businessUnitId: shipment.toUnitId,
        resourceTypeId: shipment.resourceTypeId,
        quantity: shipment.quantity,
        quality: shipment.quality,
      });
    }

    // Update shipment status
    await db.update(shipments)
      .set({
        status: "delivered",
        actualArrivalTurn: currentTurn,
      })
      .where(eq(shipments.id, shipmentId));

    return true;
  },

  // Process all in-transit shipments for a turn
  async processShipments(currentTurn: number): Promise<{ delivered: number; delayed: number }> {
    const db = getDb();
    let delivered = 0;
    let delayed = 0;

    // Get all shipments that should arrive this turn
    const dueShipments = await db.select()
      .from(shipments)
      .where(and(
        eq(shipments.status, "in_transit"),
        lte(shipments.expectedArrivalTurn, currentTurn)
      ));

    for (const shipment of dueShipments) {
      // Check route reliability for potential delay
      const [route] = await db.select().from(supplyRoutes).where(eq(supplyRoutes.id, shipment.routeId));
      const reliability = route ? parseFloat(route.reliability) : 0.95;

      if (Math.random() < reliability) {
        await this.deliverShipment(shipment.id, currentTurn);
        delivered++;
      } else {
        // Delay the shipment
        await db.update(shipments)
          .set({
            status: "delayed",
            expectedArrivalTurn: currentTurn + 1,
          })
          .where(eq(shipments.id, shipment.id));
        delayed++;
      }
    }

    return { delivered, delayed };
  },

  // Warehouse operations
  async getWarehouse(unitId: number): Promise<Warehouse | null> {
    const db = getDb();
    const [warehouse] = await db.select().from(warehouses).where(eq(warehouses.businessUnitId, unitId));
    return warehouse || null;
  },

  async createWarehouse(data: InsertWarehouse): Promise<Warehouse> {
    const db = getDb();
    const result = await db.insert(warehouses).values(data);
    const [created] = await db.select().from(warehouses).where(eq(warehouses.id, result[0].insertId));
    return created;
  },

  async updateWarehouseCapacity(unitId: number, usedCapacity: number): Promise<void> {
    const db = getDb();
    await db.update(warehouses)
      .set({ usedCapacity: usedCapacity.toFixed(2) })
      .where(eq(warehouses.businessUnitId, unitId));
  },
};

// ============================================================================
// CONTRACT SERVICE
// ============================================================================

export const contractService = {
  // Create a new contract
  async createContract(
    sellerCompanyId: number,
    buyerCompanyId: number,
    data: Partial<InsertContract>
  ): Promise<Contract> {
    const db = getDb();
    const result = await db.insert(contracts).values({
      sellerCompanyId,
      buyerCompanyId,
      title: data.title || "Supply Contract",
      description: data.description,
      status: "draft",
      startTurn: data.startTurn || 1,
      endTurn: data.endTurn || 100,
      deliveryFrequency: data.deliveryFrequency || "per_turn",
      proposedBy: sellerCompanyId,
      ...data,
    } as InsertContract);

    const [created] = await db.select().from(contracts).where(eq(contracts.id, result[0].insertId));
    return created;
  },

  // Add item to contract
  async addContractItem(contractId: number, data: Partial<InsertContractItem>): Promise<ContractItem> {
    const db = getDb();
    const result = await db.insert(contractItems).values({
      contractId,
      resourceTypeId: data.resourceTypeId!,
      quantityPerDelivery: data.quantityPerDelivery!,
      totalQuantity: data.totalQuantity!,
      pricePerUnit: data.pricePerUnit!,
      minQuality: data.minQuality || "0.80",
      fromUnitId: data.fromUnitId,
      toUnitId: data.toUnitId,
    } as InsertContractItem);

    const [created] = await db.select().from(contractItems).where(eq(contractItems.id, result[0].insertId));

    // Update contract total value
    const items = await db.select().from(contractItems).where(eq(contractItems.contractId, contractId));
    const totalValue = items.reduce((sum, item) =>
      sum + parseFloat(item.totalQuantity) * parseFloat(item.pricePerUnit), 0
    );

    await db.update(contracts)
      .set({ totalValue: totalValue.toFixed(2) })
      .where(eq(contracts.id, contractId));

    return created;
  },

  // Get contracts for a company
  async getContracts(companyId: number, status?: string): Promise<Contract[]> {
    const db = getDb();
    let conditions = [or(
      eq(contracts.sellerCompanyId, companyId),
      eq(contracts.buyerCompanyId, companyId)
    )];

    if (status) {
      conditions.push(eq(contracts.status, status as Contract["status"]));
    }

    return db.select().from(contracts).where(and(...conditions)).orderBy(desc(contracts.createdAt));
  },

  // Get contract by ID with items
  async getContractWithItems(contractId: number): Promise<{ contract: Contract; items: ContractItem[] } | null> {
    const db = getDb();
    const [contract] = await db.select().from(contracts).where(eq(contracts.id, contractId));
    if (!contract) return null;

    const items = await db.select().from(contractItems).where(eq(contractItems.contractId, contractId));
    return { contract, items };
  },

  // Propose a contract (change status to proposed)
  async proposeContract(contractId: number, companyId: number): Promise<boolean> {
    const db = getDb();
    const [contract] = await db.select().from(contracts).where(eq(contracts.id, contractId));

    if (!contract) return false;
    if (contract.sellerCompanyId !== companyId && contract.buyerCompanyId !== companyId) return false;
    if (contract.status !== "draft") return false;

    await db.update(contracts)
      .set({ status: "proposed", proposedBy: companyId })
      .where(eq(contracts.id, contractId));

    return true;
  },

  // Accept a contract
  async acceptContract(contractId: number, companyId: number): Promise<boolean> {
    const db = getDb();
    const [contract] = await db.select().from(contracts).where(eq(contracts.id, contractId));

    if (!contract) return false;
    if (contract.proposedBy === companyId) return false; // Can't accept own proposal
    if (contract.status !== "proposed") return false;

    await db.update(contracts)
      .set({
        status: "active",
        signedAt: new Date(),
      })
      .where(eq(contracts.id, contractId));

    return true;
  },

  // Reject a contract
  async rejectContract(contractId: number): Promise<boolean> {
    const db = getDb();
    await db.update(contracts)
      .set({ status: "cancelled" })
      .where(eq(contracts.id, contractId));
    return true;
  },

  // Record a delivery
  async recordDelivery(
    contractId: number,
    contractItemId: number,
    quantity: number,
    quality: number,
    deliveryTurn: number,
    shipmentId?: number
  ): Promise<{ success: boolean; penalty: number }> {
    const db = getDb();

    const [item] = await db.select().from(contractItems).where(eq(contractItems.id, contractItemId));
    const [contract] = await db.select().from(contracts).where(eq(contracts.id, contractId));

    if (!item || !contract) return { success: false, penalty: 0 };

    // Check quality
    let penalty = 0;
    let status: "delivered" | "late" | "rejected" = "delivered";

    if (quality < parseFloat(item.minQuality)) {
      penalty = parseFloat(contract.qualityPenaltyPercent) / 100 * quantity * parseFloat(item.pricePerUnit);
      status = "rejected";
    }

    // Record delivery
    await db.insert(contractDeliveries).values({
      contractId,
      contractItemId,
      shipmentId,
      quantity: quantity.toFixed(4),
      quality: quality.toFixed(2),
      deliveryTurn,
      status,
      penaltyApplied: penalty.toFixed(2),
    } as InsertContractDelivery);

    // Update contract item delivered quantity
    await db.update(contractItems)
      .set({
        deliveredQuantity: (parseFloat(item.deliveredQuantity) + quantity).toFixed(4),
      })
      .where(eq(contractItems.id, contractItemId));

    // Update contract total delivered
    await db.update(contracts)
      .set({
        totalDelivered: (parseFloat(contract.totalDelivered) + quantity * parseFloat(item.pricePerUnit)).toFixed(2),
      })
      .where(eq(contracts.id, contractId));

    return { success: true, penalty };
  },

  // Check for contract completion
  async checkContractCompletion(contractId: number): Promise<boolean> {
    const db = getDb();
    const [contract] = await db.select().from(contracts).where(eq(contracts.id, contractId));
    if (!contract || contract.status !== "active") return false;

    const items = await db.select().from(contractItems).where(eq(contractItems.contractId, contractId));

    const allDelivered = items.every(
      item => parseFloat(item.deliveredQuantity) >= parseFloat(item.totalQuantity)
    );

    if (allDelivered) {
      await db.update(contracts)
        .set({ status: "completed" })
        .where(eq(contracts.id, contractId));
      return true;
    }

    return false;
  },
};

// ============================================================================
// QUALITY CONTROL SERVICE
// ============================================================================

export const qualityService = {
  // Get or create quality standards for a company
  async getQualityStandards(companyId: number) {
    const db = getDb();
    let [standards] = await db.select().from(qualityStandards).where(eq(qualityStandards.companyId, companyId));

    if (!standards) {
      // Create default standards
      await db.insert(qualityStandards).values({
        companyId,
        inputQualityThreshold: "0.70",
        outputQualityTarget: "0.90",
        inspectionFrequency: "periodic",
        inspectionRigor: 50,
        qualityBonusEnabled: true,
      } as InsertQualityStandard);
      [standards] = await db.select().from(qualityStandards).where(eq(qualityStandards.companyId, companyId));
    }

    return standards;
  },

  // Update quality standards
  async updateQualityStandards(
    companyId: number,
    updates: Partial<InsertQualityStandard>
  ) {
    const db = getDb();
    await db.update(qualityStandards)
      .set(updates)
      .where(eq(qualityStandards.companyId, companyId));

    return this.getQualityStandards(companyId);
  },

  // Perform quality inspection
  async performInspection(
    businessUnitId: number,
    resourceTypeId: number,
    batchSize: number,
    currentTurn: number
  ): Promise<QualityInspection> {
    const db = getDb();

    // Get the unit and company
    const [unit] = await db.select().from(businessUnits).where(eq(businessUnits.id, businessUnitId));
    if (!unit) throw new Error("Unit not found");

    const standards = await this.getQualityStandards(unit.companyId);

    // Get employees for qualification
    const [emp] = await db.select().from(employees).where(eq(employees.businessUnitId, businessUnitId));
    const inspectorQual = emp ? parseFloat(emp.qualification) : 1.0;

    // Get inventory quality
    const [inv] = await db.select()
      .from(inventory)
      .where(and(
        eq(inventory.businessUnitId, businessUnitId),
        eq(inventory.resourceTypeId, resourceTypeId)
      ));

    const actualQuality = inv ? parseFloat(inv.quality) : 0.5;

    // Sample size based on rigor (5-20% of batch)
    const rigorFactor = standards.inspectionRigor / 100;
    const sampleSize = Math.max(1, Math.floor(batchSize * (0.05 + rigorFactor * 0.15)));

    // Measured quality has some variance based on inspector qualification
    const variance = (1 - inspectorQual) * 0.1;
    const measuredQuality = Math.max(0, Math.min(1, actualQuality + (Math.random() - 0.5) * variance));

    // Determine if passed
    const passed = measuredQuality >= parseFloat(standards.inputQualityThreshold);

    // Detect defects
    const defectsFound = passed ? 0 : Math.floor((1 - measuredQuality) * sampleSize);
    const defectTypes = passed ? [] : ["dimensional", "surface", "material"].filter(() => Math.random() > 0.5);

    // Record inspection
    const result = await db.insert(qualityInspections).values({
      businessUnitId,
      resourceTypeId,
      batchSize: batchSize.toFixed(4),
      sampleSize: sampleSize.toFixed(4),
      measuredQuality: measuredQuality.toFixed(2),
      passThreshold: standards.inputQualityThreshold,
      passed,
      defectsFound,
      defectTypes,
      inspectorQualification: inspectorQual.toFixed(2),
      turn: currentTurn,
    } as InsertQualityInspection);

    const [inspection] = await db.select().from(qualityInspections).where(eq(qualityInspections.id, result[0].insertId));
    return inspection;
  },

  // Get inspection history
  async getInspectionHistory(businessUnitId: number, limit = 20): Promise<QualityInspection[]> {
    const db = getDb();
    return db.select()
      .from(qualityInspections)
      .where(eq(qualityInspections.businessUnitId, businessUnitId))
      .orderBy(desc(qualityInspections.createdAt))
      .limit(limit);
  },

  // Calculate production quality based on inputs
  calculateProductionQuality(
    inputQualities: number[],
    employeeQualification: number,
    equipmentCondition: number
  ): number {
    if (inputQualities.length === 0) return 0.5;

    // Average input quality
    const avgInputQuality = inputQualities.reduce((a, b) => a + b, 0) / inputQualities.length;

    // Employee contribution (20%)
    const employeeFactor = employeeQualification * 0.2;

    // Equipment contribution (20%)
    const equipmentFactor = (equipmentCondition / 100) * 0.2;

    // Input quality contribution (60%)
    const inputFactor = avgInputQuality * 0.6;

    return Math.min(1, inputFactor + employeeFactor + equipmentFactor);
  },
};

// ============================================================================
// TECHNOLOGY RESEARCH SERVICE
// ============================================================================

export const technologyService = {
  // Get all available technologies
  async getAllTechnologies(): Promise<Technology[]> {
    const db = getDb();
    return db.select().from(technologies);
  },

  // Get technologies for a company
  async getCompanyTechnologies(companyId: number) {
    const db = getDb();
    return db.select()
      .from(companyTechnologies)
      .where(eq(companyTechnologies.companyId, companyId));
  },

  // Check if company has technology
  async hasTechnology(companyId: number, technologyId: number): Promise<boolean> {
    const db = getDb();
    const [tech] = await db.select()
      .from(companyTechnologies)
      .where(and(
        eq(companyTechnologies.companyId, companyId),
        eq(companyTechnologies.technologyId, technologyId),
        eq(companyTechnologies.isCompleted, true)
      ));
    return !!tech;
  },

  // Start researching a technology
  async startResearch(companyId: number, technologyId: number): Promise<{ success: boolean; message: string }> {
    const db = getDb();

    // Check if already researching or completed
    const [existing] = await db.select()
      .from(companyTechnologies)
      .where(and(
        eq(companyTechnologies.companyId, companyId),
        eq(companyTechnologies.technologyId, technologyId)
      ));

    if (existing?.isCompleted) {
      return { success: false, message: "Technology already researched" };
    }

    // Get technology and check prerequisites
    const [tech] = await db.select().from(technologies).where(eq(technologies.id, technologyId));
    if (!tech) return { success: false, message: "Technology not found" };

    if (tech.prerequisites && tech.prerequisites.length > 0) {
      for (const prereqId of tech.prerequisites) {
        const hasPrereq = await this.hasTechnology(companyId, prereqId);
        if (!hasPrereq) {
          return { success: false, message: "Missing prerequisite technology" };
        }
      }
    }

    if (existing) {
      // Continue existing research
      return { success: true, message: "Continuing research" };
    }

    // Start new research
    await db.insert(companyTechnologies).values({
      companyId,
      technologyId,
      researchProgress: 0,
      isCompleted: false,
    });

    return { success: true, message: "Research started" };
  },

  // Add research progress
  async addResearchProgress(companyId: number, points: number): Promise<Technology[]> {
    const db = getDb();

    // Get active research
    const activeResearch = await db.select()
      .from(companyTechnologies)
      .where(and(
        eq(companyTechnologies.companyId, companyId),
        eq(companyTechnologies.isCompleted, false)
      ));

    const completedTechs: Technology[] = [];

    for (const research of activeResearch) {
      const [tech] = await db.select().from(technologies).where(eq(technologies.id, research.technologyId));
      if (!tech) continue;

      const newProgress = research.researchProgress + points;

      if (newProgress >= tech.researchCost) {
        // Complete the research
        await db.update(companyTechnologies)
          .set({
            researchProgress: tech.researchCost,
            isCompleted: true,
            completedAt: new Date(),
          })
          .where(eq(companyTechnologies.id, research.id));

        completedTechs.push(tech);
      } else {
        // Update progress
        await db.update(companyTechnologies)
          .set({ researchProgress: newProgress })
          .where(eq(companyTechnologies.id, research.id));
      }
    }

    return completedTechs;
  },

  // Get available technologies for research
  async getAvailableTechnologies(companyId: number): Promise<Technology[]> {
    const db = getDb();

    const allTechs = await this.getAllTechnologies();
    const companyTechs = await this.getCompanyTechnologies(companyId);

    const completedIds = new Set(
      companyTechs.filter(ct => ct.isCompleted).map(ct => ct.technologyId)
    );
    const inProgressIds = new Set(
      companyTechs.filter(ct => !ct.isCompleted).map(ct => ct.technologyId)
    );

    return allTechs.filter(tech => {
      // Not already completed or in progress
      if (completedIds.has(tech.id) || inProgressIds.has(tech.id)) return false;

      // Check prerequisites
      if (tech.prerequisites && tech.prerequisites.length > 0) {
        return tech.prerequisites.every(prereqId => completedIds.has(prereqId));
      }

      return true;
    });
  },
};

// ============================================================================
// TURN PROCESSING SERVICE
// ============================================================================

export const turnProcessingService = {
  // Log turn processing phase
  async logPhase(
    turn: number,
    phase: InsertTurnProcessingLog["phase"],
    status: InsertTurnProcessingLog["status"],
    details?: Record<string, unknown>,
    error?: string
  ): Promise<void> {
    const db = getDb();
    await db.insert(turnProcessingLog).values({
      turn,
      phase,
      status,
      details,
      error,
      startedAt: status === "processing" ? new Date() : undefined,
      completedAt: status === "completed" || status === "failed" ? new Date() : undefined,
    } as InsertTurnProcessingLog);
  },

  // Get current game state
  async getGameState() {
    const db = getDb();
    const [state] = await db.select().from(gameState).limit(1);
    return state;
  },

  // Process a full game turn
  async processTurn(): Promise<{ success: boolean; details: Record<string, unknown> }> {
    const db = getDb();
    const state = await this.getGameState();
    if (!state) return { success: false, details: { error: "No game state" } };

    const currentTurn = state.currentTurn;
    const results: Record<string, unknown> = { turn: currentTurn };

    try {
      // Phase 1: Start
      await this.logPhase(currentTurn, "start", "processing");
      await this.logPhase(currentTurn, "start", "completed");

      // Phase 2: Production
      await this.logPhase(currentTurn, "production", "processing");
      const productionResults = await this.processProduction(currentTurn);
      results.production = productionResults;
      await this.logPhase(currentTurn, "production", "completed", productionResults);

      // Phase 3: Shipments
      await this.logPhase(currentTurn, "shipments", "processing");
      const shipmentResults = await supplyChainService.processShipments(currentTurn);
      results.shipments = shipmentResults;
      await this.logPhase(currentTurn, "shipments", "completed", shipmentResults);

      // Phase 4: Contracts
      await this.logPhase(currentTurn, "contracts", "processing");
      const contractResults = await this.processContracts(currentTurn);
      results.contracts = contractResults;
      await this.logPhase(currentTurn, "contracts", "completed", contractResults);

      // Phase 5: Salaries
      await this.logPhase(currentTurn, "salaries", "processing");
      const salaryResults = await this.processSalaries();
      results.salaries = salaryResults;
      await this.logPhase(currentTurn, "salaries", "completed", salaryResults);

      // Phase 6: Maintenance
      await this.logPhase(currentTurn, "maintenance", "processing");
      const maintenanceResults = await this.processMaintenance();
      results.maintenance = maintenanceResults;
      await this.logPhase(currentTurn, "maintenance", "completed", maintenanceResults);

      // Phase 7: Taxes
      await this.logPhase(currentTurn, "taxes", "processing");
      const taxResults = await this.processTaxes();
      results.taxes = taxResults;
      await this.logPhase(currentTurn, "taxes", "completed", taxResults);

      // Phase 8: Events (world events processing)
      await this.logPhase(currentTurn, "events", "processing");
      await this.logPhase(currentTurn, "events", "completed");

      // Phase 9: Complete - advance turn
      await this.logPhase(currentTurn, "complete", "processing");
      await db.update(gameState)
        .set({
          currentTurn: currentTurn + 1,
          lastTurnProcessed: new Date(),
        })
        .where(eq(gameState.id, state.id));
      await this.logPhase(currentTurn, "complete", "completed", results);

      return { success: true, details: results };

    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : "Unknown error";
      await this.logPhase(currentTurn, "complete", "failed", results, errorMsg);
      return { success: false, details: { ...results, error: errorMsg } };
    }
  },

  // Process production for all companies
  async processProduction(currentTurn: number): Promise<{ completed: number; failed: number }> {
    const db = getDb();
    let completed = 0;
    let failed = 0;

    // Get all active production queue items
    const queueItems = await db.select()
      .from(productionQueue)
      .where(eq(productionQueue.isActive, true));

    for (const item of queueItems) {
      try {
        // Get recipe
        const [recipe] = await db.select().from(productionRecipes).where(eq(productionRecipes.id, item.recipeId));
        if (!recipe) continue;

        // Get unit and employees
        const [unit] = await db.select().from(businessUnits).where(eq(businessUnits.id, item.businessUnitId));
        const [emp] = await db.select().from(employees).where(eq(employees.businessUnitId, item.businessUnitId));

        if (!unit || !emp || emp.count < recipe.laborRequired) continue;

        // Calculate progress this turn (based on efficiency and employees)
        const efficiencyMultiplier = parseFloat(unit.efficiency);
        const employeeMultiplier = Math.min(1, emp.count / recipe.laborRequired);
        const progressThisTurn = (100 / recipe.timeRequired) * efficiencyMultiplier * employeeMultiplier;

        const newProgress = parseFloat(item.progress) + progressThisTurn;

        if (newProgress >= 100) {
          // Production complete - add to inventory
          const outputQty = parseFloat(item.quantity) * parseFloat(recipe.outputQuantity);

          // Calculate quality based on inputs
          const inputQualities: number[] = [];
          if (recipe.inputResources) {
            for (const input of recipe.inputResources) {
              const [inv] = await db.select()
                .from(inventory)
                .where(and(
                  eq(inventory.businessUnitId, item.businessUnitId),
                  eq(inventory.resourceTypeId, input.resourceId)
                ));
              if (inv) inputQualities.push(parseFloat(inv.quality));
            }
          }

          const outputQuality = qualityService.calculateProductionQuality(
            inputQualities,
            parseFloat(emp.qualification),
            unit.condition
          );

          // Add to inventory
          const [existingInv] = await db.select()
            .from(inventory)
            .where(and(
              eq(inventory.businessUnitId, item.businessUnitId),
              eq(inventory.resourceTypeId, recipe.outputResourceId)
            ));

          if (existingInv) {
            const existingQty = parseFloat(existingInv.quantity);
            const avgQuality = ((existingQty * parseFloat(existingInv.quality)) + (outputQty * outputQuality)) / (existingQty + outputQty);

            await db.update(inventory)
              .set({
                quantity: (existingQty + outputQty).toFixed(4),
                quality: avgQuality.toFixed(2),
              })
              .where(eq(inventory.id, existingInv.id));
          } else {
            await db.insert(inventory).values({
              businessUnitId: item.businessUnitId,
              resourceTypeId: recipe.outputResourceId,
              quantity: outputQty.toFixed(4),
              quality: outputQuality.toFixed(2),
            });
          }

          // Deactivate queue item
          await db.update(productionQueue)
            .set({ isActive: false, progress: "100.00" })
            .where(eq(productionQueue.id, item.id));

          completed++;
        } else {
          // Update progress
          await db.update(productionQueue)
            .set({ progress: newProgress.toFixed(2) })
            .where(eq(productionQueue.id, item.id));
        }
      } catch {
        failed++;
      }
    }

    return { completed, failed };
  },

  // Process contract deliveries
  async processContracts(currentTurn: number): Promise<{ processed: number; penalties: number }> {
    const db = getDb();
    let processed = 0;
    let penalties = 0;

    // Get all active contracts
    const activeContracts = await db.select()
      .from(contracts)
      .where(eq(contracts.status, "active"));

    for (const contract of activeContracts) {
      // Check if delivery is due this turn based on frequency
      let isDue = false;
      switch (contract.deliveryFrequency) {
        case "per_turn":
          isDue = true;
          break;
        case "weekly":
          isDue = currentTurn % 7 === 0;
          break;
        case "monthly":
          isDue = currentTurn % 30 === 0;
          break;
        case "quarterly":
          isDue = currentTurn % 90 === 0;
          break;
        case "one_time":
          isDue = currentTurn === contract.startTurn;
          break;
      }

      if (isDue) {
        // Process pending deliveries
        const pendingDeliveries = await db.select()
          .from(contractDeliveries)
          .where(and(
            eq(contractDeliveries.contractId, contract.id),
            eq(contractDeliveries.status, "pending")
          ));

        for (const delivery of pendingDeliveries) {
          const result = await contractService.recordDelivery(
            contract.id,
            delivery.contractItemId,
            parseFloat(delivery.quantity),
            parseFloat(delivery.quality),
            currentTurn,
            delivery.shipmentId || undefined
          );

          if (result.success) processed++;
          if (result.penalty > 0) penalties += result.penalty;
        }

        // Check for completion
        await contractService.checkContractCompletion(contract.id);
      }
    }

    return { processed, penalties };
  },

  // Process salary payments
  async processSalaries(): Promise<{ companiesPaid: number; totalPaid: number }> {
    const db = getDb();
    let companiesPaid = 0;
    let totalPaid = 0;

    const allCompanies = await db.select().from(companies);

    for (const company of allCompanies) {
      // Get all business units for the company
      const units = await db.select().from(businessUnits).where(eq(businessUnits.companyId, company.id));

      let companySalaries = 0;

      for (const unit of units) {
        const [emp] = await db.select().from(employees).where(eq(employees.businessUnitId, unit.id));
        if (emp && emp.count > 0) {
          companySalaries += emp.count * parseFloat(emp.salary);
        }
      }

      if (companySalaries > 0) {
        // Deduct from company cash
        const newCash = Math.max(0, parseFloat(company.cash) - companySalaries);
        await db.update(companies)
          .set({ cash: newCash.toFixed(2) })
          .where(eq(companies.id, company.id));

        // Record transaction
        await db.insert(transactions).values({
          type: "salary",
          companyId: company.id,
          amount: (-companySalaries).toFixed(2),
          description: "Employee salaries payment",
        });

        companiesPaid++;
        totalPaid += companySalaries;
      }
    }

    return { companiesPaid, totalPaid };
  },

  // Process maintenance costs
  async processMaintenance(): Promise<{ unitsMaintained: number; totalCost: number }> {
    const db = getDb();
    let unitsMaintained = 0;
    let totalCost = 0;

    const allUnits = await db.select().from(businessUnits).where(eq(businessUnits.isActive, true));

    for (const unit of allUnits) {
      // Base maintenance cost by type
      const baseCosts: Record<string, number> = {
        office: 500,
        store: 1000,
        factory: 5000,
        mine: 10000,
        farm: 2000,
        laboratory: 7500,
      };

      const maintenanceCost = (baseCosts[unit.type] || 1000) * (unit.size / 100);

      // Get company
      const [company] = await db.select().from(companies).where(eq(companies.id, unit.companyId));
      if (!company) continue;

      // Deduct maintenance
      const newCash = Math.max(0, parseFloat(company.cash) - maintenanceCost);
      await db.update(companies)
        .set({ cash: newCash.toFixed(2) })
        .where(eq(companies.id, company.id));

      // Degrade condition slightly if not fully maintained
      if (parseFloat(company.cash) < maintenanceCost) {
        await db.update(businessUnits)
          .set({ condition: Math.max(0, unit.condition - 5) })
          .where(eq(businessUnits.id, unit.id));
      }

      // Record transaction
      await db.insert(transactions).values({
        type: "maintenance",
        companyId: company.id,
        amount: (-maintenanceCost).toFixed(2),
        description: `Maintenance for ${unit.name}`,
        relatedUnitId: unit.id,
      });

      unitsMaintained++;
      totalCost += maintenanceCost;
    }

    return { unitsMaintained, totalCost };
  },

  // Process taxes
  async processTaxes(): Promise<{ companiesTaxed: number; totalTaxes: number }> {
    const db = getDb();
    let companiesTaxed = 0;
    let totalTaxes = 0;

    const allCompanies = await db.select().from(companies);

    for (const company of allCompanies) {
      // Get all units and calculate tax based on city tax rates
      const units = await db.select().from(businessUnits).where(eq(businessUnits.companyId, company.id));

      let companyTax = 0;

      for (const unit of units) {
        const [city] = await db.select().from(cities).where(eq(cities.id, unit.cityId));
        if (!city) continue;

        // Tax based on unit value and city tax rate
        const unitValue = unit.size * 100; // Simplified valuation
        const tax = unitValue * parseFloat(city.taxRate);
        companyTax += tax;
      }

      if (companyTax > 0) {
        // Deduct from company cash
        const newCash = Math.max(0, parseFloat(company.cash) - companyTax);
        await db.update(companies)
          .set({ cash: newCash.toFixed(2) })
          .where(eq(companies.id, company.id));

        // Record transaction
        await db.insert(transactions).values({
          type: "tax",
          companyId: company.id,
          amount: (-companyTax).toFixed(2),
          description: "Property and business taxes",
        });

        companiesTaxed++;
        totalTaxes += companyTax;
      }
    }

    return { companiesTaxed, totalTaxes };
  },

  // Get turn processing history
  async getTurnHistory(limit = 10) {
    const db = getDb();
    return db.select()
      .from(turnProcessingLog)
      .orderBy(desc(turnProcessingLog.createdAt))
      .limit(limit);
  },
};

// ============================================================================
// SEED FUNCTIONS
// ============================================================================

export async function seedSupplyRoutes() {
  const db = getDb();

  // Check if routes already exist
  const existingRoutes = await db.select().from(supplyRoutes).limit(1);
  if (existingRoutes.length > 0) return;

  // Get all cities
  const allCities = await db.select().from(cities);
  if (allCities.length < 2) return;

  // Create routes between all city pairs
  const routes: InsertSupplyRoute[] = [];

  for (let i = 0; i < allCities.length; i++) {
    for (let j = i + 1; j < allCities.length; j++) {
      const city1 = allCities[i];
      const city2 = allCities[j];

      // Calculate approximate distance (simplified)
      const distance = Math.abs(i - j) * 500 + Math.random() * 200;

      // Road route (both directions)
      routes.push({
        fromCityId: city1.id,
        toCityId: city2.id,
        distance: distance.toFixed(2),
        baseShippingCost: (distance * 0.1).toFixed(2),
        transitTime: Math.ceil(distance / 500),
        routeType: "road",
        reliability: "0.95",
      });

      routes.push({
        fromCityId: city2.id,
        toCityId: city1.id,
        distance: distance.toFixed(2),
        baseShippingCost: (distance * 0.1).toFixed(2),
        transitTime: Math.ceil(distance / 500),
        routeType: "road",
        reliability: "0.95",
      });
    }
  }

  if (routes.length > 0) {
    await db.insert(supplyRoutes).values(routes);
  }
}

export async function seedTechnologies() {
  const db = getDb();

  // Check if technologies already exist
  const existingTechs = await db.select().from(technologies).limit(1);
  if (existingTechs.length > 0) return;

  const techData = [
    // Production Technologies
    {
      code: "adv_machinery",
      name: "Advanced Machinery",
      category: "production" as const,
      description: "Improves production efficiency by 10%",
      researchCost: 1000,
      prerequisites: null,
      effects: [{ type: "production_efficiency", value: 0.1 }],
    },
    {
      code: "automation_1",
      name: "Basic Automation",
      category: "production" as const,
      description: "Reduces labor requirements by 15%",
      researchCost: 2000,
      prerequisites: [1], // adv_machinery
      effects: [{ type: "labor_reduction", value: 0.15 }],
    },
    {
      code: "automation_2",
      name: "Advanced Automation",
      category: "production" as const,
      description: "Reduces labor requirements by additional 20%",
      researchCost: 5000,
      prerequisites: [2], // automation_1
      effects: [{ type: "labor_reduction", value: 0.2 }],
    },
    {
      code: "quality_control",
      name: "Quality Control Systems",
      category: "production" as const,
      description: "Improves output quality by 15%",
      researchCost: 1500,
      prerequisites: null,
      effects: [{ type: "quality_bonus", value: 0.15 }],
    },

    // Commerce Technologies
    {
      code: "market_analysis",
      name: "Market Analysis",
      category: "commerce" as const,
      description: "Better pricing insights, +5% sale prices",
      researchCost: 800,
      prerequisites: null,
      effects: [{ type: "sale_price_bonus", value: 0.05 }],
    },
    {
      code: "logistics_opt",
      name: "Logistics Optimization",
      category: "commerce" as const,
      description: "Reduces shipping costs by 20%",
      researchCost: 1200,
      prerequisites: null,
      effects: [{ type: "shipping_cost_reduction", value: 0.2 }],
    },
    {
      code: "supply_chain",
      name: "Supply Chain Management",
      category: "commerce" as const,
      description: "Faster shipment times, improved reliability",
      researchCost: 2500,
      prerequisites: [6], // logistics_opt
      effects: [{ type: "shipping_time_reduction", value: 0.25 }, { type: "reliability_bonus", value: 0.05 }],
    },

    // Management Technologies
    {
      code: "hr_training",
      name: "HR Training Programs",
      category: "management" as const,
      description: "Improves employee qualification growth by 25%",
      researchCost: 1000,
      prerequisites: null,
      effects: [{ type: "qualification_growth", value: 0.25 }],
    },
    {
      code: "efficiency_mgmt",
      name: "Efficiency Management",
      category: "management" as const,
      description: "Reduces maintenance costs by 15%",
      researchCost: 1500,
      prerequisites: null,
      effects: [{ type: "maintenance_reduction", value: 0.15 }],
    },
    {
      code: "corporate_culture",
      name: "Corporate Culture",
      category: "management" as const,
      description: "Improves employee morale and retention",
      researchCost: 2000,
      prerequisites: [8], // hr_training
      effects: [{ type: "morale_bonus", value: 0.2 }],
    },

    // Science Technologies
    {
      code: "research_methods",
      name: "Advanced Research Methods",
      category: "science" as const,
      description: "Increases research speed by 25%",
      researchCost: 1500,
      prerequisites: null,
      effects: [{ type: "research_speed", value: 0.25 }],
    },
    {
      code: "innovation_lab",
      name: "Innovation Laboratory",
      category: "science" as const,
      description: "Unlocks advanced production recipes",
      researchCost: 3000,
      prerequisites: [11], // research_methods
      effects: [{ type: "unlock_recipes", value: 1 }],
    },
    {
      code: "green_tech",
      name: "Green Technology",
      category: "science" as const,
      description: "Reduces operating costs and improves reputation",
      researchCost: 4000,
      prerequisites: [11], // research_methods
      effects: [{ type: "operating_cost_reduction", value: 0.1 }, { type: "reputation_bonus", value: 5 }],
    },
  ];

  await db.insert(technologies).values(techData);
}
