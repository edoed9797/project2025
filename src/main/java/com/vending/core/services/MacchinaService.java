package com.vending.core.services;

import com.vending.core.models.*;
import com.vending.core.repositories.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servizio per la gestione delle macchine distributrici.
 * Gestisce le operazioni di business logic per il ciclo di vita delle macchine,
 * incluse erogazione di bevande, gestione stati e transazioni finanziarie.
 */
public class MacchinaService {
    private final MacchinaRepository macchinaRepository;
    private final RicavoRepository ricavoRepository;
    private final TransazioneRepository transazioneRepository;

    /**
     * Costruttore del servizio.
     *
     * @param macchinaRepository repository per le macchine
     * @param ricavoRepository repository per i ricavi
     * @param transazioneRepository repository per le transazioni
     */
    public MacchinaService(MacchinaRepository macchinaRepository, 
                          RicavoRepository ricavoRepository,
                          TransazioneRepository transazioneRepository) {
        this.macchinaRepository = macchinaRepository;
        this.ricavoRepository = ricavoRepository;
        this.transazioneRepository = transazioneRepository;
    }

    /**
     * Recupera tutte le macchine nel sistema.
     *
     * @return Lista delle macchine
     */
    public List<Macchina> getTutteMacchine() {
        return macchinaRepository.findAll();
    }

    /**
     * Aggiorna lo stato di una macchina.
     *
     * @param id ID della macchina
     * @param nuovoStatoId nuovo stato da impostare
     * @return macchina aggiornata
     */
    public Macchina aggiornaMacchina(int id, int nuovoStatoId) {
        Macchina macchina = macchinaRepository.findById(id);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina non trovata: " + id);
        }

        if (!isStatoValido(nuovoStatoId)) {
            throw new IllegalArgumentException("Stato non valido: " + nuovoStatoId);
        }

        // Gestione cambio stato da manutenzione ad attiva
        if (macchina.getStatoId() == 2 && nuovoStatoId == 1) {
            //macchina.setDataUltimaManutenzione(LocalDateTime.now());
        }

        // Svuotamento cassa pre-manutenzione
        if (nuovoStatoId == 2 && macchina.getStatoId() != 2 && 
            macchina.getCassaAttuale() > 0) {
            
            svuotaCassa(macchina.getId());
        }

        macchina.setStatoId(nuovoStatoId);
        return macchinaRepository.update(macchina);
    }

    /**
     * Verifica se uno stato è valido.
     *
     * @param statoId stato da verificare
     * @return true se lo stato è valido
     */
    private boolean isStatoValido(int statoId) {
        return statoId >= 1 && statoId <= 3; // 1=Attiva, 2=In manutenzione, 3=Fuori servizio
    }

    /**
     * Recupera una macchina tramite ID.
     *
     * @param id ID della macchina
     * @return la macchina se trovata, null altrimenti
     */
    public Macchina getMacchinaById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID non valido");
        }
        return macchinaRepository.findById(id);
    }

    /**
     * Recupera le macchine di un istituto.
     *
     * @param istitutoId ID dell'istituto
     * @return lista delle macchine dell'istituto
     * @throws SQLException 
     */
    public List<Macchina> getMacchineByIstituto(int istitutoId) throws SQLException {
        if (istitutoId <= 0) {
            throw new IllegalArgumentException("ID istituto non valido");
        }
        return macchinaRepository.findByIstitutoId(istitutoId);
    }

    /**
     * Crea una nuova macchina.
     *
     * @param macchina macchina da creare
     * @return macchina creata
     */
    public Macchina creaMacchina(Macchina macchina) {
        validaMacchina(macchina);
        macchina.setStatoId(1); // Attiva
        macchina.setCreditoAttuale(0.0);
        return macchinaRepository.save(macchina);
    }

    /**
     * Aggiorna una macchina esistente.
     *
     * @param macchina macchina da aggiornare
     * @return macchina aggiornata
     */
    public Macchina aggiornaMacchina(Macchina macchina) {
        validaMacchina(macchina);
        return macchinaRepository.update(macchina);
    }

    /**
     * Elimina una macchina.
     *
     * @param id ID della macchina
     * @return true se l'eliminazione ha successo
     */
    public boolean eliminaMacchina(int id) {
        return macchinaRepository.delete(id);
    }

    /**
     * Inserisce denaro nella macchina.
     *
     * @param macchinaId ID della macchina
     * @param importo importo da inserire
     * @return true se l'inserimento ha successo
     */
    public boolean inserisciDenaro(int macchinaId, Double importo) {
        Macchina macchina = getMacchinaById(macchinaId);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina non trovata");
        }

        if (macchina.getStatoId() != 1) {
            throw new IllegalStateException("Macchina non attiva");
        }

        if (!macchina.puoAccettareDenaro(importo)) {
            return false;
        }

        macchina.setCreditoAttuale(macchina.getCreditoAttuale() + importo);
        macchinaRepository.update(macchina);
        return true;
    }

    /**
     * Eroga una bevanda.
     *
     * @param macchinaId ID della macchina
     * @param bevandaId ID della bevanda
     * @return transazione completata
     */
    public Transazione erogaBevanda(int macchinaId, int bevandaId) {
        Macchina macchina = getMacchinaById(macchinaId);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina non trovata");
        }

        if (macchina.getStatoId() != 1) {
            throw new IllegalStateException("Macchina non attiva");
        }

        Bevanda bevanda = macchina.getBevande().stream()
                .filter(b -> b.getId() == bevandaId)
                .findFirst()
                .orElse(null);

        if (bevanda == null) {
            throw new IllegalArgumentException("Bevanda non disponibile");
        }

        if (!macchina.hasCreditorSufficiente(bevanda.getPrezzo())) {
            throw new IllegalStateException("Credito insufficiente");
        }

        verificaDisponibilitaCialde(macchina, bevanda);

        // Crea transazione
        Transazione transazione = new Transazione();
        transazione.setMacchinaId(macchinaId);
        transazione.setBevandaId(bevandaId);
        transazione.setImporto(bevanda.getPrezzo());
        transazione.setDataOra(LocalDateTime.now());

        // Aggiorna stato macchina
        macchina.setCreditoAttuale(macchina.getCreditoAttuale() - bevanda.getPrezzo());
        macchina.setCassaAttuale(macchina.getCassaAttuale() + bevanda.getPrezzo());
        decrementaCialde(macchina, bevanda);

        macchinaRepository.update(macchina);
        return transazioneRepository.save(transazione);
    }

    /**
     * Restituisce il credito residuo.
     *
     * @param macchinaId ID della macchina
     * @return importo restituito
     */
    public Double restituisciCredito(int macchinaId) {
        Macchina macchina = getMacchinaById(macchinaId);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina non trovata");
        }

        Double credito = macchina.getCreditoAttuale();
        macchina.setCreditoAttuale(0.0);
        macchinaRepository.update(macchina);
        return credito;
    }

    /**
     * Svuota la cassa di una macchina.
     *
     * @param macchinaId ID della macchina
     */
    public void svuotaCassa(int macchinaId) {
        Macchina macchina = getMacchinaById(macchinaId);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina non trovata");
        }

        if (macchina.getCassaAttuale() > 0) {
            Ricavo ricavo = new Ricavo();
            ricavo.setMacchinaId(macchinaId);
            ricavo.setImporto(macchina.getCassaAttuale());
            ricavo.setDataOra(LocalDateTime.now());
            ricavoRepository.save(ricavo);

            macchina.setCassaAttuale(0.0);
            macchinaRepository.update(macchina);
        }
    }

    /**
     * Valida i dati di una macchina.
     */
    private void validaMacchina(Macchina macchina) {
        if (macchina.getIstitutoId() <= 0) {
            throw new IllegalArgumentException("Istituto non valido");
        }
        if (macchina.getCassaMassima() <= 0) {
            throw new IllegalArgumentException("Capacità cassa non valida");
        }
    }

    /**
     * Verifica la disponibilità delle cialde.
     */
    private void verificaDisponibilitaCialde(Macchina macchina, Bevanda bevanda) {
        for (Cialda cialda : bevanda.getCialde()) {
            QuantitaCialde qc = macchina.getCialde().stream()
                    .filter(q -> q.getCialdaId() == cialda.getId())
                    .findFirst()
                    .orElse(null);

            if (qc == null) {
                throw new IllegalStateException("Cialda non disponibile: " + cialda.getNome());
            }

            if (qc.getQuantita() <= 0) {
                throw new IllegalStateException("Cialda esaurita: " + cialda.getNome());
            }
        }
    }

    /**
     * Decrementa le cialde utilizzate.
     */
    private void decrementaCialde(Macchina macchina, Bevanda bevanda) {
        for (Cialda cialda : bevanda.getCialde()) {
            macchina.getCialde().stream()
                    .filter(q -> q.getCialdaId() == cialda.getId())
                    .forEach(QuantitaCialde::decrementaQuantita);
        }
    }
}