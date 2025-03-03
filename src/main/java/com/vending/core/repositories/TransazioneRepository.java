package com.vending.core.repositories;

import com.vending.Main;
import com.vending.core.models.Transazione;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Repository per la gestione delle transazioni delle macchine distributrici.
 * Gestisce le operazioni CRUD per la tabella 'transazione' e utilizza la vista
 * 'transazionirecenti' per le consultazioni recenti.
 */
public class TransazioneRepository {
    private final DatabaseConnection dbConnection;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Costruttore del repository.
     */
    public TransazioneRepository() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public int getLastTransactionId() {
        String sql = "SELECT MAX(ID_Transazione) as last_id FROM transazione";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int lastId = rs.getInt("last_id");
                // Se la tabella è vuota, restituisci 0
                return lastId > 0 ? lastId : 0;
            }
            return 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero dell'ultimo ID transazione", e);
        }
    }

    /**
     * Trova tutte le transazioni ordinate per data.
     *
     * @return lista di tutte le transazioni
     */
    public List<Transazione> findAll() {
        List<Transazione> transazioni = new ArrayList<>();
        String sql = "SELECT t.*, i.Nome as NomeIstituto, b.Nome as NomeBevanda " +
                    "FROM transazione t " +
                    "JOIN macchina m ON t.ID_Macchina = m.ID_Macchina " +
                    "JOIN istituto i ON m.ID_Istituto = i.ID_Istituto " +
                    "JOIN bevanda b ON t.ID_Bevanda = b.ID_Bevanda " +
                    "ORDER BY t.DataOra DESC";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                transazioni.add(mapResultSetToTransazione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle transazioni", e);
        }
        return transazioni;
    }

    /**
     * Trova una transazione tramite ID.
     *
     * @param id ID della transazione
     * @return Optional contenente la transazione se trovata
     */
    public Optional<Transazione> findById(int id) {
        String sql = "SELECT t.*, i.Nome as NomeIstituto, b.Nome as NomeBevanda " +
                    "FROM transazione t " +
                    "JOIN macchina m ON t.ID_Macchina = m.ID_Macchina " +
                    "JOIN istituto i ON m.ID_Istituto = i.ID_Istituto " +
                    "JOIN bevanda b ON t.ID_Bevanda = b.ID_Bevanda " +
                    "WHERE t.ID_Transazione = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToTransazione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero della transazione", e);
        }
        return Optional.empty();
    }

    /**
     * Trova le transazioni di una macchina.
     *
     * @param macchinaId ID della macchina
     * @return lista delle transazioni della macchina
     */
    public List<Transazione> findByMacchinaId(int macchinaId) {
        List<Transazione> transazioni = new ArrayList<>();
        String sql = "SELECT t.*, i.Nome as NomeIstituto, b.Nome as NomeBevanda " +
                    "FROM transazione t " +
                    "JOIN macchina m ON t.ID_Macchina = m.ID_Macchina " +
                    "JOIN istituto i ON m.ID_Istituto = i.ID_Istituto " +
                    "JOIN bevanda b ON t.ID_Bevanda = b.ID_Bevanda " +
                    "WHERE t.ID_Macchina = ? " +
                    "ORDER BY t.DataOra DESC";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, macchinaId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                transazioni.add(mapResultSetToTransazione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle transazioni della macchina", e);
        }
        return transazioni;
    }

    /**
     * Trova le transazioni recenti.
     * Utilizza la vista 'transazionirecenti' che mostra le ultime 100 transazioni.
     *
     * @return lista delle transazioni recenti
     */
    public List<Transazione> findTransazioniRecenti() {
        List<Transazione> transazioni = new ArrayList<>();
        String sql = "SELECT * FROM transazionirecenti";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                transazioni.add(mapResultSetToTransazione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero delle transazioni recenti", e);
        }
        return transazioni;
    }

    /**
     * Salva una nuova transazione.
     *
     * @param transazione transazione da salvare
     * @return transazione salvata con ID generato
     */
    public Transazione save(Transazione transazione) {
        String sql = "INSERT INTO transazione (ID_Macchina, ID_Bevanda, Importo, DataOra) " +
                    "VALUES (?, ?, ?, ?)";
                    
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, transazione.getMacchinaId());
            stmt.setInt(2, transazione.getBevandaId());
            stmt.setDouble(3, transazione.getImporto());
            stmt.setTimestamp(4, Timestamp.valueOf(transazione.getDataOra()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La creazione della transazione è fallita");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transazione.setId(generatedKeys.getInt(1));
                    return transazione;
                } else {
                    throw new SQLException("La creazione della transazione è fallita, nessun ID ottenuto");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il salvataggio della transazione", e);
        }
    }

    public Transazione update(Transazione transazione) {
        String sql = "UPDATE transazione SET " +
                     "ID_Macchina = ?, " +
                     "ID_Bevanda = ?, " +
                     "Importo = ?, " +
                     "DataOra = ? " +
                     "WHERE ID_Transazione = ?";
    
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, transazione.getMacchinaId());
            stmt.setInt(2, transazione.getBevandaId());
            stmt.setDouble(3, transazione.getImporto());
            stmt.setTimestamp(4, Timestamp.valueOf(transazione.getDataOra()));
            stmt.setInt(5, transazione.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'aggiornamento della transazione " + transazione.getId() + " è fallito");
            }
            
            logger.info("Transazione {} aggiornata con successo", transazione.getId());
            return transazione;
            
        } catch (SQLException e) {
            logger.error("Errore durante l'aggiornamento della transazione {}: {}", 
                        transazione.getId(), e.getMessage());
            throw new RuntimeException("Errore durante l'aggiornamento della transazione", e);
        }
    }

    /**
     * Calcola il totale delle transazioni di una macchina in un periodo.
     *
     * @param macchinaId ID della macchina
     * @param dataInizio data iniziale del periodo
     * @param dataFine data finale del periodo
     * @return totale delle transazioni
     */
    public Double calcolaTotaleMacchina(int macchinaId, LocalDateTime dataInizio, LocalDateTime dataFine) {
        String sql = "SELECT SUM(Importo) as totale FROM transazione " +
                    "WHERE ID_Macchina = ? AND DataOra BETWEEN ? AND ?";
                    
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, macchinaId);
            stmt.setTimestamp(2, Timestamp.valueOf(dataInizio));
            stmt.setTimestamp(3, Timestamp.valueOf(dataFine));
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.ofNullable(rs.getDouble("totale"))
                    .orElse(0.0);
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il calcolo del totale", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto Transazione.
     */
    private Transazione mapResultSetToTransazione(ResultSet rs) throws SQLException {
        Transazione transazione = new Transazione();
        transazione.setId(rs.getInt("ID_Transazione"));
        transazione.setMacchinaId(rs.getInt("ID_Macchina"));
        transazione.setBevandaId(rs.getInt("ID_Bevanda"));
        transazione.setNomeIstituto(rs.getString("NomeIstituto"));
        transazione.setNomeBevanda(rs.getString("NomeBevanda"));
        transazione.setImporto(rs.getDouble("Importo"));
        transazione.setDataOra(rs.getTimestamp("DataOra").toLocalDateTime());
        return transazione;
    }
}