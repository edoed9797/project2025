package com.vending.core.services;

import com.vending.core.models.Istituto;
import com.vending.core.models.Macchina;
import com.vending.core.repositories.IstitutoRepository;
import com.vending.core.repositories.MacchinaRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * Servizio per la gestione degli istituti nel sistema di distribuzione automatica.
 * 
 * Questa classe fornisce operazioni di business logic per la gestione degli istituti,
 * inclusi metodi per la creazione, lettura, aggiornamento ed eliminazione degli istituti,
 * nonché la gestione delle macchine associate.
 */
public class IstitutoService {
    private final IstitutoRepository istitutoRepository;
    private final MacchinaRepository macchinaRepository;

    /**
     * Costruttore per il servizio degli istituti.
     * 
     * @param istitutoRepository Repository per le operazioni sui dati degli istituti
     * @param macchinaRepository Repository per le operazioni sui dati delle macchine
     */
    public IstitutoService(IstitutoRepository istitutoRepository, MacchinaRepository macchinaRepository) {
        this.istitutoRepository = istitutoRepository;
        this.macchinaRepository = macchinaRepository;
    }

    /**
     * Recupera tutti gli istituti presenti nel sistema.
     * 
     * @return Lista di tutti gli istituti
     * @throws SQLException 
     */
    public List<Istituto> getTuttiIstituti() throws SQLException {
        return istitutoRepository.findAll();
    }

    /**
     * Recupera un istituto specifico tramite il suo ID.
     * 
     * @param id Identificativo dell'istituto
     * @return L'istituto corrispondente all'ID
     */
    public Istituto getIstitutoById(int id) {
        return istitutoRepository.findById(id).orElse(null);
    }

    /**
     * Crea un nuovo istituto nel sistema.
     * 
     * Esegue la validazione dell'istituto prima del salvataggio.
     * 
     * @param istituto Istituto da creare
     * @return L'istituto salvato con il suo nuovo ID
     * @throws IllegalArgumentException Se la validazione fallisce
     */
    public Istituto creaIstituto(Istituto istituto) {
        validaIstituto(istituto);
        return istitutoRepository.save(istituto);
    }

    /**
     * Aggiorna un istituto esistente nel sistema.
     * 
     * Esegue la validazione dell'istituto prima dell'aggiornamento.
     * 
     * @param istituto Istituto da aggiornare
     * @return L'istituto aggiornato
     * @throws IllegalArgumentException Se la validazione fallisce
     */
    public Istituto aggiornaIstituto(Istituto istituto) {
        validaIstituto(istituto);
        return istitutoRepository.update(istituto);
    }

    /**
     * Elimina un istituto dal sistema.
     * 
     * L'eliminazione è consentita solo se non ci sono macchine associate all'istituto.
     * 
     * @param id Identificativo dell'istituto da eliminare
     * @return true se l'eliminazione ha avuto successo, false altrimenti
     * @throws SQLException 
     * @throws IllegalStateException Se l'istituto ha macchine associate
     */
    public boolean eliminaIstituto(int id) throws SQLException {
        List<Macchina> macchine = macchinaRepository.findByIstitutoId(id);
        if (!macchine.isEmpty()) {
            throw new IllegalStateException("Non è possibile eliminare un istituto con macchine associate");
        }
        return istitutoRepository.delete(id);
    }

    /**
     * Assegna una macchina a un istituto.
     * 
     * @param istitutoId Identificativo dell'istituto a cui assegnare la macchina
     * @param macchina Macchina da assegnare
     * @throws IllegalArgumentException Se l'istituto non esiste
     */
    public void assegnaMacchina(int istitutoId, Macchina macchina) {
        Istituto istituto = getIstitutoById(istitutoId);
        if (istituto == null) {
            throw new IllegalArgumentException("Istituto non trovato");
        }
        macchina.setIstitutoId(istitutoId);
        macchinaRepository.save(macchina);
    }

    /**
     * Rimuove una macchina da un istituto.
     * 
     * @param istitutoId Identificativo dell'istituto da cui rimuovere la macchina
     * @param macchinaId Identificativo della macchina da rimuovere
     * @throws IllegalArgumentException Se la macchina non esiste o non è associata all'istituto
     */
    public void rimuoviMacchina(int istitutoId, int macchinaId) {
        Macchina macchina = macchinaRepository.findById(macchinaId);
        if (macchina == null || macchina.getIstitutoId() != istitutoId) {
            throw new IllegalArgumentException("Macchina non trovata o non associata all'istituto");
        }
        macchinaRepository.delete(macchinaId);
    }

    /**
     * Convalida un istituto prima del salvataggio o dell'aggiornamento.
     * 
     * Verifica che il nome e l'indirizzo dell'istituto non siano nulli o vuoti.
     * 
     * @param istituto Istituto da validare
     * @throws IllegalArgumentException Se la validazione fallisce
     */
    private void validaIstituto(Istituto istituto) {
        if (istituto.getNome() == null || istituto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome istituto obbligatorio");
        }
        if (istituto.getIndirizzo() == null || istituto.getIndirizzo().trim().isEmpty()) {
            throw new IllegalArgumentException("Indirizzo istituto obbligatorio");
        }
    }
}