package com.vending.iot.bridge;

public class BridgeConfig {
    private final String brokerLocalUrl;
    private final String brokerCentraleUrl;
    private final String prefissoScuola;

    public BridgeConfig(String brokerLocalUrl, String brokerCentraleUrl, String prefissoScuola) {
        this.brokerLocalUrl = brokerLocalUrl;
        this.brokerCentraleUrl = brokerCentraleUrl;
        this.prefissoScuola = prefissoScuola;
    }

    public String getBrokerLocalUrl() {
        return brokerLocalUrl;
    }

    public String getBrokerCentraleUrl() {
        return brokerCentraleUrl;
    }

    public String getTopicCentrale(String scuolaId, String topicLocale) {
        return prefissoScuola + "/" + scuolaId + "/" + topicLocale;
    }

    public String getTopicLocale(String scuolaId, String topicCentrale) {
        String prefix = prefissoScuola + "/" + scuolaId + "/";
        return topicCentrale.startsWith(prefix) ? topicCentrale.substring(prefix.length()) : topicCentrale;
    }
}
