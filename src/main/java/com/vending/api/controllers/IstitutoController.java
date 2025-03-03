package com.vending.api.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vending.core.models.Istituto;
import com.vending.core.repositories.IstitutoRepository;
import com.vending.core.repositories.MacchinaRepository;
import spark.Request;
import spark.Response;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller per la gestione delle richieste HTTP relative agli istituti.
 */
public class IstitutoController {
    private final IstitutoRepository istitutoRepository;
    private final MacchinaRepository macchinaRepository;
    private final Gson gson;

    /**
     * Costruttore del controller.
     *
     * @param istitutoRepository repository per gli istituti
     * @param macchinaRepository repository per le macchine
     */
    public IstitutoController(IstitutoRepository istitutoRepository, MacchinaRepository macchinaRepository) {
        this.istitutoRepository = istitutoRepository;
        this.macchinaRepository = macchinaRepository;
        this.gson = new Gson();
    }

    /**
     * Recupera tutti gli istituti.
     */
    public Object getAll(Request req, Response res) {
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
     * Recupera un istituto tramite ID.
     */
    public Object getById(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Optional<Istituto> istituto = istitutoRepository.findById(id);

            if (istituto.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("istituto", istituto.get());
                response.put("macchineAttive", istitutoRepository.contaMacchineAttive(id));

                res.type("application/json");
                return gson.toJson(response);
            } else {
                res.status(404);
                return gson.toJson(Map.of("errore", "Istituto non trovato"));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero dell'istituto: " + e.getMessage()));
        }
    }

    /**
     * Crea un nuovo istituto.
     */
    public Object create(Request req, Response res) {
        try {
            Type requestType = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> requestData = gson.fromJson(req.body(), requestType);

            String nome = requestData.get("nome");
            String indirizzo = requestData.get("indirizzo");

            if (nome == null || nome.trim().isEmpty() || indirizzo == null || indirizzo.trim().isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Nome e indirizzo sono obbligatori"));
            }

            if (istitutoRepository.existsByNome(nome)) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Esiste già un istituto con questo nome"));
            }

            Istituto istituto = new Istituto();
            istituto.setNome(nome);
            istituto.setIndirizzo(indirizzo);

            Istituto created = istitutoRepository.save(istituto);
            res.status(201);
            res.type("application/json");
            return gson.toJson(created);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella creazione dell'istituto: " + e.getMessage()));
        }
    }

    /**
     * Aggiorna un istituto esistente.
     */
    public Object update(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            Optional<Istituto> esistente = istitutoRepository.findById(id);

            if (esistente.isEmpty()) {
                res.status(404);
                return gson.toJson(Map.of("errore", "Istituto non trovato"));
            }

            Type requestType = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> requestData = gson.fromJson(req.body(), requestType);

            Istituto istituto = esistente.get();
            String nuovoNome = requestData.get("nome");
            String nuovoIndirizzo = requestData.get("indirizzo");

            if (nuovoNome != null && !nuovoNome.equals(istituto.getNome())) {
                if (istitutoRepository.existsByNome(nuovoNome)) {
                    res.status(400);
                    return gson.toJson(Map.of("errore", "Esiste già un istituto con questo nome"));
                }
                istituto.setNome(nuovoNome);
            }

            if (nuovoIndirizzo != null) {
                istituto.setIndirizzo(nuovoIndirizzo);
            }

            Istituto updated = istitutoRepository.update(istituto);
            res.type("application/json");
            return gson.toJson(updated);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'aggiornamento dell'istituto: " + e.getMessage()));
        }
    }

    /**
     * Elimina un istituto.
     */
    public Object delete(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));
            try {
                boolean deleted = istitutoRepository.delete(id);
                if (deleted) {
                    res.status(204);
                    return "";
                } else {
                    res.status(404);
                    return gson.toJson(Map.of("errore", "Istituto non trovato"));
                }
            } catch (IllegalStateException e) {
                res.status(400);
                return gson.toJson(Map.of("errore", e.getMessage()));
            }
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'eliminazione dell'istituto: " + e.getMessage()));
        }
    }
}