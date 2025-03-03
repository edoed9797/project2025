-- MySQL dump 10.13  Distrib 8.0.38, for Win64 (x86_64)
--
-- Host: localhost    Database: pissir
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `adminlogin`
--

DROP TABLE IF EXISTS `adminlogin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `adminlogin` (
  `ID_AdminLogin` int NOT NULL,
  `ID_Utente` int DEFAULT NULL,
  `Username` varchar(255) DEFAULT NULL,
  `PasswordHash` varchar(255) DEFAULT NULL,
  `UltimoAccesso` datetime DEFAULT NULL,
  PRIMARY KEY (`ID_AdminLogin`),
  UNIQUE KEY `ID_Utente` (`ID_Utente`),
  UNIQUE KEY `Username` (`Username`),
  KEY `idx_username` (`Username`),
  CONSTRAINT `adminlogin_ibfk_1` FOREIGN KEY (`ID_Utente`) REFERENCES `utente` (`ID_Utente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `adminlogin`
--

LOCK TABLES `adminlogin` WRITE;
/*!40000 ALTER TABLE `adminlogin` DISABLE KEYS */;
/*!40000 ALTER TABLE `adminlogin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bevanda`
--

DROP TABLE IF EXISTS `bevanda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bevanda` (
  `ID_Bevanda` int NOT NULL,
  `Nome` varchar(255) DEFAULT NULL,
  `Prezzo` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`ID_Bevanda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bevanda`
--

LOCK TABLES `bevanda` WRITE;
/*!40000 ALTER TABLE `bevanda` DISABLE KEYS */;
INSERT INTO `bevanda` VALUES (1,'Espresso',0.70),(2,'Cappuccino',1.00),(3,'Te al limone',0.60),(4,'Cioccolata calda',1.20),(5,'Caffè al ginseng',1.00),(6,'Caffè d\'orzo',0.65),(7,'Te nero',0.80),(8,'Latte macchiato',1.10),(9,'Camomilla',0.90),(10,'Mocaccino',1.30);
/*!40000 ALTER TABLE `bevanda` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bevandahacialda`
--

DROP TABLE IF EXISTS `bevandahacialda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bevandahacialda` (
  `ID_BevandaHaCialda` int NOT NULL,
  `ID_Bevanda` int DEFAULT NULL,
  `ID_Cialda` int DEFAULT NULL,
  PRIMARY KEY (`ID_BevandaHaCialda`),
  KEY `ID_Bevanda` (`ID_Bevanda`),
  KEY `ID_Cialda` (`ID_Cialda`),
  CONSTRAINT `bevandahacialda_ibfk_1` FOREIGN KEY (`ID_Bevanda`) REFERENCES `bevanda` (`ID_Bevanda`),
  CONSTRAINT `bevandahacialda_ibfk_2` FOREIGN KEY (`ID_Cialda`) REFERENCES `cialda` (`ID_Cialda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bevandahacialda`
--

LOCK TABLES `bevandahacialda` WRITE;
/*!40000 ALTER TABLE `bevandahacialda` DISABLE KEYS */;
INSERT INTO `bevandahacialda` VALUES (1,1,1),(2,1,2),(3,2,1),(4,3,3),(5,4,4),(6,5,5),(7,6,7),(8,7,8),(9,8,9),(10,9,10),(11,10,1),(12,10,4),(13,2,9),(14,1,6),(15,3,6);
/*!40000 ALTER TABLE `bevandahacialda` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `bevandecialdeecosti`
--

DROP TABLE IF EXISTS `bevandecialdeecosti`;
/*!50001 DROP VIEW IF EXISTS `bevandecialdeecosti`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `bevandecialdeecosti` AS SELECT 
 1 AS `ID_Bevanda`,
 1 AS `NomeBevanda`,
 1 AS `Prezzo`,
 1 AS `Cialde`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `cialda`
--

DROP TABLE IF EXISTS `cialda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cialda` (
  `ID_Cialda` int NOT NULL,
  `Nome` varchar(255) DEFAULT NULL,
  `TipoCialda` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID_Cialda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cialda`
--

LOCK TABLES `cialda` WRITE;
/*!40000 ALTER TABLE `cialda` DISABLE KEYS */;
INSERT INTO `cialda` VALUES (1,'Arabica','Caffè'),(2,'Robusta','Caffè'),(3,'Te verde','Tè'),(4,'Cioccolato','Cacao'),(5,'Ginseng','Caffè'),(6,'Zucchero','Additivo'),(7,'Orzo','Caffè'),(8,'Te nero','Tè'),(9,'Latte','Additivo'),(10,'Camomilla','Tisana');
/*!40000 ALTER TABLE `cialda` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `disponibilitacialde`
--

DROP TABLE IF EXISTS `disponibilitacialde`;
/*!50001 DROP VIEW IF EXISTS `disponibilitacialde`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `disponibilitacialde` AS SELECT 
 1 AS `ID_Macchina`,
 1 AS `NomeIstituto`,
 1 AS `NomeCialda`,
 1 AS `Quantita`,
 1 AS `QuantitaMassima`,
 1 AS `DaRifornire`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `istituto`
--

DROP TABLE IF EXISTS `istituto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `istituto` (
  `ID_Istituto` int NOT NULL,
  `Nome` varchar(255) DEFAULT NULL,
  `Indirizzo` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID_Istituto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `istituto`
--

LOCK TABLES `istituto` WRITE;
/*!40000 ALTER TABLE `istituto` DISABLE KEYS */;
INSERT INTO `istituto` VALUES (1,'Liceo Scientifico Galileo Ferraris','Corso Montevecchio 67, Torino'),(2,'IIS Baldessano-Roccati','Viale Garibaldi 7, Carmagnola'),(3,'Liceo Classico Cavour','Corso Tassoni 15, Torino'),(4,'IIS Majorana','Via Frattini 11, Torino'),(5,'Liceo Artistico Cottini','Via Castelgomberto 20, Torino'),(6,'IIS Avogadro','Corso San Maurizio 8, Torino'),(7,'Liceo Linguistico Gioberti','Via Sant Ottavio 9, Torino'),(8,'ITIS Pininfarina','Via Ponchielli 16, Moncalieri');
/*!40000 ALTER TABLE `istituto` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `macchina`
--

DROP TABLE IF EXISTS `macchina`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `macchina` (
  `ID_Macchina` int NOT NULL,
  `ID_Istituto` int DEFAULT NULL,
  `Stato` int DEFAULT NULL,
  `CassaAttuale` decimal(10,2) DEFAULT NULL,
  `CassaMassima` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`ID_Macchina`),
  KEY `ID_Istituto` (`ID_Istituto`),
  KEY `Stato` (`Stato`),
  CONSTRAINT `macchina_ibfk_1` FOREIGN KEY (`ID_Istituto`) REFERENCES `istituto` (`ID_Istituto`),
  CONSTRAINT `macchina_ibfk_2` FOREIGN KEY (`Stato`) REFERENCES `statomacchina` (`ID_StatoMacchina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `macchina`
--

LOCK TABLES `macchina` WRITE;
/*!40000 ALTER TABLE `macchina` DISABLE KEYS */;
INSERT INTO `macchina` VALUES (1,1,1,150.50,500.00),(2,2,1,75.25,300.00),(3,3,2,0.00,400.00),(4,4,1,200.75,600.00),(5,5,3,0.00,500.00),(6,6,1,100.00,400.00),(7,7,4,50.00,300.00),(8,8,1,180.00,550.00),(9,1,5,0.00,450.00),(10,2,1,90.00,350.00);
/*!40000 ALTER TABLE `macchina` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `macchinahabevanda`
--

DROP TABLE IF EXISTS `macchinahabevanda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `macchinahabevanda` (
  `ID_MacchinaHaBevanda` int NOT NULL,
  `ID_Macchina` int DEFAULT NULL,
  `ID_Bevanda` int DEFAULT NULL,
  PRIMARY KEY (`ID_MacchinaHaBevanda`),
  KEY `ID_Macchina` (`ID_Macchina`),
  KEY `ID_Bevanda` (`ID_Bevanda`),
  CONSTRAINT `macchinahabevanda_ibfk_1` FOREIGN KEY (`ID_Macchina`) REFERENCES `macchina` (`ID_Macchina`),
  CONSTRAINT `macchinahabevanda_ibfk_2` FOREIGN KEY (`ID_Bevanda`) REFERENCES `bevanda` (`ID_Bevanda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `macchinahabevanda`
--

LOCK TABLES `macchinahabevanda` WRITE;
/*!40000 ALTER TABLE `macchinahabevanda` DISABLE KEYS */;
INSERT INTO `macchinahabevanda` VALUES (1,1,1),(2,1,2),(3,2,1),(4,2,3),(5,3,4),(6,4,5),(7,4,1),(8,5,2),(9,6,6),(10,7,7),(11,8,8),(12,9,9),(13,10,10),(14,1,3),(15,2,4);
/*!40000 ALTER TABLE `macchinahabevanda` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `macchinenegliistituti`
--

DROP TABLE IF EXISTS `macchinenegliistituti`;
/*!50001 DROP VIEW IF EXISTS `macchinenegliistituti`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `macchinenegliistituti` AS SELECT 
 1 AS `ID_Macchina`,
 1 AS `NomeIstituto`,
 1 AS `Indirizzo`,
 1 AS `StatoMacchina`,
 1 AS `CassaAttuale`,
 1 AS `CassaMassima`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `quantitacialde`
--

DROP TABLE IF EXISTS `quantitacialde`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quantitacialde` (
  `ID_QuantitaCialde` int NOT NULL,
  `ID_Macchina` int DEFAULT NULL,
  `ID_Cialda` int DEFAULT NULL,
  `Quantita` int DEFAULT NULL,
  `QuantitaMassima` int DEFAULT NULL,
  PRIMARY KEY (`ID_QuantitaCialde`),
  KEY `ID_Macchina` (`ID_Macchina`),
  KEY `ID_Cialda` (`ID_Cialda`),
  CONSTRAINT `quantitacialde_ibfk_1` FOREIGN KEY (`ID_Macchina`) REFERENCES `macchina` (`ID_Macchina`),
  CONSTRAINT `quantitacialde_ibfk_2` FOREIGN KEY (`ID_Cialda`) REFERENCES `cialda` (`ID_Cialda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quantitacialde`
--

LOCK TABLES `quantitacialde` WRITE;
/*!40000 ALTER TABLE `quantitacialde` DISABLE KEYS */;
INSERT INTO `quantitacialde` VALUES (1,1,1,50,100),(2,1,2,30,100),(3,2,3,40,80),(4,3,4,0,60),(5,4,5,25,50),(6,5,6,100,200),(7,6,7,35,70),(8,7,8,20,60),(9,8,9,15,40),(10,9,10,30,80),(11,10,1,45,90),(12,1,6,150,300);
/*!40000 ALTER TABLE `quantitacialde` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `ricavigiornalieri`
--

DROP TABLE IF EXISTS `ricavigiornalieri`;
/*!50001 DROP VIEW IF EXISTS `ricavigiornalieri`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ricavigiornalieri` AS SELECT 
 1 AS `ID_Macchina`,
 1 AS `NomeIstituto`,
 1 AS `Data`,
 1 AS `RicavoGiornaliero`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `ricavo`
--

DROP TABLE IF EXISTS `ricavo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ricavo` (
  `ID_Ricavo` int NOT NULL,
  `ID_Macchina` int DEFAULT NULL,
  `Importo` decimal(10,2) DEFAULT NULL,
  `DataOra` datetime DEFAULT NULL,
  PRIMARY KEY (`ID_Ricavo`),
  KEY `ID_Macchina` (`ID_Macchina`),
  CONSTRAINT `ricavo_ibfk_1` FOREIGN KEY (`ID_Macchina`) REFERENCES `macchina` (`ID_Macchina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ricavo`
--

LOCK TABLES `ricavo` WRITE;
/*!40000 ALTER TABLE `ricavo` DISABLE KEYS */;
INSERT INTO `ricavo` VALUES (1,1,75.50,'2023-11-10 20:00:00'),(2,2,50.25,'2023-11-10 20:00:00'),(3,4,100.75,'2023-11-10 20:00:00'),(4,6,45.00,'2024-05-10 20:00:00'),(5,8,80.00,'2024-09-25 20:00:00'),(6,10,60.50,'2024-09-25 20:00:00');
/*!40000 ALTER TABLE `ricavo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `statomacchina`
--

DROP TABLE IF EXISTS `statomacchina`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statomacchina` (
  `ID_StatoMacchina` int NOT NULL,
  `Descrizione` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID_StatoMacchina`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `statomacchina`
--

LOCK TABLES `statomacchina` WRITE;
/*!40000 ALTER TABLE `statomacchina` DISABLE KEYS */;
INSERT INTO `statomacchina` VALUES (1,'Attiva'),(2,'In manutenzione'),(3,'Fuori servizio');
/*!40000 ALTER TABLE `statomacchina` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transazione`
--

DROP TABLE IF EXISTS `transazione`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transazione` (
  `ID_Transazione` int NOT NULL,
  `ID_Macchina` int DEFAULT NULL,
  `ID_Bevanda` int DEFAULT NULL,
  `Importo` decimal(10,2) DEFAULT NULL,
  `DataOra` datetime DEFAULT NULL,
  PRIMARY KEY (`ID_Transazione`),
  KEY `ID_Macchina` (`ID_Macchina`),
  KEY `ID_Bevanda` (`ID_Bevanda`),
  CONSTRAINT `transazione_ibfk_1` FOREIGN KEY (`ID_Macchina`) REFERENCES `macchina` (`ID_Macchina`),
  CONSTRAINT `transazione_ibfk_2` FOREIGN KEY (`ID_Bevanda`) REFERENCES `bevanda` (`ID_Bevanda`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transazione`
--

LOCK TABLES `transazione` WRITE;
/*!40000 ALTER TABLE `transazione` DISABLE KEYS */;
INSERT INTO `transazione` VALUES (1,1,1,0.50,'2023-11-10 08:30:00'),(2,2,2,1.00,'2023-11-10 09:15:00'),(3,4,3,0.80,'2023-11-10 10:00:00'),(4,1,4,1.20,'2023-11-10 11:30:00'),(5,2,5,1.00,'2024-01-20 14:00:00'),(6,6,6,0.70,'2024-01-20 15:30:00'),(7,8,7,0.80,'2024-04-18 16:45:00'),(8,10,8,1.10,'2024-04-18 17:20:00'),(9,1,9,0.90,'2024-09-25 18:00:00'),(10,4,10,1.30,'2024-09-25 19:30:00');
/*!40000 ALTER TABLE `transazione` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `transazionirecenti`
--

DROP TABLE IF EXISTS `transazionirecenti`;
/*!50001 DROP VIEW IF EXISTS `transazionirecenti`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `transazionirecenti` AS SELECT 
 1 AS `ID_Transazione`,
 1 AS `ID_Macchina`,
 1 AS `NomeIstituto`,
 1 AS `NomeBevanda`,
 1 AS `Importo`,
 1 AS `DataOra`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `utente`
--

DROP TABLE IF EXISTS `utente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utente` (
  `ID_Utente` int NOT NULL,
  `Nome` varchar(255) DEFAULT NULL,
  `Ruolo` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID_Utente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `utente`
--

LOCK TABLES `utente` WRITE;
/*!40000 ALTER TABLE `utente` DISABLE KEYS */;
INSERT INTO `utente` VALUES (1,'Anna Neri','Tecnico'),(2,'Paolo Gialli','Amministratore'),(3,'Chiara Viola','Operatore');
/*!40000 ALTER TABLE `utente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `bevandecialdeecosti`
--

/*!50001 DROP VIEW IF EXISTS `bevandecialdeecosti`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `bevandecialdeecosti` AS select `b`.`ID_Bevanda` AS `ID_Bevanda`,`b`.`Nome` AS `NomeBevanda`,`b`.`Prezzo` AS `Prezzo`,group_concat(`c`.`Nome` separator ', ') AS `Cialde` from ((`bevanda` `b` join `bevandahacialda` `bhc` on((`b`.`ID_Bevanda` = `bhc`.`ID_Bevanda`))) join `cialda` `c` on((`bhc`.`ID_Cialda` = `c`.`ID_Cialda`))) group by `b`.`ID_Bevanda` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `disponibilitacialde`
--

/*!50001 DROP VIEW IF EXISTS `disponibilitacialde`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `disponibilitacialde` AS select `m`.`ID_Macchina` AS `ID_Macchina`,`i`.`Nome` AS `NomeIstituto`,`c`.`Nome` AS `NomeCialda`,`qc`.`Quantita` AS `Quantita`,`qc`.`QuantitaMassima` AS `QuantitaMassima`,(`qc`.`QuantitaMassima` - `qc`.`Quantita`) AS `DaRifornire` from (((`quantitacialde` `qc` join `macchina` `m` on((`qc`.`ID_Macchina` = `m`.`ID_Macchina`))) join `istituto` `i` on((`m`.`ID_Istituto` = `i`.`ID_Istituto`))) join `cialda` `c` on((`qc`.`ID_Cialda` = `c`.`ID_Cialda`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `macchinenegliistituti`
--

/*!50001 DROP VIEW IF EXISTS `macchinenegliistituti`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `macchinenegliistituti` AS select `m`.`ID_Macchina` AS `ID_Macchina`,`i`.`Nome` AS `NomeIstituto`,`i`.`Indirizzo` AS `Indirizzo`,`sm`.`Descrizione` AS `StatoMacchina`,`m`.`CassaAttuale` AS `CassaAttuale`,`m`.`CassaMassima` AS `CassaMassima` from ((`macchina` `m` join `istituto` `i` on((`m`.`ID_Istituto` = `i`.`ID_Istituto`))) join `statomacchina` `sm` on((`m`.`Stato` = `sm`.`ID_StatoMacchina`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `ricavigiornalieri`
--

/*!50001 DROP VIEW IF EXISTS `ricavigiornalieri`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `ricavigiornalieri` AS select `m`.`ID_Macchina` AS `ID_Macchina`,`i`.`Nome` AS `NomeIstituto`,cast(`r`.`DataOra` as date) AS `Data`,sum(`r`.`Importo`) AS `RicavoGiornaliero` from ((`ricavo` `r` join `macchina` `m` on((`r`.`ID_Macchina` = `m`.`ID_Macchina`))) join `istituto` `i` on((`m`.`ID_Istituto` = `i`.`ID_Istituto`))) group by `m`.`ID_Macchina`,`i`.`Nome`,cast(`r`.`DataOra` as date) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `transazionirecenti`
--

/*!50001 DROP VIEW IF EXISTS `transazionirecenti`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `transazionirecenti` AS select `t`.`ID_Transazione` AS `ID_Transazione`,`m`.`ID_Macchina` AS `ID_Macchina`,`i`.`Nome` AS `NomeIstituto`,`b`.`Nome` AS `NomeBevanda`,`t`.`Importo` AS `Importo`,`t`.`DataOra` AS `DataOra` from (((`transazione` `t` join `macchina` `m` on((`t`.`ID_Macchina` = `m`.`ID_Macchina`))) join `istituto` `i` on((`m`.`ID_Istituto` = `i`.`ID_Istituto`))) join `bevanda` `b` on((`t`.`ID_Bevanda` = `b`.`ID_Bevanda`))) order by `t`.`DataOra` desc limit 100 */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-01-06 19:49:41