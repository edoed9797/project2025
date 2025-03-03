package com.vending.core.services;

import com.vending.core.models.Transazione;
import com.vending.core.repositories.TransazioneRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servizio che gestisce la logica di business relativa alle transazioni delle macchine distributrici.
 * Si occupa della registrazione e dell'analisi delle vendite di bevande effettuate dalle
 * macchine installate negli istituti.
 */
public class TransazioneService {
    private final TransazioneRepository transazioneRepository;

    /**
     * Costruisce un nuovo servizio transazioni.
     *
     * @param transazioneRepository repository per l'accesso ai dati delle transazioni
     */
    public TransazioneService(TransazioneRepository transazioneRepository) {
        this.transazioneRepository = transazioneRepository;
    }

    /**
     * Recupera tutte le transazioni registrate nel sistema, ordinate per data decrescente.
     *
     * @return lista di tutte le transazioni registrate
     * @throws RuntimeException se si verifica un errore durante il recupero dei dati
     */
    public List<Transazione> getTutteTransazioni() {
        return transazioneRepository.findAll();
    }

    /**
     * Recupera le transazioni recenti (ultime 100) dal sistema.
     * Utilizza la vista 'transazionirecenti' del database.
     *
     * @return lista delle transazioni recenti
     * @throws RuntimeException se si verifica un errore durante il recupero dei dati
     */
    public List<Transazione> getTransazioniRecenti() {
        return transazioneRepository.findTransazioniRecenti();
    }

    /**
     * Recupera una transazione specifica tramite il suo ID.
     *
     * @param id ID della transazione da recuperare
     * @return Optional contenente la transazione se trovata
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public Optional<Transazione> getTransazione(int id) {
        return transazioneRepository.findById(id);
    }

    /**
     * Recupera tutte le transazioni di una specifica macchina.
     *
     * @param macchinaId ID della macchina di cui recuperare le transazioni
     * @return lista delle transazioni della macchina specificata
     * @throws IllegalArgumentException se l'ID della macchina non è valido
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public List<Transazione> getTransazioniMacchina(int macchinaId) {
        validaMacchinaId(macchinaId);
        return transazioneRepository.findByMacchinaId(macchinaId);
    }

    /**
     * Calcola il totale delle transazioni di una macchina in un determinato periodo.
     *
     * @param macchinaId ID della macchina
     * @param dataInizio data e ora di inizio del periodo
     * @param dataFine data e ora di fine del periodo
     * @return totale delle transazioni nel periodo specificato
     * @throws IllegalArgumentException se i parametri non sono validi
     * @throws RuntimeException se si verifica un errore durante il calcolo
     */
    public double calcolaTotaleMacchina(int macchinaId, LocalDateTime dataInizio, LocalDateTime dataFine) {
        validaMacchinaId(macchinaId);
        validaPeriodo(dataInizio, dataFine);
        return transazioneRepository.calcolaTotaleMacchina(macchinaId, dataInizio, dataFine);
    }

    /**
     * Registra una nuova transazione nel sistema.
     *
     * @param transazione transazione da registrare
     * @return la transazione registrata con ID generato
     * @throws IllegalArgumentException se i dati della transazione non sono validi
     * @throws RuntimeException se si verifica un errore durante il salvataggio
     */
    public Transazione registraTransazione(Transazione transazione) {
        validaTransazione(transazione);
        transazione.setDataOra(LocalDateTime.now());
        return transazioneRepository.save(transazione);
    }

    /**
     * Valida i dati di una transazione.
     *
     * @param transazione transazione da validare
     * @throws IllegalArgumentException se i dati della transazione non sono validi
     */
    private void validaTransazione(Transazione transazione) {
        if (transazione == null) {
            throw new IllegalArgumentException("La transazione non può essere null");
        }
        if (transazione.getMacchinaId() <= 0) {
            throw new IllegalArgumentException("L'ID della macchina deve essere maggiore di zero");
        }
        if (transazione.getBevandaId() <= 0) {
            throw new IllegalArgumentException("L'ID della bevanda deve essere maggiore di zero");
        }
        if (transazione.getImporto() <= 0) {
            throw new IllegalArgumentException("L'importo deve essere maggiore di zero");
        }
    }

    /**
     * Valida l'ID di una macchina.
     *
     * @param macchinaId ID della macchina da validare
     * @throws IllegalArgumentException se l'ID non è valido
     */
    private void validaMacchinaId(int macchinaId) {
        if (macchinaId <= 0) {
            throw new IllegalArgumentException("L'ID della macchina deve essere maggiore di zero");
        }
    }

    /**
     * Valida un periodo temporale.
     *
     * @param dataInizio data di inizio del periodo
     * @param dataFine data di fine del periodo
     * @throws IllegalArgumentException se le date non sono valide
     */
    private void validaPeriodo(LocalDateTime dataInizio, LocalDateTime dataFine) {
        if (dataInizio == null || dataFine == null) {
            throw new IllegalArgumentException("Le date non possono essere null");
        }
        if (dataInizio.isAfter(dataFine)) {
            throw new IllegalArgumentException("La data di inizio deve essere precedente alla data di fine");
        }
    }
}