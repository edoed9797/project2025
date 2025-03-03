package com.vending.core.models;

/**
 * Modello che rappresenta una cialda utilizzata nelle macchine distributrici.
 * Questa classe riflette la struttura della tabella 'cialda' nel database
 * e viene utilizzata nelle relazioni con le bevande attraverso la tabella 'bevandahacialda'.
 */
public class Cialda {
    private int id;
    private String nome;
    private String tipoCialda;

    /**
     * Costruttore predefinito.
     * Inizializza una nuova istanza di Cialda senza parametri.
     */
    public Cialda() {}

    /**
     * Costruttore con parametri principali.
     *
     * @param nome Nome della cialda
     * @param tipoCialda Tipo della cialda (es. Caffè, Tè, Tisana, Additivo)
     */
    public Cialda(String nome, String tipoCialda) {
        this.nome = nome;
        this.tipoCialda = tipoCialda;
    }

    /**
     * Restituisce l'ID univoco della cialda.
     *
     * @return ID della cialda
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID della cialda.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce il nome della cialda.
     *
     * @return Nome della cialda
     */
    public String getNome() {
        return nome;
    }

    /**
     * Imposta il nome della cialda.
     *
     * @param nome Nuovo nome da impostare
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Restituisce il tipo della cialda.
     * I tipi possibili includono: Caffè, Tè, Tisana, Additivo, ecc.
     *
     * @return Tipo della cialda
     */
    public String getTipoCialda() {
        return tipoCialda;
    }

    /**
     * Imposta il tipo della cialda.
     *
     * @param tipoCialda Nuovo tipo da impostare
     */
    public void setTipoCialda(String tipoCialda) {
        this.tipoCialda = tipoCialda;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cialda cialda = (Cialda) o;
        return id == cialda.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Cialda{" +
               "id=" + id +
               ", nome='" + nome + '\'' +
               ", tipoCialda='" + tipoCialda + '\'' +
               '}';
    }
}