package com.vending.core.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modello che rappresenta una macchina distributrice automatica.
 * Si basa sulla struttura del database definita nella tabella 'macchina'
 * e nelle sue relazioni.
 */
public class Macchina {
    private int id;
    private int istitutoId;
    private String nomeIstituto;
    private int statoId;
    private String statoDescrizione;
    private double cassaAttuale;
    private double cassaMassima;
    private double creditoAttuale;
    private List<QuantitaCialde> cialde;
    private List<Bevanda> bevande;

    /**
     * Costruttore predefinito che inizializza le liste e imposta i valori predefiniti.
     * Lo stato predefinito è 1 (Attiva) come definito nella tabella 'statomacchina'.
     */
    public Macchina() {
        this.cialde = new ArrayList<>();
        this.bevande = new ArrayList<>();
        this.creditoAttuale = 0.0;
        this.statoId = 1; // Stato "Attiva" come da tabella statomacchina
        this.cassaAttuale = 0.0;
    }

    // Getters e Setters
    /**
     * @return ID univoco della macchina
     */
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    /**
     * @return ID dell'istituto dove è installata la macchina
     */
    public int getIstitutoId() { return istitutoId; }
    public void setIstitutoId(int istitutoId) { this.istitutoId = istitutoId; }

    /**
     * @return Nome dell'istituto dove è installata la macchina
     */
    public String getNomeIstituto() { return nomeIstituto; }
    public void setNomeIstituto(String nomeIstituto) { this.nomeIstituto = nomeIstituto; }

    /**
     * @return ID dello stato della macchina dalla tabella 'statomacchina'
     */
    public int getStatoId() { return statoId; }
    public void setStatoId(int statoId) { this.statoId = statoId; }

    /**
     * @return Descrizione testuale dello stato della macchina
     */
    public String getStatoDescrizione() { return statoDescrizione; }
    public void setStatoDescrizione(String statoDescrizione) { this.statoDescrizione = statoDescrizione; }

    /**
     * @return Ammontare corrente in cassa
     */
    public double getCassaAttuale() { return cassaAttuale; }
    public void setCassaAttuale(double cassaAttuale) { this.cassaAttuale = cassaAttuale; }

    /**
     * @return Capacità  massima della cassa
     */
    public double getCassaMassima() { return cassaMassima; }
    public void setCassaMassima(double cassaMassima) { this.cassaMassima = cassaMassima; }

    /**
     * @return Credito attualmente inserito dall'utente
     */
    public double getCreditoAttuale() { return creditoAttuale; }
    public void setCreditoAttuale(double creditoAttuale) { this.creditoAttuale = creditoAttuale; }

    /**
     * @return Lista delle quantità  di cialde disponibili
     */
    public List<QuantitaCialde> getCialde() { return cialde; }
    public void setCialde(List<QuantitaCialde> cialde) { this.cialde = cialde; }

    /**
     * @return Lista delle bevande disponibili
     */
    public List<Bevanda> getBevande() { return bevande; }
    public void setBevande(List<Bevanda> bevande) { this.bevande = bevande; }

    
    /**
     * Verifica se la macchina può accettare un determinato importo.
     * 
     * @param importo Importo da verificare
     * @return true se l'importo può essere accettato
     */
    public boolean puoAccettareDenaro(double importo) {
        if(cassaAttuale+creditoAttuale+importo > cassaMassima) {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Verifica se c'è credito sufficiente per una erogazione.
     * 
     * @param importo Importo da verificare
     * @return true se il credito è sufficiente
     */
    public boolean hasCreditorSufficiente(double importo) {
        if(creditoAttuale >= importo) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Verifica se la cassa ha raggiunto la capacità  massima.
     * 
     * @return true se la cassa è piena
     */
    public boolean isCassaPiena() {
        if(cassaAttuale == cassaMassima) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Verifica se la macchina è attiva e operativa.
     * 
     * @return true se la macchina è attiva
     */
    public boolean isAttiva() {
        return statoId == 1; // 1 = Attiva nella tabella statomacchina
    }

    /**
     * Aggiunge una bevanda alla lista delle bevande disponibili.
     * 
     * @param bevanda Bevanda da aggiungere
     */
    public void aggiungiBevanda(Bevanda bevanda) {
        bevande.add(bevanda);
    }

    /**
     * Aggiunge una quantità  di cialde alla lista.
     * 
     * @param cialda QuantitaCialde da aggiungere
     */
    public void aggiungiCialda(QuantitaCialde cialda) {
        cialde.add(cialda);
    }
}