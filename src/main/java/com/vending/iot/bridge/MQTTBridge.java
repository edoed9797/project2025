package com.vending.iot.bridge;

import com.vending.iot.mqtt.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge MQTT che gestisce la comunicazione tra broker MQTT locale e centrale.
 * Questa classe implementa un meccanismo di bridging bidirezionale che inoltra
 * i messaggi tra un broker MQTT locale (di una scuola) e un broker MQTT
 * centrale,
 * applicando regole di inoltro configurabili per topic specifici.
 *
 * @author [Esoardo Giovanni Fracchia]
 * @version 1.0
 * @see com.vending.iot.mqtt.MQTTClient
 * @see com.vending.iot.bridge.BridgeConfig
 */
public class MQTTBridge {
    private final String scuolaId;
    private final MQTTClient clientLocale;
    private final MQTTClient clientCentrale;
    private final Set<String> topicDaInoltrare;
    private final BridgeConfig config;

    /**
     * Costruisce un nuovo bridge MQTT per una specifica scuola.
     * Inizializza le connessioni ai broker locale e centrale e configura
     * il meccanismo di bridging.
     *
     * @param scuolaId identificativo univoco della scuola
     * @param config   configurazione del bridge contenente parametri di connessione
     *                 e mappatura topic
     * @throws MqttException se si verificano errori durante l'inizializzazione
     *                       delle connessioni MQTT
     */
    public MQTTBridge(String scuolaId, BridgeConfig config) throws MqttException {
        this.scuolaId = scuolaId;
        this.config = config;
        this.topicDaInoltrare = ConcurrentHashMap.newKeySet();
        this.clientLocale = new MQTTClient("bridge_locale_" + scuolaId);
        this.clientCentrale = new MQTTClient("bridge_centrale_" + scuolaId);

        inizializzaBridge();
    }

    /**
     * Inizializza le sottoscrizioni ai topic su entrambi i broker e
     * configura i callback per l'inoltro dei messaggi.
     *
     * @throws MqttException se si verificano errori durante la sottoscrizione ai
     *                       topic
     */
    private void inizializzaBridge() throws MqttException {
        // Sottoscrizione ai topic locali
        clientLocale.subscribe("#", (topic, messaggio) -> {
            if (deveEssereInoltrato(topic)) {
                inoltraAlCentrale(topic, messaggio);
            }
        });

        // Sottoscrizione ai topic centrali
        clientCentrale.subscribe("#", (topic, messaggio) -> {
            if (deveEssereInoltrato(topic)) {
                inoltraAlLocale(topic, messaggio);
            }
        });
    }

    /**
     * Aggiunge un pattern di topic alla lista dei topic da inoltrare.
     * Supporta wildcards MQTT (+ e #).
     *
     * @param topic pattern del topic da aggiungere alla lista di inoltro
     */
    public void aggiungiTopicDaInoltrare(String topic) {
        topicDaInoltrare.add(topic);
    }

    /**
     * Rimuove un pattern di topic dalla lista dei topic da inoltrare.
     *
     * @param topic pattern del topic da rimuovere dalla lista di inoltro
     */
    public void rimuoviTopicDaInoltrare(String topic) {
        topicDaInoltrare.remove(topic);
    }

    /**
     * Verifica se un topic deve essere inoltrato in base ai pattern configurati.
     * Converte i pattern MQTT in espressioni regolari per il matching.
     *
     * @param topic topic da verificare
     * @return true se il topic corrisponde a uno dei pattern da inoltrare
     */
    private boolean deveEssereInoltrato(String topic) {
        return topicDaInoltrare.stream()
                .anyMatch(pattern -> topic.matches(pattern.replace("+", "[^/]+").replace("#", ".*")));
    }

    /**
     * Inoltra un messaggio dal broker locale al broker centrale,
     * applicando le regole di mappatura dei topic configurate.
     *
     * @param topic     topic originale del messaggio
     * @param messaggio contenuto del messaggio da inoltrare
     */
    private void inoltraAlCentrale(String topic, String messaggio) {
        try {
            String topicCentrale = config.getTopicCentrale(scuolaId, topic);
            clientCentrale.publish(topicCentrale, messaggio);
        } catch (MqttException e) {
            System.err.println("Errore nell'inoltro al broker centrale: " + e.getMessage());
        }
    }

    /**
     * Inoltra un messaggio dal broker centrale al broker locale,
     * applicando le regole di mappatura dei topic configurate.
     *
     * @param topic     topic originale del messaggio
     * @param messaggio contenuto del messaggio da inoltrare
     */
    private void inoltraAlLocale(String topic, String messaggio) {
        try {
            String topicLocale = config.getTopicLocale(scuolaId, topic);
            clientLocale.publish(topicLocale, messaggio);
        } catch (MqttException e) {
            System.err.println("Errore nell'inoltro al broker locale: " + e.getMessage());
        }
    }

    /**
     * Chiude le connessioni MQTT e libera le risorse associate al bridge.
     * Deve essere chiamato quando il bridge non è più necessario.
     */
    public void spegni() {
        clientLocale.disconnect();
        clientCentrale.disconnect();
    }
}