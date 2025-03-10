package com.vending.core.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
* Singleton class per gestire la connessione al database MySQL.
* Implementa il pattern Singleton thread-safe per garantire una singola istanza di connessione.
*/
public class DatabaseConnection {
   private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
   private static volatile DatabaseConnection instance;
   private static final String URL = "jdbc:mysql://localhost:3306/pissir?allowPublicKeyRetrieval=true&useSSL=false";
   private static final String USER = "root";
   private static final String PASSWORD = "Pissir2024!";
   private HikariDataSource dataSource;
   
   /**
    * Costruttore privato che inizializza il connection pool.
    */
   private DatabaseConnection() {
       try {
           // Registra il driver MySQL
           Class.forName("com.mysql.cj.jdbc.Driver");
           logger.info("MySQL Driver caricato con successo");
           
           // Configura il pool di connessioni HikariCP
           HikariConfig config = new HikariConfig();
           config.setJdbcUrl(URL);
           config.setUsername(USER);
           config.setPassword(PASSWORD);
           config.setMaximumPoolSize(10); // Numero massimo di connessioni nel pool
           config.setMinimumIdle(5);      // Numero minimo di connessioni inattive
           config.setIdleTimeout(60000);  // Tempo massimo di inattività (ms)
           config.addDataSourceProperty("cachePrepStmts", "true");
           config.addDataSourceProperty("prepStmtCacheSize", "250");
           config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
           
           dataSource = new HikariDataSource(config);
           logger.info("Connection pool inizializzato con successo");
       } catch (ClassNotFoundException e) {
           logger.error("Errore nel caricamento del MySQL Driver", e);
           throw new RuntimeException("MySQL Driver non trovato: " + e.getMessage(), e);
       }
   }
   
   /**
    * Restituisce l'istanza singleton del gestore del pool di connessioni.
    * 
    * @return l'istanza di DatabaseConnection
    */
   public static DatabaseConnection getInstance() {
       if (instance == null) {
           synchronized (DatabaseConnection.class) {
               if (instance == null) {
                   instance = new DatabaseConnection();
               }
           }
       }
       return instance;
   }
   
   /**
    * Ottiene una connessione dal pool.
    * Ogni chiamata restituisce una connessione diversa e indipendente.
    * 
    * @return Connection oggetto connessione al database
    * @throws SQLException se la connessione fallisce
    */
   public Connection getConnection() throws SQLException {
       try {
           return dataSource.getConnection();
       } catch (SQLException e) {
           logger.error("Errore durante l'ottenimento di una connessione dal pool", e);
           throw new SQLException("Impossibile ottenere una connessione: " + e.getMessage(), e);
       }
   }
   
   /**
    * Chiude il pool di connessioni.
    */
   public void closePool() {
       if (dataSource != null && !dataSource.isClosed()) {
           dataSource.close();
           logger.info("Pool di connessioni chiuso con successo");
       }
   }
   
   /**
    * Verifica lo stato del pool di connessioni.
    * 
    * @return true se il pool è attivo, false altrimenti
    */
   public boolean isPoolActive() {
       return dataSource != null && !dataSource.isClosed();
   }
   
   /**
    * Verifica lo stato della connessione.
    * 
    * @return true se la connessione è attiva, false altrimenti
    
   public boolean isConnected() {
       try {
           return connection != null && !connection.isClosed() && connection.isValid(1);
       } catch (SQLException e) {
           logger.error("Errore durante la verifica della connessione", e);
           return false;
       }
   }*/
}