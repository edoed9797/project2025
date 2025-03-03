package com.vending.api.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vending.core.models.Manutenzione;
import com.vending.core.repositories.ManutenzioneRepository;
import spark.Request;
import spark.Response;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Controller per la gestione delle richieste HTTP relative alle manutenzioni.
 */
public class ManutenzioneController {
    private final ManutenzioneRepository manutenzioneRepository;
    private final Gson gson;

    /**
     * Costruttore del controller.
     *
     * @param manutenzioneRepository repository per le manutenzioni
     */
    public ManutenzioneController(ManutenzioneRepository manutenzioneRepository) {
        this.manutenzioneRepository = manutenzioneRepository;
        this.gson = new Gson();
    }

    /**
     * Recupera tutte le manutenzioni in corso.
     */
    public Object getManutenzioni(Request req, Response res) {
        try {
            List<Manutenzione> manutenzioni = manutenzioneRepository.findAll();
            res.type("application/json");
            return gson.toJson(manutenzioni);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle manutenzioni: " + e.getMessage()));
        }
    }

    /**
     * Recupera le manutenzioni di un istituto.
     */
    public Object getManutenzioniIstituto(Request req, Response res) {
        try {
            int istitutoId = Integer.parseInt(req.params(":istitutoId"));
            List<Manutenzione> manutenzioni = manutenzioneRepository.findByIstitutoId(istitutoId);
            res.type("application/json");
            return gson.toJson(manutenzioni);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID istituto non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle manutenzioni: " + e.getMessage()));
        }
    }

    /**
     * Recupera le manutenzioni assegnate a un tecnico.
     */
    public Object getManutenzioniTecnico(Request req, Response res) {
        try {
            int tecnicoId = Integer.parseInt(req.params(":tecnicoId"));
            List<Manutenzione> manutenzioni = manutenzioneRepository.findByTecnicoId(tecnicoId);
            res.type("application/json");
            return gson.toJson(manutenzioni);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID tecnico non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel recupero delle manutenzioni: " + e.getMessage()));
        }
    }

    /**
     * Inizia una nuova manutenzione.
     */
    public Object iniziaManutenzione(Request req, Response res) {
        try {
            Type requestType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> requestData = gson.fromJson(req.body(), requestType);

            int macchinaId = ((Number) requestData.get("macchinaId")).intValue();
            int tecnicoId = ((Number) requestData.get("tecnicoId")).intValue();

            if (macchinaId <= 0 || tecnicoId <= 0) {
                res.status(400);
                return gson.toJson(Map.of("errore", "ID macchina o tecnico non validi"));
            }

            Manutenzione manutenzione = new Manutenzione();
            manutenzione.setMacchinaId(macchinaId);
            manutenzione.setTecnicoId(tecnicoId);

            Manutenzione nuovaManutenzione = manutenzioneRepository.save(manutenzione);
            res.status(201);
            res.type("application/json");
            return gson.toJson(nuovaManutenzione);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nell'avvio della manutenzione: " + e.getMessage()));
        }
    }

    /**
     * Completa una manutenzione.
     */
    public Object completaManutenzione(Request req, Response res) {
        try {
            int id = Integer.parseInt(req.params(":id"));

            return manutenzioneRepository.findById(id)
                    .map(manutenzione -> {
                        try {
                            Manutenzione manutenzioneCompletata = manutenzioneRepository
                                    .completaManutenzione(manutenzione);
                            res.type("application/json");
                            return gson.toJson(manutenzioneCompletata);
                        } catch (Exception e) {
                            res.status(500);
                            return gson.toJson(
                                    Map.of("errore", "Errore nel completamento della manutenzione: " + e.getMessage()));
                        }
                    })
                    .orElseGet(() -> {
                        res.status(404);
                        return gson.toJson(Map.of("errore", "Manutenzione non trovata"));
                    });
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("errore", "ID non valido"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel completamento della manutenzione: " + e.getMessage()));
        }
    }

    /**
     * Imposta una macchina come fuori servizio.
     */
    public Object setFuoriServizio(Request req, Response res) {
        try {
            int macchinaId = Integer.parseInt(req.params(":id"));
            if (manutenzioneRepository.setFuoriServizio(macchinaId)) {
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
            return gson.toJson(Map.of("errore", "Errore nell'impostazione fuori servizio: " + e.getMessage()));
        }
    }
}