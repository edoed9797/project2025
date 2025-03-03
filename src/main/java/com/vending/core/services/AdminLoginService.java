package com.vending.core.services;

import com.vending.core.models.AdminLogin;
import com.vending.core.models.Utente;
import com.vending.core.repositories.AdminLoginRepository;
import com.vending.core.repositories.UtenteRepository;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servizio che gestisce la logica di business per l'autenticazione e la gestione
 * degli accessi amministrativi al sistema.
 */
public class AdminLoginService {
    private final AdminLoginRepository adminLoginRepository;
    private final UtenteRepository utenteRepository;

    /**
     * Costruttore del servizio.
     *
     * @param adminLoginRepository repository per l'accesso ai dati di AdminLogin
     * @param utenteRepository repository per l'accesso ai dati degli Utenti
     */
    public AdminLoginService(AdminLoginRepository adminLoginRepository, UtenteRepository utenteRepository) {
        this.adminLoginRepository = adminLoginRepository;
        this.utenteRepository = utenteRepository;
    }

    /**
     * Autentica un utente utilizzando username e password.
     *
     * @param username nome utente
     * @param password password in chiaro
     * @return Optional contenente l'AdminLogin se l'autenticazione ha successo
     */
    public Optional<AdminLogin> autenticaUtente(String username, String password) {
        Optional<AdminLogin> loginOpt = adminLoginRepository.findByUsername(username);
        
        if (loginOpt.isPresent()) {
            AdminLogin login = loginOpt.get();
            if (login.verificaPassword(password)) {
                login.aggiornaUltimoAccesso();
                adminLoginRepository.update(login);
                return Optional.of(login);
            }
        }
        return Optional.empty();
    }

    /**
     * Crea un nuovo accesso amministrativo per un utente.
     *
     * @param utenteId ID dell'utente
     * @param username nome utente desiderato
     * @param password password in chiaro
     * @return AdminLogin creato
     * @throws IllegalArgumentException se l'utente non esiste o lo username è già in uso
     */
    public AdminLogin creaAccessoAmministrativo(int utenteId, String username, String password) {
        // Verifica esistenza utente
        Utente utente = utenteRepository.findById(utenteId)
            .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        // Verifica unicità username
        if (adminLoginRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username già in uso");
        }

        // Crea nuovo accesso
        AdminLogin adminLogin = new AdminLogin(utenteId, username, password);
        adminLogin.setUtente(utente);
        
        return adminLoginRepository.save(adminLogin);
    }

    /**
     * Modifica la password di un accesso amministrativo.
     *
     * @param adminLoginId ID dell'accesso amministrativo
     * @param vecchiaPassword password attuale
     * @param nuovaPassword nuova password
     * @return true se la modifica ha successo
     * @throws IllegalArgumentException se l'accesso non esiste o la vecchia password è errata
     */
    public boolean modificaPassword(int adminLoginId, String vecchiaPassword, String nuovaPassword) {
        AdminLogin login = adminLoginRepository.findById(adminLoginId)
            .orElseThrow(() -> new IllegalArgumentException("Accesso amministrativo non trovato"));

        if (!login.verificaPassword(vecchiaPassword)) {
            throw new IllegalArgumentException("Password attuale non corretta");
        }

        login.setPassword(nuovaPassword);
        adminLoginRepository.update(login);
        return true;
    }

    /**
     * Disattiva un accesso amministrativo.
     *
     * @param adminLoginId ID dell'accesso amministrativo
     * @return true se la disattivazione ha successo
     */
    public boolean disattivaAccesso(int adminLoginId) {
        return adminLoginRepository.delete(adminLoginId);
    }

    /**
     * Verifica se un username è disponibile.
     *
     * @param username username da verificare
     * @return true se lo username è disponibile
     */
    public boolean isUsernameDisponibile(String username) {
        return adminLoginRepository.findByUsername(username).isEmpty();
    }

    /**
     * Recupera l'accesso amministrativo di un utente.
     *
     * @param utenteId ID dell'utente
     * @return Optional contenente l'AdminLogin se esistente
     */
    public Optional<AdminLogin> getAccessoPerUtente(int utenteId) {
        return adminLoginRepository.findByUtenteId(utenteId);
    }

    /**
     * Verifica se un utente ha accesso amministrativo.
     *
     * @param utenteId ID dell'utente
     * @return true se l'utente ha un accesso amministrativo
     */
    public boolean haAccessoAmministrativo(int utenteId) {
        return adminLoginRepository.findByUtenteId(utenteId).isPresent();
    }

    /**
     * Recupera l'ultima data di accesso di un utente.
     *
     * @param username username dell'utente
     * @return Optional contenente la data dell'ultimo accesso
     */
    public Optional<LocalDateTime> getUltimoAccesso(String username) {
        return adminLoginRepository.findByUsername(username)
            .map(AdminLogin::getUltimoAccesso);
    }
}