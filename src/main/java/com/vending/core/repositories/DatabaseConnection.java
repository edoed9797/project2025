package com.vending.core.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   
   private volatile Connection connection;
   
   /**
    * Costruttore privato che inizializza il driver MySQL.
    * @throws RuntimeException se il driver non viene trovato
    */
   private DatabaseConnection() {
       try {
           Class.forName("com.mysql.cj.jdbc.Driver");
           logger.info("MySQL Driver caricato con successo");
       } catch (ClassNotFoundException e) {
           logger.error("Errore nel caricamento del MySQL Driver", e);
           throw new RuntimeException("MySQL Driver non trovato: " + e.getMessage(), e);
       }
   }
   
   /**
    * Restituisce l'istanza singleton della connessione al database.
    * Implementa il double-checked locking per thread safety.
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
    * Restituisce una connessione attiva al database.
    * Se la connessione non esiste o è chiusa, ne crea una nuova.
    * 
    * @return Connection oggetto connessione al database
    * @throws SQLException se la connessione fallisce
    */
   public Connection getConnection() throws SQLException {
       if (connection == null || connection.isClosed()) {
           synchronized (this) {
               if (connection == null || connection.isClosed()) {
                   try {
                       connection = DriverManager.getConnection(URL, USER, PASSWORD);
                   } catch (SQLException e) {
                       logger.error("Errore durante la connessione al database", e);
                       throw new SQLException("Impossibile connettersi al database: " + e.getMessage(), e);
                   }
               }
           }
       }
       return connection;
   }
   
   /**
    * Chiude la connessione al database se attiva.
    */
   public void closeConnection() {
       if (connection != null) {
           try {
               connection.close();
               connection = null;
               logger.info("Connessione al database chiusa con successo");
           } catch (SQLException e) {
               logger.error("Errore durante la chiusura della connessione", e);
           }
       }
   }
   
   /**
    * Verifica lo stato della connessione.
    * 
    * @return true se la connessione è attiva, false altrimenti
    */
   public boolean isConnected() {
       try {
           return connection != null && !connection.isClosed() && connection.isValid(1);
       } catch (SQLException e) {
           logger.error("Errore durante la verifica della connessione", e);
           return false;
       }
   }
}