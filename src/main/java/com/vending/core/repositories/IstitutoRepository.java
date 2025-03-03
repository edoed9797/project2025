package com.vending.core.repositories;

import com.vending.core.models.Istituto;
import com.vending.core.models.Macchina;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione degli istituti nel database.
 * Gestisce le operazioni CRUD per la tabella 'istituto' e le relazioni
 * con la tabella 'macchina'.
 */
public class IstitutoRepository {
    private final DatabaseConnection dbConnection;
    private final MacchinaRepository macchinaRepository;

    /**
     * Costruttore del repository.
     *
     * @param macchinaRepository repository per l'accesso ai dati delle macchine
     */
    public IstitutoRepository(MacchinaRepository macchinaRepository) {
        this.dbConnection = DatabaseConnection.getInstance();
        this.macchinaRepository = macchinaRepository;
    }

    /**
     * Trova tutti gli istituti.
     *
     * @return lista di tutti gli istituti con le relative macchine
     */
    public List<Istituto> findAll() throws SQLException {
        List<Istituto> istituti = new ArrayList<>();
        String sql = "SELECT * FROM istituto";
        
        try (Connection conn = dbConnection.getConnection()) {
            // Lista temporanea per gli istituti base
            List<Istituto> istitutiTemp = new ArrayList<>();
            
            // Prima recupera tutti gli istituti base
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Istituto istituto = mapResultSetToIstituto(rs);
                    istitutiTemp.add(istituto);
                }
            }
            
            // Poi carica le macchine per ogni istituto
            for (Istituto istituto : istitutiTemp) {
                caricaMacchine(istituto);
				istituti.add(istituto);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero degli istituti", e);
        }
        
        return istituti;
    }

    /**
     * Trova un istituto tramite ID.
     *
     * @param id ID dell'istituto
     * @return Optional contenente l'istituto se trovato
     */
    public Optional<Istituto> findById(int id) {
        String sql = "SELECT * FROM istituto WHERE ID_Istituto = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Istituto istituto = mapResultSetToIstituto(rs);
                caricaMacchine(istituto);
                return Optional.of(istituto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il recupero dell'istituto", e);
        }
        return Optional.empty();
    }

    /**
     * Salva un nuovo istituto.
     * Recupera l'ultimo ID presente e imposta l'ID del nuovo istituto come last ID + 1.
     *
     * @param istituto istituto da salvare
     * @return istituto salvato con ID generato
     * @throws IllegalArgumentException se l'istituto è null o contiene dati non validi
     * @throws RuntimeException se si verifica un errore durante il salvataggio
     */
    public Istituto save(Istituto istituto) {
        if (istituto == null) {
            throw new IllegalArgumentException("L'istituto non può essere null");
        }

        // Verifica che i campi obbligatori non siano vuoti o nulli
        if (istituto.getNome() == null || istituto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Il nome dell'istituto non può essere vuoto");
        }
        if (istituto.getIndirizzo() == null || istituto.getIndirizzo().trim().isEmpty()) {
            throw new IllegalArgumentException("L'indirizzo dell'istituto non può essere vuoto");
        }

        // Query per ottenere l'ultimo ID
        String lastIdQuery = "SELECT MAX(ID_Istituto) AS lastId FROM istituto";
        String insertSql = "INSERT INTO istituto (ID_Istituto, Nome, Indirizzo) VALUES (?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            // Recupera l'ultimo ID
            ResultSet rs = stmt.executeQuery(lastIdQuery);
            int lastId = 0;
            if (rs.next()) {
                lastId = rs.getInt("lastId");
            }

            // Imposta l'ID del nuovo istituto come lastId + 1
            int newId = lastId + 1;
            istituto.setId(newId);

            // Esegui l'inserimento
            insertStmt.setInt(1, newId);
            insertStmt.setString(2, istituto.getNome());
            insertStmt.setString(3, istituto.getIndirizzo());

            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creazione dell'istituto fallita, nessuna riga modificata");
            }

            return istituto;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il salvataggio dell'istituto", e);
        }
    }

    /**
     * Aggiorna un istituto esistente.
     *
     * @param istituto istituto da aggiornare
     * @return istituto aggiornato
     * @throws IllegalArgumentException se l'istituto è null o contiene dati non validi
     * @throws RuntimeException se si verifica un errore durante l'aggiornamento
     */
    public Istituto update(Istituto istituto) {
        if (istituto == null) {
            throw new IllegalArgumentException("L'istituto non può essere null");
        }

        // Verifica che i campi obbligatori non siano vuoti o nulli
        if (istituto.getNome() == null || istituto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Il nome dell'istituto non può essere vuoto");
        }
        if (istituto.getIndirizzo() == null || istituto.getIndirizzo().trim().isEmpty()) {
            throw new IllegalArgumentException("L'indirizzo dell'istituto non può essere vuoto");
        }
        if (istituto.getId() <= 0) {
            throw new IllegalArgumentException("L'ID dell'istituto non è valido");
        }

        String sql = "UPDATE istituto SET Nome = ?, Indirizzo = ? WHERE ID_Istituto = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, istituto.getNome());
            stmt.setString(2, istituto.getIndirizzo());
            stmt.setInt(3, istituto.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("L'aggiornamento dell'istituto è fallito, nessuna riga modificata");
            }
            return istituto;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'aggiornamento dell'istituto", e);
        }
    }

    /**
     * Elimina un istituto.
     *
     * @param id ID dell'istituto da eliminare
     * @return true se l'eliminazione ha successo
     * @throws SQLException 
     */
    public boolean delete(int id) throws SQLException {
        // Verifica che non ci siano macchine associate
        List<Macchina> macchine = macchinaRepository.findByIstitutoId(id);
        if (!macchine.isEmpty()) {
            throw new IllegalStateException("Non e' possibile eliminare un istituto con macchine associate");
        }

        String sql = "DELETE FROM istituto WHERE ID_Istituto = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante l'eliminazione dell'istituto", e);
        }
    }

    /**
     * Converte un ResultSet in un oggetto Istituto.
     */
    private Istituto mapResultSetToIstituto(ResultSet rs) throws SQLException {
        Istituto istituto = new Istituto();
        istituto.setId(rs.getInt("ID_Istituto"));
        istituto.setNome(rs.getString("Nome"));
        istituto.setIndirizzo(rs.getString("Indirizzo"));
        istituto.setMacchine(new ArrayList<>()); // Inizializza la lista delle macchine
        return istituto;
    }

    /**
     * Carica le macchine associate all'istituto.
     * @throws SQLException 
     */
    private void caricaMacchine(Istituto istituto) throws SQLException {
        List<Macchina> macchine = macchinaRepository.findByIstitutoId(istituto.getId());
        istituto.setMacchine(macchine);
    }

    /**
     * Verifica se esiste un istituto con il nome specificato.
     *
     * @param nome nome da verificare
     * @return true se esiste gi� un istituto con quel nome
     */
    public boolean existsByNome(String nome) {
        String sql = "SELECT COUNT(*) FROM istituto WHERE Nome = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nome);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante la verifica del nome istituto", e);
        }
        return false;
    }

    /**
     * Conta il numero di macchine attive in un istituto.
     *
     * @param id ID dell'istituto
     * @return numero di macchine attive
     */
    public int contaMacchineAttive(int id) {
        String sql = "SELECT COUNT(*) FROM macchina WHERE ID_Istituto = ? AND Stato = 1";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore durante il conteggio delle macchine attive", e);
        }
        return 0;
    }
}