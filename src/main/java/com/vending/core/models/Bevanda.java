package com.vending.core.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modello che rappresenta una bevanda disponibile nei distributori automatici.
 * Questa classe riflette la struttura della tabella 'bevanda' nel database
 * e gestisce le relazioni con le cialde attraverso la tabella 'bevandahacialda'.
 */
public class Bevanda {
    private int id;
    private String nome;
    private double prezzo;
    private List<Cialda> cialde;
    private String composizioneCialde; // Campo per la vista BevandeCialdeECosti

    /**
     * Costruttore predefinito.
     * Inizializza la lista delle cialde associate alla bevanda.
     */
    public Bevanda() {
        this.cialde = new ArrayList<>();
    }

    /**
     * Costruttore con parametri principali.
     *
     * @param nome Nome della bevanda
     * @param prezzo Prezzo della bevanda
     */
    public Bevanda(String nome, double prezzo) {
        this();
        this.nome = nome;
        this.prezzo = prezzo;
    }

    /**
     * Restituisce l'ID univoco della bevanda.
     *
     * @return ID della bevanda
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID della bevanda.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce il nome della bevanda.
     *
     * @return Nome della bevanda
     */
    public String getNome() {
        return nome;
    }

    /**
     * Imposta il nome della bevanda.
     *
     * @param nome Nuovo nome da impostare
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Restituisce il prezzo della bevanda.
     *
     * @return Prezzo della bevanda in double
     */
    public double getPrezzo() {
        return prezzo;
    }

    /**
     * Imposta il prezzo della bevanda.
     *
     * @param prezzo Nuovo prezzo da impostare
     */
    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    /**
     * Restituisce la lista delle cialde necessarie per preparare la bevanda.
     *
     * @return Lista delle cialde associate
     */
    public List<Cialda> getCialde() {
        return cialde;
    }

    /**
     * Imposta la lista delle cialde necessarie per la bevanda.
     *
     * @param cialde Nuova lista di cialde
     */
    public void setCialde(List<Cialda> cialde) {
        this.cialde = cialde;
    }

    /**
     * Restituisce la descrizione della composizione delle cialde.
     * Questo campo viene popolato dalla vista BevandeCialdeECosti.
     *
     * @return Stringa contenente l'elenco delle cialde
     */
    public String getComposizioneCialde() {
        return composizioneCialde;
    }

    /**
     * Imposta la descrizione della composizione delle cialde.
     *
     * @param composizioneCialde Nuova descrizione della composizione
     */
    public void setComposizioneCialde(String composizioneCialde) {
        this.composizioneCialde = composizioneCialde;
    }

    /**
     * Verifica se la bevanda è disponibile in base alla presenza delle cialde necessarie.
     *
     * @param quantitaCialde Mappa delle quantità di cialde disponibili
     * @return true se tutte le cialde necessarie sono disponibili in quantità sufficiente
     */
    public boolean isDisponibile(List<QuantitaCialde> quantitaCialde) {
        for (Cialda cialda : cialde) {
            boolean cialdaDisponibile = quantitaCialde.stream()
                .anyMatch(qc -> qc.getCialdaId() == cialda.getId() && qc.getQuantita() > 0);
            if (!cialdaDisponibile) {
                return false;
            }
        }
        return true;
    }

    /**
     * Aggiunge una cialda alla lista delle cialde necessarie per la bevanda.
     * Verifica che la cialda non sia già presente prima di aggiungerla.
     *
     * @param cialda Cialda da aggiungere
     */
    public void aggiungiCialda(Cialda cialda) {
        if (!cialde.contains(cialda)) {
            cialde.add(cialda);
        }
    }

    /**
     * Rimuove una cialda dalla lista delle cialde necessarie per la bevanda.
     *
     * @param cialda Cialda da rimuovere
     */
    public void rimuoviCialda(Cialda cialda) {
        cialde.remove(cialda);
    }

    /**
     * Calcola il numero totale di cialde necessarie per preparare la bevanda.
     *
     * @return Numero totale di cialde richieste
     */
    public int getNumeroCialdeNecessarie() {
        return cialde.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bevanda bevanda = (Bevanda) o;
        return id == bevanda.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}