CREATE TABLE `contract_deliveries` (
	`id` int AUTO_INCREMENT NOT NULL,
	`contractId` int NOT NULL,
	`contractItemId` int NOT NULL,
	`shipmentId` int,
	`quantity` decimal(20,4) NOT NULL,
	`quality` decimal(5,2) NOT NULL,
	`deliveryTurn` int NOT NULL,
	`status` enum('pending','delivered','late','rejected') NOT NULL DEFAULT 'pending',
	`penaltyApplied` decimal(12,2) NOT NULL DEFAULT '0',
	`notes` text,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `contract_deliveries_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `contract_items` (
	`id` int AUTO_INCREMENT NOT NULL,
	`contractId` int NOT NULL,
	`resourceTypeId` int NOT NULL,
	`quantityPerDelivery` decimal(20,4) NOT NULL,
	`totalQuantity` decimal(20,4) NOT NULL,
	`deliveredQuantity` decimal(20,4) NOT NULL DEFAULT '0',
	`pricePerUnit` decimal(12,2) NOT NULL,
	`minQuality` decimal(5,2) NOT NULL DEFAULT '0.80',
	`fromUnitId` int,
	`toUnitId` int,
	CONSTRAINT `contract_items_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `contracts` (
	`id` int AUTO_INCREMENT NOT NULL,
	`sellerCompanyId` int NOT NULL,
	`buyerCompanyId` int NOT NULL,
	`title` varchar(256) NOT NULL,
	`description` text,
	`status` enum('draft','proposed','negotiating','active','completed','cancelled','breached') NOT NULL DEFAULT 'draft',
	`startTurn` int NOT NULL,
	`endTurn` int NOT NULL,
	`deliveryFrequency` enum('per_turn','weekly','monthly','quarterly','one_time') NOT NULL DEFAULT 'per_turn',
	`latePenaltyPercent` decimal(5,2) NOT NULL DEFAULT '5.00',
	`qualityPenaltyPercent` decimal(5,2) NOT NULL DEFAULT '10.00',
	`breachPenalty` decimal(20,2) NOT NULL DEFAULT '0',
	`totalValue` decimal(20,2) NOT NULL DEFAULT '0',
	`totalDelivered` decimal(20,2) NOT NULL DEFAULT '0',
	`proposedBy` int NOT NULL,
	`signedAt` timestamp,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `contracts_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `quality_inspections` (
	`id` int AUTO_INCREMENT NOT NULL,
	`businessUnitId` int NOT NULL,
	`resourceTypeId` int NOT NULL,
	`batchSize` decimal(20,4) NOT NULL,
	`sampleSize` decimal(20,4) NOT NULL,
	`measuredQuality` decimal(5,2) NOT NULL,
	`passThreshold` decimal(5,2) NOT NULL DEFAULT '0.80',
	`passed` boolean NOT NULL,
	`defectsFound` int NOT NULL DEFAULT 0,
	`defectTypes` json,
	`inspectorQualification` decimal(5,2) NOT NULL DEFAULT '1.00',
	`notes` text,
	`turn` int NOT NULL,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `quality_inspections_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `quality_standards` (
	`id` int AUTO_INCREMENT NOT NULL,
	`companyId` int NOT NULL,
	`inputQualityThreshold` decimal(5,2) NOT NULL DEFAULT '0.70',
	`outputQualityTarget` decimal(5,2) NOT NULL DEFAULT '0.90',
	`inspectionFrequency` enum('none','random','periodic','every_batch') NOT NULL DEFAULT 'periodic',
	`inspectionRigor` int NOT NULL DEFAULT 50,
	`qualityBonusEnabled` boolean NOT NULL DEFAULT true,
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `quality_standards_id` PRIMARY KEY(`id`),
	CONSTRAINT `quality_standards_companyId_unique` UNIQUE(`companyId`)
);
--> statement-breakpoint
CREATE TABLE `shipments` (
	`id` int AUTO_INCREMENT NOT NULL,
	`companyId` int NOT NULL,
	`fromUnitId` int NOT NULL,
	`toUnitId` int NOT NULL,
	`routeId` int NOT NULL,
	`resourceTypeId` int NOT NULL,
	`quantity` decimal(20,4) NOT NULL,
	`quality` decimal(5,2) NOT NULL DEFAULT '1.00',
	`shippingCost` decimal(12,2) NOT NULL,
	`status` enum('pending','in_transit','delivered','delayed','lost') NOT NULL DEFAULT 'pending',
	`departureTurn` int NOT NULL,
	`expectedArrivalTurn` int NOT NULL,
	`actualArrivalTurn` int,
	`contractId` int,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `shipments_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `supply_routes` (
	`id` int AUTO_INCREMENT NOT NULL,
	`fromCityId` int NOT NULL,
	`toCityId` int NOT NULL,
	`distance` decimal(10,2) NOT NULL,
	`baseShippingCost` decimal(12,2) NOT NULL,
	`transitTime` int NOT NULL DEFAULT 1,
	`routeType` enum('road','rail','sea','air') NOT NULL DEFAULT 'road',
	`reliability` decimal(5,2) NOT NULL DEFAULT '0.95',
	`maxCapacityPerTurn` decimal(12,2) NOT NULL DEFAULT '10000',
	`isActive` boolean NOT NULL DEFAULT true,
	CONSTRAINT `supply_routes_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `turn_processing_log` (
	`id` int AUTO_INCREMENT NOT NULL,
	`turn` int NOT NULL,
	`phase` enum('start','production','shipments','contracts','salaries','maintenance','taxes','events','complete') NOT NULL,
	`status` enum('pending','processing','completed','failed') NOT NULL DEFAULT 'pending',
	`details` json,
	`error` text,
	`startedAt` timestamp,
	`completedAt` timestamp,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `turn_processing_log_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `warehouses` (
	`id` int AUTO_INCREMENT NOT NULL,
	`businessUnitId` int NOT NULL,
	`storageCapacity` decimal(12,2) NOT NULL DEFAULT '1000',
	`usedCapacity` decimal(12,2) NOT NULL DEFAULT '0',
	`temperatureControlled` boolean NOT NULL DEFAULT false,
	`securityLevel` int NOT NULL DEFAULT 1,
	`handlingEfficiency` decimal(5,2) NOT NULL DEFAULT '1.00',
	`operatingCostPerTurn` decimal(12,2) NOT NULL DEFAULT '500',
	`preservationBonus` decimal(5,2) NOT NULL DEFAULT '0.00',
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `warehouses_id` PRIMARY KEY(`id`),
	CONSTRAINT `warehouses_businessUnitId_unique` UNIQUE(`businessUnitId`)
);
