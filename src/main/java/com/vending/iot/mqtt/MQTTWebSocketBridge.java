package com.vending.iot.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge MQTT che collega le connessioni WebSocket (porta 9002) alle connessioni MQTT standard (porta 8883).
 * Permette all'interfaccia web di comunicare con i componenti backend del sistema.
 * Utilizza MQTTBrokerManager per centralizzare la gestione delle connessioni MQTT.
 */
public class MQTTWebSocketBridge {
    private static final Logger logger = LoggerFactory.getLogger(MQTTWebSocketBridge.class);
    
    private final MQTTClient mqttClientStandard; // Client connesso su 8883 (SSL)
    private final MQTTClient mqttClientWebSocket; // Client connesso su 9002 (WebSocket)
    private final ExecutorService executorService;
    private final AtomicBoolean running;
    private final Map<String, String> topicMappings;
    
    /**
     * Crea un nuovo bridge MQTT tra WebSocket e connessioni standard.
     */
    public MQTTWebSocketBridge() throws MqttException {
        // Utilizziamo identificatori univoci per i client
        String clientIdStandard = "bridge_standard_" + System.currentTimeMillis();
        String clientIdWebSocket = "bridge_websocket_" + System.currentTimeMillis();
        
        // Utilizziamo il broker manager per ottenere i client MQTT
        this.mqttClientStandard = new MQTTClient(clientIdStandard);
        this.mqttClientWebSocket = new MQTTClient(clientIdWebSocket);
        
        this.executorService = Executors.newSingleThreadExecutor();
        this.running = new AtomicBoolean(true);
        this.topicMappings = new HashMap<>();
        
        // Configura i mapping dei topic che vuoi intercettare
        setupTopicMappings();
    }
    
    /**
     * Configura i mapping dei topic tra WebSocket e MQTT standard.
     */
    private void setupTopicMappings() {
        // Topic per le macchine
        addTopicMapping("macchine/+/stato", "macchine/+/stato");
        addTopicMapping("macchine/+/cassa/#", "macchine/+/cassa/#");
        addTopicMapping("macchine/+/bevande/#", "macchine/+/bevande/#");
        addTopicMapping("macchine/+/cialde/#", "macchine/+/cialde/#");
        addTopicMapping("macchine/+/manutenzione/#", "macchine/+/manutenzione/#");
        addTopicMapping("macchine/+/operazioni/#", "macchine/+/operazioni/#");
        addTopicMapping("macchine/+/comandi/#", "macchine/+/comandi/#");
    }
    
    /**
     * Aggiunge un mapping tra topic WebSocket e MQTT standard.
     */
    public void addTopicMapping(String wsTopicPattern, String standardTopicPattern) {
        topicMappings.put(wsTopicPattern, standardTopicPattern);
    }
    
    /**
     * Avvia il bridge MQTT.
     */
    public void start() throws MqttException {
        // Connetti i client MQTT
        connectStandardClient();
        connectWebSocketClient();
        
        // Avvia il demone che mantiene attivo il bridge
        executorService.submit(this::keepAlive);
        
        logger.info("MQTT WebSocket Bridge avviato con successo");
    }
    
    /**
     * Connette il client MQTT standard (8883).
     */
    private void connectStandardClient() throws MqttException {
        // Controlla se il client è già connesso
        if (mqttClientStandard.isConnected()) {
            logger.info("Client MQTT standard già connesso");
            return;
        }
        
        // Le opzioni di connessione sono gestite dal broker manager
        // È comunque necessario sottoscrivere ai topic
        
        // Sottoscrivi a tutti i topic necessari sul client standard
        for (String standardTopic : topicMappings.values()) {
            mqttClientStandard.subscribe(standardTopic, (topic, message) -> {
                // Inoltra il messaggio al client WebSocket
                forwardToWebSocket(topic, message);
            });
        }
        
        logger.info("Client MQTT standard connesso su {}", MQTTConfig.BROKER_URL);
    }
    
    /**
     * Connette il client MQTT WebSocket (9002).
     */
    private void connectWebSocketClient() throws MqttException {
        // Controlla se il client è già connesso
        if (mqttClientWebSocket.isConnected()) {
            logger.info("Client MQTT WebSocket già connesso");
            return;
        }
        
        // La connessione è gestita dal broker manager, ma è ancora necessario
        // sottoscrivere ai topic
        
        // Sottoscrivi a tutti i topic necessari sul client WebSocket
        for (String wsTopic : topicMappings.keySet()) {
            mqttClientWebSocket.subscribe(wsTopic, (topic, message) -> {
                // Inoltra il messaggio al client standard
                forwardToStandard(topic, message);
            });
        }
        
        logger.info("Client MQTT WebSocket connesso su {}", MQTTConfig.BROKER_WS_URL);
    }
    
    /**
     * Inoltra un messaggio dal client standard al client WebSocket.
     */
    private void forwardToWebSocket(String topic, String message) {
        try {
            mqttClientWebSocket.publish(topic, message);
            logger.debug("Messaggio inoltrato a WebSocket: {}", topic);
        } catch (MqttException e) {
            logger.error("Errore nell'inoltro del messaggio a WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Inoltra un messaggio dal client WebSocket al client standard.
     */
    private void forwardToStandard(String topic, String message) {
        try {
            mqttClientStandard.publish(topic, message);
            logger.debug("Messaggio inoltrato a MQTT standard: {}", topic);
        } catch (MqttException e) {
            logger.error("Errore nell'inoltro del messaggio a MQTT standard: {}", e.getMessage());
        }
    }
    
    /**
     * Mantiene attivo il bridge e gestisce la riconnessione in caso di problemi.
     */
    private void keepAlive() {
        while (running.get()) {
            try {
                // Verifica la connessione dei client
                if (!mqttClientStandard.isConnected()) {
                    logger.warn("Client MQTT standard disconnesso, tentativo di riconnessione...");
                    connectStandardClient();
                }
                
                if (!mqttClientWebSocket.isConnected()) {
                    logger.warn("Client MQTT WebSocket disconnesso, tentativo di riconnessione...");
                    connectWebSocketClient();
                }
                
                Thread.sleep(10000); // Controlla ogni 10 secondi
            } catch (Exception e) {
                logger.error("Errore nel keep-alive del bridge: {}", e.getMessage());
                try {
                    Thread.sleep(5000); // Attendi 5 secondi prima di riprovare
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * Arresta il bridge MQTT.
     */
    public void stop() {
        running.set(false);
        executorService.shutdownNow();
        
        // Disconnetti i client tramite MQTTClient, che ora utilizza il broker manager
        mqttClientStandard.disconnect();
        mqttClientWebSocket.disconnect();
        
        logger.info("MQTT WebSocket Bridge arrestato");
    }
}