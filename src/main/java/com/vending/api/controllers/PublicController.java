package com.vending.api.controllers;

import com.google.gson.Gson;
import com.vending.core.models.*;
import com.vending.core.repositories.*;
import spark.Request;
import spark.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller che gestisce tutti gli endpoint pubblici del sistema.
 * Fornisce accesso alle funzionalità base per gli utenti non autenticati.
 */
public class PublicController {
    private final MacchinaRepository macchinaRepository;
    private final BevandaRepository bevandaRepository;
    private final IstitutoRepository istitutoRepository;
    private final TransazioneRepository transazioneRepository;
    private final Gson gson;

    /**
     * Costruttore del controller pubblico.
     */
    public PublicController(
            MacchinaRepository macchinaRepository,
            BevandaRepository bevandaRepository,
            IstitutoRepository istitutoRepository,
            TransazioneRepository transazioneRepository) {
        this.macchinaRepository = macchinaRepository;
        this.bevandaRepository = bevandaRepository;
        this.istitutoRepository = istitutoRepository;
        this.transazioneRepository = transazioneRepository;
        this.gson = new Gson();
    }

    /**
     * Recupera tutte le macchine attive.
     */
    public Object getMacchine(Request req, Response res) {
        try {
            List<com.vending.core.models.Macchina> macchine = macchinaRepository.findAll();
            res.type("application/json");
            return gson.toJson(macchine);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle macchine: " + e.getMessage()));
        }
    }

    /**
     * Recupera una macchina specifica per ID.
     */
    public Object getMacchinaById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            com.vending.core.models.Macchina macchina = macchinaRepository.findById(id);

            if (macchina != null) {
                res.type("application/json");
                return gson.toJson(macchina);
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero della macchina: " + e.getMessage()));
        }
    }

    /**
     * Recupera tutte le macchine di un istituto.
     */
    public Object getMacchineByIstituto(Request req, Response res) {
        try {
            int istitutoId = Integer.parseInt(req.params(":istitutoId"));
            List<com.vending.core.models.Macchina> macchine = macchinaRepository.findByIstitutoId(istitutoId);
            res.type("application/json");
            return gson.toJson(macchine);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID istituto non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle macchine: " + e.getMessage()));
        }
    }

    /**
     * Recupera lo stato attuale di una macchina.
     */
    public Object getStatoMacchina(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            com.vending.core.models.Macchina macchina = macchinaRepository.findById(id);

            if (macchina != null) {
                Map<String, Object> stato = Map.of(
                        "id", macchina.getId(),
                        "stato", macchina.getStatoId(),
                        "statoDescrizione", macchina.getStatoDescrizione(),
                        "creditoAttuale", macchina.getCreditoAttuale(),
                        "bevande", macchina.getBevande(),
                        "cialde", macchina.getCialde());

                res.type("application/json");
                return gson.toJson(stato);
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero dello stato: " + e.getMessage()));
        }
    }

    /**
     * Recupera tutte le bevande disponibili.
     */
    public Object getBevande(Request req, Response res) {
        try {
            List<Bevanda> bevande = bevandaRepository.findAll();
            res.type("application/json");
            return gson.toJson(bevande);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle bevande: " + e.getMessage()));
        }
    }

    /**
     * Recupera una bevanda specifica per ID.
     */
    public Object getBevandaById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Optional<Bevanda> bevanda = bevandaRepository.findById(id);

            if (bevanda.isPresent()) {
                res.type("application/json");
                return gson.toJson(bevanda.get());
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Bevanda non trovata"));
            }
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero della bevanda: " + e.getMessage()));
        }
    }

    /**
     * Recupera tutti gli istituti.
     */
    public Object getIstituti(Request req, Response res) {
        try {
            List<Istituto> istituti = istitutoRepository.findAll();
            res.type("application/json");
            return gson.toJson(istituti);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero degli istituti: " + e.getMessage()));
        }
    }

    /**
     * Recupera un istituto specifico per ID.
     */
    public Object getIstitutoById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Optional<Istituto> istituto = istitutoRepository.findById(id);

            if (istituto.isPresent()) {
                res.type("application/json");
                return gson.toJson(istituto.get());
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Istituto non trovato"));
            }
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero dell'istituto: " + e.getMessage()));
        }
    }

    /**
     * Gestisce l'erogazione di una bevanda.
     */
    public Object erogaBevanda(Request req, Response res) {
        try {
            int macchinaId = Integer.parseInt(req.params(":id"));
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            int bevandaId = ((Number) body.get("bevandaId")).intValue();

            com.vending.core.models.Macchina macchina = macchinaRepository.findById(macchinaId);
            if (macchina == null) {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }

            // Crea una nuova transazione
            Transazione transazione = new Transazione();
            transazione.setMacchinaId(macchinaId);
            transazione.setBevandaId(bevandaId);
            transazione = transazioneRepository.save(transazione);

            res.status(201);
            return gson.toJson(transazione);

        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore durante l'erogazione: " + e.getMessage()));
        }
    }

    /**
     * Gestisce l'inserimento del credito.
     */
    public Object inserisciCredito(Request req, Response res) {
        try {
            int macchinaId = Integer.parseInt(req.params(":id"));
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            double importo = ((Number) body.get("importo")).doubleValue();

            com.vending.core.models.Macchina macchina = macchinaRepository.findById(macchinaId);
            if (macchina == null) {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }

            // Aggiorna il credito
            double att = macchina.getCreditoAttuale();
            macchina.setCreditoAttuale(att + importo);
            macchinaRepository.update(macchina);

            return gson.toJson(Map.of(
                    "creditoAttuale", att,
                    "messaggio", "Credito aggiornato con successo"));

        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore durante l'inserimento del credito: " + e.getMessage()));
        }
    }

    public Object getBevandeMacchina(int macchinaId, Response res) {
        try {
            // Verifica esistenza macchina
            Macchina macchina = macchinaRepository.findById(macchinaId);
            if (macchina == null) {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }

            // Verifica stato macchina
            if (macchina.getStatoId() != 1) { // 1 = Attiva
                res.status(400);
                return gson.toJson(Map.of("errore", "Macchina non attiva"));
            }

            // Recupera le bevande disponibili per la macchina
            List<Map<String, Object>> bevande = macchina.getBevande().stream()
                    .map(bevanda -> {
                        Map<String, Object> bevandaInfo = new HashMap<>();
                        bevandaInfo.put("id", bevanda.getId());
                        bevandaInfo.put("nome", bevanda.getNome());
                        bevandaInfo.put("prezzo", bevanda.getPrezzo());

                        // Verifica disponibilità cialde per la bevanda
                        boolean disponibile = bevanda.isDisponibile(macchina.getCialde());
                        bevandaInfo.put("disponibile", disponibile);

                        // Aggiunge informazioni sulle cialde necessarie
                        List<Map<String, Object>> cialdeInfo = bevanda.getCialde().stream()
                                .map(cialda -> {
                                    Map<String, Object> info = new HashMap<>();
                                    info.put("id", cialda.getId());
                                    info.put("nome", cialda.getNome());
                                    info.put("tipo", cialda.getTipoCialda());

                                    // Trova la quantità disponibile della cialda
                                    Optional<QuantitaCialde> quantita = macchina.getCialde().stream()
                                            .filter(qc -> qc.getCialdaId() == cialda.getId())
                                            .findFirst();

                                    info.put("quantitaDisponibile",
                                            quantita.map(QuantitaCialde::getQuantita).orElse(0));
                                    return info;
                                })
                                .collect(Collectors.toList());

                        bevandaInfo.put("cialde", cialdeInfo);
                        return bevandaInfo;
                    })
                    .collect(Collectors.toList());

            res.type("application/json");
            return gson.toJson(bevande);

        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle bevande: " + e.getMessage()));
        }
    }

    public Object verificaDisponibilitaBevanda(int bevandaId, int macchinaId, Response res) {
        try {
            // Verifica esistenza macchina
            Macchina macchina = macchinaRepository.findById(macchinaId);
            if (macchina == null) {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }

            // Verifica stato macchina
            if (macchina.getStatoId() != 1) { // 1 = Attiva
                res.status(400);
                return gson.toJson(Map.of("errore", "Macchina non attiva"));
            }

            // Trova la bevanda nella macchina
            Optional<Bevanda> bevandaOpt = macchina.getBevande().stream()
                    .filter(b -> b.getId() == bevandaId)
                    .findFirst();

            if (bevandaOpt.isEmpty()) {
                res.status(404);
                return gson.toJson(Map.of("errore", "Bevanda non disponibile in questa macchina"));
            }

            Bevanda bevanda = bevandaOpt.get();
            boolean disponibile = bevanda.isDisponibile(macchina.getCialde());

            Map<String, Object> risultato = new HashMap<>();
            risultato.put("bevandaId", bevandaId);
            risultato.put("disponibile", disponibile);

            // Se non è disponibile, aggiungi informazioni sulle cialde mancanti
            if (!disponibile) {
                List<Map<String, Object>> cialdeMancanti = bevanda.getCialde().stream()
                        .filter(cialda -> {
                            Optional<QuantitaCialde> qc = macchina.getCialde().stream()
                                    .filter(q -> q.getCialdaId() == cialda.getId())
                                    .findFirst();
                            return qc.isEmpty() || qc.get().getQuantita() == 0;
                        })
                        .map(cialda -> {
                            Map<String, Object> info = new HashMap<>();
                            info.put("id", cialda.getId());
                            info.put("nome", cialda.getNome());
                            info.put("tipo", cialda.getTipoCialda());
                            return info;
                        })
                        .collect(Collectors.toList());

                risultato.put("cialdeMancanti", cialdeMancanti);
            }

            res.type("application/json");
            return gson.toJson(risultato);

        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella verifica disponibilità: " + e.getMessage()));
        }
    }
}