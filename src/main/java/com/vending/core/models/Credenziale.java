package com.vending.core.models;

import java.time.LocalDateTime;

public class Credenziale {
    private int ID_Credenziale;
    private int ID_Utente;
    private String Username;
    private String PasswordHash;
    private LocalDateTime UltimoAccesso;

    // Getter e Setter
    public int getID_Credenziale() { return ID_Credenziale; }
    public void setID_Credenziale(int ID_Credenziale) { this.ID_Credenziale = ID_Credenziale; }

    public int getID_Utente() { return ID_Utente; }
    public void setID_Utente(int ID_Utente) { this.ID_Utente = ID_Utente; }

    public String getUsername() { return Username; }
    public void setUsername(String Username) { this.Username = Username; }

    public String getPasswordHash() { return PasswordHash; }
    public void setPasswordHash(String PasswordHash) { this.PasswordHash = PasswordHash; }

    public LocalDateTime getUltimoAccesso() { return UltimoAccesso; }
    public void setUltimoAccesso(LocalDateTime UltimoAccesso) { this.UltimoAccesso = UltimoAccesso; }
}