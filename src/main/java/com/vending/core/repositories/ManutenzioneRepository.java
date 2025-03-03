package com.vending.core.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vending.core.models.Manutenzione;

/**
 * Repository per la gestione delle manutenzioni delle macchine distributrici.
 * Gestisce le interazioni con la tabella 'manutenzione' per tenere traccia degli interventi di manutenzione.
 */
public class ManutenzioneRepository {
    private final DatabaseConnection dbConnection;

    /**
     * Costruttore del repository.
     */
    public ManutenzioneRepository() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Trova tutte le manutenzioni.
     *
     * @return lista delle manutenzioni
     */
    public List<Manutenzione> findAll() {
        List<Manutenzione> manutenzioni = new ArrayList<>();
        String sql = "SELECT * FROM manutenzione";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                manutenzioni.add(mapResultSetToManutenzione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero delle manutenzioni", e);
        }
        return manutenzioni;
    }

    /**
     * Trova una specifica manutenzione.
     *
     * @param id ID della manutenzione
     * @return Optional contenente la manutenzione se trovata
     */
    public Optional<Manutenzione> findById(int id) {
        String sql = "SELECT * FROM manutenzione WHERE ID_Manutenzione = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToManutenzione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero della manutenzione", e);
        }
        return Optional.empty();
    }

    /**
     * Recupera tutte le manutenzioni di una specifica macchina.
     *
     * @param macchinaId ID della macchina
     * @return lista delle manutenzioni della macchina
     */
    public List<Manutenzione> findByMacchinaId(int macchinaId) {
        List<Manutenzione> manutenzioni = new ArrayList<>();
        String sql = "SELECT * FROM manutenzione WHERE ID_Macchina = ? ORDER BY DataRichiesta DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, macchinaId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                manutenzioni.add(mapResultSetToManutenzione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero delle manutenzioni per macchina", e);
        }
        return manutenzioni;
    }

    /**
     * Trova tutte le manutenzioni per un istituto.
     *
     * @param istitutoId ID dell'istituto
     * @return lista delle manutenzioni nell'istituto
     */
    public List<Manutenzione> findByIstitutoId(int istitutoId) {
        List<Manutenzione> manutenzioni = new ArrayList<>();
        String sql = "SELECT m.* FROM manutenzione m " +
                     "JOIN macchina ma ON m.ID_Macchina = ma.ID_Macchina " +
                     "WHERE ma.ID_Istituto = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, istitutoId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                manutenzioni.add(mapResultSetToManutenzione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero delle manutenzioni per istituto", e);
        }
        return manutenzioni;
    }

    /**
     * Trova tutte le manutenzioni assegnate a un tecnico.
     *
     * @param tecnicoId ID del tecnico
     * @return lista delle manutenzioni assegnate al tecnico
     */
    public List<Manutenzione> findByTecnicoId(int tecnicoId) {
        List<Manutenzione> manutenzioni = new ArrayList<>();
        String sql = "SELECT * FROM manutenzione WHERE ID_Tecnico = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tecnicoId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                manutenzioni.add(mapResultSetToManutenzione(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel recupero delle manutenzioni per tecnico", e);
        }
        return manutenzioni;
    }

    /**
     * Salva una nuova manutenzione.
     *
     * @param manutenzione dati della manutenzione
     * @return manutenzione salvata
     */
    public Manutenzione save(Manutenzione manutenzione) {
        String sql = "INSERT INTO manutenzione (ID_Macchina, TipoIntervento, Descrizione, DataRichiesta, Stato, Urgenza, ID_Tecnico, Note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, manutenzione.getMacchinaId());
            stmt.setString(2, manutenzione.getTipoIntervento());
            stmt.setString(3, manutenzione.getDescrizione());
            stmt.setTimestamp(4, Timestamp.valueOf(manutenzione.getDataRichiesta()));
            stmt.setString(5, manutenzione.getStato());
            stmt.setString(6, manutenzione.getUrgenza());
            stmt.setInt(7, manutenzione.getTecnicoId());
            stmt.setString(8, manutenzione.getNote());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creazione manutenzione fallita, nessuna riga inserita.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    manutenzione.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creazione manutenzione fallita, nessun ID ottenuto.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel salvataggio della manutenzione", e);
        }
        return manutenzione;
    }

    /**
     * Aggiorna una manutenzione esistente.
     *
     * @param manutenzione dati della manutenzione da aggiornare
     * @return manutenzione aggiornata
     */
    public Manutenzione update(Manutenzione manutenzione) {
        String sql = "UPDATE manutenzione SET ID_Macchina = ?, TipoIntervento = ?, Descrizione = ?, " +
                     "DataRichiesta = ?, DataCompletamento = ?, Stato = ?, Urgenza = ?, ID_Tecnico = ?, Note = ? " +
                     "WHERE ID_Manutenzione = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, manutenzione.getMacchinaId());
            stmt.setString(2, manutenzione.getTipoIntervento());
            stmt.setString(3, manutenzione.getDescrizione());
            stmt.setTimestamp(4, Timestamp.valueOf(manutenzione.getDataRichiesta()));
            stmt.setTimestamp(5, manutenzione.getDataCompletamento() != null ? 
                Timestamp.valueOf(manutenzione.getDataCompletamento()) : null);
            stmt.setString(6, manutenzione.getStato());
            stmt.setString(7, manutenzione.getUrgenza());
            stmt.setInt(8, manutenzione.getTecnicoId());
            stmt.setString(9, manutenzione.getNote());
            stmt.setInt(10, manutenzione.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Aggiornamento manutenzione fallito, nessuna riga aggiornata.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'aggiornamento della manutenzione", e);
        }
        return manutenzione;
    }

    /**
     * Completa una manutenzione.
     *
     * @param manutenzione dati della manutenzione completata
     * @return manutenzione completata
     */
    public Manutenzione completaManutenzione(Manutenzione manutenzione) {
        String sql = "UPDATE manutenzione SET Stato = 'COMPLETATA', DataCompletamento = ?, Note = ?, ID_Tecnico = ? " +
                     "WHERE ID_Manutenzione = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, manutenzione.getNote());
            stmt.setInt(3, manutenzione.getTecnicoId());
            stmt.setInt(4, manutenzione.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Completamento manutenzione fallito, nessuna riga aggiornata.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel completamento della manutenzione", e);
        }
        return manutenzione;
    }

    /**
     * Imposta una manutenzione come fuori servizio.
     *
     * @param manutenzioneId ID della manutenzione
     * @return true se l'operazione ha successo
     */
    public boolean setFuoriServizio(int manutenzioneId) {
        String sql = "UPDATE manutenzione SET Stato = 'FUORI_SERVIZIO' WHERE ID_Manutenzione = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, manutenzioneId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'impostazione fuori servizio", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto Manutenzione.
     */
    private Manutenzione mapResultSetToManutenzione(ResultSet rs) throws SQLException {
        Manutenzione manutenzione = new Manutenzione();
        manutenzione.setId(rs.getInt("ID_Manutenzione"));
        manutenzione.setMacchinaId(rs.getInt("ID_Macchina"));
        manutenzione.setTipoIntervento(rs.getString("TipoIntervento"));
        manutenzione.setDescrizione(rs.getString("Descrizione"));
        manutenzione.setDataRichiesta(rs.getTimestamp("DataRichiesta").toLocalDateTime());
        manutenzione.setDataCompletamento(rs.getTimestamp("DataCompletamento") != null ?
                rs.getTimestamp("DataCompletamento").toLocalDateTime() : null);
        manutenzione.setStato(rs.getString("Stato"));
        manutenzione.setUrgenza(rs.getString("Urgenza"));
        manutenzione.setTecnicoId(rs.getInt("ID_Tecnico"));
        manutenzione.setNote(rs.getString("Note"));
        return manutenzione;
    }
}