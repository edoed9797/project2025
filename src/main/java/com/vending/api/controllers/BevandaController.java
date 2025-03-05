package com.vending.api.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vending.core.models.Bevanda;
import com.vending.core.services.BevandaService;
import spark.Request;
import spark.Response;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller per la gestione delle richieste HTTP relative alle bevande.
 */
public class BevandaController {
    private final BevandaService bevandaService;
    private final Gson gson;

    /**
     * Costruttore del controller.
     *
     * @param bevandaService servizio per la gestione delle bevande
     */
    public BevandaController(BevandaService bevandaService) {
        this.bevandaService = bevandaService;
        this.gson = new Gson();
    }

    /**
     * Recupera tutte le bevande.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return lista JSON delle bevande
     */
    public Object getAll(Request req, Response res) {
        try {
            List<Bevanda> bevande = bevandaService.getTutteBevande();
            res.type("application/json");
            return gson.toJson(bevande);
        } catch (Exception e) {
            e.printStackTrace(); // Per debugging
            System.err.println("Errore dettagliato: " + e.getMessage());
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle bevande: " + e.getMessage()));
        }
    }

    /**
     * Recupera una bevanda tramite ID.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return JSON della bevanda
     */
    public Object getById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Optional<Bevanda> bevanda = bevandaService.getBevandaById(id);

            if (bevanda.isPresent()) {
                res.type("application/json");
                return gson.toJson(bevanda.get());
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Bevanda non trovata"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero della bevanda: " + e.getMessage()));
        }
    }
    
    /**
     * Crea una nuova bevanda.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return JSON della bevanda creata
     */
    public Object create(Request req, Response res) {
        try {
            Type requestType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            String nome = (String) requestData.get("nome");
            Double prezzo = ((Number) requestData.get("prezzo")).doubleValue();
            @SuppressWarnings("unchecked")
            List<Integer> cialdeIds = (List<Integer>) requestData.get("cialdeIds");

            if (nome == null || prezzo == null || cialdeIds == null) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Parametri mancanti"));
            }

            Bevanda nuovaBevanda = bevandaService.creaBevanda(nome, prezzo, cialdeIds);
            res.status(201);
            res.type("application/json");
            return gson.toJson(nuovaBevanda);
        } catch (IllegalArgumentException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella creazione della bevanda: " + e.getMessage()));
        }
    }

    /**
     * Aggiorna una bevanda esistente.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return JSON della bevanda aggiornata
     */
    public Object update(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Type requestType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            String nome = (String) requestData.get("nome");
            Double prezzo = requestData.get("prezzo") != null ? ((Number) requestData.get("prezzo")).doubleValue()
                    : null;
            @SuppressWarnings("unchecked")
            List<Integer> cialdeIds = (List<Integer>) requestData.get("cialdeIds");

            try {
                Bevanda bevandaAggiornata = bevandaService.aggiornaBevanda(id, nome, prezzo, cialdeIds);
                res.type("application/json");
                return gson.toJson(bevandaAggiornata);
            } catch (IllegalArgumentException e) {
                res.status(404);
                return gson.toJson(Map.of("errore", e.getMessage()));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'aggiornamento della bevanda: " + e.getMessage()));
        }
    }

    /**
     * Elimina una bevanda.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return risposta vuota o errore
     */
    public Object delete(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            if (bevandaService.eliminaBevanda(id)) {
                res.status(204);
                return "";
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Bevanda non trovata"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'eliminazione della bevanda: " + e.getMessage()));
        }
    }

    /**
     * Aggiunge una cialda a una bevanda.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return risposta vuota o errore
     */
    @SuppressWarnings("unused")
    public Object aggiungiCialda(Request req, Response res) {
        try {
            int bevandaId = Integer.parseInt(req.params(":id"));
            Type requestType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            Integer cialdaId = ((Number) requestData.get("cialdaId")).intValue();
            if (cialdaId == null) {
                res.status(400);
                return gson.toJson(Map.of("errore", "ID cialda mancante"));
            }

            bevandaService.aggiungiCialda(bevandaId, cialdaId);
            res.status(204);
            return "";
        } catch (IllegalArgumentException e) {
            res.status(404);
            return gson.toJson(Map.of("errore", e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'aggiunta della cialda: " + e.getMessage()));
        }
    }

    /**
     * Rimuove una cialda da una bevanda.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return risposta vuota o errore
     */
    public Object rimuoviCialda(Request req, Response res) {
        try {
            int bevandaId = Integer.parseInt(req.params(":id"));
            int cialdaId = Integer.parseInt(req.params(":cialdaId"));

            bevandaService.rimuoviCialda(bevandaId, cialdaId);
            res.status(204);
            return "";
        } catch (IllegalArgumentException e) {
            res.status(404);
            return gson.toJson(Map.of("errore", e.getMessage()));
        } catch (IllegalStateException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", e.getMessage()));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella rimozione della cialda: " + e.getMessage()));
        }
    }
}