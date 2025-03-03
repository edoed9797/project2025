package com.vending.utils.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Currency;

public class MoneyUtil {
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final NumberFormat FORMATTER = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private static final double[] MONETE_VALIDE = {0.05, 0.10, 0.20, 0.50, 1.00, 2.00};

    public static BigDecimal arrotondaDueDecimali(BigDecimal importo) {
        return importo.setScale(2, RoundingMode.HALF_UP);
    }

    public static String formattaValuta(BigDecimal importo) {
        return FORMATTER.format(importo);
    }

    public static BigDecimal parseImporto(String importo) {
        try {
            String cleaned = importo.replace(EUR.getSymbol(), "")
                                  .replace(".", "")
                                  .replace(",", ".");
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato importo non valido");
        }
    }

    public static boolean isMonetaValida(double importo) {
        for (double moneta : MONETE_VALIDE) {
            if (Math.abs(importo - moneta) < 0.001) {
                return true;
            }
        }
        return false;
    }

    public static BigDecimal calcolaResto(BigDecimal importoPagato, BigDecimal importoDovuto) {
        if (importoPagato.compareTo(importoDovuto) < 0) {
            throw new IllegalArgumentException("Importo pagato insufficiente");
        }
        return arrotondaDueDecimali(importoPagato.subtract(importoDovuto));
    }
}