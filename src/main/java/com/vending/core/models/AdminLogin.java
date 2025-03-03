package com.vending.core.models;

import java.time.LocalDateTime;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Classe che rappresenta le credenziali di accesso amministrativo al sistema.
 * Questa classe riflette la struttura della tabella 'adminlogin' nel database
 * e gestisce l'autenticazione degli utenti amministrativi.
 */
public class AdminLogin {
    private int id;
    private int utenteId;
    private String username;
    private String passwordHash;
    private LocalDateTime ultimoAccesso;
    private Utente utente;  // Relazione con l'entità Utente

    /**
     * Costruttore predefinito.
     */
    public AdminLogin() {
        this.ultimoAccesso = LocalDateTime.now();
    }

    /**
     * Costruttore con parametri principali.
     *
     * @param utenteId ID dell'utente associato
     * @param username Nome utente per l'accesso
     * @param password Password in chiaro che verrà crittografata
     */
    public AdminLogin(int utenteId, String username, String password) {
        this();
        this.utenteId = utenteId;
        this.username = username;
        setPassword(password);
    }

    /**
     * Restituisce l'ID univoco del login amministrativo.
     *
     * @return ID del login
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID del login amministrativo.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce l'ID dell'utente associato.
     *
     * @return ID dell'utente
     */
    public int getUtenteId() {
        return utenteId;
    }

    /**
     * Imposta l'ID dell'utente associato.
     *
     * @param utenteId Nuovo ID dell'utente
     */
    public void setUtenteId(int utenteId) {
        this.utenteId = utenteId;
    }

    /**
     * Restituisce il nome utente per l'accesso.
     *
     * @return Nome utente
     */
    public String getUsername() {
        return username;
    }

    /**
     * Imposta il nome utente per l'accesso.
     *
     * @param username Nuovo nome utente
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Restituisce l'hash della password.
     *
     * @return Hash della password
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Imposta direttamente l'hash della password.
     * Utile per operazioni di caricamento dal database o sincronizzazione.
     * 
     * @param passwordHash Hash della password da impostare
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Imposta e cripta la password.
     * La password viene sottoposta a hashing prima del salvataggio.
     *
     * @param password Password in chiaro da crittografare
     */
    public void setPassword(String password) {
        if (password != null && !password.isEmpty()) {
            this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        }
    }

    /**
     * Restituisce la data e ora dell'ultimo accesso.
     *
     * @return Data e ora dell'ultimo accesso
     */
    public LocalDateTime getUltimoAccesso() {
        return ultimoAccesso;
    }

    /**
     * Imposta la data e ora dell'ultimo accesso.
     *
     * @param ultimoAccesso Nuova data e ora dell'ultimo accesso
     */
    public void setUltimoAccesso(LocalDateTime ultimoAccesso) {
        this.ultimoAccesso = ultimoAccesso;
    }

    /**
     * Restituisce l'utente associato a questo login.
     *
     * @return Oggetto Utente associato
     */
    public Utente getUtente() {
        return utente;
    }

    /**
     * Imposta l'utente associato a questo login.
     *
     * @param utente Nuovo utente da associare
     */
    public void setUtente(Utente utente) {
        this.utente = utente;
        if (utente != null) {
            this.utenteId = utente.getId();
        }
    }

    /**
     * Verifica se la password fornita corrisponde all'hash memorizzato.
     *
     * @param plainTextPassword Password in chiaro da verificare
     * @return true se la password è corretta
     */
    public boolean verificaPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty() || passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(plainTextPassword, passwordHash);
    }

    /**
     * Aggiorna l'ultimo accesso al momento corrente.
     */
    public void aggiornaUltimoAccesso() {
        this.ultimoAccesso = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminLogin that = (AdminLogin) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "AdminLogin{" +
               "id=" + id +
               ", utenteId=" + utenteId +
               ", username='" + username + '\'' +
               ", ultimoAccesso=" + ultimoAccesso +
               '}';
    }
}