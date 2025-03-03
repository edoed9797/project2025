package com.vending.api.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vending.Main;
import com.vending.core.models.AdminLogin;
import com.vending.core.models.Utente;
import com.vending.core.services.AdminLoginService;
import com.vending.core.services.UtenteService;
import com.vending.security.jwt.JWTService;
import spark.Request;
import spark.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller responsabile della gestione delle richieste HTTP relative
 * all'autenticazione.
 * Gestisce le operazioni di login, registrazione e gestione token per gli
 * utenti del sistema.
 * 
 * <p>
 * Questo controller utilizza:
 * <ul>
 * <li>{@link AdminLoginService} per la gestione dell'autenticazione e delle
 * credenziali</li>
 * <li>{@link UtenteService} per la gestione degli utenti</li>
 * <li>{@link JWTService} per la generazione e validazione dei token JWT</li>
 * </ul>
 * </p>
 */
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private final AdminLoginService adminLoginService;
    private final UtenteService utenteService;
    private final JWTService jwtService;
    private final Gson gson;

    /**
     * Costruisce un nuovo AuthController.
     * 
     * @param adminLoginService il servizio per la gestione degli accessi
     *                          amministrativi
     * @param utenteService     il servizio per la gestione degli utenti
     */
    public AuthController(AdminLoginService adminLoginService, UtenteService utenteService) {
        this.adminLoginService = adminLoginService;
        this.utenteService = utenteService;
        this.jwtService = new JWTService();
        this.gson = new Gson();
    }

    /**
     * Gestisce le richieste di login degli utenti.
     * 
     * <p>
     * Questa operazione si aspetta una richiesta POST con un corpo JSON contenente:
     * <ul>
     * <li>username: String - nome utente per l'accesso</li>
     * <li>password: String - password dell'utente</li>
     * </ul>
     * </p>
     * 
     * <p>
     * La risposta può essere:
     * <ul>
     * <li>200 OK: login riuscito, con token JWT e informazioni utente</li>
     * <li>400 Bad Request: dati mancanti o non validi</li>
     * <li>401 Unauthorized: credenziali non valide</li>
     * <li>500 Internal Server Error: errore interno del server</li>
     * </ul>
     * </p>
     *
     * @param req la richiesta HTTP contenente le credenziali
     * @param res la risposta HTTP da inviare al client
     * @return oggetto JSON contenente il risultato dell'operazione
     */
    public Object login(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(
                req.body(), 
                new TypeToken<Map<String, String>>(){}.getType()
            );
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Username e password sono obbligatori"));
            }

            Optional<AdminLogin> loginResult = adminLoginService.autenticaUtente(username, password);

            if (loginResult.isPresent()) {
                AdminLogin login = loginResult.get();
                Utente utente = login.getUtente();
                String token = jwtService.generaToken(utente);

                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("ruolo", utente.getRuolo());
                response.put("username", login.getUsername());

                res.status(200);
                return gson.toJson(response);
            } else {
                res.status(401);
                return gson.toJson(Map.of("errore", "Credenziali non valide"));
            }
        } catch (Exception e) {
            logger.error("Errore durante il login", e);
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore interno del server"));
        }
    }

    /**
     * Gestisce le richieste di registrazione di nuovi utenti.
     * 
     * <p>
     * Questa operazione si aspetta una richiesta POST con un corpo JSON contenente:
     * <ul>
     * <li>nome: String - nome completo dell'utente</li>
     * <li>username: String - nome utente desiderato per l'accesso</li>
     * <li>password: String - password desiderata</li>
     * <li>ruolo: String - ruolo dell'utente nel sistema</li>
     * </ul>
     * </p>
     * 
     * <p>
     * La risposta può essere:
     * <ul>
     * <li>201 Created: registrazione riuscita, con dettagli utente</li>
     * <li>400 Bad Request: dati mancanti o non validi</li>
     * <li>500 Internal Server Error: errore interno del server</li>
     * </ul>
     * </p>
     *
     * @param req la richiesta HTTP contenente i dati di registrazione
     * @param res la risposta HTTP da inviare al client
     * @return oggetto JSON contenente il risultato dell'operazione
     */
    public Object registrazione(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(
                req.body(), 
                new TypeToken<Map<String, String>>(){}.getType()
            );
            String nome = body.get("nome");
            String username = body.get("username");
            String password = body.get("password");
            String ruolo = body.get("ruolo");

            if (nome == null || username == null || password == null || ruolo == null) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Tutti i campi sono obbligatori"));
            }

            if (!adminLoginService.isUsernameDisponibile(username)) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Username già in uso"));
            }

            int idRuolo = utenteService.trovaIdRuolo(ruolo);
            if (idRuolo == -1) {
                res.status(400);
                return gson.toJson(Map.of("errore", "Ruolo non valido"));
            }

            Utente nuovoUtente = new Utente();
            nuovoUtente.setNome(nome);
            nuovoUtente.setUsername(username);
            nuovoUtente.setPassword(password);
            nuovoUtente.setRuoloId(idRuolo);
            if (ruolo.equals("1" )|| ruolo.equals("tecnico")|| ruolo.equals("Tecnico")) {
                nuovoUtente.setRuolo("Tecnico");
            }
            if (ruolo.equals("2")|| ruolo.equals("amministratore")|| ruolo.equals("Amministratore")) {
                nuovoUtente.setRuolo("Amministratore");
            }
            if (ruolo.equals("3") || ruolo.equals("operatore") || ruolo.equals("Operatore")) {
                nuovoUtente.setRuolo("Operatore");
            }

            Utente utenteSalvato = utenteService.creaUtente(nuovoUtente);
            AdminLogin accessoCreato = adminLoginService.creaAccessoAmministrativo(
                    utenteSalvato.getId(),
                    username,
                    password);

            Map<String, Object> response = new HashMap<>();
            response.put("utente", utenteSalvato);
            response.put("username", accessoCreato.getUsername());

            res.status(201);
            return gson.toJson(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione durante la registrazione", e);
            res.status(400);
            return gson.toJson(Map.of("errore", e.getMessage()));
        } catch (Exception e) {
            logger.error("Errore durante la registrazione", e);
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore interno del server"));
        }
    }

    /**
     * Verifica la validità di un token JWT.
     * 
     * <p>
     * Questa operazione si aspetta un header 'Authorization'
     * contenente il token JWT nel formato 'Bearer {token}'.
     * </p>
     *
     * @param req la richiesta HTTP contenente il token da verificare
     * @param res la risposta HTTP da inviare al client
     * @return oggetto JSON contenente il risultato della verifica
     */
    public Object verificaToken(Request req, Response res) {
        try {
            String token = req.headers("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                res.status(401);
                return gson.toJson(Map.of("errore", "Token mancante o non valido"));
            }

            token = token.substring(7);
            boolean isValid = jwtService.verificaToken(token);

            if (!isValid) {
                res.status(401);
                return gson.toJson(Map.of("errore", "Token non valido"));
            }

            res.type("application/json");
            return gson.toJson(Map.of("valido", true));

        } catch (Exception e) {
            logger.error("Errore durante la verifica del token", e);
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nella verifica del token"));
        }
    }

    /**
     * Genera un nuovo token di accesso anonimo.
     * Utile per accessi temporanei o funzionalità limitate.
     *
     * @param req la richiesta HTTP
     * @param res la risposta HTTP da inviare al client
     * @return oggetto JSON contenente il token anonimo generato
     */
    public Object getAnonymousToken(Request req, Response res) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", "anonymous_" + UUID.randomUUID().toString());
            claims.put("role", "anonymous");
            claims.put("createdAt", System.currentTimeMillis());

            String token = JWTService.generateAnonymousToken(claims);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", "anonymous");

            res.type("application/json");
            res.status(200);
            return gson.toJson(response);
        } catch (Exception e) {
            logger.error("Errore nella generazione del token anonimo", e);
            res.status(500);
            return gson.toJson(Map.of("error", "Errore nella generazione del token anonimo"));
        }
    }

    /**
     * Rinnova un token JWT esistente.
     * Verifica la validità del token attuale e ne genera uno nuovo se valido.
     * 
     * <p>
     * Questa operazione richiede un token valido nell'header Authorization
     * nel formato 'Bearer {token}'
     * </p>
     *
     * <p>
     * La risposta può essere:
     * <ul>
     * <li>200 OK: token rinnovato con successo</li>
     * <li>401 Unauthorized: token non valido o scaduto</li>
     * <li>500 Internal Server Error: errore durante il rinnovo</li>
     * </ul>
     * </p>
     *
     * @param req la richiesta HTTP contenente il token da rinnovare
     * @param res la risposta HTTP
     * @return oggetto JSON contenente il nuovo token o un messaggio di errore
     */
    public Object refreshToken(Request req, Response res) {
        try {
            String token = req.headers("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                res.status(401);
                return gson.toJson(Map.of("errore", "Token mancante o non valido"));
            }

            token = token.substring(7); // Rimuove "Bearer "

            // Verifica la validità del token
            if (!jwtService.verificaToken(token)) {
                res.status(401);
                return gson.toJson(Map.of("errore", "Token non valido"));
            }

            // Estrae l'ID utente dal token
            int userId = jwtService.getUtenteIdDaToken(token);

            // Recupera i dati di login dell'utente
            Optional<AdminLogin> loginOpt = adminLoginService.getAccessoPerUtente(userId);

            if (loginOpt.isEmpty()) {
                res.status(401);
                return gson.toJson(Map.of("errore", "Utente non trovato"));
            }

            AdminLogin login = loginOpt.get();
            Utente utente = login.getUtente();

            // Aggiorna l'ultimo accesso
            login.aggiornaUltimoAccesso();
            // adminLoginRepository.updateUltimoAccesso(login);

            // Genera un nuovo token
            String nuovoToken = jwtService.generaToken(utente);

            // Prepara la risposta
            Map<String, Object> response = new HashMap<>();
            response.put("token", nuovoToken);
            response.put("ruolo", utente.getRuolo());
            response.put("username", login.getUsername());
            response.put("timestamp", System.currentTimeMillis());

            res.type("application/json");
            res.status(200);
            return gson.toJson(response);

        } catch (Exception e) {
            logger.error("Errore nel refresh del token", e);
            res.status(500);
            return gson.toJson(Map.of("errore", "Errore nel refresh del token: " + e.getMessage()));
        }
    }
}