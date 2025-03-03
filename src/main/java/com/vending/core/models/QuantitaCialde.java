package com.vending.core.models;

/**
 * Modello che rappresenta la quantità di cialde disponibili in una macchina.
 * Questa classe riflette la struttura della tabella 'quantitacialde' nel database
 * e gestisce le relazioni con le tabelle 'macchina' e 'cialda'.
 */
public class QuantitaCialde {
    private int id;
    private int macchinaId;
    private int cialdaId;
    private String nomeCialda;    // Dal JOIN con la tabella cialda
    private String tipoCialda;    // Dal JOIN con la tabella cialda
    private int quantita;
    private int quantitaMassima;
    private int sogliaMinima;     // Calcolato come 20% della quantità massima

    /**
     * Costruttore predefinito.
     */
    public QuantitaCialde() {
        this.sogliaMinima = 0;
    }

    /**
     * Costruttore con parametri principali.
     *
     * @param macchinaId ID della macchina
     * @param cialdaId ID della cialda
     * @param quantita Quantità attuale di cialde
     * @param quantitaMassima Quantità massima di cialde
     */
    public QuantitaCialde(int macchinaId, int cialdaId, int quantita, int quantitaMassima) {
        this.macchinaId = macchinaId;
        this.cialdaId = cialdaId;
        this.quantita = quantita;
        this.quantitaMassima = quantitaMassima;
        this.sogliaMinima = calcolaSogliaMinima();
    }

    /**
     * Restituisce l'ID univoco del record.
     *
     * @return ID del record di quantità cialde
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID del record.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce l'ID della macchina.
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
     * Restituisce l'ID della cialda.
     *
     * @return ID della cialda
     */
    public int getCialdaId() {
        return cialdaId;
    }

    /**
     * Imposta l'ID della cialda.
     *
     * @param cialdaId Nuovo ID della cialda
     */
    public void setCialdaId(int cialdaId) {
        this.cialdaId = cialdaId;
    }

    /**
     * Restituisce il nome della cialda.
     *
     * @return Nome della cialda
     */
    public String getNomeCialda() {
        return nomeCialda;
    }

    /**
     * Imposta il nome della cialda.
     *
     * @param nomeCialda Nuovo nome della cialda
     */
    public void setNomeCialda(String nomeCialda) {
        this.nomeCialda = nomeCialda;
    }

    /**
     * Restituisce il tipo della cialda.
     *
     * @return Tipo della cialda
     */
    public String getTipoCialda() {
        return tipoCialda;
    }

    /**
     * Imposta il tipo della cialda.
     *
     * @param tipoCialda Nuovo tipo della cialda
     */
    public void setTipoCialda(String tipoCialda) {
        this.tipoCialda = tipoCialda;
    }

    /**
     * Restituisce la quantità attuale di cialde.
     *
     * @return Quantità attuale
     */
    public int getQuantita() {
        return quantita;
    }

    /**
     * Imposta la quantità di cialde.
     *
     * @param quantita Nuova quantità da impostare
     */
    public void setQuantita(int quantita) {
        this.quantita = quantita;
    }

    /**
     * Restituisce la quantità massima di cialde.
     *
     * @return Quantità massima
     */
    public int getQuantitaMassima() {
        return quantitaMassima;
    }

    /**
     * Imposta la quantità massima di cialde e ricalcola la soglia minima.
     *
     * @param quantitaMassima Nuova quantità massima
     */
    public void setQuantitaMassima(int quantitaMassima) {
        this.quantitaMassima = quantitaMassima;
        this.sogliaMinima = calcolaSogliaMinima();
    }

    /**
     * Restituisce la soglia minima di cialde.
     *
     * @return Soglia minima
     */
    public int getSogliaMinima() {
        return sogliaMinima;
    }

    /**
     * Calcola la soglia minima come 20% della quantità massima.
     *
     * @return Soglia minima calcolata
     */
    private int calcolaSogliaMinima() {
        return (int) (quantitaMassima * 0.2);
    }

    /**
     * Verifica se è necessario il rifornimento delle cialde.
     *
     * @return true se la quantità è sotto la soglia minima
     */
    public boolean necessitaRifornimento() {
        return quantita <= sogliaMinima;
    }

    /**
     * Verifica se c'è una quantità sufficiente di cialde.
     *
     * @param quantitaRichiesta Quantità necessaria
     * @return true se la quantità disponibile è sufficiente
     */
    public boolean haQuantitaSufficiente(int quantitaRichiesta) {
        return quantita >= quantitaRichiesta;
    }

    /**
     * Decrementa la quantità di cialde di una unità.
     */
    public void decrementaQuantita() {
        if (quantita > 0) {
            quantita--;
        }
    }

    /**
     * Riempie il contenitore di cialde alla quantità massima.
     */
    public void rifornisci() {
        this.quantita = this.quantitaMassima;
    }

    /**
     * Calcola la quantità di cialde da rifornire.
     *
     * @return Quantità necessaria per il rifornimento completo
     */
    public int getQuantitaDaRifornire() {
        return quantitaMassima - quantita;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuantitaCialde that = (QuantitaCialde) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "QuantitaCialde{" +
               "id=" + id +
               ", macchinaId=" + macchinaId +
               ", nomeCialda='" + nomeCialda + '\'' +
               ", quantita=" + quantita +
               ", quantitaMassima=" + quantitaMassima +
               ", necessitaRifornimento=" + necessitaRifornimento() +
               '}';
    }
}