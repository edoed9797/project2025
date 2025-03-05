package com.vending.iot.mqtt;

public class MQTTConfig {
	
	// Configurazione broker - connessione non sicura per test
	//public static final String BROKER_URL = "tcp://localhost:1883";
    public static final String BROKER_URL = "ssl://localhost:8883";
    public static final String BROKER_WS_URL = "ws://localhost:9002";
    public static final String CLIENT_ID_PREFIX = "pissir_";
    public static final String USERNAME = "20019309";
    public static final String PASSWORD = "Pissir2024!";
    public static final int QOS = 1;
    public static final boolean SSL_ENABLED = true;
    
    // Configurazione SSL
    public static final String TRUSTSTORE_PATH = "C:\\mosquitto\\certs\\truststore.jks";
    public static final String TRUSTSTORE_PASSWORD = "Pissir2024!";
    public static final String TRUSTSTORE_TYPE = "JKS";
    public static final String KEYSTORE_PATH = "C:\\mosquitto\\certs\\client.jks";
    public static final String KEYSTORE_PASSWORD = "Pissir2024!";
    
    // Timeout e retry
    public static final int CONNECTION_TIMEOUT = 10;
    public static final int KEEP_ALIVE_INTERVAL = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int RETRY_INTERVAL = 5000;
    
}
