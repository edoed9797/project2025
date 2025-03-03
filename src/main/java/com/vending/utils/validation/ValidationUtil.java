package com.vending.utils.validation;

import java.util.regex.Pattern;
import java.math.BigDecimal;

public class ValidationUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[0-9]{10,13}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    );

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidImporto(BigDecimal importo) {
        return importo != null && importo.compareTo(BigDecimal.ZERO) > 0 
            && importo.scale() <= 2;
    }

    public static boolean isValidQuantita(int quantita) {
        return quantita >= 0;
    }

    public static boolean isValidPercentuale(double percentuale) {
        return percentuale >= 0 && percentuale <= 100;
    }

    public static String sanitizeInput(String input) {
        if (input == null) return null;
        // Rimuove caratteri pericolosi e XSS
        return input.replaceAll("[<>\"'%;()&+]", "");
    }

    public static String normalizeString(String input) {
        if (input == null) return null;
        return input.trim().toLowerCase();
    }
}