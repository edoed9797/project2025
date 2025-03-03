package com.vending.core.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modello che rappresenta una richiesta di manutenzione per una macchina distributrice.
 * La classe gestisce le informazioni relative agli interventi di manutenzione,
 * integrando i dati dello stato della macchina e delle necessità di rifornimento.
 */
public class Manutenzione {
    private int id;
    private int macchinaId;
    private String tipoIntervento;  // Tipo di intervento (es. RIFORNIMENTO_CIALDE, SVUOTAMENTO_CASSA)
    private String descrizione;     // Descrizione del problema o dell'intervento
    private LocalDateTime dataRichiesta;
    private LocalDateTime dataCompletamento;
    private String stato;           // Stato della manutenzione (IN_ATTESA, IN_CORSO, COMPLETATA)
    private String urgenza;         // Urgenza dell'intervento (BASSA, MEDIA, ALTA)
    private int tecnicoId;          // ID del tecnico assegnato
    private String note;            // Note aggiuntive
    private List<QuantitaCialde> cialdeDaRifornire; // Lista delle cialde da rifornire

    /**
     * Costruttore predefinito.
     * Inizializza una nuova richiesta di manutenzione con data corrente e stato "IN_ATTESA".
     */
    public Manutenzione() {
        this.dataRichiesta = LocalDateTime.now();
        this.stato = "IN_ATTESA";
        this.urgenza = "MEDIA"; // Urgenza predefinita
        this.cialdeDaRifornire = new ArrayList<>();
    }

    /**
     * Costruttore con parametri principali.
     *
     * @param macchinaId ID della macchina che richiede manutenzione
     * @param tipoIntervento Tipo di intervento richiesto
     * @param descrizione Descrizione del problema o dell'intervento
     * @param urgenza Urgenza dell'intervento (BASSA, MEDIA, ALTA)
     */
    public Manutenzione(int macchinaId, String tipoIntervento, String descrizione, String urgenza) {
        this();
        this.macchinaId = macchinaId;
        this.tipoIntervento = tipoIntervento;
        this.descrizione = descrizione;
        this.urgenza = urgenza;
    }

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMacchinaId() {
        return macchinaId;
    }

    public void setMacchinaId(int macchinaId) {
        this.macchinaId = macchinaId;
    }

    public String getTipoIntervento() {
        return tipoIntervento;
    }

    public void setTipoIntervento(String tipoIntervento) {
        this.tipoIntervento = tipoIntervento;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public LocalDateTime getDataRichiesta() {
        return dataRichiesta;
    }

    public void setDataRichiesta(LocalDateTime dataRichiesta) {
        this.dataRichiesta = dataRichiesta;
    }

    public LocalDateTime getDataCompletamento() {
        return dataCompletamento;
    }

    public void setDataCompletamento(LocalDateTime dataCompletamento) {
        this.dataCompletamento = dataCompletamento;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public String getUrgenza() {
        return urgenza;
    }

    public void setUrgenza(String urgenza) {
        this.urgenza = urgenza;
    }

    public int getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(int tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<QuantitaCialde> getCialdeDaRifornire() {
        return cialdeDaRifornire;
    }

    public void setCialdeDaRifornire(List<QuantitaCialde> cialdeDaRifornire) {
        this.cialdeDaRifornire = cialdeDaRifornire;
    }

    /**
     * Verifica se la manutenzione è in attesa.
     *
     * @return true se la manutenzione è in attesa, false altrimenti
     */
    public boolean isInAttesa() {
        return "IN_ATTESA".equals(stato);
    }

    /**
     * Verifica se la manutenzione è in corso.
     *
     * @return true se la manutenzione è in corso, false altrimenti
     */
    public boolean isInCorso() {
        return "IN_CORSO".equals(stato);
    }

    /**
     * Verifica se la manutenzione è completata.
     *
     * @return true se la manutenzione è completata, false altrimenti
     */
    public boolean isCompletata() {
        return "COMPLETATA".equals(stato);
    }

    /**
     * Imposta la manutenzione come completata.
     *
     * @param note Note di completamento
     * @param tecnicoId ID del tecnico che ha completato la manutenzione
     */
    public void completaManutenzione(String note, int tecnicoId) {
        this.stato = "COMPLETATA";
        this.dataCompletamento = LocalDateTime.now();
        this.note = note;
        this.tecnicoId = tecnicoId;
    }

    /**
     * Imposta la manutenzione come fuori servizio.
     */
    public void setFuoriServizio() {
        this.stato = "FUORI_SERVIZIO";
    }

    /**
     * Calcola la durata della manutenzione in minuti.
     *
     * @return Durata in minuti, -1 se la manutenzione non è completata
     */
    public long getDurataInMinuti() {
        if (dataCompletamento == null) {
            return -1;
        }
        return java.time.Duration.between(dataRichiesta, dataCompletamento).toMinutes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Manutenzione that = (Manutenzione) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Manutenzione{" +
                "id=" + id +
                ", macchinaId=" + macchinaId +
                ", tipoIntervento='" + tipoIntervento + '\'' +
                ", descrizione='" + descrizione + '\'' +
                ", dataRichiesta=" + dataRichiesta +
                ", dataCompletamento=" + dataCompletamento +
                ", stato='" + stato + '\'' +
                ", urgenza='" + urgenza + '\'' +
                ", tecnicoId=" + tecnicoId +
                ", note='" + note + '\'' +
                '}';
    }
}