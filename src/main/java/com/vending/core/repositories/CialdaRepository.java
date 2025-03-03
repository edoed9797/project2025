package com.vending.core.repositories;

import com.vending.core.models.Cialda;
import com.vending.core.models.QuantitaCialde;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione delle cialde nel database.
 * Gestisce le operazioni CRUD per la tabella 'cialda' e le relazioni
 * con la tabella 'quantitacialde'.
 */
public class CialdaRepository {
    private final DatabaseConnection dbConnection;

    /**
     * Costruttore del repository.
     */
    public CialdaRepository() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Trova tutte le cialde disponibili.
     *
     * @return lista di tutte le cialde
     */
    public List<Cialda> findAll() {
        List<Cialda> cialde = new ArrayList<>();
        String sql = "SELECT * FROM cialda";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                cialde.add(mapResultSetToCialda(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle cialde", e);
        }
        return cialde;
    }

    /**
     * Trova una cialda tramite ID.
     *
     * @param id ID della cialda
     * @return Optional contenente la cialda se trovata
     */
    public Optional<Cialda> findById(int id) {
        String sql = "SELECT * FROM cialda WHERE ID_Cialda = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToCialda(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero della cialda", e);
        }
        return Optional.empty();
    }

    /**
     * Trova le cialde di un determinato tipo.
     *
     * @param tipoCialda tipo di cialda da cercare
     * @return lista delle cialde del tipo specificato
     */
    public List<Cialda> findByTipo(String tipoCialda) {
        List<Cialda> cialde = new ArrayList<>();
        String sql = "SELECT * FROM cialda WHERE TipoCialda = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tipoCialda);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                cialde.add(mapResultSetToCialda(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle cialde per tipo", e);
        }
        return cialde;
    }

    /**
     * Trova la quantità disponibile di una cialda in una macchina.
     *
     * @param idCialda ID della cialda
     * @param idMacchina ID della macchina
     * @return Optional contenente la quantità di cialde se trovata
     */
    public Optional<QuantitaCialde> getQuantitaDisponibileByMacchina(int idCialda, int idMacchina) {
        String sql = "SELECT qc.*, c.Nome as NomeCialda, c.TipoCialda " +
                    "FROM quantitacialde qc " +
                    "JOIN cialda c ON qc.ID_Cialda = c.ID_Cialda " +
                    "WHERE qc.ID_Cialda = ? AND qc.ID_Macchina = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idCialda);
            stmt.setInt(2, idMacchina);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToQuantitaCialde(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero della quantità cialde", e);
        }
        return Optional.empty();
    }

    /**
     * Salva una nuova cialda.
     *
     * @param cialda cialda da salvare
     * @return cialda salvata con ID generato
     */
    public Cialda save(Cialda cialda) {
        String sql = "INSERT INTO cialda (Nome, TipoCialda) VALUES (?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, cialda.getNome());
            stmt.setString(2, cialda.getTipoCialda());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La creazione della cialda è fallita");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cialda.setId(generatedKeys.getInt(1));
                    return cialda;
                } else {
                    throw new SQLException("La creazione della cialda è fallita, nessun ID ottenuto");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il salvataggio della cialda", e);
        }
    }

    /**
     * Aggiorna una cialda esistente.
     *
     * @param cialda cialda da aggiornare
     * @return cialda aggiornata
     */
    public Cialda update(Cialda cialda) {
        String sql = "UPDATE cialda SET Nome = ?, TipoCialda = ? WHERE ID_Cialda = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cialda.getNome());
            stmt.setString(2, cialda.getTipoCialda());
            stmt.setInt(3, cialda.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'aggiornamento della cialda è fallito");
            }
            return cialda;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'aggiornamento della cialda", e);
        }
    }

    /**
     * Elimina una cialda.
     *
     * @param id ID della cialda da eliminare
     * @return true se l'eliminazione ha successo
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM cialda WHERE ID_Cialda = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Prima elimina tutte le relazioni
                eliminaQuantitaCialde(conn, id);
                
                stmt.setInt(1, id);
                int affectedRows = stmt.executeUpdate();
                
                conn.commit();
                return affectedRows > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'eliminazione della cialda", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto Cialda.
     */
    private Cialda mapResultSetToCialda(ResultSet rs) throws SQLException {
        Cialda cialda = new Cialda();
        cialda.setId(rs.getInt("ID_Cialda"));
        cialda.setNome(rs.getString("Nome"));
        cialda.setTipoCialda(rs.getString("TipoCialda"));
        return cialda;
    }

    /**
     * Converte un ResultSet in un oggetto QuantitaCialde.
     */
    private QuantitaCialde mapResultSetToQuantitaCialde(ResultSet rs) throws SQLException {
        QuantitaCialde qc = new QuantitaCialde();
        qc.setId(rs.getInt("ID_QuantitaCialde"));
        qc.setMacchinaId(rs.getInt("ID_Macchina"));
        qc.setCialdaId(rs.getInt("ID_Cialda"));
        qc.setQuantita(rs.getInt("Quantita"));
        qc.setQuantitaMassima(rs.getInt("QuantitaMassima"));
        qc.setNomeCialda(rs.getString("NomeCialda"));
        qc.setTipoCialda(rs.getString("TipoCialda"));
        return qc;
    }

    /**
     * Elimina tutte le quantità cialde associate a una cialda.
     */
    private void eliminaQuantitaCialde(Connection conn, int cialdaId) throws SQLException {
        String sql = "DELETE FROM quantitacialde WHERE ID_Cialda = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cialdaId);
            stmt.executeUpdate();
        }
    }
}