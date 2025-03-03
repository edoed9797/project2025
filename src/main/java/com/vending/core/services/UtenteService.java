package com.vending.core.services;

import com.vending.core.models.Utente;
import com.vending.core.repositories.UtenteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servizio che gestisce la logica di business relativa agli utenti del sistema.
 * Gestisce l'autenticazione, la creazione e la modifica degli account utente,
 * utilizzando la crittografia BCrypt integrata nel modello Utente.
 */
public class UtenteService {
    private final UtenteRepository utenteRepository;

    /**
     * Costruisce un nuovo servizio utenti.
     *
     * @param utenteRepository repository per l'accesso ai dati degli utenti
     */
    public UtenteService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    /**
     * Autentica un utente tramite username e password.
     * In caso di successo, aggiorna anche la data dell'ultimo accesso.
     *
     * @param username nome utente
     * @param password password in chiaro
     * @return utente autenticato o null se l'autenticazione fallisce
     * @throws RuntimeException se si verifica un errore durante l'autenticazione
     */
    public Utente autenticaUtente(String username, String password) {
        Utente utente = utenteRepository.findByUsername(username);
        if (utente != null && utente.verifyPassword(password)) {
            utente.aggiornaUltimoAccesso();
            return utenteRepository.update(utente);
        }
        return null;
    }

    /**
     * Recupera tutti gli utenti registrati nel sistema.
     *
     * @return lista di tutti gli utenti
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public List<Utente> getTuttiUtenti() {
        return utenteRepository.findAll();
    }

    /**
     * Recupera gli utenti con un determinato ruolo.
     *
     * @param ruolo ruolo degli utenti da cercare
     * @return lista degli utenti con il ruolo specificato
     * @throws IllegalArgumentException se il ruolo non è valido
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public List<Utente> getUtentiPerRuolo(String ruolo) {
        validaRuolo(ruolo);
        return utenteRepository.findByRuolo(ruolo);
    }

    /**
     * Recupera un utente specifico tramite il suo ID.
     *
     * @param id ID dell'utente da recuperare
     * @return Optional contenente l'utente se trovato
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public Optional<Utente> getUtenteById(int id) {
        return utenteRepository.findById(id);
    }

    /**
     * Crea un nuovo utente nel sistema.
     * La password viene automaticamente crittografata dal modello Utente.
     *
     * @param utente utente da creare
     * @return l'utente creato con ID generato
     * @throws IllegalArgumentException se i dati dell'utente non sono validi
     * @throws RuntimeException se si verifica un errore durante il salvataggio
     */
    public Utente creaUtente(Utente utente) {
        // Verifica che l'utente non esista già
        if (utenteRepository.findByUsername(utente.getUsername()) != null) {
            throw new IllegalArgumentException("Username già in uso");
        }

        // Valida l'utente prima di salvarlo
        validaUtente(utente);

        // Salva l'utente nel database
        return utenteRepository.save(utente);
    }

    /**
     * Aggiorna i dati di un utente esistente.
     * Se viene fornita una nuova password, questa viene crittografata.
     *
     * @param utente utente da aggiornare
     * @return l'utente aggiornato
     * @throws IllegalArgumentException se i dati dell'utente non sono validi
     * @throws RuntimeException se si verifica un errore durante l'aggiornamento
     */
    public Utente aggiornaUtente(Utente utente) {
        validaUtente(utente);
        return utenteRepository.update(utente);
    }

    /**
     * Elimina un utente dal sistema.
     *
     * @param id ID dell'utente da eliminare
     * @return true se l'eliminazione ha successo
     * @throws RuntimeException se si verifica un errore durante l'eliminazione
     */
    public boolean eliminaUtente(int id) {
        return utenteRepository.delete(id);
    }
    
    /**
     * Trova l'ID di un ruolo per nome.
     *
     * @param nomeRuolo Il nome del ruolo da cercare.
     * @return L'ID del ruolo, o -1 se non trovato.
     */
    public int trovaIdRuolo(String nomeRuolo) {
        switch (nomeRuolo) {
            case "amministratore":
                return 2;
            case "tecnico":
                return 1;
            case "operatore":
                return 3;
            default:
                return -1; // Ruolo non valido
        }
    }

    /**
     * Valida i dati di un utente.
     *
     * @param utente utente da validare
     * @throws IllegalArgumentException se i dati dell'utente non sono validi
     */
    private void validaUtente(Utente utente) {
        if (utente == null) {
            throw new IllegalArgumentException("L'utente non può essere null");
        }
        if (utente.getNome() == null || utente.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Il nome è obbligatorio");
        }
        if (utente.getUsername() == null || utente.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Lo username è obbligatorio");
        }
        if (utente.getPasswordHash() == null && 
            (utente.getId() == 0)) { // Solo per nuovi utenti
            throw new IllegalArgumentException("La password è obbligatoria per i nuovi utenti");
        }
    }

    /**
     * Valida il ruolo di un utente.
     *
     * @param ruolo ruolo da validare
     * @throws IllegalArgumentException se il ruolo non è valido
     */
    private void validaRuolo(String ruolo) {
        if (ruolo == null || ruolo.trim().isEmpty()) {
            throw new IllegalArgumentException("Il ruolo è obbligatorio");
        }
        if (!ruolo.equals("Amministratore") && !ruolo.equals("Tecnico") && !ruolo.equals("Operatore")) {
            throw new IllegalArgumentException("Ruolo non valido. I ruoli ammessi sono: Amministratore, Tecnico, Operatore");
        }
    }
    
    
}