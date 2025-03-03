package com.vending.core.repositories;

import com.vending.core.models.Bevanda;
import com.vending.core.models.Cialda;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione delle bevande nel database.
 * Gestisce le operazioni CRUD per la tabella 'bevanda' e le relazioni
 * con la tabella 'bevandahacialda'.
 */
public class BevandaRepository {
    private final DatabaseConnection dbConnection;
    private final CialdaRepository cialdaRepository;

    /**
     * Costruttore del repository.
     *
     * @param cialdaRepository repository per l'accesso ai dati delle cialde
     */
    public BevandaRepository(CialdaRepository cialdaRepository) {
        this.dbConnection = DatabaseConnection.getInstance();
        this.cialdaRepository = cialdaRepository;
    }

    /**
     * Trova tutte le bevande disponibili.
     *
     * @return lista di tutte le bevande con le relative cialde
     */
    public List<Bevanda> findAll() {
        List<Bevanda> bevande = new ArrayList<>();
        String sql = "SELECT * FROM bevanda";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Bevanda bevanda = mapResultSetToBevanda(rs);
                caricaCialde(bevanda);
                bevande.add(bevanda);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle bevande", e);
        }
        return bevande;
    }

    /**
     * Trova una bevanda tramite ID.
     *
     * @param id ID della bevanda
     * @return Optional contenente la bevanda se trovata
     */
    public Optional<Bevanda> findById(int id) {
        String sql = "SELECT * FROM bevanda WHERE ID_Bevanda = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Bevanda bevanda = mapResultSetToBevanda(rs);
                caricaCialde(bevanda);
                return Optional.of(bevanda);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero della bevanda", e);
        }
        return Optional.empty();
    }

    /**
     * Trova le bevande disponibili per una specifica macchina.
     *
     * @param macchinaId ID della macchina
     * @return lista delle bevande disponibili per la macchina
     */
    public List<Bevanda> findByMacchinaId(int macchinaId) {
        List<Bevanda> bevande = new ArrayList<>();
        String sql = "SELECT b.* FROM bevanda b " +
                    "JOIN macchinahabevanda mhb ON b.ID_Bevanda = mhb.ID_Bevanda " +
                    "WHERE mhb.ID_Macchina = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, macchinaId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Bevanda bevanda = mapResultSetToBevanda(rs);
                caricaCialde(bevanda);
                bevande.add(bevanda);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle bevande della macchina", e);
        }
        return bevande;
    }

    /**
     * Salva una nuova bevanda.
     *
     * @param bevanda bevanda da salvare
     * @return bevanda salvata con ID generato
     */
    public Bevanda save(Bevanda bevanda) {
        String sql = "INSERT INTO bevanda (Nome, Prezzo) VALUES (?, ?)";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, bevanda.getNome());
                stmt.setDouble(2, bevanda.getPrezzo());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La creazione della bevanda è fallita");
                }
                
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bevanda.setId(generatedKeys.getInt(1));
                        salvaCialde(conn, bevanda);
                        conn.commit();
                        return bevanda;
                    } else {
                        throw new SQLException("La creazione della bevanda è fallita, nessun ID ottenuto");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il salvataggio della bevanda", e);
        }
    }

    /**
     * Aggiorna una bevanda esistente.
     *
     * @param bevanda bevanda da aggiornare
     * @return bevanda aggiornata
     */
    public Bevanda update(Bevanda bevanda) {
        String sql = "UPDATE bevanda SET Nome = ?, Prezzo = ? WHERE ID_Bevanda = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, bevanda.getNome());
                stmt.setDouble(2, bevanda.getPrezzo());
                stmt.setInt(3, bevanda.getId());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("L'aggiornamento della bevanda è fallito");
                }
                
                aggiornaCialde(conn, bevanda);
                conn.commit();
                return bevanda;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'aggiornamento della bevanda", e);
        }
    }

    /**
     * Elimina una bevanda.
     *
     * @param id ID della bevanda da eliminare
     * @return true se l'eliminazione ha successo
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM bevanda WHERE ID_Bevanda = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                eliminaCialde(conn, id);
                
                stmt.setInt(1, id);
                int affectedRows = stmt.executeUpdate();
                
                conn.commit();
                return affectedRows > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'eliminazione della bevanda", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto Bevanda.
     */
    private Bevanda mapResultSetToBevanda(ResultSet rs) throws SQLException {
        Bevanda bevanda = new Bevanda();
        bevanda.setId(rs.getInt("ID_Bevanda"));
        bevanda.setNome(rs.getString("Nome"));
        bevanda.setPrezzo(rs.getDouble("Prezzo"));
        return bevanda;
    }

    /**
     * Carica le cialde associate alla bevanda.
     */
    private void caricaCialde(Bevanda bevanda) throws SQLException {
        String sql = "SELECT c.* FROM cialda c " +
                    "JOIN bevandahacialda bhc ON c.ID_Cialda = bhc.ID_Cialda " +
                    "WHERE bhc.ID_Bevanda = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, bevanda.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Cialda cialda = new Cialda();
                cialda.setId(rs.getInt("ID_Cialda"));
                cialda.setNome(rs.getString("Nome"));
                cialda.setTipoCialda(rs.getString("TipoCialda"));
                bevanda.getCialde().add(cialda);
            }
        }
    }

    /**
     * Salva le associazioni tra bevanda e cialde.
     */
    private void salvaCialde(Connection conn, Bevanda bevanda) throws SQLException {
        String sql = "INSERT INTO bevandahacialda (ID_Bevanda, ID_Cialda) VALUES (?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Cialda cialda : bevanda.getCialde()) {
                stmt.setInt(1, bevanda.getId());
                stmt.setInt(2, cialda.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Aggiorna le associazioni tra bevanda e cialde.
     */
    private void aggiornaCialde(Connection conn, Bevanda bevanda) throws SQLException {
        String deleteSQL = "DELETE FROM bevandahacialda WHERE ID_Bevanda = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
            stmt.setInt(1, bevanda.getId());
            stmt.executeUpdate();
        }
        salvaCialde(conn, bevanda);
    }

    /**
     * Elimina tutte le associazioni tra bevanda e cialde.
     */
    private void eliminaCialde(Connection conn, int bevandaId) throws SQLException {
        String sql = "DELETE FROM bevandahacialda WHERE ID_Bevanda = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bevandaId);
            stmt.executeUpdate();
        }
    }
}