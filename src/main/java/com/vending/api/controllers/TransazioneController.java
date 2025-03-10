package com.vending.api.controllers;

import com.google.gson.Gson;
import com.vending.ServiceRegistry;
import com.vending.core.models.Transazione;
import com.vending.core.services.TransazioneService;
import spark.Request;
import spark.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransazioneController {
    private final TransazioneService transazioneService;
    private final Gson gson;

    public TransazioneController(TransazioneService transazioneService) {
        this.transazioneService = transazioneService;
        this.gson =  ServiceRegistry.get(Gson.class);
    }

    public Object getAllTransazioni(Request req, Response res) {
        try {
            List<Transazione> transazioni = transazioneService.getTutteTransazioni();
            res.type("application/json");
            return gson.toJson(transazioni);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle transazioni: " + e.getMessage()));
        }
    }

    public Object getTransazioneById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Optional<Transazione> transazione = transazioneService.getTransazione(id);
            
            if (transazione.isPresent()) {
                res.type("application/json");
                return gson.toJson(transazione.get());
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Transazione non trovata"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero della transazione: " + e.getMessage()));
        }
    }

    public Object getTransazioniByMacchina(Request req, Response res) {
        try {
            int macchinaId = Integer.parseInt(req.params(":macchinaId"));
            List<Transazione> transazioni = transazioneService.getTransazioniMacchina(macchinaId);
            res.type("application/json");
            return gson.toJson(transazioni);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID macchina non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle transazioni: " + e.getMessage()));
        }
    }

    public Object getTransazioniRecenti(Request req, Response res) {
        try {
            List<Transazione> transazioni = transazioneService.getTransazioniRecenti();
            res.type("application/json");
            return gson.toJson(transazioni);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle transazioni recenti: " + e.getMessage()));
        }
    }

    public Object createTransazione(Request req, Response res) {
        try {
            Transazione transazione = gson.fromJson(req.body(), Transazione.class);
            transazione = transazioneService.registraTransazione(transazione);
            res.status(201);
            res.type("application/json");
            return gson.toJson(transazione);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella creazione della transazione: " + e.getMessage()));
        }
    }
}