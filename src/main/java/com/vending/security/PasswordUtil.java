package com.vending.security;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    public boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
    
     public boolean verificaPassword(String password, String passwordUtente) {
       try {
           String[] parts = passwordUtente.split(":");
           byte[] salt = Base64.getDecoder().decode(parts[0]);
           byte[] hashSalvato = Base64.getDecoder().decode(parts[1]);
           byte[] hashCalcolato = generaHash(password.toCharArray(), salt);
           
           return comparaHash(hashSalvato, hashCalcolato);
       } catch (Exception e) {
           throw new RuntimeException("Errore verifica password", e);
       }
   }

   private byte[] generaHash(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
       PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 256);
       SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
       return factory.generateSecret(spec).getEncoded();
   }

   private boolean comparaHash(byte[] hash1, byte[] hash2) {
       if (hash1.length != hash2.length) return false;
       int diff = 0;
       for (int i = 0; i < hash1.length; i++) {
           diff |= hash1[i] ^ hash2[i];
       }
       return diff == 0;
   }
}