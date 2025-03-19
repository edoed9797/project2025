package com.vending.api.middleware;

import com.google.gson.Gson;
import com.vending.ServiceRegistry;
import com.vending.security.jwt.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Middleware per la gestione dell'autenticazione e autorizzazione delle
 * richieste HTTP.
 * Questa classe gestisce la verifica dei token JWT, l'accesso agli endpoint
 * pubblici e i controlli di autorizzazione basati sui ruoli.
 */
public class AuthMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);
    private final JWTService jwtService;
    private final Gson gson;
    
    /**
     * Costruttore della classe AuthMiddleware.
     * Inizializza il servizio JWT e l'oggetto Gson per la serializzazione JSON.
     */
    public AuthMiddleware() {
    	this.jwtService = ServiceRegistry.get(JWTService.class);
        this.gson = new Gson();
    }

    // Lista di endpoint che non richiedono autenticazione (completamente pubblici)
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/anonymous",
            "/api/auth/verify",
            "/css",
            "/js",
            "/index.html",
            "/favicon.ico",
            "/api/istituti",
            "/api/istituti/*",
            "/api/macchine",
            "/api/macchine/*",
            "/api/bevande",
            "/api/bevande/*"
    );

    // Lista di endpoint accessibili a utenti anonimi (token anonimo richiesto)
    private static final List<String> ANONYMOUS_ENDPOINTS = Arrays.asList(
            "/api/macchine",
            "/api/istituti",
            "/api/bevande"
    );

    /**
     * Verifica l'autenticazione di una richiesta HTTP.
     * Controlla se l'endpoint è pubblico, verifica la presenza e validità del token JWT,
     * e gestisce i token anonimi.
     *
     * @param req l'oggetto Request di Spark contenente gli headers e il path della richiesta
     * @param res l'oggetto Response di Spark per impostare lo status e il corpo della risposta
     * @return true se l'autenticazione ha successo, false altrimenti
     */
    public boolean autenticazione(Request req, Response res) {
        String path = req.pathInfo();
        
        // Log dettagliato per debug
        String authHeader = req.headers("Authorization");
        logger.debug("Auth check: path={}, method={}, hasAuthHeader={}", 
                     path, req.requestMethod(), (authHeader != null));
        
        // Se è un endpoint pubblico, autorizza immediatamente
        if (isPublicEndpoint(path) || path.startsWith("/api/macchine") || path.startsWith("/api/istituti")) {
            return true;
        }

        // Se è un endpoint per risorse statiche, autorizza
        if (path.startsWith("/pages/") || path.endsWith(".html") || 
            path.endsWith(".css") || path.endsWith(".js")) {
            logger.debug("Static resource: {}", path);
            return true;
        }

        // Per gli altri endpoint, richiedi un token di autenticazione
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing/malformed auth header for: {}", path);
            res.status(401);
            res.body(gson.toJson(Map.of(
                "errore", "Token di autorizzazione mancante o malformato",
                "codice", "AUTH_HEADER_INVALID"
            )));
            return false;
        }

        String token = authHeader.substring(7); // Rimuove "Bearer "
        
        // Debug del token per identificare problemi
        if (token.isEmpty()) {
            logger.warn("Token empty for path: {}", path);
            res.status(401);
            res.body(gson.toJson(Map.of("errore", "Token vuoto")));
            return false;
        }

        // Per endpoint accessibili da utenti anonimi
        if (isAnonymousAccessible(path)) {
            // Se il token contiene "anonymous" (verifica rapida senza decodifica)
            if (token.contains("anonymous") || token.contains("roleKey\":\"anonymous\"")) {
                logger.debug("Anonymous access granted to: {}", path);
                return true;
            }
        }

        // Verifica standard del token JWT
        try {
            if (jwtService.verificaToken(token)) {
                setUserInfo(req, token);
                return true;
            } else {
                res.status(401);
                res.body(gson.toJson(Map.of(
                    "errore", "Token non valido o scaduto",
                    "codice", "AUTH_TOKEN_INVALID"
                )));
                return false;
            }
        } catch (Exception e) {
            logger.error("Token verification error: {} for path: {}", e.getMessage(), path);
            res.status(401);
            res.body(gson.toJson(Map.of(
                "errore", "Errore nella verifica del token: " + e.getMessage(),
                "codice", "AUTH_ERROR"
            )));
            return false;
        }
    }

    /**
     * Verifica se un path è accessibile con token anonimo.
     */
    private boolean isAnonymousAccessible(String path) {
        return ANONYMOUS_ENDPOINTS.stream()
            .anyMatch(endpoint -> path.equals(endpoint) || 
                     path.startsWith(endpoint + "/"));
    }

    /**
     * Verifica se un path è un endpoint pubblico che non richiede autenticazione.
     */
    private boolean isPublicEndpoint(String path) {
<<<<<<< HEAD
        // Aggiungi esplicitamente gli endpoint che devono essere pubblici
        
        for (String endpoint : PUBLIC_ENDPOINTS) {
=======
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
                "/api/auth/*",
                "/pages/client/machineSelection.html",
                "/css/*",
                "/js/*"
        };

        for (String endpoint : publicEndpoints) {
>>>>>>> db87796c018d1cbad937929e10d85e2abf0d0ff6
            if (endpoint.endsWith("/*")) {
                String baseEndpoint = endpoint.substring(0, endpoint.length() - 2);
                if (path.startsWith(baseEndpoint)) {
                    return true;
                }
            } else if (path.equals(endpoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Imposta informazioni utente come attributi della richiesta.
     */
    private void setUserInfo(Request req, String token) {
        try {
            String ruolo = jwtService.getClaimFromToken(token, "roleKey");
            String username = jwtService.getClaimFromToken(token, "username");
            
            req.attribute("userRole", ruolo);
            req.attribute("username", username);
            
            logger.debug("Set user info: role={}, username={}", ruolo, username);
        } catch (Exception e) {
            logger.warn("Cannot set user info from token: {}", e.getMessage());
        }
    }

    /**
     * Verifica autorizzazione amministrativa.
     */
    public boolean autorizzazioneAdmin(Request req, Response res) {
        String userRole = (String) req.attribute("userRole");
        
        logger.debug("Admin auth check: userRole={}", userRole);

        if (userRole == null || userRole.isEmpty()) {
            res.status(401);
            res.body(gson.toJson(Map.of("errore", "Ruolo dell'utente mancante")));
            return false;
        }

        // Verifica se il ruolo è "Amministratore", "Impiegato" o "Tecnico"
        if (!"Amministratore".equals(userRole) && 
            !"Impiegato".equals(userRole) && 
            !"Tecnico".equals(userRole)) {
            
            res.status(403);
            res.body(gson.toJson(Map.of("errore", "Accesso non autorizzato!")));
            return false;
        }

        return true;
    }
}