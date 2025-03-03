package com.vending.core.models;

import java.time.LocalDateTime;

/**
 * Modello che rappresenta una transazione di vendita di una bevanda.
 * Questa classe riflette la struttura della tabella 'transazione' nel database
 * e include informazioni aggiuntive dalle tabelle correlate e dalla vista 'transazionirecenti'.
 */
public class Transazione {
    private int id;
    private int macchinaId;
    private String nomeIstituto;    // Dal JOIN con la tabella istituto
    private int bevandaId;
    private String nomeBevanda;     // Dal JOIN con la tabella bevanda
    private Double importo;
    private LocalDateTime dataOra;

    /**
     * Costruttore predefinito.
     * Inizializza la data e ora al momento corrente.
     */
    public Transazione() {
        this.dataOra = LocalDateTime.now();
    }

    /**
     * Costruttore con parametri principali.
     *
     * @param macchinaId ID della macchina che ha eseguito la transazione
     * @param bevandaId ID della bevanda erogata
     * @param importo Importo della transazione
     */
    public Transazione(int macchinaId, int bevandaId, Double importo) {
        this();
        this.macchinaId = macchinaId;
        this.bevandaId = bevandaId;
        this.importo = importo;
    }

    /**
     * Restituisce l'ID univoco della transazione.
     *
     * @return ID della transazione
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID della transazione.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce l'ID della macchina che ha eseguito la transazione.
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
     * Restituisce l'ID della bevanda erogata.
     *
     * @return ID della bevanda
     */
    public int getBevandaId() {
        return bevandaId;
    }

    /**
     * Imposta l'ID della bevanda.
     *
     * @param bevandaId Nuovo ID della bevanda
     */
    public void setBevandaId(int bevandaId) {
        this.bevandaId = bevandaId;
    }

    /**
     * Restituisce il nome della bevanda erogata.
     *
     * @return Nome della bevanda
     */
    public String getNomeBevanda() {
        return nomeBevanda;
    }

    /**
     * Imposta il nome della bevanda.
     *
     * @param nomeBevanda Nuovo nome della bevanda
     */
    public void setNomeBevanda(String nomeBevanda) {
        this.nomeBevanda = nomeBevanda;
    }

    /**
     * Restituisce l'importo della transazione.
     *
     * @return Importo della transazione
     */
    public Double getImporto() {
        return importo;
    }

    /**
     * Imposta l'importo della transazione.
     *
     * @param importo Nuovo importo da impostare
     */
    public void setImporto(Double importo) {
        this.importo = importo;
    }

    /**
     * Restituisce la data e ora della transazione.
     *
     * @return Data e ora della transazione
     */
    public LocalDateTime getDataOra() {
        return dataOra;
    }

    /**
     * Imposta la data e ora della transazione.
     *
     * @param dataOra Nuova data e ora da impostare
     */
    public void setDataOra(LocalDateTime dataOra) {
        this.dataOra = dataOra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transazione that = (Transazione) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Transazione{" +
               "id=" + id +
               ", macchinaId=" + macchinaId +
               ", nomeIstituto='" + nomeIstituto + '\'' +
               ", nomeBevanda='" + nomeBevanda + '\'' +
               ", importo=" + importo +
               ", dataOra=" + dataOra +
               '}';
    }
}