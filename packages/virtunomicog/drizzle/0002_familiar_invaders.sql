CREATE TABLE `agent_decisions` (
	`id` int AUTO_INCREMENT NOT NULL,
	`agentId` int NOT NULL,
	`decisionType` enum('trade','negotiation','investment','hiring','partnership','conflict','cooperation') NOT NULL,
	`context` json NOT NULL,
	`options` json NOT NULL,
	`chosenOption` varchar(64) NOT NULL,
	`reasoning` text NOT NULL,
	`personalityFactors` json,
	`emotionalFactors` json,
	`outcome` enum('pending','success','failure','neutral') NOT NULL DEFAULT 'pending',
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `agent_decisions_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `agent_emotional_states` (
	`id` int AUTO_INCREMENT NOT NULL,
	`agentId` int NOT NULL,
	`happiness` int NOT NULL DEFAULT 50,
	`satisfaction` int NOT NULL DEFAULT 50,
	`stress` int NOT NULL DEFAULT 30,
	`anger` int NOT NULL DEFAULT 10,
	`fear` int NOT NULL DEFAULT 10,
	`trust` int NOT NULL DEFAULT 50,
	`needFinancialSecurity` int NOT NULL DEFAULT 50,
	`needRecognition` int NOT NULL DEFAULT 50,
	`needAutonomy` int NOT NULL DEFAULT 50,
	`needBelonging` int NOT NULL DEFAULT 50,
	`needGrowth` int NOT NULL DEFAULT 50,
	`overallMood` int NOT NULL DEFAULT 50,
	`stressLevel` int NOT NULL DEFAULT 30,
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `agent_emotional_states_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `agent_memories` (
	`id` int AUTO_INCREMENT NOT NULL,
	`agentId` int NOT NULL,
	`type` enum('experience','knowledge','trauma','achievement','relationship') NOT NULL,
	`content` text NOT NULL,
	`emotionalImpact` int NOT NULL DEFAULT 0,
	`importance` int NOT NULL DEFAULT 50,
	`relatedAgentId` int,
	`relatedCompanyId` int,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`expiresAt` timestamp,
	CONSTRAINT `agent_memories_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `agent_motivations` (
	`id` int AUTO_INCREMENT NOT NULL,
	`agentId` int NOT NULL,
	`type` enum('short_term','long_term','core_value') NOT NULL,
	`description` text NOT NULL,
	`priority` int NOT NULL DEFAULT 50,
	`progress` int NOT NULL DEFAULT 0,
	`isActive` boolean NOT NULL DEFAULT true,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`completedAt` timestamp,
	CONSTRAINT `agent_motivations_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `agent_relationships` (
	`id` int AUTO_INCREMENT NOT NULL,
	`agentId1` int NOT NULL,
	`agentId2` int NOT NULL,
	`type` enum('business','personal','professional','familial','competitive','mentor','ally','neutral','hostile') NOT NULL,
	`trust` int NOT NULL DEFAULT 50,
	`affection` int NOT NULL DEFAULT 50,
	`respect` int NOT NULL DEFAULT 50,
	`loyalty` int NOT NULL DEFAULT 50,
	`dependency` int NOT NULL DEFAULT 0,
	`tension` int NOT NULL DEFAULT 0,
	`interactionCount` int NOT NULL DEFAULT 0,
	`lastInteraction` timestamp,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `agent_relationships_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `agents` (
	`id` int AUTO_INCREMENT NOT NULL,
	`companyId` int,
	`name` varchar(128) NOT NULL,
	`type` enum('customer','supplier','employee','partner','investor','competitor') NOT NULL,
	`avatar` varchar(512),
	`bio` text,
	`openness` int NOT NULL DEFAULT 50,
	`conscientiousness` int NOT NULL DEFAULT 50,
	`extraversion` int NOT NULL DEFAULT 50,
	`agreeableness` int NOT NULL DEFAULT 50,
	`neuroticism` int NOT NULL DEFAULT 50,
	`impulsiveness` int NOT NULL DEFAULT 50,
	`riskTolerance` int NOT NULL DEFAULT 50,
	`empathy` int NOT NULL DEFAULT 50,
	`leadership` int NOT NULL DEFAULT 50,
	`independence` int NOT NULL DEFAULT 50,
	`autonomyLevel` int NOT NULL DEFAULT 50,
	`isActive` boolean NOT NULL DEFAULT true,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `agents_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `event_propagation` (
	`id` int AUTO_INCREMENT NOT NULL,
	`sourceType` enum('business','narrative','agent') NOT NULL,
	`sourceEventId` int NOT NULL,
	`sourceEventType` varchar(64) NOT NULL,
	`targetType` enum('business','narrative','agent') NOT NULL,
	`targetEventType` varchar(64) NOT NULL,
	`propagationData` json,
	`isProcessed` boolean NOT NULL DEFAULT false,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`processedAt` timestamp,
	CONSTRAINT `event_propagation_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `lore_entries` (
	`id` int AUTO_INCREMENT NOT NULL,
	`worldId` int NOT NULL,
	`category` enum('history','legend','culture','religion','politics','science','economics','geography') NOT NULL,
	`title` varchar(256) NOT NULL,
	`content` text NOT NULL,
	`isPublic` boolean NOT NULL DEFAULT true,
	`tags` json,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `lore_entries_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `scheduled_events` (
	`id` int AUTO_INCREMENT NOT NULL,
	`worldId` int,
	`type` enum('world_event','agent_action','market_change','narrative_trigger') NOT NULL,
	`triggerType` enum('time_based','condition_based','recurring') NOT NULL,
	`triggerTurn` int,
	`triggerCondition` json,
	`eventData` json NOT NULL,
	`priority` int NOT NULL DEFAULT 50,
	`isProcessed` boolean NOT NULL DEFAULT false,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `scheduled_events_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `world_events` (
	`id` int AUTO_INCREMENT NOT NULL,
	`worldId` int NOT NULL,
	`type` enum('economic','political','natural','technological','social','conflict','discovery') NOT NULL,
	`name` varchar(256) NOT NULL,
	`description` text,
	`importance` int NOT NULL DEFAULT 50,
	`effects` json,
	`affectedCityIds` json,
	`affectedResourceIds` json,
	`startTurn` int NOT NULL,
	`endTurn` int,
	`isActive` boolean NOT NULL DEFAULT true,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `world_events_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `worlds` (
	`id` int AUTO_INCREMENT NOT NULL,
	`name` varchar(128) NOT NULL,
	`genre` enum('modern','historical','futuristic','fantasy','steampunk','cyberpunk') NOT NULL DEFAULT 'modern',
	`description` text,
	`timePeriod` varchar(64),
	`technologyLevel` int NOT NULL DEFAULT 50,
	`economicSystem` enum('capitalism','socialism','mixed','feudal','post_scarcity') NOT NULL DEFAULT 'capitalism',
	`worldRules` json,
	`isActive` boolean NOT NULL DEFAULT true,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `worlds_id` PRIMARY KEY(`id`)
);
