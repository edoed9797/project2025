package com.vending.api.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vending.core.models.Utente;
import com.vending.core.repositories.UtenteRepository;
import com.vending.utils.date.LocalDateTimeTypeAdapter;
import spark.Request;
import spark.Response;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller per la gestione delle richieste HTTP relative agli utenti.
 */
public class UtenteController {
    private final UtenteRepository utenteRepository;
    private final Gson gson;

    /**
     * Costruttore del controller.
     *
     * @param utenteRepository repository per la gestione degli utenti
     */
    public UtenteController(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .create();
    }

    /**
     * Recupera tutti gli utenti.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return lista JSON degli utenti
     */
    public Object getAll(Request req, Response res) {
        try {
            List<Utente> utenti = utenteRepository.findAll();
            res.type("application/json");
            return gson.toJson(utenti);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero degli utenti: " + e.getMessage()));
        }
    }

    /**
     * Recupera un utente tramite ID.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return JSON dell'utente
     */
    public Object getById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Optional<Utente> utente = utenteRepository.findById(id);

            if (utente.isPresent()) {
                res.type("application/json");
                return gson.toJson(utente.get());
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Utente non trovato"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero dell'utente: " + e.getMessage()));
        }
    }

    /**
     * Recupera un utente tramite username.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return JSON dell'utente
     */
    public Object getByUsername(Request req, Response res) {
        try {
            String username = req.params(":username");
            Utente utente = utenteRepository.findByUsername(username);

            if (utente != null) {
                res.type("application/json");
                return gson.toJson(utente);
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Utente non trovato"));
            }
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero dell'utente: " + e.getMessage()));
        }
    }

    /**
     * Recupera gli utenti per ruolo.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return lista JSON degli utenti con il ruolo specificato
     */
    public Object getByRuolo(Request req, Response res) {
        try {
            String ruolo = req.params(":ruolo");
            List<Utente> utenti = utenteRepository.findByRuolo(ruolo);
            res.type("application/json");
            return gson.toJson(utenti);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero degli utenti per ruolo: " + e.getMessage()));
        }
    }

    /**
     * Crea un nuovo utente.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return JSON dell'utente creato
     */
    public Object create(Request req, Response res) {
        try {
            Type requestType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            String nome = (String) requestData.get("nome");
            String ruolo = (String) requestData.get("ruolo");
            String username = (String) requestData.get("username");
            String password = (String) requestData.get("password");

            if (nome == null || ruolo == null || username == null || password == null) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Parametri mancanti"));
            }

            Utente utente = new Utente();
            utente.setNome(nome);
            utente.setRuolo(ruolo);
            utente.setUsername(username);
            utente.setPassword(password);

            Utente nuovoUtente = utenteRepository.save(utente);
            res.status(201);
            res.type("application/json");
            return gson.toJson(nuovoUtente);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella creazione dell'utente: " + e.getMessage()));
        }
    }

    /**
     * Aggiorna un utente esistente.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return JSON dell'utente aggiornato
     */
    public Object update(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Type requestType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            String nome = (String) requestData.get("nome");
            String ruolo = (String) requestData.get("ruolo");

            if (nome == null || ruolo == null) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Parametri mancanti"));
            }

            Utente utente = new Utente();
            utente.setId(id);
            utente.setNome(nome);
            utente.setRuolo(ruolo);

            Utente utenteAggiornato = utenteRepository.update(utente);
            res.type("application/json");
            return gson.toJson(utenteAggiornato);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'aggiornamento dell'utente: " + e.getMessage()));
        }
    }

    /**
     * Elimina un utente.
     *
     * @param req richiesta HTTP
     * @param res risposta HTTP
     * @return risposta vuota o errore
     */
    public Object delete(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            boolean deleted = utenteRepository.delete(id);

            if (deleted) {
                res.status(204);
                return "";
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Utente non trovato"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'eliminazione dell'utente: " + e.getMessage()));
        }
    }
}