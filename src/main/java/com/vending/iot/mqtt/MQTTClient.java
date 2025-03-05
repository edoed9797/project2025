package com.vending.iot.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Classe che rappresenta un client MQTT per la connessione, pubblicazione e sottoscrizione a un broker MQTT.
 * Questa versione utilizza MQTTBrokerManager per centralizzare la gestione delle connessioni MQTT.
 */
public class MQTTClient {
    private static final Logger logger = LoggerFactory.getLogger(MQTTClient.class);
    private final MqttClient client;
    private final String clientId;
    private final Map<String, BiConsumer<String, String>> topicHandlers;
    private volatile boolean isConnecting;
    private int retryCount;

    /**
     * Costruttore per creare un'istanza del client MQTT.
     *
     * @param prefix Prefisso da utilizzare per generare l'ID univoco del client.
     * @throws MqttException Se si verifica un errore durante l'inizializzazione del client.
     * @throws IllegalArgumentException Se il prefisso è nullo o vuoto.
     */
    public MQTTClient(String prefix) throws MqttException {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Il prefisso del client non può essere vuoto");
        }

        this.clientId = prefix;
        this.topicHandlers = new ConcurrentHashMap<>();
        this.retryCount = 0;
        this.isConnecting = false;

        try {
            // Utilizza il manager per ottenere il client MQTT
            this.client = MQTTBrokerManager.getInstance().getClient(clientId);
            configureMqttCallback();
            logger.info("Client MQTT inizializzato con ID: {}", clientId);
        } catch (MqttException e) {
            logger.error("Errore durante l'inizializzazione del client MQTT: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Configura il callback per gestire eventi come la perdita di connessione, l'arrivo di messaggi e il completamento della consegna.
     */
    private void configureMqttCallback() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                logger.warn("Connessione persa per il client {}: {}", clientId, cause.getMessage());
                scheduleReconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                try {
                    String payload = new String(message.getPayload());
                    BiConsumer<String, String> handler = topicHandlers.get(topic);
                    if (handler != null) {
                        handler.accept(topic, payload);
                    } else {
                        // Cerca handler con wildcards
                        for (Map.Entry<String, BiConsumer<String, String>> entry : topicHandlers.entrySet()) {
                            String pattern = entry.getKey().replace("+", "[^/]+").replace("#", ".*");
                            if (topic.matches(pattern)) {
                                entry.getValue().accept(topic, payload);
                                return;
                            }
                        }
                        logger.debug("Nessun handler trovato per il topic: {}", topic);
                    }
                } catch (Exception e) {
                    logger.error("Errore durante l'elaborazione del messaggio: {}", e.getMessage());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                logger.debug("Consegna completata per il messaggio con ID: {}", token.getMessageId());
            }
        });
    }

    /**
     * Pianifica un tentativo di riconnessione al broker MQTT in caso di perdita di connessione.
     */
    private synchronized void scheduleReconnect() {
        if (!isConnecting && retryCount < MQTTConfig.MAX_RETRY_ATTEMPTS) {
            logger.info("Tentativo di riconnessione {} in corso...", retryCount + 1);
            
            new Thread(() -> {
                try {
                    // Backoff esponenziale: aumenta l'attesa ad ogni tentativo
                    long waitTime = MQTTConfig.RETRY_INTERVAL * (long)Math.pow(2, retryCount);
                    Thread.sleep(waitTime);
                    tentaRiconnessione();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else if (retryCount >= MQTTConfig.MAX_RETRY_ATTEMPTS) {
            logger.error("Numero massimo di tentativi di riconnessione raggiunto");
        }
    }

    /**
     * Pubblica un messaggio su un topic specifico.
     *
     * @param topic Il topic su cui pubblicare il messaggio.
     * @param message Il messaggio da pubblicare.
     * @throws MqttException Se si verifica un errore durante la pubblicazione.
     */
    public void publish(String topic, String message) throws MqttException {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(MQTTConfig.QOS);
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            logger.error("Errore durante la pubblicazione sul topic {}: {}", topic, e.getMessage());
            throw e;
        }
    }

    /**
     * Pubblica un messaggio con ritenzione su un topic specifico.
     *
     * @param topic Il topic su cui pubblicare il messaggio.
     * @param message Il messaggio da pubblicare.
     * @throws MqttException Se si verifica un errore durante la pubblicazione.
     */
    public void publishRetained(String topic, String message) throws MqttException {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(MQTTConfig.QOS);
            mqttMessage.setRetained(true);
            client.publish(topic, mqttMessage);
            logger.debug("Messaggio ritenuto pubblicato sul topic: {}", topic);
        } catch (MqttException e) {
            logger.error("Errore durante la pubblicazione con ritenzione sul topic {}: {}", 
                topic, e.getMessage());
            throw e;
        }
    }

    /**
     * Sottoscrive il client a un topic specifico e registra un handler per gestire i messaggi in arrivo.
     *
     * @param topic Il topic a cui sottoscriversi.
     * @param handler L'handler per gestire i messaggi in arrivo.
     * @throws MqttException Se si verifica un errore durante la sottoscrizione.
     */
    public void subscribe(String topic, BiConsumer<String, String> handler) throws MqttException {
        try {
            client.subscribe(topic, MQTTConfig.QOS);
            topicHandlers.put(topic, handler);
            logger.info("Client {} sottoscritto al topic: {}", clientId, topic);
        } catch (MqttException e) {
            logger.error("Errore durante la sottoscrizione al topic {}: {}", topic, e.getMessage());
            throw e;
        }
    }

    /**
     * Disconnette il client dal broker MQTT.
     */
    public void disconnect() {
        // Utilizza il manager per rimuovere il client
        MQTTBrokerManager.getInstance().removeClient(clientId);
    }

    /**
     * Verifica se il client è attualmente connesso al broker.
     *
     * @return true se il client è connesso, false altrimenti.
     */
    public boolean isConnected() {
        return client.isConnected();
    }
    
    private void tentaRiconnessione() {
        if (!client.isConnected() && retryCount < MQTTConfig.MAX_RETRY_ATTEMPTS) {
            retryCount++;
            logger.info("Tentativo di riconnessione {} per il client {}", retryCount, clientId);
            try {
                // Utilizza il broker manager per riconnettersi
                MQTTBrokerManager.getInstance().connect(client, 
                    MQTTBrokerManager.getInstance().createDefaultConnectOptions());
                
                // Se la connessione è riuscita
                if (client.isConnected()) {
                    logger.info("Riconnessione riuscita per il client {}", clientId);
                    retryCount = 0;
                    
                    // Ripristina le sottoscrizioni
                    for (String topic : topicHandlers.keySet()) {
                        client.subscribe(topic, MQTTConfig.QOS);
                        logger.debug("Sottoscrizione ripristinata per topic: {}", topic);
                    }
                }
            } catch (MqttException e) {
                logger.error("Riconnessione fallita per il client {}: {}", clientId, e.getMessage());
                // Pianifica il prossimo tentativo
                scheduleReconnect();
            }
        } else if (retryCount >= MQTTConfig.MAX_RETRY_ATTEMPTS) {
            logger.error("Numero massimo di tentativi di riconnessione raggiunto per il client {}", clientId);
        }
    }
}