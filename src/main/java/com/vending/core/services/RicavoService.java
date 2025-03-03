package com.vending.core.services;

import com.vending.core.models.Ricavo;
import com.vending.core.repositories.RicavoRepository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servizio che gestisce la logica di business relativa ai ricavi delle macchine distributrici.
 * Si occupa della registrazione, del recupero e dell'analisi dei ricavi generati 
 * dalle macchine installate negli istituti.
 */
public class RicavoService {
    private final RicavoRepository ricavoRepository;

    /**
     * Costruisce un nuovo servizio ricavi.
     *
     * @param ricavoRepository repository per l'accesso ai dati dei ricavi
     */
    public RicavoService(RicavoRepository ricavoRepository) {
        this.ricavoRepository = ricavoRepository;
    }

    /**
     * Recupera tutti i ricavi registrati nel sistema, ordinati per data decrescente.
     *
     * @return lista di tutti i ricavi registrati
     * @throws RuntimeException se si verifica un errore durante il recupero dei dati
     */
    public List<Ricavo> getTuttiRicavi() {
        return ricavoRepository.findAll();
    }

    /**
     * Recupera tutti i ricavi di una specifica macchina.
     *
     * @param macchinaId ID della macchina di cui recuperare i ricavi
     * @return lista dei ricavi della macchina specificata
     * @throws RuntimeException se si verifica un errore durante il recupero dei dati
     */
    public List<Ricavo> getRicaviMacchina(int macchinaId) {
        validaMacchinaId(macchinaId);
        return ricavoRepository.findByMacchinaId(macchinaId);
    }

    /**
     * Calcola il totale dei ricavi di una specifica macchina.
     *
     * @param macchinaId ID della macchina di cui calcolare il totale dei ricavi
     * @return totale dei ricavi della macchina
     * @throws IllegalArgumentException se l'ID della macchina non è valido
     * @throws RuntimeException se si verifica un errore durante il calcolo
     */
    public double getTotaleRicaviMacchina(int macchinaId) {
        validaMacchinaId(macchinaId);
        return ricavoRepository.getTotaleRicaviByMacchina(macchinaId);
    }

    /**
     * Calcola il totale dei ricavi in un determinato periodo di tempo.
     *
     * @param dataInizio data e ora di inizio del periodo
     * @param dataFine data e ora di fine del periodo
     * @return totale dei ricavi nel periodo specificato
     * @throws IllegalArgumentException se le date non sono valide
     * @throws RuntimeException se si verifica un errore durante il calcolo
     */
    public double getTotaleRicaviPeriodo(LocalDateTime dataInizio, LocalDateTime dataFine) {
        validaPeriodo(dataInizio, dataFine);
        return ricavoRepository.getTotaleRicaviPeriodo(dataInizio, dataFine);
    }

    /**
     * Recupera il ricavo giornaliero di un istituto per una data specifica.
     *
     * @param istitutoId ID dell'istituto
     * @param data data per cui recuperare il ricavo
     * @return Optional contenente il ricavo giornaliero se presente
     * @throws IllegalArgumentException se l'ID dell'istituto non è valido
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public Optional<Double> getRicavoGiornaliero(int istitutoId, LocalDate data) {
        validaIstitutoId(istitutoId);
        if (data == null) {
            throw new IllegalArgumentException("La data non può essere null");
        }
        return ricavoRepository.findRicavoGiornaliero(istitutoId, data);
    }

    /**
     * Registra un nuovo ricavo per una macchina.
     *
     * @param macchinaId ID della macchina che ha generato il ricavo
     * @param importo importo del ricavo
     * @param notaOperatore nota opzionale dell'operatore
     * @return il ricavo registrato
     * @throws IllegalArgumentException se i parametri non sono validi
     * @throws RuntimeException se si verifica un errore durante il salvataggio
     */
    public Ricavo registraRicavo(int macchinaId, double importo, String notaOperatore) {
        validaRicavo(macchinaId, importo);
        
        Ricavo ricavo = new Ricavo();
        ricavo.setMacchinaId(macchinaId);
        ricavo.setImporto(importo);
        ricavo.setDataOra(LocalDateTime.now());
        
        return ricavoRepository.save(ricavo);
    }

    /**
     * Valida i parametri di un ricavo.
     *
     * @param macchinaId ID della macchina da validare
     * @param importo importo da validare
     * @throws IllegalArgumentException se i parametri non sono validi
     */
    private void validaRicavo(int macchinaId, double importo) {
        validaMacchinaId(macchinaId);
        if (importo <= 0) {
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
     * Valida l'ID di un istituto.
     *
     * @param istitutoId ID dell'istituto da validare
     * @throws IllegalArgumentException se l'ID non è valido
     */
    private void validaIstitutoId(int istitutoId) {
        if (istitutoId <= 0) {
            throw new IllegalArgumentException("L'ID dell'istituto deve essere maggiore di zero");
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