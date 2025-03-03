package com.vending.core.repositories;

import com.vending.core.models.Utente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione degli utenti del sistema.
 * Gestisce le operazioni CRUD per la tabella 'utente' e le sue relazioni
 * con la tabella 'adminlogin'.
 */
public class UtenteRepository {
    private final DatabaseConnection dbConnection;

    /**
     * Costruttore del repository.
     */
    public UtenteRepository() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Trova tutti gli utenti.
     *
     * @return lista di tutti gli utenti
     */
    public List<Utente> findAll() {
        List<Utente> utenti = new ArrayList<>();
        String sql = "SELECT u.*, al.Username, al.UltimoAccesso " +
                    "FROM utente u " +
                    "LEFT JOIN adminlogin al ON u.ID_Utente = al.ID_Utente";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                utenti.add(mapResultSetToUtente(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero degli utenti", e);
        }
        return utenti;
    }

    /**
     * Trova un utente tramite ID.
     *
     * @param id ID dell'utente
     * @return Optional contenente l'utente se trovato
     */
    public Optional<Utente> findById(int id) {
        String sql = "SELECT u.*, al.Username, al.UltimoAccesso " +
                    "FROM utente u " +
                    "LEFT JOIN adminlogin al ON u.ID_Utente = al.ID_Utente " +
                    "WHERE u.ID_Utente = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUtente(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero dell'utente", e);
        }
        return Optional.empty();
    }

    /**
     * Trova un utente tramite username.
     *
     * @param username username da cercare
     * @return Optional contenente l'utente se trovato
     */
    public Utente findByUsername(String username) {
        String sql = "SELECT u.*, al.Username, al.UltimoAccesso " +
                    "FROM utente u " +
                    "JOIN adminlogin al ON u.ID_Utente = al.ID_Utente " +
                    "WHERE al.Username = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUtente(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante la ricerca dell'utente", e);
        }
        return null;
    }

    /**
     * Trova gli utenti per ruolo.
     *
     * @param ruolo ruolo da cercare
     * @return lista degli utenti con il ruolo specificato
     */
    public List<Utente> findByRuolo(String ruolo) {
        List<Utente> utenti = new ArrayList<>();
        String sql = "SELECT u.*, al.Username, al.UltimoAccesso " +
                    "FROM utente u " +
                    "LEFT JOIN adminlogin al ON u.ID_Utente = al.ID_Utente " +
                    "WHERE u.Ruolo = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ruolo);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                utenti.add(mapResultSetToUtente(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero degli utenti per ruolo", e);
        }
        return utenti;
    }

    /**
     * Salva un nuovo utente.
     *
     * @param utente utente da salvare
     * @return utente salvato con ID generato
     */
    public Utente save(Utente utente) {
        String sqlUtente = "INSERT INTO utente (Nome, Ruolo) VALUES (?, ?)";
        String sqlAdmin = "INSERT INTO adminlogin (ID_AdminLogin, ID_Utente, Username, PasswordHash, UltimoAccesso) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Inserimento nella tabella utente
            try (PreparedStatement stmtUtente = conn.prepareStatement(sqlUtente, Statement.RETURN_GENERATED_KEYS)) {
                stmtUtente.setString(1, utente.getNome());
                stmtUtente.setString(2, utente.getRuolo());
                
                int affectedRows = stmtUtente.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creazione utente fallita");
                }

                // Ottieni l'ID generato
                try (ResultSet generatedKeys = stmtUtente.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        utente.setId(userId);
                        
                        // Inserimento nella tabella adminlogin
                        try (PreparedStatement stmtAdmin = conn.prepareStatement(sqlAdmin)) {
                            int adminLoginId = generateNewAdminLoginId(conn); // Passa la connessione esistente
                            
                            stmtAdmin.setInt(1, adminLoginId);
                            stmtAdmin.setInt(2, userId);
                            stmtAdmin.setString(3, utente.getUsername());
                            stmtAdmin.setString(4, utente.getPasswordHash());
                            stmtAdmin.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                            
                            stmtAdmin.executeUpdate();
                        }
                        
                        conn.commit();
                        return utente;
                    } else {
                        conn.rollback();
                        throw new SQLException("Creazione utente fallita, nessun ID ottenuto");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Errore durante il salvataggio dell'utente", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore di connessione al database", e);
        }
    }

    // Metodo helper modificato per usare la stessa connessione
    private int generateNewAdminLoginId(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(ID_AdminLogin), 0) + 1 FROM adminlogin";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 1;
        }
    }

    /**
     * Aggiorna un utente esistente.
     *
     * @param utente utente da aggiornare
     * @return utente aggiornato
     */
    public Utente update(Utente utente) {
        String sql = "UPDATE utente SET Nome = ?, Ruolo = ? WHERE ID_Utente = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, utente.getNome());
            stmt.setString(2, utente.getRuolo());
            stmt.setInt(3, utente.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'aggiornamento dell'utente è fallito");
            }
            return utente;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'aggiornamento dell'utente", e);
        }
    }

    /**
     * Elimina un utente.
     *
     * @param id ID dell'utente da eliminare
     * @return true se l'eliminazione ha successo
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM utente WHERE ID_Utente = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Prima elimina eventuali login associati
                deleteAdminLogin(conn, id);
                
                // Poi elimina l'utente
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
            throw new RuntimeException("Errore durante l'eliminazione dell'utente", e);
        }
    }

    /**
     * Elimina i dati di login di un utente.
     */
    private void deleteAdminLogin(Connection conn, int utenteId) throws SQLException {
        String sql = "DELETE FROM adminlogin WHERE ID_Utente = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utenteId);
            stmt.executeUpdate();
        }
    }

    /**
     * Converte un ResultSet in un oggetto Utente.
     */
    private Utente mapResultSetToUtente(ResultSet rs) throws SQLException {
        Utente utente = new Utente();
        utente.setId(rs.getInt("ID_Utente"));
        utente.setNome(rs.getString("Nome"));
        utente.setRuolo(rs.getString("Ruolo"));
        
        // Gestione dei campi opzionali da adminlogin
        String username = rs.getString("Username");
        if (username != null) {
            utente.setUsername(username);
            utente.setUltimoAccesso(rs.getTimestamp("UltimoAccesso").toLocalDateTime());
        }
        
        return utente;
    }
}