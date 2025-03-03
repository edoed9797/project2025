package com.vending.iot.monitor;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class StatoMacchina {
    private String stato;
    private double livelloCassa;
    private double cassaMassima;
    private int livelloCialde;
    private int cialdeMassime;
    private double creditoAttuale;
    private List<Allarme> allarmi;
    private LocalDateTime ultimoAggiornamento;
    private boolean richiedeManutenzione;
    private String ultimaManutenzione;

    public StatoMacchina() {
        this.allarmi = new ArrayList<>();
        this.ultimoAggiornamento = LocalDateTime.now();
    }

    // Getters e Setters
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }

    public double getLivelloCassa() { return livelloCassa; }
    public void setLivelloCassa(double livelloCassa) { this.livelloCassa = livelloCassa; }

    public double getCassaMassima() { return cassaMassima; }
    public void setCassaMassima(double cassaMassima) { this.cassaMassima = cassaMassima; }

    public int getLivelloCialde() { return livelloCialde; }
    public void setLivelloCialde(int livelloCialde) { this.livelloCialde = livelloCialde; }

    public int getCialdeMassime() { return cialdeMassime; }
    public void setCialdeMassime(int cialdeMassime) { this.cialdeMassime = cialdeMassime; }

    public double getCreditoAttuale() { return creditoAttuale; }
    public void setCreditoAttuale(double creditoAttuale) { this.creditoAttuale = creditoAttuale; }

    public List<Allarme> getAllarmi() { return new ArrayList<>(allarmi); }

    public LocalDateTime getUltimoAggiornamento() { return ultimoAggiornamento; }
    public void aggiornaTimestamp() { this.ultimoAggiornamento = LocalDateTime.now(); }

    public void aggiungiAllarme(Allarme allarme) {
        this.allarmi.add(allarme);
        this.richiedeManutenzione = true;
    }

    public void risolviAllarmi() {
        this.allarmi.clear();
        this.richiedeManutenzione = false;
    }

    public boolean isRichiedeManutenzione() { return richiedeManutenzione; }
    
    public String getUltimaManutenzione() { return ultimaManutenzione; }
    public void setUltimaManutenzione(String ultimaManutenzione) { 
        this.ultimaManutenzione = ultimaManutenzione; 
    }
}
