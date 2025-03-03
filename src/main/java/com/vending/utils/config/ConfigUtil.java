package com.vending.utils.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class per la gestione della configurazione dell'applicazione.
 */
public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

    /**
     * Carica le proprietà da un file di configurazione.
     * Cerca prima nel classpath, poi nella directory corrente.
     *
     * @param filename il nome del file di configurazione
     * @return Properties oggetto contenente le proprietà caricate
     * @throws RuntimeException se il file non può essere caricato
     */
    public static Properties loadProperties(String filename) {
        Properties properties = new Properties();
        boolean loaded = false;

        // Prima prova a caricare dal classpath
        try (InputStream input = ConfigUtil.class.getClassLoader().getResourceAsStream(filename)) {
            if (input != null) {
                properties.load(input);
                loaded = true;
                logger.info("Configurazione caricata dal classpath: {}", filename);
            }
        } catch (IOException e) {
            logger.warn("Impossibile caricare la configurazione dal classpath: {}", e.getMessage());
        }

        // Se non trovato nel classpath, prova nella directory corrente
        if (!loaded) {
            try (FileInputStream input = new FileInputStream(filename)) {
                properties.load(input);
                logger.info("Configurazione caricata dal filesystem: {}", filename);
            } catch (IOException e) {
                logger.error("Impossibile caricare il file di configurazione: {}", filename, e);
                throw new RuntimeException("Impossibile caricare il file di configurazione: " + filename, e);
            }
        }

        // Logga le proprietà caricate (escludendo le password)
        logProperties(properties);

        return properties;
    }

    /**
     * Logga le proprietà caricate, mascherando le informazioni sensibili.
     *
     * @param properties le proprietà da loggare
     */
    private static void logProperties(Properties properties) {
        if (logger.isDebugEnabled()) {
            properties.stringPropertyNames().stream()
                .sorted()
                .forEach(key -> {
                    String value = properties.getProperty(key);
                    // Maschera le password e altre informazioni sensibili
                    if (key.toLowerCase().contains("password") || 
                        key.toLowerCase().contains("secret") ||
                        key.toLowerCase().contains("key")) {
                        value = "********";
                    }
                    logger.debug("{}={}", key, value);
                });
        }
    }

    /**
     * Ottiene una proprietà come stringa, con valore di default.
     *
     * @param properties le proprietà
     * @param key la chiave della proprietà
     * @param defaultValue il valore di default
     * @return il valore della proprietà o il default se non trovata
     */
    public static String getProperty(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.debug("Proprietà {} non trovata, uso il default: {}", key, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * Ottiene una proprietà come intero, con valore di default.
     *
     * @param properties le proprietà
     * @param key la chiave della proprietà
     * @param defaultValue il valore di default
     * @return il valore della proprietà come intero o il default se non trovata/non valida
     */
    public static int getIntProperty(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.debug("Proprietà {} non trovata, uso il default: {}", key, defaultValue);
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Valore non valido per {}: {}, uso il default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Ottiene una proprietà come boolean, con valore di default.
     *
     * @param properties le proprietà
     * @param key la chiave della proprietà
     * @param defaultValue il valore di default
     * @return il valore della proprietà come boolean o il default se non trovata
     */
    public static boolean getBooleanProperty(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.debug("Proprietà {} non trovata, uso il default: {}", key, defaultValue);
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}