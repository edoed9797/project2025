package com.vending.security.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionService {
    private final SecretKey chiave;
    private final SecureRandom random;

    public EncryptionService() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        this.chiave = keyGen.generateKey();
        this.random = new SecureRandom();
    }

    public String cripta(String testo) {
        try {
            byte[] iv = generaIV();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, chiave, new IvParameterSpec(iv));
            
            byte[] testoEncoded = testo.getBytes();
            byte[] testoCifrato = cipher.doFinal(testoEncoded);
            
            byte[] risultato = new byte[iv.length + testoCifrato.length];
            System.arraycopy(iv, 0, risultato, 0, iv.length);
            System.arraycopy(testoCifrato, 0, risultato, iv.length, testoCifrato.length);
            
            return Base64.getEncoder().encodeToString(risultato);
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la cifratura", e);
        }
    }

    public String decripta(String testoCifrato) {
        try {
            byte[] decodedData = Base64.getDecoder().decode(testoCifrato);
            
            byte[] iv = new byte[16];
            byte[] message = new byte[decodedData.length - 16];
            
            System.arraycopy(decodedData, 0, iv, 0, 16);
            System.arraycopy(decodedData, 16, message, 0, message.length);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, chiave, new IvParameterSpec(iv));
            
            byte[] decrypted = cipher.doFinal(message);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la decifratura", e);
        }
    }

    private byte[] generaIV() {
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }
}