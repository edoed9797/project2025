package com.vending.security.auth;

import com.vending.core.models.Utente;
import com.vending.security.jwt.JWTService;
import com.vending.core.repositories.UtenteRepository;

public class AuthenticationService {

    private final UtenteRepository utenteRepository;
    private final PasswordService passwordService;
    private final JWTService jwtService;

    public AuthenticationService(UtenteRepository utenteRepository, PasswordService passwordService, JWTService jwtService) {
        this.utenteRepository = utenteRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    /**
     * Autentica un utente in base a username e password.
     *
     * @param username Il nome utente.
     * @param password La password in chiaro.
     * @return Un token JWT se l'autenticazione ha successo, altrimenti null.
     */
    public String authenticate(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("Username o password non validi"); // Log per debug
            return null;
        }

        // Cerca l'utente nel repository
        Utente utente = utenteRepository.findByUsername(username);

        if (utente == null) {
            System.out.println("Utente non trovato"); // Log per debug
            return null;
        }

        // Verifica la password
        if (passwordService.verifyPassword(password, utente.getPasswordHash())) {
            // Genera un token JWT
            return jwtService.generaToken(utente);
        } else {
            System.out.println("Password non valida"); // Log per debug
            return null;
        }
    }

    /**
     * Verifica se un token JWT è valido.
     *
     * @param token Il token JWT da verificare.
     * @return True se il token è valido, altrimenti false.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            System.out.println("Token non valido: nullo o vuoto"); // Log per debug
            return false;
        }

        return jwtService.verificaToken(token);
    }
}