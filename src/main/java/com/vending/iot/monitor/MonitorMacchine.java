package com.vending.iot.monitor;

import com.google.gson.Gson;
import com.vending.iot.mqtt.MQTTClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MonitorMacchine {
    private final Map<Integer, StatoMacchina> statiMacchine;
    private final MQTTClient mqttClient;
    private final AlertManager alertManager;
    private final Gson gson;

    public MonitorMacchine() throws MqttException {
        this.statiMacchine = new ConcurrentHashMap<>();
        
        // Utilizza il broker manager per ottenere il client MQTT
        this.mqttClient = new MQTTClient("monitor_macchine");
        this.alertManager = new AlertManager(mqttClient);
        this.gson = new Gson();
        
        inizializzaMonitoraggio();
    }

    private void inizializzaMonitoraggio() throws MqttException {
        // Sottoscrizione agli stati delle macchine
        mqttClient.subscribe("macchine/+/stato", (topic, messaggio) -> {
            int macchinaId = estraiIdMacchina(topic);
            StatoMacchina stato = gson.fromJson(messaggio, StatoMacchina.class);
            aggiornaStatoMacchina(macchinaId, stato);
        });

        // Sottoscrizione agli allarmi
        mqttClient.subscribe("macchine/+/allarmi", (topic, messaggio) -> {
            int macchinaId = estraiIdMacchina(topic);
            Allarme allarme = gson.fromJson(messaggio, Allarme.class);
            gestisciAllarme(macchinaId, allarme);
        });
    }

    private int estraiIdMacchina(String topic) {
        String[] parts = topic.split("/");
        return Integer.parseInt(parts[1]);
    }

    private void aggiornaStatoMacchina(int macchinaId, StatoMacchina stato) {
        StatoMacchina statoPrec = statiMacchine.put(macchinaId, stato);
        
        // Verifica cambiamenti significativi
        if (statoPrec != null) {
            verificaCambiamenti(macchinaId, statoPrec, stato);
        }

        // Pubblica aggiornamento
        pubblicaStatoAggiornato(macchinaId, stato);
    }

    private void verificaCambiamenti(int macchinaId, StatoMacchina statoPrec, StatoMacchina statoNuovo) {
        // Verifica livello cialde
        if (statoNuovo.getLivelloCialde() < (statoNuovo.getCialdeMassime() * 0.2)) {
            alertManager.inviaAlert(
                macchinaId,
                "CIALDE_BASSE",
                "Livello cialde sotto il 20%",
                2
            );
        }

        // Verifica cassa
        if (statoNuovo.getLivelloCassa() > (statoNuovo.getCassaMassima() * 0.9)) {
            alertManager.inviaAlert(
                macchinaId,
                "CASSA_PIENA",
                "Livello cassa sopra il 90%",
                2
            );
        }

        // Verifica cambio stato
        if (!statoPrec.getStato().equals(statoNuovo.getStato())) {
            alertManager.inviaAlert(
                macchinaId,
                "CAMBIO_STATO",
                "Cambio stato da " + statoPrec.getStato() + " a " + statoNuovo.getStato(),
                1
            );
        }
    }

    private void gestisciAllarme(int macchinaId, Allarme allarme) {
        // Aggiorna stato macchina con allarme
        StatoMacchina stato = statiMacchine.get(macchinaId);
        if (stato != null) {
            stato.aggiungiAllarme(allarme);
            statiMacchine.put(macchinaId, stato);
        }

        // Invia alert appropriato
        alertManager.inviaAlert(
            macchinaId,
            allarme.getTipo(),
            allarme.getMessaggio(),
            allarme.getSeverita()
        );
    }

    private void pubblicaStatoAggiornato(int macchinaId, StatoMacchina stato) {
        try {
            String topic = "macchine/" + macchinaId + "/stato/monitoraggio";
            mqttClient.publishRetained(topic, gson.toJson(stato));
        } catch (MqttException e) {
            System.err.println("Errore nella pubblicazione dello stato: " + e.getMessage());
        }
    }

    public Map<Integer, StatoMacchina> getStatiMacchine() {
        return new ConcurrentHashMap<>(statiMacchine);
    }

    public StatoMacchina getStatoMacchina(int macchinaId) {
        return statiMacchine.get(macchinaId);
    }

    public void spegni() {
        mqttClient.disconnect();
    }
}