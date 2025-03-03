package com.vending.core.services;

import com.vending.core.models.Bevanda;
import com.vending.core.models.Cialda;
import com.vending.core.repositories.BevandaRepository;
import com.vending.core.repositories.CialdaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servizio che gestisce la logica di per le bevande disponibili
 * nelle macchine distributrici.
 */
public class BevandaService {
    private final BevandaRepository bevandaRepository;
    private final CialdaRepository cialdaRepository;

    /**
     * Costruttore del servizio.
     *
     * @param bevandaRepository repository per l'accesso ai dati delle bevande
     * @param cialdaRepository repository per l'accesso ai dati delle cialde
     */
    public BevandaService(BevandaRepository bevandaRepository, CialdaRepository cialdaRepository) {
        this.bevandaRepository = bevandaRepository;
        this.cialdaRepository = cialdaRepository;
    }

    /**
     * Recupera tutte le bevande disponibili.
     *
     * @return lista di tutte le bevande
     */
    public List<Bevanda> getTutteBevande() {
        return bevandaRepository.findAll();
    }

    /**
     * Recupera una bevanda specifica.
     *
     * @param id ID della bevanda
     * @return Optional contenente la bevanda se trovata
     */
    public Optional<Bevanda> getBevandaById(int id) {
        return bevandaRepository.findById(id);
    }

    /**
     * Crea una nuova bevanda.
     *
     * @param nome nome della bevanda
     * @param prezzo prezzo della bevanda
     * @param cialdeIds lista degli ID delle cialde necessarie
     * @return bevanda creata
     */
    public Bevanda creaBevanda(String nome, Double prezzo, List<Integer> cialdeIds) {
        // Valida i parametri base
        validaParametriBevanda(nome, prezzo);

        // Verifica e recupera le cialde
        List<Cialda> cialde = recuperaCialde(cialdeIds);

        // Crea e salva la bevanda
        Bevanda bevanda = new Bevanda(nome, prezzo);
        bevanda.setCialde(cialde);
        return bevandaRepository.save(bevanda);
    }

    /**
     * Aggiorna una bevanda esistente.
     *
     * @param id ID della bevanda da aggiornare
     * @param nome nuovo nome
     * @param prezzo nuovo prezzo 
     * @param cialdeIds nuova lista di ID delle cialde
     * @return bevanda aggiornata
     */
    public Bevanda aggiornaBevanda(int id, String nome, Double prezzo, List<Integer> cialdeIds) {
        // Recupera la bevanda esistente
        Bevanda bevanda = bevandaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Bevanda non trovata"));

        // Aggiorna solo i campi forniti
        if (nome != null && !nome.trim().isEmpty()) {
            bevanda.setNome(nome);
        }
        if (prezzo != null && prezzo > 0) {
            bevanda.setPrezzo(prezzo);
        }
        if (cialdeIds != null && !cialdeIds.isEmpty()) {
            bevanda.setCialde(recuperaCialde(cialdeIds));
        }

        return bevandaRepository.update(bevanda);
    }

    /**
     * Elimina una bevanda.
     *
     * @param id ID della bevanda da eliminare
     * @return true se l'eliminazione ha successo
     */
    public boolean eliminaBevanda(int id) {
        return bevandaRepository.delete(id);
    }

    /**
     * Aggiunge una cialda a una bevanda.
     *
     * @param bevandaId ID della bevanda
     * @param cialdaId ID della cialda da aggiungere
     */
    public void aggiungiCialda(int bevandaId, int cialdaId) {
        // Recupera bevanda e cialda
        Bevanda bevanda = bevandaRepository.findById(bevandaId)
            .orElseThrow(() -> new IllegalArgumentException("Bevanda non trovata"));
        
        Cialda cialda = cialdaRepository.findById(cialdaId)
            .orElseThrow(() -> new IllegalArgumentException("Cialda non trovata"));

        // Aggiunge la cialda e aggiorna
        bevanda.aggiungiCialda(cialda);
        bevandaRepository.update(bevanda);
    }

    /**
     * Rimuove una cialda da una bevanda.
     *
     * @param bevandaId ID della bevanda
     * @param cialdaId ID della cialda da rimuovere
     */
    public void rimuoviCialda(int bevandaId, int cialdaId) {
        // Recupera la bevanda
        Bevanda bevanda = bevandaRepository.findById(bevandaId)
            .orElseThrow(() -> new IllegalArgumentException("Bevanda non trovata"));

        // Verifica che rimanga almeno una cialda
        if (bevanda.getCialde().size() <= 1) {
            throw new IllegalStateException("La bevanda deve mantenere almeno una cialda");
        }

        // Rimuove la cialda e aggiorna
        bevanda.getCialde().removeIf(c -> c.getId() == cialdaId);
        bevandaRepository.update(bevanda);
    }

    /**
     * Recupera le cialde per una lista di ID.
     */
    private List<Cialda> recuperaCialde(List<Integer> cialdeIds) {
        return cialdeIds.stream()
            .map(id -> cialdaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cialda non trovata: " + id)))
            .collect(Collectors.toList());
    }

    /**
     * Valida i parametri base di una bevanda.
     */
    private void validaParametriBevanda(String nome, Double prezzo) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome bevanda obbligatorio");
        }
        if (prezzo == null || prezzo <= 0) {
            throw new IllegalArgumentException("Prezzo non valido");
        }
    }
}