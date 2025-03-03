package com.vending.core.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modello che rappresenta un istituto dove sono installate le macchine distributrici.
 * Questa classe riflette la struttura della tabella 'istituto' nel database
 * e gestisce la relazione one-to-many con le macchine.
 */
public class Istituto {
    private int ID_istituto;
    private String nome;
    private String indirizzo;
    private List<Macchina> macchine;

    /**
     * Costruttore predefinito.
     * Inizializza la lista delle macchine associate all'istituto.
     */
    public Istituto() {
        this.macchine = new ArrayList<>();
    }

    /**
     * Costruttore con parametri principali.
     *
     * @param nome Nome dell'istituto
     * @param indirizzo Indirizzo completo dell'istituto
     */
    public Istituto(String nome, String indirizzo) {
        this();
        this.nome = nome;
        this.indirizzo = indirizzo;
    }

    /**
     * Restituisce l'ID univoco dell'istituto.
     *
     * @return ID dell'istituto
     */
    public int getId() {
        return ID_istituto;
    }

    /**
     * Imposta l'ID dell'istituto.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.ID_istituto = id;
    }

    /**
     * Restituisce il nome dell'istituto.
     *
     * @return Nome dell'istituto
     */
    public String getNome() {
        return nome;
    }

    /**
     * Imposta il nome dell'istituto.
     *
     * @param nome Nuovo nome da impostare
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Restituisce l'indirizzo dell'istituto.
     *
     * @return Indirizzo completo dell'istituto
     */
    public String getIndirizzo() {
        return indirizzo;
    }

    /**
     * Imposta l'indirizzo dell'istituto.
     *
     * @param indirizzo Nuovo indirizzo da impostare
     */
    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    /**
     * Restituisce la lista delle macchine installate nell'istituto.
     *
     * @return Lista delle macchine associate
     */
    public List<Macchina> getMacchine() {
        return macchine;
    }

    /**
     * Imposta la lista delle macchine installate nell'istituto.
     *
     * @param macchine Nuova lista di macchine
     */
    public void setMacchine(List<Macchina> macchine) {
        this.macchine = macchine;
    }

    /**
     * Aggiunge una macchina alla lista delle macchine dell'istituto.
     *
     * @param macchina Macchina da aggiungere
     */
    public void aggiungiMacchina(Macchina macchina) {
        macchine.add(macchina);
    }

    /**
     * Rimuove una macchina dalla lista delle macchine dell'istituto.
     *
     * @param macchina Macchina da rimuovere
     */
    public void rimuoviMacchina(Macchina macchina) {
        macchine.remove(macchina);
    }

    /**
     * Conta il numero di macchine attive nell'istituto.
     *
     * @return Numero di macchine con stato "Attiva"
     */
    public long contaMacchineAttive() {
        return macchine.stream()
                .filter(m -> m.getStatoId() == 1) // 1 = Attiva nella tabella statomacchina
                .count();
    }

    /**
     * Verifica se l'istituto ha almeno una macchina installata.
     *
     * @return true se l'istituto ha almeno una macchina
     */
    public boolean haMacchine() {
        return !macchine.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Istituto istituto = (Istituto) o;
        return ID_istituto == istituto.ID_istituto;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(ID_istituto);
    }

    @Override
    public String toString() {
        return "Istituto{" +
               "ID_istituto=" + ID_istituto +
               ", nome='" + nome + '\'' +
               ", indirizzo='" + indirizzo + '\'' +
               ", numeroMacchine=" + macchine.size() +
               '}';
    }
}