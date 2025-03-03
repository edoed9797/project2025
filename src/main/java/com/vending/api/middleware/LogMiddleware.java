package com.vending.api.middleware;

import com.vending.utils.log.LogUtil;
import spark.Request;
import spark.Response;

/**
 * Gestione dei log delle richieste HTTP e degli errori.
 * Questa classe fornisce funzionalità per registrare informazioni dettagliate
 * sulle richieste in arrivo e gli eventuali errori che si verificano durante
 * l'elaborazione delle richieste.
 * 
 * @author Edoardo Giovanni Fracchia
 */
public class LogMiddleware {

    /**
     * Registra i dettagli di una richiesta HTTP e della sua risposta.
     * Il log include il metodo HTTP, il percorso richiesto, lo status code
     * della risposta e l'indirizzo IP del client.
     * 
     * @param req l'oggetto Request di Spark che contiene i dettagli della richiesta
     * @param res l'oggetto Response di Spark che contiene i dettagli della risposta
     */
    public void logRequest(Request req, Response res) {
        // Formatta il messaggio di log con i dettagli della richiesta
        String logMessage = String.format(
                "Richiesta: %s %s - Risposta: %d - IP: %s",
                req.requestMethod(), // Metodo HTTP (GET, POST, etc.)
                req.pathInfo(), // Percorso della richiesta
                res.status(), // Status code HTTP
                req.ip() // Indirizzo IP del client
        );
        // Registra il messaggio come informazione
        LogUtil.info(logMessage);
    }

    /**
     * Registra i dettagli di un errore verificatosi durante l'elaborazione
     * di una richiesta HTTP. Include il metodo HTTP, il percorso e il
     * messaggio di errore.
     * 
     * @param req l'oggetto Request di Spark associato alla richiesta che ha generato l'errore
     * @param e   l'eccezione che si è verificata durante l'elaborazione della richiesta
     */
    public void logError(Request req, Exception e) {
        // Formatta il messaggio di errore con i dettagli della richiesta
        String errorMessage = String.format(
                "Errore richiesta: %s %s: %s",
                req.requestMethod(), // Metodo HTTP della richiesta
                req.pathInfo(), // Percorso della richiesta
                e.getMessage() // Messaggio di errore dell'eccezione
        );
        // Registra il messaggio come errore, includendo lo stack trace
        LogUtil.error(errorMessage, e);
    }
}