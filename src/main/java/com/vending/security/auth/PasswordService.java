package com.vending.security.auth;

import org.mindrot.jbcrypt.BCrypt;
import java.util.Objects;

public class PasswordService {

    /**
     * Genera un hash della password utilizzando BCrypt.
     *
     * @param password La password in chiaro da hashare.
     * @return L'hash della password.
     * @throws IllegalArgumentException Se la password è nulla o vuota.
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        // Genera un hash BCrypt con un salt automatico
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("Password hashata: " + hashedPassword); // Log per debug
        return hashedPassword;
    }

    /**
     * Verifica se la password in chiaro corrisponde all'hash memorizzato.
     *
     * @param plainTextPassword La password in chiaro da verificare.
     * @param hashedPassword    L'hash memorizzato della password.
     * @return true se la password è corretta, false altrimenti.
     */
    public boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty() || hashedPassword == null) {
            System.out.println("Password o hash non validi"); // Log per debug
            return false;
        }

        // Log per debug
        System.out.println("Password in chiaro: " + plainTextPassword);
        System.out.println("Hash memorizzato: " + hashedPassword);

        // Verifica la password utilizzando BCrypt
        boolean isMatch = BCrypt.checkpw(plainTextPassword, hashedPassword);
        System.out.println("Risultato verifica: " + isMatch); // Log per debug

        return isMatch;
    }

    /**
     * Verifica se l'hash memorizzato è un hash BCrypt valido.
     *
     * @param hashedPassword L'hash memorizzato della password.
     * @return true se l'hash è valido, false altrimenti.
     */
    public boolean isHashValid(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            System.out.println("Hash non valido: nullo o vuoto"); // Log per debug
            return false;
        }

        // Verifica se l'hash inizia con il prefisso di BCrypt ($2a$)
        boolean isValid = hashedPassword.startsWith("$2a$");
        System.out.println("Hash valido (BCrypt)? " + isValid); // Log per debug

        return isValid;
    }
}