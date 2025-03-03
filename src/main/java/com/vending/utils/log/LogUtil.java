package com.vending.utils.log;

import java.io.*;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.vending.utils.date.DateUtil;

/**
* Gestione del logging dell'applicazione.
* Implementa un sistema di logging asincrono con coda di messaggi e thread dedicato per la scrittura su file.
* I log vengono salvati in file giornalieri nella directory "logs".
*
* @author Edoardo Giovanni Fracchia
*/
public class LogUtil {
    private static final String LOG_DIR = "logs";

    //Coda thread-safe per i messaggi di log da processare 
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();

    //Thread dedicato alla scrittura dei log su file
    private static final Thread loggerThread;

    //Flag per terminare il thread di logging
    private static volatile boolean isRunning = true;

    /**
    * Blocco statico di inizializzazione.
    * Crea la directory dei log se non esiste e avvia il thread di logging.
    */
    static {
        new File(LOG_DIR).mkdirs();

        // Inizializza thread di logging
        loggerThread = new Thread(() -> {
            while (isRunning || !logQueue.isEmpty()) {
                try {
                    LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (entry != null) {
                        scriviLog(entry);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    /**
    * Registra un messaggio di log INFO.
    *
    * @param message Il messaggio da loggare
    */
    public static void info(String message) {
        aggiungiLog(new LogEntry(LogLevel.INFO, message));
    }

    /**
    * Registra un messaggio di log WARNING.
    *
    * @param message Il messaggio da loggare
    */
    public static void warning(String message) {
        aggiungiLog(new LogEntry(LogLevel.WARNING, message));
    }

     /**
    * Registra un messaggio di log ERROR, includendo i dettagli dell'eccezione.
    *
    * @param message Il messaggio da loggare
    * @param throwable L'eccezione da includere nel log
    */
    public static void error(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");
        sb.append("Exception: ").append(throwable.getMessage()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        aggiungiLog(new LogEntry(LogLevel.ERROR, sb.toString()));
    }

    /**
    * Registra un messaggio di log DEBUG.
    *
    * @param message Il messaggio da loggare
    */
    public static void debug(String message) {
        aggiungiLog(new LogEntry(LogLevel.DEBUG, message));
    }

    /**
    * Aggiunge un nuovo messaggio alla coda di logging.
    *
    * @param entry L'entry di log da aggiungere alla coda
    */
    private static void aggiungiLog(LogEntry entry) {
        try {
            logQueue.put(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
    * Il file di log viene creato nella directory configurata con nome basato sulla data corrente.
    *
    * @param entry L'entry di log da scrivere su file
    */
    private static void scriviLog(LogEntry entry) {
        String nomeFile = LOG_DIR + File.separator + 
            LocalDateTime.now().toLocalDate() + ".log";
            
        try (PrintWriter writer = new PrintWriter(new FileWriter(nomeFile, true))) {
            writer.println(entry.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        isRunning = false;
        loggerThread.interrupt();
        try {
            loggerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
    * Classe interna che rappresenta una singola entry di log.
    * Contiene il livello, il messaggio e il timestamp del log.
    */
    private static class LogEntry {
        private final LogLevel level;
        private final String message;
        private final LocalDateTime timestamp;

        public LogEntry(LogLevel level, String message) {
            this.level = level;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s",
                timestamp.format(DateUtil.FORMATTER),
                level,
                message);
        }
    }

    /**
    * Definisce i possibili livelli di logging.
    */
    private enum LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
}