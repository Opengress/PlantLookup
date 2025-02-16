BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "TPlantType" (
	"ID"	INTEGER,
	"Description"	TEXT NOT NULL UNIQUE,
	PRIMARY KEY("ID" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "TBioStatus" (
	"ID"	INTEGER,
	"Description"	TEXT NOT NULL UNIQUE,
	PRIMARY KEY("ID" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "TGrowthForm" (
	"ID"	INTEGER,
	"Description"	TEXT NOT NULL UNIQUE,
	PRIMARY KEY("ID" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "TThreatenedStatus" (
	"ID"	INTEGER,
	"Description"	TEXT NOT NULL UNIQUE,
	PRIMARY KEY("ID" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "Plants" (
	"NVSCode"	TEXT NOT NULL UNIQUE,
	"TaxonID"	INTEGER NOT NULL UNIQUE,
	"SpeciesName"	TEXT NOT NULL,
	"Family"	TEXT,
	"Genus"	TEXT,
	"Species"	TEXT,
	"BioStatusID"	INTEGER NOT NULL,
	"PlantTypeID"	INTEGER NOT NULL,
	"GrowthFormID"	INTEGER NOT NULL,
	"ThreatenedStatusID"	INTEGER NOT NULL,
	FOREIGN KEY("BioStatusID") REFERENCES "TBioStatus"("ID"),
	FOREIGN KEY("GrowthFormID") REFERENCES "TGrowthForm"("ID"),
	FOREIGN KEY("ThreatenedStatusID") REFERENCES "TThreatenedStatus"("ID"),
	FOREIGN KEY("PlantTypeID") REFERENCES "TPlantType"("ID"),
	PRIMARY KEY("NVSCode")
);
CREATE TABLE IF NOT EXISTS "Aliases" (
	"PlantNVSCode"	TEXT NOT NULL,
	"AliasName"	TEXT NOT NULL,
	FOREIGN KEY("PlantNVSCode") REFERENCES "Plants"("NVSCode"),
	PRIMARY KEY("PlantNVSCode","AliasName")
);
INSERT INTO "TPlantType" ("ID","Description") VALUES (1,'Vascular'),
 (2,'Non Vascular');
INSERT INTO "TBioStatus" ("ID","Description") VALUES (1,'Absent'),
 (2,'Exotic'),
 (3,'Indigenous Endemic'),
 (4,'Indigenous Non-Endemic'),
 (5,'Indigenous Unspecified'),
 (6,'Uncertain'),
 (7,'Unknown');
INSERT INTO "TGrowthForm" ("ID","Description") VALUES (1,'Fern'),
 (2,'Forb'),
 (3,'Graminoid'),
 (4,'GrassTree'),
 (5,'HerbaceousMixed'),
 (6,'Mistletoe'),
 (7,'Mixed'),
 (8,'NonVascular'),
 (9,'Palm'),
 (10,'Shrub'),
 (11,'SubShrub'),
 (12,'Tree'),
 (13,'TreeFern'),
 (14,'Unknown'),
 (15,'Vine'),
 (16,'WoodyMixed');
INSERT INTO "TThreatenedStatus" ("ID","Description") VALUES (1,'Coloniser'),
 (2,'Data Deficient'),
 (3,'Declining'),
 (4,'Nationally Critical'),
 (5,'Nationally Endangered'),
 (6,'Nationally Vulnerable'),
 (7,'Nationally Uncommon'),
 (8,'Not Threatened'),
 (9,'Not threatened (assumed)'),
 (10,'Recovering'),
 (11,'Relict'),
 (12,'Taxonomically Indistinct'),
 (13,'Unknown'),
 (14,'Vagrant');
COMMIT;
