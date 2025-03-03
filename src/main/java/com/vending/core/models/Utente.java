package com.vending.core.models;

import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;

/**
 * Classe che rappresenta un utente del sistema di distribuzione automatica.
 * Questa classe riflette la struttura delle tabelle 'utente' e 'adminlogin' nel database,
 * gestendo le informazioni dell'utente e la sicurezza delle credenziali attraverso BCrypt.
 */
public class Utente {
    private int id;
    private String nome;
    private String ruolo;
    private int ruoloId;
    private String username;        // Da adminlogin
    private String passwordHash;    // Da adminlogin
    private LocalDateTime ultimoAccesso; // Da adminlogin
    

    /**
     * Costruttore predefinito.
     */
    public Utente() {}

    /**
     * Costruttore con parametri principali.
     *
     * @param nome Nome completo dell'utente
     * @param ruolo Ruolo dell'utente (Amministratore, Tecnico, Operatore)
     * @param username Nome utente per l'accesso
     * @param password Password in chiaro che verrà crittografata
     */
    public Utente(String nome, String ruolo, int ruoloId, String username, String password) {
        this.nome = nome;
        this.ruolo = ruolo;
        this.ruoloId = ruoloId;
        this.username = username;
        setPassword(password);
    }

    /**
     * Restituisce l'ID univoco dell'utente.
     *
     * @return ID dell'utente
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID dell'utente.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce il nome dell'utente.
     *
     * @return Nome completo dell'utente
     */
    public String getNome() {
        return nome;
    }

    /**
     * Imposta il nome dell'utente.
     *
     * @param nome Nuovo nome da impostare
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Restituisce il ruolo dell'utente.
     *
     * @return Ruolo dell'utente nel sistema
     */
    public String getRuolo() {
        return ruolo;
    }

    /**
     * Imposta il ruolo dell'utente.
     *
     * @param ruolo Nuovo ruolo da impostare
     */
    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }
    
    /**
     * Restituisce l'ID del ruolo dell'utente.
     *
     * @return ID del ruolo dell'utente
     */
    public int getRuoloId() {
        return ruoloId;
    }

    /**
     * Imposta l'ID del ruolo dell'utente.
     *
     * @param ruoloId Nuovo ID del ruolo da impostare
     */
    public void setRuoloId(int ruoloId) {
        this.ruoloId = ruoloId;
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
     * @param username Nuovo nome utente da impostare
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
     * Imposta e cripta la password dell'utente.
     * La password viene sottoposta a hashing prima del salvataggio.
     *
     * @param password Password in chiaro da crittografare
     */
    public void setPassword(String password) {
        if (password != null && !password.isEmpty()) {
            this.passwordHash = getPasswordHash(password);
        }
    }

    public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
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
     * Verifica se l'utente è un amministratore.
     *
     * @return true se l'utente è un amministratore
     */
    public boolean isAmministratore() {
        return "Amministratore".equals(ruolo);
    }

    /**
     * Verifica se l'utente è un tecnico.
     *
     * @return true se l'utente è un tecnico
     */
    public boolean isTecnico() {
        return "Tecnico".equals(ruolo);
    }

    /**
     * Verifica se l'utente è un operatore.
     *
     * @return true se l'utente è un operatore
     */
    public boolean isOperatore() {
        return "Operatore".equals(ruolo);
    }
    

    /**
     * Genera un hash sicuro della password usando BCrypt.
     *
     * @param password Password in chiaro da crittografare
     * @return Hash della password
     * @throws IllegalArgumentException se la password è null o vuota
     */
    public static String getPasswordHash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La password non può essere null o vuota");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Verifica se la password fornita corrisponde all'hash memorizzato.
     *
     * @param plainTextPassword Password in chiaro da verificare
     * @return true se la password è corretta
     */
    public boolean verifyPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty() || passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(plainTextPassword, passwordHash);
    }

    /**
     * Aggiorna l'ultimo accesso dell'utente al momento corrente.
     */
    public void aggiornaUltimoAccesso() {
        this.ultimoAccesso = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Utente utente = (Utente) o;
        return id == utente.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Utente{" +
               "id=" + id +
               ", nome='" + nome + '\'' +
               ", ruolo='" + ruolo + '\'' +
               ", username='" + username + '\'' +
               ", ultimoAccesso=" + ultimoAccesso +
               '}';
    }
}