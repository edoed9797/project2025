package com.vending.iot.mqtt;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.function.Consumer;

public class MessageHandler {
    private final Gson gson;

    public MessageHandler() {
        this.gson = new Gson();
    }

    public <T> void gestisciMessaggio(String payload, Class<T> tipo, Consumer<T> handler) {
        try {
            T oggetto = gson.fromJson(payload, tipo);
            handler.accept(oggetto);
        } catch (JsonSyntaxException e) {
            System.err.println("Errore nella deserializzazione del messaggio: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore nella gestione del messaggio: " + e.getMessage());
        }
    }

    public String creaMessaggio(Object oggetto) {
        try {
            return gson.toJson(oggetto);
        } catch (Exception e) {
            System.err.println("Errore nella serializzazione del messaggio: " + e.getMessage());
            return "{}";
        }
    }

    public static class MessaggioStato {
        public String stato;
        public long timestamp;

        public MessaggioStato(String stato) {
            this.stato = stato;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class MessaggioErrore {
        public String codice;
        public String descrizione;
        public long timestamp;

        public MessaggioErrore(String codice, String descrizione) {
            this.codice = codice;
            this.descrizione = descrizione;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class MessaggioAllarme {
        public String tipo;
        public String messaggio;
        public int livelloSeverita;
        public long timestamp;

        public MessaggioAllarme(String tipo, String messaggio, int livelloSeverita) {
            this.tipo = tipo;
            this.messaggio = messaggio;
            this.livelloSeverita = livelloSeverita;
            this.timestamp = System.currentTimeMillis();
        }
    }
}