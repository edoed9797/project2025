package com.vending.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.vending.core.models.Utente;
import com.vending.security.config.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.Map;

/**
 * Servizio per la gestione dei token JWT (JSON Web Token).
 * Gestisce la creazione, verifica e decodifica dei token di autenticazione.
 */
public class JWTService {
    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);
    private final Algorithm algorithm;
    private static final String SECRET = "Pissir2025!";
    private static final Algorithm algo = Algorithm.HMAC256(SECRET);

    /**
     * Costruttore che inizializza l'algoritmo di firma JWT usando la chiave segreta configurata.
     */
    public JWTService() {
        this.algorithm = algo;
    }

    /**
     * Genera un nuovo token JWT per l'utente specificato.
     *
     * @param utente l'utente per cui generare il token
     * @return il token JWT generato
     */
    public String generaToken(Utente utente) {
        return JWT.create()
                .withSubject(String.valueOf(utente.getId()))
                .withClaim("username", utente.getUsername())
                .withClaim("roleKey", utente.getRuolo())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConfig.JWT_EXPIRATION))
                .sign(algo); // Usa l'algoritmo statico
    }
    

    public static String generateAnonymousToken(Map<String, Object> tokenData) {
        try {
            return JWT.create()
                .withSubject("anonymous")
                .withClaim("username", "anonymous_user")
                .withClaim("roleKey", "anonymous")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)))
                .sign(algo); // Usa l'algoritmo statico
        } catch (Exception e) {
            throw new RuntimeException("Error generating anonymous JWT token", e);
        }
    }

    /**
     * Verifica la validità di un token JWT.
     *
     * @param token il token da verificare
     * @return true se il token è valido, false altrimenti
     */
    public boolean verificaToken(String token) {
    	try {
            // Verifica se è un token anonimo (inizia con "anonymous_")
            if (token.startsWith("anonymous_")) {
                return true; // Considera valido il token anonimo
            }
            
            // Normale verifica per i token autenticati
            JWT.require(algorithm)
               .build()
               .verify(token);
            return true;
        } catch (JWTVerificationException e) {
            logger.warn("Token JWT non valido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Estrae il ruolo dell'utente dal token JWT.
     *
     * @param token il token da cui estrarre il ruolo
     * @return il ruolo dell'utente
     * @throws JWTVerificationException se il token non è valido
     */
    public String getRuoloDaToken(String token) {
        try {
            DecodedJWT jwt = decodificaToken(token);
            return jwt.getClaim("roleKey").asString();
        } catch (Exception e) {
            logger.error("Errore nell'estrazione del ruolo dal token", e);
            throw new JWTVerificationException("Impossibile estrarre il ruolo dal token", e);
        }
    }

    /**
     * Estrae l'ID dell'utente dal token JWT.
     *
     * @param token il token da cui estrarre l'ID
     * @return l'ID dell'utente
     * @throws JWTVerificationException se il token non è valido
     */
    public int getUtenteIdDaToken(String token) {
        try {
            DecodedJWT jwt = decodificaToken(token);
            return Integer.parseInt(jwt.getSubject());
        } catch (Exception e) {
            logger.error("Errore nell'estrazione dell'ID utente dal token", e);
            throw new JWTVerificationException("Impossibile estrarre l'ID utente dal token", e);
        }
    }

    /**
     * Estrae l'username dell'utente dal token JWT.
     *
     * @param token il token da cui estrarre l'username
     * @return l'username dell'utente
     * @throws JWTVerificationException se il token non è valido
     */
    public String getUsernameDaToken(String token) {
        try {
            DecodedJWT jwt = decodificaToken(token);
            return jwt.getClaim("username").asString();
        } catch (Exception e) {
            logger.error("Errore nell'estrazione dell'username dal token", e);
            throw new JWTVerificationException("Impossibile estrarre l'username dal token", e);
        }
    }

    /**
     * Verifica se un token JWT è scaduto.
     *
     * @param token il token da verificare
     * @return true se il token è scaduto, false altrimenti
     */
    public boolean isTokenScaduto(String token) {
        try {
            DecodedJWT jwt = decodificaToken(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            logger.error("Errore nella verifica della scadenza del token", e);
            return true;
        }
    }

    /**
     * Estrae un claim specifico dal token JWT.
     *
     * @param token il token da cui estrarre il claim
     * @param claimName il nome del claim da estrarre
     * @return il valore del claim come stringa
     * @throws JWTVerificationException se il token non è valido o il claim non esiste
     */
    public String getClaimFromToken(String token, String claimName) {
        try {
            DecodedJWT jwt = decodificaToken(token);
            String claim = jwt.getClaim(claimName).asString();
            if (claim == null) {
                throw new JWTVerificationException("Claim " + claimName + " non trovato nel token");
            }
            return claim;
        } catch (Exception e) {
            logger.error("Errore nell'estrazione del claim {} dal token", claimName, e);
            throw new JWTVerificationException("Impossibile estrarre il claim dal token", e);
        }
    }

    /**
     * Decodifica un token JWT.
     *
     * @param token il token da decodificare
     * @return il token decodificato
     * @throws JWTVerificationException se il token non è valido
     */
    private DecodedJWT decodificaToken(String token) {
        try {
            return JWT.require(algorithm)
                     .build()
                     .verify(token);
        } catch (Exception e) {
            logger.error("Errore nella decodifica del token JWT", e);
            throw new JWTVerificationException("Token non valido", e);
        }
    }
}