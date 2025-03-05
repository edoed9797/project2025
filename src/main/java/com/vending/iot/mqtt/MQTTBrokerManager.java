package com.vending.iot.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Singleton per la gestione centralizzata del broker MQTT.
 * Questa classe garantisce l'utilizzo di un'unica istanza di broker MQTT in tutta l'applicazione,
 * gestendo la creazione e il riutilizzo dei client MQTT.
 */
public class MQTTBrokerManager {
    private static final Logger logger = LoggerFactory.getLogger(MQTTBrokerManager.class);
    private static MQTTBrokerManager instance;
    
    // Mappa dei client MQTT già creati, indicizzati per ID client
    private final Map<String, MqttClient> mqttClients = new ConcurrentHashMap<>();
    
    // Opzioni di connessione predefinite
    private final MqttConnectOptions defaultOptions;
    
    /**
     * Costruttore privato per il pattern Singleton
     */
    private MQTTBrokerManager() {
        this.defaultOptions = createDefaultConnectOptions();
        logger.info("MQTTBrokerManager inizializzato");
    }
    
    /**
     * Ottiene l'istanza singleton del manager.
     * 
     * @return l'istanza del manager
     */
    public static synchronized MQTTBrokerManager getInstance() {
        if (instance == null) {
            instance = new MQTTBrokerManager();
        }
        return instance;
    }
    
    /**
     * Crea un client MQTT o restituisce quello esistente con lo stesso ID.
     * 
     * @param clientId ID del client MQTT
     * @return il client MQTT creato o esistente
     * @throws MqttException se si verifica un errore durante la creazione
     */
    public synchronized MqttClient getClient(String clientId) throws MqttException {
        String fullClientId = MQTTConfig.CLIENT_ID_PREFIX + clientId;
        
        // Se il client esiste già e è connesso, restituiscilo
        if (mqttClients.containsKey(fullClientId) && mqttClients.get(fullClientId).isConnected()) {
            return mqttClients.get(fullClientId);
        }
        
        // Altrimenti crea un nuovo client
        try {
            MqttClient client = new MqttClient(
                MQTTConfig.BROKER_URL, 
                fullClientId, 
                new MemoryPersistence()
            );
            
            // Connetti il client con le opzioni predefinite
            connect(client, defaultOptions);
            
            // Memorizza il client per riuso futuro
            mqttClients.put(fullClientId, client);
            logger.info("Client MQTT creato: {}", fullClientId);
            
            return client;
            
        } catch (MqttException e) {
            logger.error("Errore nella creazione del client MQTT: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Connette un client MQTT con opzioni personalizzate.
     * 
     * @param client il client MQTT da connettere
     * @param options le opzioni di connessione
     * @throws MqttException se si verifica un errore durante la connessione
     */
    public void connect(MqttClient client, MqttConnectOptions options) throws MqttException {
        if (!client.isConnected()) {
            try {
                client.connect(options);
                logger.info("Client MQTT connesso: {}", client.getClientId());
            } catch (MqttException e) {
                logger.error("Errore nella connessione del client MQTT: {}", e.getMessage(), e);
                throw e;
            }
        }
    }
    
    /**
     * Crea le opzioni di connessione predefinite.
     * 
     * @return opzioni di connessione configurate
     */
    MqttConnectOptions createDefaultConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        
        // Configurazione base
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(MQTTConfig.CONNECTION_TIMEOUT);
        options.setKeepAliveInterval(MQTTConfig.KEEP_ALIVE_INTERVAL);
        
        // Credenziali
        options.setUserName(MQTTConfig.USERNAME);
        options.setPassword(MQTTConfig.PASSWORD.toCharArray());
        
        // Configurazione SSL se abilitata
        if (MQTTConfig.SSL_ENABLED) {
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
                logger.error("Errore nella configurazione SSL: {}", e.getMessage(), e);
            }
        }
        
        // Configurazione LWT (Last Will and Testament)
        String willTopic = "system/clients/status";
        options.setWill(willTopic, "offline".getBytes(), 1, true);
        
        return options;
    }
    
    /**
     * Disconnette tutti i client MQTT gestiti.
     */
    public void disconnectAll() {
        for (Map.Entry<String, MqttClient> entry : mqttClients.entrySet()) {
            try {
                MqttClient client = entry.getValue();
                if (client.isConnected()) {
                    client.disconnect();
                    client.close();
                    logger.info("Client MQTT disconnesso: {}", client.getClientId());
                }
            } catch (MqttException e) {
                logger.error("Errore nella disconnessione del client MQTT: {}", e.getMessage(), e);
            }
        }
        mqttClients.clear();
    }
    
    /**
     * Disconnette e rimuove un client specifico.
     * 
     * @param clientId l'ID del client da rimuovere
     */
    public void removeClient(String clientId) {
        String fullClientId = MQTTConfig.CLIENT_ID_PREFIX + clientId;
        MqttClient client = mqttClients.get(fullClientId);
        
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                    client.close();
                }
                mqttClients.remove(fullClientId);
                logger.info("Client MQTT rimosso: {}", fullClientId);
            } catch (MqttException e) {
                logger.error("Errore nella rimozione del client MQTT: {}", e.getMessage(), e);
            }
        }
    }
}