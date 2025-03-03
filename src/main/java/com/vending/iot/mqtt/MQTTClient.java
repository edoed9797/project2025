package com.vending.iot.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.function.BiConsumer;

/**
 * Classe che rappresenta un client MQTT per la connessione, pubblicazione e sottoscrizione a un broker MQTT.
 * Supporta la riconnessione automatica, la gestione di messaggi in arrivo e la pubblicazione di messaggi.
 */
public class MQTTClient {
    private static final Logger logger = LoggerFactory.getLogger(MQTTClient.class);
    private final MqttClient client;
    private final String clientId;
    private final Map<String, BiConsumer<String, String>> topicHandlers;
    private volatile boolean isConnecting;
    private int retryCount;
    private final Queue<PendingMessage> pendingMessages = new ConcurrentLinkedQueue<>();

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

        this.clientId = MQTTConfig.CLIENT_ID_PREFIX + prefix + "_" + UUID.randomUUID();
        this.topicHandlers = new ConcurrentHashMap<>();
        this.retryCount = 0;
        this.isConnecting = false;

        try {
            this.client = new MqttClient(MQTTConfig.BROKER_URL, clientId, new MemoryPersistence());
            initializeClient();
            logger.info("Client MQTT inizializzato con ID: {}", clientId);
        } catch (MqttException e) {
            logger.error("Errore durante l'inizializzazione del client MQTT: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Inizializza il client MQTT configurando le opzioni di connessione e il callback.
     *
     * @throws MqttException Se si verifica un errore durante la configurazione.
     */
    private void initializeClient() throws MqttException {
        MqttConnectOptions options = createConnectOptions();
        configureMqttCallback();
        connect(options);
    }

    /**
     * Crea e configura le opzioni di connessione per il client MQTT.
     *
     * @return Un'istanza di {@link MqttConnectOptions} configurata.
     * @throws MqttException Se si verifica un errore durante la configurazione SSL.
     */
    private MqttConnectOptions createConnectOptions() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(MQTTConfig.CONNECTION_TIMEOUT);
        options.setKeepAliveInterval(MQTTConfig.KEEP_ALIVE_INTERVAL);
        String pw= "Pissir2024!";
        options.setUserName("20019309");
        options.setPassword(pw.toCharArray());
        
        configureSSL(options);
        configureLWT(options);
        
        return options;
    }

    /**
     * Configura le opzioni SSL per la connessione MQTT.
     *
     * @param options Opzioni di connessione MQTT da configurare.
     * @throws MqttException Se si verifica un errore durante la configurazione SSL.
     */
    private void configureSSL(MqttConnectOptions options) throws MqttException {
        try {
            // Configura il truststore
            System.setProperty("javax.net.ssl.trustStore", MQTTConfig.TRUSTSTORE_PATH);
            System.setProperty("javax.net.ssl.trustStorePassword", MQTTConfig.TRUSTSTORE_PASSWORD);
            System.setProperty("javax.net.ssl.trustStoreType", MQTTConfig.TRUSTSTORE_TYPE);

            // Configura il keystore per il certificato client
            System.setProperty("javax.net.ssl.keyStore", MQTTConfig.KEYSTORE_PATH);
            System.setProperty("javax.net.ssl.keyStorePassword", MQTTConfig.KEYSTORE_PASSWORD);
            System.setProperty("javax.net.ssl.keyStoreType", MQTTConfig.TRUSTSTORE_TYPE);

            Properties sslProperties = new Properties();
            sslProperties.setProperty("com.ibm.ssl.protocol", "TLSv1.2");
            options.setSSLProperties(sslProperties);
        } catch (Exception e) {
            logger.error("Errore nella configurazione SSL: {}", e.getMessage());
            throw new MqttException(new RuntimeException("Errore nella configurazione SSL", e));
        }
    }

    /**
     * Configura il Last Will and Testament (LWT) per il client MQTT.
     *
     * @param options Opzioni di connessione MQTT da configurare.
     */
    private void configureLWT(MqttConnectOptions options) {
        String willTopic = "clients/" + clientId + "/status";
        String willMessage = "offline";
        options.setWill(willTopic, willMessage.getBytes(), 1, true);
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
     * Connette il client al broker MQTT utilizzando le opzioni specificate.
     *
     * @param options Opzioni di connessione MQTT.
     * @throws MqttException Se si verifica un errore durante la connessione.
     */
    public synchronized void connect(MqttConnectOptions options) throws MqttException {
        if (isConnecting) return;
        
        try {
            isConnecting = true;
            if (!client.isConnected()) {
                client.connect(options);
                logger.info("Client {} connesso con successo", clientId);
                retryCount = 0;
                restoreSubscriptions();
            }
        } catch (MqttException e) {
            logger.error("Errore durante la connessione: {}", e.getMessage());
            throw e;
        } finally {
            isConnecting = false;
        }
    }

    /**
     * Ripristina le sottoscrizioni ai topic dopo una riconnessione.
     */
    private void restoreSubscriptions() {
        topicHandlers.keySet().forEach(topic -> {
            try {
                client.subscribe(topic, MQTTConfig.QOS);
                logger.debug("Sottoscrizione ripristinata per il topic: {}", topic);
            } catch (MqttException e) {
                logger.error("Errore nel ripristino della sottoscrizione per il topic {}: {}", 
                    topic, e.getMessage());
            }
        });
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
            // Se c'è un errore di connessione, bufferizza il messaggio
            if (e.getReasonCode() == MqttException.REASON_CODE_CONNECTION_LOST) {
                pendingMessages.offer(new PendingMessage(topic, message));
                logger.warn("Connessione persa. Messaggio bufferizzato per topic: {}", topic);
            }
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
    
    private void inviaMessaggiInAttesa() {
        while (!pendingMessages.isEmpty()) {
            PendingMessage msg = pendingMessages.poll();
            try {
                MqttMessage mqttMessage = new MqttMessage(msg.getMessage().getBytes());
                mqttMessage.setQos(MQTTConfig.QOS);
                client.publish(msg.getTopic(), mqttMessage);
                logger.info("Messaggio bufferizzato inviato con successo al topic: {}", msg.getTopic());
            } catch (MqttException e) {
                // Se c'è ancora un errore, rimetti il messaggio nella coda
                pendingMessages.offer(msg);
                logger.error("Errore nell'invio del messaggio bufferizzato: {}", e.getMessage());
                break;
            }
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
     * Disconnette il client dal broker MQTT e chiude la connessione.
     */
    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
            logger.info("Client {} disconnesso con successo", clientId);
        } catch (MqttException e) {
            logger.error("Errore durante la disconnessione del client {}: {}", 
                clientId, e.getMessage());
        }
    }

    /**
     * Verifica se il client è attualmente connesso al broker.
     *
     * @return true se il client è connesso, false altrimenti.
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Connette il client a un broker MQTT specifico utilizzando l'URL e le opzioni fornite.
     *
     * @param brokerUrl L'URL del broker MQTT.
     * @param options Opzioni di connessione MQTT.
     * @throws MqttException Se si verifica un errore durante la connessione.
     * @throws IllegalArgumentException Se l'URL del broker è nullo o vuoto.
     */
    public void connectWithBrokerUrl(String brokerUrl, MqttConnectOptions options) throws MqttException {
        if (brokerUrl == null || brokerUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("L'URL del broker non può essere vuoto");
        }
        this.client.connect(options);
        logger.info("Client {} connesso con successo al broker: {}", clientId, brokerUrl);
    }
    
    private void tentaRiconnessione() {
        if (!client.isConnected() && retryCount < MQTTConfig.MAX_RETRY_ATTEMPTS) {
            retryCount++;
            logger.info("Tentativo di riconnessione {} per il client {}", retryCount, clientId);
            try {
                // Preparazione delle opzioni di connessione
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setAutomaticReconnect(false);
                options.setConnectionTimeout(MQTTConfig.CONNECTION_TIMEOUT);
                options.setKeepAliveInterval(MQTTConfig.KEEP_ALIVE_INTERVAL);
                
                // Imposta credenziali
                options.setUserName(MQTTConfig.USERNAME);
                options.setPassword(MQTTConfig.PASSWORD.toCharArray());
                
                // Configurazione SSL se necessario
                if (MQTTConfig.SSL_ENABLED) {
                    Properties sslProperties = new Properties();
                    sslProperties.setProperty("com.ibm.ssl.protocol", "TLSv1.2");
                    options.setSSLProperties(sslProperties);
                }
                
                // Effettua la connessione
                connect(options);
                
                // Se la connessione è riuscita, ripristina il contatore e invia messaggi in attesa
                if (client.isConnected()) {
                    logger.info("Riconnessione riuscita per il client {}", clientId);
                    retryCount = 0;
                    
                    // Ripristina le sottoscrizioni
                    for (String topic : topicHandlers.keySet()) {
                        client.subscribe(topic, MQTTConfig.QOS);
                        logger.debug("Sottoscrizione ripristinata per topic: {}", topic);
                    }
                    
                    // Invia i messaggi in attesa
                    inviaMessaggiInAttesa();
                }
            } catch (MqttException e) {
                logger.error("Riconnessione fallita per il client {}: {}", clientId, e.getMessage());
                // Pianifica il prossimo tentativo con backoff esponenziale
                scheduleReconnect();
            }
        } else if (retryCount >= MQTTConfig.MAX_RETRY_ATTEMPTS) {
            logger.error("Numero massimo di tentativi di riconnessione raggiunto per il client {}", clientId);
            // Notifica gli handler che la connessione è definitivamente persa
        }
    }
    
    private static class PendingMessage {
        private final String topic;
        private final String message;
        
        public PendingMessage(String topic, String message) {
            this.topic = topic;
            this.message = message;
        }
        
        public String getTopic() { return topic; }
        public String getMessage() { return message; }
    }
}