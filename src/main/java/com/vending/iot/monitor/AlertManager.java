package com.vending.iot.monitor;

import com.google.gson.Gson;
import com.vending.iot.mqtt.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class AlertManager {
    private final MQTTClient mqttClient;
    private final Gson gson;
    private final Map<String, Integer> contatoreFallimenti;

    public AlertManager(MQTTClient mqttClient) {
        this.mqttClient = mqttClient;
        this.gson = new Gson();
        this.contatoreFallimenti = new ConcurrentHashMap<>();
    }

    public void inviaAlert(int macchinaId, String tipo, String messaggio, int severita) {
        try {
            Alert alert = new Alert(macchinaId, tipo, messaggio, severita);
            String topic = "monitoraggio/alert/" + macchinaId;
            mqttClient.publish(topic, gson.toJson(alert));
            
            // Reset contatore fallimenti per questa macchina
            contatoreFallimenti.remove(String.valueOf(macchinaId));
            
            // Se severità alta, invia anche notifica di manutenzione
            if (severita == 3) {
                inviaNotificaManutenzione(macchinaId, messaggio);
            }
            
        } catch (MqttException e) {
            gestisciFallimentoInvio(macchinaId);
        }
    }

    private void inviaNotificaManutenzione(int macchinaId, String messaggio) {
        try {
            Map<String, Object> notifica = Map.of(
                "macchinaId", macchinaId,
                "messaggio", messaggio,
                "timestamp", System.currentTimeMillis(),
                "urgente", true
            );
            mqttClient.publish("manutenzione/richieste/" + macchinaId, gson.toJson(notifica));
        } catch (MqttException e) {
            System.err.println("Errore nell'invio notifica manutenzione: " + e.getMessage());
        }
    }

    private void gestisciFallimentoInvio(int macchinaId) {
        String key = String.valueOf(macchinaId);
        int contatore = contatoreFallimenti.getOrDefault(key, 0) + 1;
        contatoreFallimenti.put(key, contatore);
        
        if (contatore >= 3) {
            System.err.println("Errore critico: impossibile inviare alert per la macchina " + macchinaId);
            // Qui potrebbe essere implementata una logica di fallback
        }
    }

    private static class Alert {
        private final int macchinaId;
        private final String tipo;
        private final String messaggio;
        private final int severita;
        private final long timestamp;

        public Alert(int macchinaId, String tipo, String messaggio, int severita) {
            this.macchinaId = macchinaId;
            this.tipo = tipo;
            this.messaggio = messaggio;
            this.severita = severita;
            this.timestamp = System.currentTimeMillis();
        }
    }
}