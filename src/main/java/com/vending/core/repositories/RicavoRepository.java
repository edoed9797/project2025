package com.vending.core.repositories;

import com.vending.core.models.Ricavo;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione dei ricavi delle macchine distributrici.
 * Gestisce le operazioni CRUD per la tabella 'ricavo' e utilizza la vista
 * 'ricavigiornalieri' per le statistiche aggregate.
 */
public class RicavoRepository {
    private final DatabaseConnection dbConnection;

    /**
     * Costruttore del repository.
     */
    public RicavoRepository() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Trova tutti i ricavi ordinati per data.
     *
     * @return lista di tutti i ricavi
     */
    public List<Ricavo> findAll() {
        List<Ricavo> ricavi = new ArrayList<>();
        String sql = "SELECT r.*, i.Nome as NomeIstituto " +
                    "FROM ricavo r " +
                    "JOIN macchina m ON r.ID_Macchina = m.ID_Macchina " +
                    "JOIN istituto i ON m.ID_Istituto = i.ID_Istituto " +
                    "ORDER BY r.DataOra DESC";
        
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                ricavi.add(mapResultSetToRicavo(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero dei ricavi", e);
        }
        return ricavi;
    }

    /**
     * Trova i ricavi di una specifica macchina.
     *
     * @param macchinaId ID della macchina
     * @return lista dei ricavi della macchina
     */
    public List<Ricavo> findByMacchinaId(int macchinaId) {
        List<Ricavo> ricavi = new ArrayList<>();
        String sql = "SELECT r.*, i.Nome as NomeIstituto " +
                    "FROM ricavo r " +
                    "JOIN macchina m ON r.ID_Macchina = m.ID_Macchina " +
                    "JOIN istituto i ON m.ID_Istituto = i.ID_Istituto " +
                    "WHERE r.ID_Macchina = ? " +
                    "ORDER BY r.DataOra DESC";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, macchinaId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ricavi.add(mapResultSetToRicavo(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero dei ricavi della macchina", e);
        }
        return ricavi;
    }

    /**
     * Trova i ricavi giornalieri di un istituto.
     *
     * @param istitutoId ID dell'istituto
     * @param data data di riferimento
     * @return ricavo giornaliero
     */
    public Optional<Double> findRicavoGiornaliero(int istitutoId, LocalDate data) {
        String sql = "SELECT SUM(RicavoGiornaliero) as Totale FROM ricavigiornalieri " +
                    "WHERE ID_Istituto = ? AND Data = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, istitutoId);
            stmt.setDate(2, java.sql.Date.valueOf(data));
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.ofNullable(rs.getDouble("Totale"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il calcolo del ricavo giornaliero", e);
        }
        return Optional.empty();
    }

    /**
     * Salva un nuovo ricavo.
     *
     * @param ricavo ricavo da salvare
     * @return ricavo salvato con ID generato
     */
    public Ricavo save(Ricavo ricavo) {
        String sql = "INSERT INTO ricavo (ID_Macchina, Importo, DataOra) VALUES (?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, ricavo.getMacchinaId());
            stmt.setDouble(2, ricavo.getImporto());
            stmt.setTimestamp(3, Timestamp.valueOf(ricavo.getDataOra()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La creazione del ricavo è fallita");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ricavo.setId(generatedKeys.getInt(1));
                    return ricavo;
                } else {
                    throw new SQLException("La creazione del ricavo è fallita, nessun ID ottenuto");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il salvataggio del ricavo", e);
        }
    }

    /**
     * Calcola il totale dei ricavi di una macchina.
     *
     * @param macchinaId ID della macchina
     * @return totale dei ricavi
     */
    public Double getTotaleRicaviByMacchina(int macchinaId) {
        String sql = "SELECT SUM(Importo) as totale FROM ricavo WHERE ID_Macchina = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, macchinaId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("totale");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il calcolo del totale ricavi", e);
        }
    }

    /**
     * Calcola il totale dei ricavi in un periodo.
     *
     * @param dataInizio data iniziale del periodo
     * @param dataFine data finale del periodo
     * @return totale dei ricavi nel periodo
     */
    public Double getTotaleRicaviPeriodo(LocalDateTime dataInizio, LocalDateTime dataFine) {
        String sql = "SELECT SUM(Importo) as totale FROM ricavo WHERE DataOra BETWEEN ? AND ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(dataInizio));
            stmt.setTimestamp(2, Timestamp.valueOf(dataFine));
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("totale");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il calcolo del totale ricavi del periodo", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto Ricavo.
     */
    private Ricavo mapResultSetToRicavo(ResultSet rs) throws SQLException {
        Ricavo ricavo = new Ricavo();
        ricavo.setId(rs.getInt("ID_Ricavo"));
        ricavo.setMacchinaId(rs.getInt("ID_Macchina"));
        ricavo.setNomeIstituto(rs.getString("NomeIstituto"));
        ricavo.setImporto(rs.getDouble("Importo"));
        ricavo.setDataOra(rs.getTimestamp("DataOra").toLocalDateTime());
        return ricavo;
    }
}