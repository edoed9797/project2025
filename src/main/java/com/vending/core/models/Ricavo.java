package com.vending.core.models;

import java.time.LocalDateTime;

/**
 * Modello che rappresenta un ricavo da una macchina distributrice.
 * Questa classe riflette la struttura della tabella 'ricavo' nel database
 * e include informazioni aggiuntive dall'istituto associato.
 */
public class Ricavo {
    private int id;
    private int macchinaId;
    private String nomeIstituto;    // Dal JOIN con la tabella istituto
    private double importo;
    private LocalDateTime dataOra;
    private String indirizzoIstituto; // Dal JOIN con la tabella istituto
    private double ricavoGiornaliero; // Dalla vista ricavigiornalieri

    /**
     * Costruttore predefinito.
     * Inizializza la data e ora al momento corrente.
     */
    public Ricavo() {
        this.dataOra = LocalDateTime.now();
    }

    /**
     * Costruttore con parametri principali.
     *
     * @param macchinaId ID della macchina che ha generato il ricavo
     * @param importo Importo del ricavo
     */
    public Ricavo(int macchinaId, double importo) {
        this();
        this.macchinaId = macchinaId;
        this.importo = importo;
    }

    /**
     * Restituisce l'ID univoco del ricavo.
     *
     * @return ID del ricavo
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID del ricavo.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce l'ID della macchina che ha generato il ricavo.
     *
     * @return ID della macchina
     */
    public int getMacchinaId() {
        return macchinaId;
    }

    /**
     * Imposta l'ID della macchina.
     *
     * @param macchinaId Nuovo ID della macchina
     */
    public void setMacchinaId(int macchinaId) {
        this.macchinaId = macchinaId;
    }

    /**
     * Restituisce il nome dell'istituto dove si trova la macchina.
     *
     * @return Nome dell'istituto
     */
    public String getNomeIstituto() {
        return nomeIstituto;
    }

    /**
     * Imposta il nome dell'istituto.
     *
     * @param nomeIstituto Nuovo nome dell'istituto
     */
    public void setNomeIstituto(String nomeIstituto) {
        this.nomeIstituto = nomeIstituto;
    }

    /**
     * Restituisce l'importo del ricavo.
     *
     * @return Importo del ricavo
     */
    public double getImporto() {
        return importo;
    }

    /**
     * Imposta l'importo del ricavo.
     *
     * @param importo Nuovo importo da impostare
     */
    public void setImporto(double importo) {
        this.importo = importo;
    }

    /**
     * Restituisce la data e ora del ricavo.
     *
     * @return Data e ora del ricavo
     */
    public LocalDateTime getDataOra() {
        return dataOra;
    }

    /**
     * Imposta la data e ora del ricavo.
     *
     * @param dataOra Nuova data e ora da impostare
     */
    public void setDataOra(LocalDateTime dataOra) {
        this.dataOra = dataOra;
    }

    /**
     * Restituisce l'indirizzo dell'istituto.
     *
     * @return Indirizzo dell'istituto
     */
    public String getIndirizzoIstituto() {
        return indirizzoIstituto;
    }

    /**
     * Imposta l'indirizzo dell'istituto.
     *
     * @param indirizzoIstituto Nuovo indirizzo dell'istituto
     */
    public void setIndirizzoIstituto(String indirizzoIstituto) {
        this.indirizzoIstituto = indirizzoIstituto;
    }

    /**
     * Restituisce il ricavo giornaliero totale.
     *
     * @return Ricavo giornaliero
     */
    public double getRicavoGiornaliero() {
        return ricavoGiornaliero;
    }

    /**
     * Imposta il ricavo giornaliero totale.
     *
     * @param ricavoGiornaliero Nuovo ricavo giornaliero
     */
    public void setRicavoGiornaliero(double ricavoGiornaliero) {
        this.ricavoGiornaliero = ricavoGiornaliero;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ricavo ricavo = (Ricavo) o;
        return id == ricavo.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Ricavo{" +
               "id=" + id +
               ", macchinaId=" + macchinaId +
               ", nomeIstituto='" + nomeIstituto + '\'' +
               ", importo=" + importo +
               ", dataOra=" + dataOra +
               '}';
    }
}