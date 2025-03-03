package com.vending.core.models;

/**
 * Modello che rappresenta lo stato possibile di una macchina distributrice.
 * Questa classe riflette la struttura della tabella 'statomacchina' nel database,
 * che definisce gli stati possibili per le macchine distributrici.
 */
public class StatoMacchina {
    private int id;
    private String descrizione;
    
    /**
     * Costanti che rappresentano gli stati predefiniti dal database.
     */
    public static final int STATO_ATTIVA = 1;
    public static final int STATO_IN_MANUTENZIONE = 2;
    public static final int STATO_FUORI_SERVIZIO = 3;

    /**
     * Costruttore predefinito.
     */
    public StatoMacchina() {}

    /**
     * Costruttore con parametri principali.
     *
     * @param id ID dello stato
     * @param descrizione Descrizione dello stato
     */
    public StatoMacchina(int id, String descrizione) {
        this.id = id;
        this.descrizione = descrizione;
    }

    /**
     * Restituisce l'ID univoco dello stato.
     *
     * @return ID dello stato
     */
    public int getId() {
        return id;
    }

    /**
     * Imposta l'ID dello stato.
     *
     * @param id Nuovo ID da impostare
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Restituisce la descrizione dello stato.
     *
     * @return Descrizione dello stato
     */
    public String getDescrizione() {
        return descrizione;
    }

    /**
     * Imposta la descrizione dello stato.
     *
     * @param descrizione Nuova descrizione da impostare
     */
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    /**
     * Verifica se lo stato corrisponde a "Attiva".
     *
     * @return true se la macchina è attiva
     */
    public boolean isAttiva() {
        return id == STATO_ATTIVA;
    }

    /**
     * Verifica se lo stato corrisponde a "In manutenzione".
     *
     * @return true se la macchina è in manutenzione
     */
    public boolean isInManutenzione() {
        return id == STATO_IN_MANUTENZIONE;
    }

    /**
     * Verifica se lo stato corrisponde a "Fuori servizio".
     *
     * @return true se la macchina è fuori servizio
     */
    public boolean isFuoriServizio() {
        return id == STATO_FUORI_SERVIZIO;
    }

    /**
     * Verifica se la macchina può erogare bevande in questo stato.
     *
     * @return true se la macchina può erogare bevande
     */
    public boolean puoErogareBevande() {
        return isAttiva();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatoMacchina that = (StatoMacchina) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "StatoMacchina{" +
               "id=" + id +
               ", descrizione='" + descrizione + '\'' +
               '}';
    }
}