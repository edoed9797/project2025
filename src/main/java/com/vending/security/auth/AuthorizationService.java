package com.vending.security.auth;

import com.vending.core.models.Utente;
import com.vending.security.jwt.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servizio che gestisce le autorizzazioni degli utenti nel sistema.
 * Implementa la logica per il controllo dei permessi basati sui ruoli.
 */
public class AuthorizationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    private final JWTService jwtService;
    
    // Costanti per i claims del token
    private static final String ROLE_CLAIM = "role";
    
    // Mappa che associa ogni ruolo alle sue azioni permesse
    private static final Map<String, List<String>> ROLE_PERMISSIONS = new HashMap<>();
    
    static {
        // Permessi per amministratori
        ROLE_PERMISSIONS.put("ADMIN", Arrays.asList(
            "VIEW_SCHOOLS",
            "ADD_SCHOOL",
            "REMOVE_SCHOOL",
            "VIEW_MACHINES",
            "ADD_MACHINE",
            "REMOVE_MACHINE",
            "VIEW_MACHINE_STATUS",
            "SEND_TECHNICIAN",
            "VIEW_REVENUES",
            "MANAGE_USERS"
        ));
        
        // Permessi per impiegati
        ROLE_PERMISSIONS.put("EMPLOYEE", Arrays.asList(
            "VIEW_SCHOOLS",
            "VIEW_MACHINES",
            "VIEW_MACHINE_STATUS",
            "SEND_TECHNICIAN"
        ));
        
        // Permessi per utenti anonimi
        ROLE_PERMISSIONS.put("ANONYMOUS", Arrays.asList(
            "VIEW_SCHOOLS",
            "VIEW_MACHINES",
            "VIEW_BEVERAGES"
        ));
    }

    /**
     * Costruttore del servizio di autorizzazione.
     * 
     * @param jwtService servizio JWT per la validazione dei token
     */
    public AuthorizationService(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Verifica se un utente ha il permesso di eseguire una determinata azione.
     * 
     * @param utente l'utente da verificare
     * @param permission il permesso richiesto
     * @return true se l'utente ha il permesso, false altrimenti
     */
    public boolean hasPermission(Utente utente, String permission) {
        if (utente == null || permission == null) {
            logger.warn("Tentativo di verifica permesso con utente o permesso null");
            return false;
        }

        String ruolo = utente.getRuolo().toUpperCase();
        List<String> permissions = ROLE_PERMISSIONS.get(ruolo);
        
        if (permissions == null) {
            logger.warn("Ruolo non trovato: {}", ruolo);
            return false;
        }

        boolean hasPermission = permissions.contains(permission);
        logger.debug("Verifica permesso '{}' per utente {}: {}", 
                    permission, utente.getUsername(), hasPermission);
        
        return hasPermission;
    }

    /**
     * Verifica se un token JWT è valido e se l'utente associato ha il permesso richiesto.
     * 
     * @param token il token JWT da verificare
     * @param permission il permesso richiesto
     * @return true se il token è valido e l'utente ha il permesso, false altrimenti
     */
    public boolean validateTokenAndPermission(String token, String permission) {
        try {
            if (!jwtService.verificaToken(token)) {
                logger.warn("Token JWT non valido");
                return false;
            }

            String ruolo = jwtService.getRuoloDaToken(token);
            List<String> permissions = ROLE_PERMISSIONS.get(ruolo);
            
            if (permissions == null) {
                logger.warn("Ruolo non trovato nel token: {}", ruolo);
                return false;
            }

            return permissions.contains(permission);
            
        } catch (Exception e) {
            logger.error("Errore durante la validazione del token e permesso", e);
            return false;
        }
    }

    /**
     * Verifica se l'utente ha il ruolo di amministratore.
     * 
     * @param utente l'utente da verificare
     * @return true se l'utente è amministratore, false altrimenti
     */
    public boolean isAdmin(Utente utente) {
        if (utente == null) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(utente.getRuolo());
    }

    /**
     * Verifica se l'utente ha il ruolo di impiegato.
     * 
     * @param utente l'utente da verificare
     * @return true se l'utente è impiegato, false altrimenti
     */
    public boolean isEmployee(Utente utente) {
        if (utente == null) {
            return false;
        }
        return "EMPLOYEE".equalsIgnoreCase(utente.getRuolo());
    }

    /**
     * Ottiene la lista dei permessi associati a un ruolo specifico.
     * 
     * @param ruolo il ruolo di cui ottenere i permessi
     * @return la lista dei permessi associati al ruolo, o null se il ruolo non esiste
     */
    public List<String> getPermissionsForRole(String ruolo) {
        if (ruolo == null) {
            return null;
        }
        return ROLE_PERMISSIONS.get(ruolo.toUpperCase());
    }

    /**
     * Estrae il ruolo da un token JWT.
     * 
     * @param token il token JWT da cui estrarre il ruolo
     * @return il ruolo dell'utente contenuto nel token
     * @throws Exception se il token non è valido o non contiene il claim del ruolo
     */
    public String getRoleFromToken(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            logger.error("Token nullo o vuoto");
            throw new IllegalArgumentException("Token non può essere nullo o vuoto");
        }

        try {
            // Estraiamo il claim 'role' dal token tramite il JWTService
            String role = jwtService.getClaimFromToken(token, ROLE_CLAIM);
            
            if (role == null || role.isEmpty()) {
                logger.error("Ruolo non trovato nel token");
                throw new Exception("Ruolo non trovato nel token");
            }

            // Verifichiamo che il ruolo sia uno di quelli validi
            if (!ROLE_PERMISSIONS.containsKey(role.toUpperCase())) {
                logger.error("Ruolo non valido trovato nel token: {}", role);
                throw new Exception("Ruolo non valido nel token");
            }

            logger.debug("Ruolo estratto dal token: {}", role);
            return role.toUpperCase();

        } catch (Exception e) {
            logger.error("Errore nell'estrazione del ruolo dal token", e);
            throw new Exception("Errore nell'estrazione del ruolo dal token", e);
        }
    }
}