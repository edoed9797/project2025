package com.vending.api.middleware;

import spark.Request;
import spark.Response;

/**
 * Middleware per la gestione delle policy CORS (Cross-Origin Resource Sharing).
 * Questa classe fornisce metodi per configurare le intestazioni CORS
 * appropriate nelle risposte HTTP, consentendo le richieste cross-origin 
 * in modo sicuro.
 * 
 * @author Edoardo Giovanni Fracchia
 */
public class CORSMiddleware {

    /**
     * Applica le intestazioni CORS di base alla risposta HTTP.
     * Configura le policy per consentire richieste cross-origin con impostazioni
     * predefinite.
     * 
     * @param req l'oggetto Request di Spark che rappresenta la richiesta HTTP in
     *            arrivo
     * @param res l'oggetto Response di Spark che rappresenta la risposta HTTP da
     *            inviare
     */
    public void applicaCORS(Request req, Response res) {
        // Consente richieste da qualsiasi origine
        res.header("Access-Control-Allow-Origin", "*");
        // Specifica i metodi HTTP consentiti
        res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        // Definisce le intestazioni HTTP consentite
        res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /**
     * Gestisce le richieste OPTIONS di preflight CORS.
     * Questo metodo configura le intestazioni appropriate in risposta alle
     * richieste di preflight inviate dal browser prima delle richieste CORS effettive.
     * 
     * @param req l'oggetto Request di Spark che rappresenta la richiesta HTTP di preflight
     * @param res l'oggetto Response di Spark che rappresenta la risposta HTTP da inviare
     */
    public void gestisciOpzioniPreflight(Request req, Response res) {
        // Recupera le intestazioni richieste dal client
        String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
        if (accessControlRequestHeaders != null) {
            // Configura le intestazioni consentite in base alla richiesta
            res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
        }

        // Recupera il metodo HTTP richiesto dal client
        String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
        if (accessControlRequestMethod != null) {
            // Configura i metodi HTTP consentiti in base alla richiesta
            res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
        }
    }
}