package com.vending.api.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vending.core.models.*;
import com.vending.core.repositories.MacchinaRepository;
import com.vending.core.repositories.BevandaRepository;
import spark.Request;
import spark.Response;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller per la gestione delle richieste HTTP relative alle macchine
 * distributrici.
 */
public class MacchinaController {
    private final MacchinaRepository macchinaRepository;
    private final BevandaRepository bevandaRepository;
    private final Gson gson;

    /**
     * Costruttore del controller.
     *
     * @param macchinaRepository repository per le macchine
     */
    public MacchinaController(MacchinaRepository macchinaRepository, BevandaRepository bevandaRepository) {
        this.macchinaRepository = macchinaRepository;
        this.bevandaRepository = bevandaRepository;
        this.gson = new Gson();
    }

    /**
     * Recupera tutte le macchine.
     */
    public Object getAll(Request req, Response res) {
        try {
            List<Macchina> macchine = macchinaRepository.findAll();
            res.type("application/json");
            return gson.toJson(macchine);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle macchine: " + e.getMessage()));
        }
    }

    /**
     * Recupera una macchina tramite ID.
     */
    public Object getById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Macchina macchina = macchinaRepository.findById(id);

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
     * Recupera le macchine di un istituto.
     */
    public Object getByIstituto(Request req, Response res) {
        try {
            int istitutoId = Integer.parseInt(req.params(":istitutoId"));
            List<Macchina> macchine = macchinaRepository.findByIstitutoId(istitutoId);

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

    public Object getStatoMacchina(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Macchina macchina = macchinaRepository.findById(id);

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
     * Aggiorna lo stato di una macchina.
     */
    public Object updateStato(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Type requestType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            int nuovoStatoId = ((Number) requestData.get("statoId")).intValue();
            if (nuovoStatoId < 1 || nuovoStatoId > 3) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Stato non valido"));
            }

            boolean aggiornato = macchinaRepository.aggiornaStato(id, nuovoStatoId);
            if (aggiornato) {
                return gson.toJson(macchinaRepository.findById(id));
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'aggiornamento dello stato: " + e.getMessage()));
        }
    }
    
    /**
     * Aggiorna le bevande associate a una macchina.
     *
     * @param req richiesta HTTP contenente l'ID della macchina e la lista delle bevande
     * @param res risposta HTTP
     * @return JSON della macchina aggiornata
     */
    public Object updateBevandeMacchina(Request req, Response res) {
        try {
            int macchinaId = Integer.parseInt(req.params(":id"));
            
            // Parsa il corpo della richiesta
            Type requestType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);
            
            // Estrae la lista delle bevande dal corpo della richiesta
            @SuppressWarnings("unchecked")
            List<Integer> bevandeIds = (List<Integer>) requestData.get("bevandeIds");
            
            if (bevandeIds == null) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Lista delle bevande mancante"));
            }
            
            // Recupera la macchina
            Macchina macchina = macchinaRepository.findById(macchinaId);
            if (macchina == null) {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }
            
            // Aggiorna le bevande della macchina
            macchinaRepository.aggiornaBevande(macchina, bevandeIds);
            
            // Restituisce la macchina aggiornata
            res.type("application/json");
            return gson.toJson(macchina);
            
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID macchina non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'aggiornamento delle bevande della macchina: " + e.getMessage()));
        }
    }

    /**
     * Crea una nuova macchina.
     */
    public Object create(Request req, Response res) {
        try {
            Type requestType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            Macchina macchina = new Macchina();
            macchina.setIstitutoId(((Number) requestData.get("istitutoId")).intValue());
            macchina.setStatoId(1); // Stato iniziale: Attiva
            macchina.setCassaAttuale(0.0);
            macchina.setCassaMassima(((Number) requestData.get("cassaMassima")).doubleValue());

            Macchina nuovaMacchina = macchinaRepository.save(macchina);
            res.status(201);
            res.type("application/json");
            return gson.toJson(nuovaMacchina);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella creazione della macchina: " + e.getMessage()));
        }
    }

    /**
     * Elimina una macchina.
     */
    public Object delete(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            boolean deleted = macchinaRepository.delete(id);
            if (deleted) {
                res.status(204);
                return "";
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Macchina non trovata"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'eliminazione della macchina: " + e.getMessage()));
        }
    }
}