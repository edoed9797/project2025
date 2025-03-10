package com.vending.core.repositories;

import com.vending.core.models.AdminLogin;
import com.vending.core.models.Utente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione dei dati di accesso amministrativo nel database.
 * Gestisce le operazioni CRUD per la tabella 'adminlogin'.
 */
public class AdminLoginRepository {
    private final DatabaseConnection dbConnection;
    private final UtenteRepository utenteRepository;

    /**
     * Costruttore del repository.
     *
     * @param utenteRepository repository per l'accesso ai dati degli utenti
     */
    public AdminLoginRepository(UtenteRepository utenteRepository) {
        this.dbConnection = DatabaseConnection.getInstance();
        this.utenteRepository = utenteRepository;
    }

    /**
     * Trova tutti gli accessi amministrativi.
     *
     * @return lista di tutti gli accessi
     */
    public List<AdminLogin> findAll() {
        List<AdminLogin> adminLogins = new ArrayList<>();
        String sql = "SELECT * FROM adminlogin";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                AdminLogin adminLogin = mapResultSetToAdminLogin(rs);
                caricaUtente(adminLogin);
                adminLogins.add(adminLogin);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero degli accessi amministrativi", e);
        }
        return adminLogins;
    }

    /**
     * Trova un accesso amministrativo tramite ID.
     *
     * @param id ID dell'accesso amministrativo
     * @return Optional contenente l'accesso se trovato
     */
    public Optional<AdminLogin> findById(int id) {
        String sql = "SELECT * FROM adminlogin WHERE ID_AdminLogin = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
        	stmt.setInt(1, id);
        	try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Importante: mappare il ResultSet all'interno del blocco try-with-resources
                    AdminLogin adminLogin = mapResultSetToAdminLogin(rs);
                    // Carica l'utente dopo aver mappato i dati di base
                    caricaUtente(adminLogin);
                    return Optional.of(adminLogin);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero dell'accesso amministrativo", e);
        }
        return Optional.empty();
    }

    /**
     * Trova un accesso amministrativo tramite username.
     *
     * @param username username da cercare
     * @return Optional contenente l'accesso se trovato
     */
    public Optional<AdminLogin> findByUsername(String username) {
        String sql = "SELECT * FROM adminlogin WHERE Username = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Mappiamo i dati mentre il ResultSet è ancora aperto
                AdminLogin adminLogin = new AdminLogin();
                adminLogin.setId(rs.getInt("ID_AdminLogin"));
                adminLogin.setUtenteId(rs.getInt("ID_Utente"));
                adminLogin.setUsername(rs.getString("Username"));
                adminLogin.setPasswordHash(rs.getString("PasswordHash"));
                Timestamp timestamp = rs.getTimestamp("UltimoAccesso");
                if (timestamp != null) {
                    adminLogin.setUltimoAccesso(timestamp.toLocalDateTime());
                }
                
                // Carichiamo l'utente solo dopo aver estratto i dati base
                caricaUtente(adminLogin);
                return Optional.of(adminLogin);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero dell'accesso amministrativo", e);
        } finally {
            // Chiudiamo le risorse nell'ordine inverso di creazione
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    System.err.println("Errore durante la chiusura del ResultSet: " + e.getMessage());
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    System.err.println("Errore durante la chiusura dello Statement: " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Errore durante la chiusura della Connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Trova un accesso amministrativo tramite ID utente.
     *
     * @param utenteId ID dell'utente
     * @return Optional contenente l'accesso se trovato
     */
    public Optional<AdminLogin> findByUtenteId(int utenteId) {
        String sql = "SELECT * FROM adminlogin WHERE ID_Utente = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                AdminLogin adminLogin = mapResultSetToAdminLogin(rs);
                caricaUtente(adminLogin);
                return Optional.of(adminLogin);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero dell'accesso amministrativo", e);
        }
        return Optional.empty();
    }

    /**
     * Salva un nuovo accesso amministrativo.
     *
     * @param adminLogin accesso amministrativo da salvare
     * @return accesso amministrativo salvato con ID generato
     */
    public AdminLogin save(AdminLogin adminLogin) {
        String sql = "INSERT INTO adminlogin (ID_Utente, Username, PasswordHash, UltimoAccesso) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, adminLogin.getUtenteId());
            stmt.setString(2, adminLogin.getUsername());
            stmt.setString(3, adminLogin.getPasswordHash());
            stmt.setTimestamp(4, Timestamp.valueOf(adminLogin.getUltimoAccesso()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La creazione dell'accesso amministrativo è fallita");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    adminLogin.setId(generatedKeys.getInt(1));
                    return adminLogin;
                } else {
                    throw new SQLException("La creazione dell'accesso amministrativo è fallita, nessun ID ottenuto");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il salvataggio dell'accesso amministrativo", e);
        }
    }

    /**
     * Aggiorna un accesso amministrativo esistente.
     *
     * @param adminLogin accesso amministrativo da aggiornare
     * @return accesso amministrativo aggiornato
     */
    public AdminLogin update(AdminLogin adminLogin) {
        String sql = "UPDATE adminlogin SET Username = ?, PasswordHash = ?, UltimoAccesso = ? " +
                    "WHERE ID_AdminLogin = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, adminLogin.getUsername());
            stmt.setString(2, adminLogin.getPasswordHash());
            stmt.setTimestamp(3, Timestamp.valueOf(adminLogin.getUltimoAccesso()));
            stmt.setInt(4, adminLogin.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'aggiornamento dell'accesso amministrativo è fallito");
            }
            
            return adminLogin;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'aggiornamento dell'accesso amministrativo", e);
        }
    }

    /**
     * Elimina un accesso amministrativo.
     *
     * @param id ID dell'accesso amministrativo da eliminare
     * @return true se l'eliminazione ha successo
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM adminlogin WHERE ID_AdminLogin = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'eliminazione dell'accesso amministrativo", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto AdminLogin.
     */
    private AdminLogin mapResultSetToAdminLogin(ResultSet rs) throws SQLException {
        AdminLogin adminLogin = new AdminLogin();
        adminLogin.setId(rs.getInt("ID_AdminLogin"));
        adminLogin.setUtenteId(rs.getInt("ID_Utente"));
        adminLogin.setUsername(rs.getString("Username"));
        adminLogin.setPasswordHash(rs.getString("PasswordHash"));
        Timestamp timestamp = rs.getTimestamp("UltimoAccesso");
        if (timestamp != null) {
            adminLogin.setUltimoAccesso(timestamp.toLocalDateTime());
        }
        return adminLogin;
    }

    /**
     * Carica l'utente associato all'accesso amministrativo.
     */
    private void caricaUtente(AdminLogin adminLogin) {
        utenteRepository.findById(adminLogin.getUtenteId())
                .ifPresent(adminLogin::setUtente);
    }
}