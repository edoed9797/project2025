package com.vending.core.repositories;

import com.vending.core.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository per la gestione delle macchine distributrici nel database.
 * Gestisce tutte le operazioni CRUD e le relazioni con le altre entità
 * come bevande, cialde e stato della macchina.
 */
public class MacchinaRepository {
    private final DatabaseConnection dbConnection;

    /**
     * Costruttore del repository.
     * Inizializza la connessione al database.
     */
    public MacchinaRepository() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Recupera tutte le macchine distributrici dal database.
     * Include le informazioni sull'istituto e lo stato della macchina.
     *
     * @return Lista di tutte le macchine con i relativi dettagli
     * @throws RuntimeException se si verifica un errore durante l'accesso al database
     */
    public List<Macchina> findAll() {
        List<Macchina> macchine = new ArrayList<>();
        String sql = "SELECT macchina.*, istituto.Nome as NomeIstituto, statomacchina.Descrizione as StatoDescrizione " +
                    "FROM macchina " +
                    "JOIN istituto ON macchina.ID_Istituto = istituto.ID_Istituto " +
                    "JOIN statomacchina ON macchina.Stato = statomacchina.ID_StatoMacchina";
        
        try (Connection conn = dbConnection.getConnection()){
        	List<Macchina> macchineTemp = new ArrayList<>();
        	// Prima recupera tutte le macchine base
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Macchina macchina = mapResultSetToMacchina(rs);
                    macchineTemp.add(macchina);
                }
            }
            
            // Poi carica i dettagli per ogni macchina
            for (Macchina macchina : macchineTemp) {
                try {
                    caricaDettagliMacchina(macchina);
                    macchine.add(macchina);
                } catch (SQLException e) {
                    // Log dell'errore ma continua con le altre macchine
                    System.err.println("Errore nel caricamento dei dettagli per la macchina " + macchina.getId() + ": " + e.getMessage());
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle macchine", e);
        }
        
        return macchine;
    }

    /**
     * Trova tutte le macchine installate in un determinato istituto.
     *
     * @param istitutoId ID dell'istituto
     * @return Lista delle macchine presenti nell'istituto
     * @throws SQLException 
     * @throws RuntimeException se si verifica un errore durante l'accesso al database
     */
    public List<Macchina> findByIstitutoId(int istitutoId) throws SQLException {
        List<Macchina> macchine = new ArrayList<>();
        String sql = "SELECT macchina.*, istituto.Nome as NomeIstituto, statomacchina.Descrizione as StatoDescrizione " +
                    "FROM macchina " +
                    "JOIN istituto ON macchina.ID_Istituto = istituto.ID_Istituto " +
                    "JOIN statomacchina ON macchina.Stato = statomacchina.ID_StatoMacchina " +
                    "WHERE macchina.ID_Istituto = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
        	List<Macchina> macchineTemp = new ArrayList<>();
            stmt.setInt(1, istitutoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                	Macchina macchina = mapResultSetToMacchina(rs);
                    macchineTemp.add(macchina);
                }
            }
         // Poi carica i dettagli per ogni macchina
            for (Macchina macchina : macchineTemp) {
                try {
                    caricaDettagliMacchina(macchina);
                    macchine.add(macchina);
                } catch (SQLException e) {
                    // Log dell'errore ma continua con le altre macchine
                    System.err.println("Errore nel caricamento dei dettagli per la macchina " + macchina.getId() + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Errore durante il recupero delle macchine dell'istituto", e);
        }
        return macchine;
    }
           /* ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Macchina macchina = mapResultSetToMacchina(rs);
                caricaDettagliMacchina(macchina);
                macchine.add(macchina);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle macchine dell'istituto", e);
        }
        return macchine;
    }*/

    /**
     * Recupera una specifica macchina dal database tramite il suo ID.
     *
     * @param id ID della macchina da recuperare
     * @return La macchina trovata o null se non esiste
     * @throws RuntimeException se si verifica un errore durante l'accesso al database
     */
    public Macchina findById(int id) {
        String sql = "SELECT m.*, i.Nome as NomeIstituto, sm.Descrizione as StatoDescrizione " +
                    "FROM macchina m " +
                    "JOIN istituto i ON m.ID_Istituto = i.ID_Istituto " +
                    "JOIN statomacchina sm ON m.Stato = sm.ID_StatoMacchina " +
                    "WHERE m.ID_Macchina = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Macchina macchina = mapResultSetToMacchina(rs);
                caricaDettagliMacchina(macchina);
                return macchina;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero della macchina", e);
        }
        return null;
    }

    /**
     * Salva una nuova macchina nel database.
     * Gestisce anche il salvataggio delle relazioni con bevande e cialde.
     *
     * @param macchina La macchina da salvare
     * @return La macchina salvata con l'ID generato
     * @throws RuntimeException se si verifica un errore durante il salvataggio
     */
    public Macchina save(Macchina macchina) {
        String sql = "INSERT INTO macchina (ID_Istituto, Stato, CassaAttuale, CassaMassima) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, macchina.getIstitutoId());
                stmt.setInt(2, macchina.getStatoId());
                stmt.setDouble(3, macchina.getCassaAttuale());
                stmt.setDouble(4, macchina.getCassaMassima());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La creazione della macchina è fallita");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        macchina.setId(generatedKeys.getInt(1));
                        salvaCialde(conn, macchina);
                        salvaBevande(conn, macchina);
                        conn.commit();
                        return macchina;
                    } else {
                        throw new SQLException("La creazione della macchina è fallita, nessun ID ottenuto");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il salvataggio della macchina", e);
        }
    }
    /**
     * Aggiorna le bevande associate a una macchina.
     *
     * @param macchina La macchina di cui aggiornare le bevande
     * @param bevandeIds Lista degli ID delle bevande da associare alla macchina
     * @throws SQLException se si verifica un errore durante l'aggiornamento
     */
    public void aggiornaBevande(Macchina macchina, List<Integer> bevandeIds) throws SQLException {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Elimina le associazioni esistenti
                eliminaBevande(conn, macchina.getId());
                
                // Aggiunge le nuove associazioni
                salvaBevande(conn, macchina, bevandeIds);
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Salva le bevande associate a una macchina.
     *
     * @param conn Connessione al database
     * @param macchina La macchina di cui salvare le bevande
     * @param bevandeIds Lista degli ID delle bevande da associare alla macchina
     * @throws SQLException se si verifica un errore durante il salvataggio
     */
    private void salvaBevande(Connection conn, Macchina macchina, List<Integer> bevandeIds) throws SQLException {
        // Ottieni il prossimo ID disponibile
        int nextId = getNextMacchinaHaBevandaId(conn);
        
        String sql = "INSERT INTO macchinahabevanda (ID_MacchinaHaBevanda, ID_Macchina, ID_Bevanda) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Integer bevandaId : bevandeIds) {
                stmt.setInt(1, nextId++);
                stmt.setInt(2, macchina.getId());
                stmt.setInt(3, bevandaId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // Metodo per ottenere il prossimo ID disponibile
    private int getNextMacchinaHaBevandaId(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(ID_MacchinaHaBevanda), 0) + 1 FROM macchinahabevanda";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 1;
        }
    }
    /**
     * Aggiorna una macchina esistente nel database.
     * Aggiorna anche le relazioni con bevande e cialde.
     *
     * @param macchina La macchina da aggiornare
     * @return La macchina aggiornata
     * @throws RuntimeException se si verifica un errore durante l'aggiornamento
     */
    public Macchina update(Macchina macchina) {
        String sql = "UPDATE macchina SET Stato = ?, CassaAttuale = ?, CassaMassima = ? " +
                    "WHERE ID_Macchina = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, macchina.getStatoId());
                stmt.setDouble(2, macchina.getCassaAttuale());
                stmt.setDouble(3, macchina.getCassaMassima());
                stmt.setInt(4, macchina.getId());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("L'aggiornamento della macchina è fallito");
                }
                
                aggiornaCialde(conn, macchina);
                aggiornaBevande(conn, macchina);
                conn.commit();
                return macchina;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'aggiornamento della macchina", e);
        }
    }

    /**
     * Elimina una macchina dal database.
     * Elimina anche tutte le relazioni associate (cialde e bevande).
     *
     * @param id ID della macchina da eliminare
     * @return true se l'eliminazione ha avuto successo, false altrimenti
     * @throws RuntimeException se si verifica un errore durante l'eliminazione
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM macchina WHERE ID_Macchina = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Prima elimina tutte le relazioni
                eliminaCialde(conn, id);
                eliminaBevande(conn, id);
                
                // Poi elimina la macchina
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    int affectedRows = stmt.executeUpdate();
                    conn.commit();
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'eliminazione della macchina", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto Macchina.
     */
    private Macchina mapResultSetToMacchina(ResultSet rs) throws SQLException {
        Macchina macchina = new Macchina();
        macchina.setId(rs.getInt("ID_Macchina"));
        macchina.setIstitutoId(rs.getInt("ID_Istituto"));
        macchina.setNomeIstituto(rs.getString("NomeIstituto"));
        macchina.setStatoId(rs.getInt("Stato"));
        macchina.setStatoDescrizione(rs.getString("StatoDescrizione"));
        macchina.setCassaAttuale(rs.getDouble("CassaAttuale"));
        macchina.setCassaMassima(rs.getDouble("CassaMassima"));
        return macchina;
    }

    /**
     * Carica le cialde associate alla macchina.
     */
    private void caricaCialde(Macchina macchina) throws SQLException {
        String sql = "SELECT qc.*, c.Nome as NomeCialda, c.TipoCialda " +
                    "FROM quantitacialde qc " +
                    "JOIN cialda c ON qc.ID_Cialda = c.ID_Cialda " +
                    "WHERE qc.ID_Macchina = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, macchina.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                QuantitaCialde qc = new QuantitaCialde();
                qc.setId(rs.getInt("ID_QuantitaCialde"));
                qc.setMacchinaId(rs.getInt("ID_QuantitaCialde"));
                qc.setCialdaId(rs.getInt("ID_Cialda"));
                qc.setNomeCialda(rs.getString("NomeCialda"));
                qc.setTipoCialda(rs.getString("TipoCialda"));
                qc.setQuantita(rs.getInt("Quantita"));
                qc.setQuantitaMassima(rs.getInt("QuantitaMassima"));
                macchina.getCialde().add(qc);
            }
        }
    }
    
    /**
     * Salva le cialde associate alla macchina.
     */
    private void salvaCialde(Connection conn, Macchina macchina) throws SQLException {
        String sql = "INSERT INTO quantitacialde (ID_Macchina, ID_Cialda, Quantita, QuantitaMassima) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (QuantitaCialde qc : macchina.getCialde()) {
                stmt.setInt(1, macchina.getId());
                stmt.setInt(2, qc.getCialdaId());
                stmt.setInt(3, qc.getQuantita());
                stmt.setInt(4, qc.getQuantitaMassima());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    /**
     * Carica le bevande associate alla macchina.
     */
    private void caricaBevande(Macchina macchina) throws SQLException {
        String sql = "SELECT b.* FROM bevanda b " +
                    "JOIN macchinahabevanda mhb ON b.ID_Bevanda = mhb.ID_Bevanda " +
                    "WHERE mhb.ID_Macchina = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, macchina.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Bevanda bevanda = new Bevanda();
                bevanda.setId(rs.getInt("ID_Bevanda"));
                bevanda.setNome(rs.getString("Nome"));
                bevanda.setPrezzo(rs.getDouble("Prezzo"));
                
                macchina.getBevande().add(bevanda);
            }
        }
    }
    
    		/**
    		 * Carica le cialde che compongono ogni bevanda della macchina.
    		 * 
    		 * @param macchina la macchina di cui caricare le composizioni delle bevande
    		 * @throws SQLException se si verifica un errore nell'accesso al database
    		 */
    		private void caricaComposizioneBevande(Macchina macchina) throws SQLException {
    		    String sql = "SELECT b.ID_Bevanda, c.ID_Cialda, c.Nome, c.TipoCialda FROM bevanda b JOIN bevandahacialda bc ON b.ID_Bevanda = bc.ID_Bevanda JOIN cialda c ON bc.ID_Cialda = c.ID_Cialda WHERE b.ID_Bevanda IN (SELECT mhb.ID_Bevanda FROM macchinahabevanda mhb WHERE mhb.ID_Macchina = ?) ORDER BY b.ID_Bevanda, c.ID_Cialda";

    		    try (Connection conn = dbConnection.getConnection();
    		            PreparedStatement stmt = conn.prepareStatement(sql, 
    		                ResultSet.TYPE_SCROLL_SENSITIVE, 
    		                ResultSet.CONCUR_READ_ONLY)) {
    		           
    		           stmt.setInt(1, macchina.getId());
    		           ResultSet rs = stmt.executeQuery();

    		           // Mappa temporanea per raggruppare le cialde per ID_Bevanda
    		           Map<Integer, List<Cialda>> cialdeBevande = new HashMap<>();

    		           // Popola la mappa con tutte le cialde
    		           while (rs.next()) {
    		               int idBevanda = rs.getInt("ID_Bevanda");
    		               
    		               Cialda cialda = new Cialda();
    		               cialda.setId(rs.getInt("ID_Cialda"));
    		               cialda.setNome(rs.getString("Nome"));
    		               cialda.setTipoCialda(rs.getString("TipoCialda"));

    		               cialdeBevande.computeIfAbsent(idBevanda, k -> new ArrayList<>())
    		                           .add(cialda);
    		           }

    		           // Assegna le cialde alle bevande
    		           for (Bevanda bevanda : macchina.getBevande()) {
    		               if (bevanda.getCialde() == null) {
    		                   bevanda.setCialde(new ArrayList<>());
    		               }
    		               List<Cialda> cialdeBevanda = cialdeBevande.get(bevanda.getId());
    		               if (cialdeBevanda != null) {
    		                   bevanda.getCialde().addAll(cialdeBevanda);
    		               }
    		           }
    		       }
    		   }
    /**
     * Carica tutti i dettagli associati alla macchina.
     */
    private void caricaDettagliMacchina(Macchina macchina) throws SQLException {
        caricaCialde(macchina);
        caricaBevande(macchina);
        caricaComposizioneBevande(macchina);
    }

    /**
     * Salva le bevande associate alla macchina.
     */
    private void salvaBevande(Connection conn, Macchina macchina) throws SQLException {
        String sql = "INSERT INTO macchinahabevanda (ID_Macchina, ID_Bevanda) VALUES (?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Bevanda bevanda : macchina.getBevande()) {
                stmt.setInt(1, macchina.getId());
                stmt.setInt(2, bevanda.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Aggiorna le cialde associate alla macchina.
     * Elimina le associazioni esistenti e ne crea di nuove.
     *
     * @param conn Connessione al database
     * @param macchina Macchina di cui aggiornare le cialde
     * @throws SQLException se si verifica un errore durante l'aggiornamento
     */
    private void aggiornaCialde(Connection conn, Macchina macchina) throws SQLException {
        String deleteSQL = "DELETE FROM quantitacialde WHERE ID_Macchina = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
            stmt.setInt(1, macchina.getId());
            stmt.executeUpdate();
        }
        salvaCialde(conn, macchina);
    }

    /**
     * Aggiorna le bevande associate alla macchina.
     * Elimina le associazioni esistenti e ne crea di nuove.
     *
     * @param conn Connessione al database
     * @param macchina Macchina di cui aggiornare le bevande
     * @throws SQLException se si verifica un errore durante l'aggiornamento
     */
    private void aggiornaBevande(Connection conn, Macchina macchina) throws SQLException {
        String deleteSQL = "DELETE FROM macchinahabevanda WHERE ID_Macchina = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
            stmt.setInt(1, macchina.getId());
            stmt.executeUpdate();
        }
        salvaBevande(conn, macchina);
    }

    /**
     * Elimina tutte le cialde associate alla macchina.
     *
     * @param conn Connessione al database
     * @param macchinaId ID della macchina di cui eliminare le cialde
     * @throws SQLException se si verifica un errore durante l'eliminazione
     */
    private void eliminaCialde(Connection conn, int macchinaId) throws SQLException {
        String sql = "DELETE FROM quantitacialde WHERE ID_Macchina = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, macchinaId);
            stmt.executeUpdate();
        }
    }

    /**
     * Elimina tutte le bevande associate alla macchina.
     *
     * @param conn Connessione al database
     * @param macchinaId ID della macchina di cui eliminare le bevande
     * @throws SQLException se si verifica un errore durante l'eliminazione
     */
    private void eliminaBevande(Connection conn, int macchinaId) throws SQLException {
        String sql = "DELETE FROM macchinahabevanda WHERE ID_Macchina = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, macchinaId);
            stmt.executeUpdate();
        }
    }

    /**
     * Trova le macchine in base al loro stato.
     *
     * @param statoId ID dello stato da cercare (dalla tabella statomacchina)
     * @return Lista delle macchine nello stato specificato
     * @throws RuntimeException se si verifica un errore durante la ricerca
     */
    public List<Macchina> findByStato(int statoId) {
        List<Macchina> macchine = new ArrayList<>();
        String sql = "SELECT m.*, i.Nome as NomeIstituto, sm.Descrizione as StatoDescrizione " +
                    "FROM macchina m " +
                    "JOIN istituto i ON m.ID_Istituto = i.ID_Istituto " +
                    "JOIN statomacchina sm ON m.Stato = sm.ID_StatoMacchina " +
                    "WHERE m.Stato = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, statoId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Macchina macchina = mapResultSetToMacchina(rs);
                caricaDettagliMacchina(macchina);
                macchine.add(macchina);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle macchine per stato", e);
        }
        return macchine;
    }

    /**
     * Aggiorna lo stato di una macchina.
     *
     * @param macchinaId ID della macchina
     * @param nuovoStatoId Nuovo ID dello stato
     * @return true se l'aggiornamento ha avuto successo
     * @throws RuntimeException se si verifica un errore durante l'aggiornamento
     */
    public boolean aggiornaStato(int macchinaId, int nuovoStatoId) {
        String sql = "UPDATE macchina SET Stato = ? WHERE ID_Macchina = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nuovoStatoId);
            stmt.setInt(2, macchinaId);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'aggiornamento dello stato della macchina", e);
        }
    }
}