package com.vending.api.middleware;

import com.google.gson.Gson;
import com.vending.security.jwt.JWTService;
import spark.Request;
import spark.Response;

import java.util.Map;

/**
 * Middleware per la gestione dell'autenticazione e autorizzazione delle
 * richieste HTTP.
 * Questa classe gestisce la verifica dei token JWT, l'accesso agli endpoint
 * pubblici
 * e i controlli di autorizzazione basati sui ruoli.
 *
 * @author Edoardo Giovanni Fracchia
 * @see com.vending.security.jwt.JWTService
 */
public class AuthMiddleware {
    private final JWTService jwtService;
    private final Gson gson;

    /**
     * Costruttore della classe AuthMiddleware.
     * Inizializza il servizio JWT e l'oggetto Gson per la serializzazione JSON.
     */
    public AuthMiddleware() {
        this.jwtService = new JWTService();
        this.gson = new Gson();
    }

    /**
     * Verifica l'autenticazione di una richiesta HTTP.
     * Controlla se l'endpoint è pubblico, verifica la presenza e validità del token
     * JWT,
     * e gestisce i token anonimi.
     *
     * @param req l'oggetto Request di Spark contenente gli headers e il path della
     *            richiesta
     * @param res l'oggetto Response di Spark per impostare lo status e il corpo
     *            della risposta
     * @return true se l'autenticazione ha successo, false altrimenti
     */
    public boolean autenticazione(Request req, Response res) {
        // Verifica se il path è pubblico
        if (isPublicEndpoint(req.pathInfo())) {
            return true;
        }

        String authHeader = req.headers("Authorization");

        // Verifica la presenza e il formato dell'header Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.status(401);
            res.body(gson.toJson(Map.of("errore", "Token di autorizzazione mancante o malformato")));
            return false;
        }

        String token = authHeader.substring(7); // Rimuove "Bearer "

        // Gestione token anonimi
        if (token.startsWith("anonymous_")) {
            return true;
        }

        // Verifica del token JWT
        try {
            if (!jwtService.verificaToken(token)) {
                res.status(401);
                res.body(gson.toJson(Map.of("errore", "Token non valido")));
                return false;
            }
            return true;
        } catch (Exception e) {
            res.status(401);
            res.body(gson.toJson(Map.of("errore", "Errore nella verifica del token: " + e.getMessage())));
            return false;
        }
    }

    /**
     * Verifica se un path corrisponde a un endpoint pubblico.
     * Supporta sia path esatti che pattern con wildcard.
     *
     * @param path il path della richiesta da verificare
     * @return true se il path corrisponde a un endpoint pubblico, false altrimenti
     */
    private boolean isPublicEndpoint(String path) {
        // Lista degli endpoint pubblici con supporto per wildcard
        String[] publicEndpoints = {
                "/api/istituti",
                "/api/istituti/*",
                "/api/macchine",
                "/api/macchine/*",
                "/api/macchine/istituto/*",
                "/api/bevande",
                "/api/bevande/*",
                "/api/auth/*",
                "/api/auth/*"
        };

        for (String endpoint : publicEndpoints) {
            if (endpoint.endsWith("/*")) {
                String baseEndpoint = endpoint.substring(0, endpoint.length() - 2);
                if (path.startsWith(baseEndpoint)) {
                    return true;
                }
            } else {
                if (path.equals(endpoint)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica se l'utente ha i privilegi di amministratore.
     * Controlla il ruolo presente nell'header "user_role".
     *
     * @param req l'oggetto Request di Spark contenente l'header di autorizzazione
     * @param res l'oggetto Response di Spark per impostare lo status e il corpo
     *            della risposta
     * @return true se l'utente è un amministratore, false altrimenti
     */
    public boolean autorizzazioneAdmin(Request req, Response res) {
        String userRole = req.headers("user_role");

        if (userRole == null || userRole.isEmpty()) {
            res.status(401);
            res.body(gson.toJson(Map.of("errore", "Ruolo dell'utente mancante")));
            return false;
        }

        // Verifica se il ruolo è "Amministratore", "Impiegato" o "Tecnico"
        if (!"Amministratore".equals(userRole) && !"Impiegato".equals(userRole) && !"Tecnico".equals(userRole)) {
            res.status(403);
            res.body(gson.toJson(Map.of("errore", "Accesso non autorizzato!")));
            return false;
        }

        return true;
    }
}