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
-- Table structure for table `manutenzione`
--

DROP TABLE IF EXISTS `manutenzione`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `manutenzione` (
  `ID_Manutenzione` int NOT NULL AUTO_INCREMENT,
  `ID_Macchina` int NOT NULL,
  `ID_Tecnico` int DEFAULT NULL,
  `TipoIntervento` varchar(50) NOT NULL,
  `Descrizione` text,
  `DataRichiesta` datetime NOT NULL,
  `DataCompletamento` datetime DEFAULT NULL,
  `Stato` varchar(20) NOT NULL,
  `Note` text,
  `Urgenza` varchar(10) DEFAULT 'MEDIA',
  PRIMARY KEY (`ID_Manutenzione`),
  KEY `FK_Manutenzione_Macchina` (`ID_Macchina`),
  KEY `FK_Manutenzione_Tecnico` (`ID_Tecnico`),
  CONSTRAINT `FK_Manutenzione_Macchina` FOREIGN KEY (`ID_Macchina`) REFERENCES `macchina` (`ID_Macchina`),
  CONSTRAINT `FK_Manutenzione_Tecnico` FOREIGN KEY (`ID_Tecnico`) REFERENCES `utente` (`ID_Utente`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
-- Table structure for table `ruolo`
--

DROP TABLE IF EXISTS `ruolo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ruolo` (
  `ID_Ruolo` int NOT NULL AUTO_INCREMENT,
  `NomeRuolo` varchar(255) NOT NULL,
  PRIMARY KEY (`ID_Ruolo`),
  UNIQUE KEY `NomeRuolo` (`NomeRuolo`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
-- Table structure for table `utente`
--

DROP TABLE IF EXISTS `utente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utente` (
  `ID_Utente` int NOT NULL AUTO_INCREMENT,
  `Nome` varchar(255) DEFAULT NULL,
  `Ruolo` varchar(255) DEFAULT NULL,
  `ID_Ruolo` int DEFAULT NULL,
  PRIMARY KEY (`ID_Utente`),
  KEY `ID_Ruolo` (`ID_Ruolo`),
  CONSTRAINT `utente_ibfk_1` FOREIGN KEY (`ID_Ruolo`) REFERENCES `ruolo` (`ID_Ruolo`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-04 23:34:26
