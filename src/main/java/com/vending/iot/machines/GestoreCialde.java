package com.vending.iot.machines;

import com.google.gson.Gson;
import com.vending.iot.mqtt.MQTTClient;
import com.vending.core.models.Cialda;
import org.eclipse.paho.client.mqttv3.MqttException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GestoreCialde {
    private final int idMacchina;
    private final Map<Integer, InfoCialda> cialde;
    private final MQTTClient mqttClient;
    private final Gson gson;
    private static final double SOGLIA_RICARICA = 0.1; // 10%
    private static final double SOGLIA_AVVISO = 0.3;   // 30%

    public GestoreCialde(int idMacchina) throws MqttException {
        this.idMacchina = idMacchina;
        this.cialde = new ConcurrentHashMap<>();
        this.gson = new Gson();
        this.mqttClient = new MQTTClient("cialde_" + idMacchina);
        
        inizializzaSottoscrizioni();
    }

    private void inizializzaSottoscrizioni() throws MqttException {
        String baseTopic = "macchine/" + idMacchina + "/cialde/";
        
        // Richieste di ricarica
        mqttClient.subscribe(baseTopic + "ricarica/richiesta", (topic, messaggio) -> {
            gestisciRicaricaCialde(gson.fromJson(messaggio, RichiestaCialde.class));
        });
        
        // Richieste di verifica
        mqttClient.subscribe(baseTopic + "verifica/richiesta", (topic, messaggio) -> {
            verificaStatoCialde();
        });
        
        // Richieste di stato
        mqttClient.subscribe(baseTopic + "stato/richiesta", (topic, messaggio) -> {
            pubblicaStatoCialde();
        });
    }

    public void inizializzaCialda(int idCialda, int quantita, int quantitaMassima) {
        cialde.put(idCialda, new InfoCialda(quantita, quantitaMassima));
        pubblicaStatoCialde();
    }

    public boolean verificaDisponibilitaCialde(List<Cialda> cialdeNecessarie) {
        return cialdeNecessarie.stream()
                .allMatch(cialda -> {
                    InfoCialda info = cialde.get(cialda.getId());
                    return info != null && info.quantitaAttuale > 0;
                });
    }

    public void consumaCialde(List<Cialda> cialdeUsate) {
        cialdeUsate.forEach(cialda -> {
            InfoCialda info = cialde.get(cialda.getId());
            if (info != null) {
                info.decrementaQuantita();
                if (info.necessitaRicarica()) {
                    pubblicaAvvisoRicarica(cialda.getId());
                }
            }
        });
        pubblicaStatoCialde();
    }

    public void gestisciRicaricaCialde(RichiestaCialde richiesta) {
        InfoCialda info = cialde.get(richiesta.idCialda);
        if (info != null) {
            info.ricarica();
            pubblicaStatoCialde();
            pubblicaConfermaRicarica(richiesta.idCialda);
        }
    }

    public void verificaStatoCialde() {
        cialde.forEach((id, info) -> {
            double percentuale = (double) info.quantitaAttuale / info.quantitaMassima;
            if (percentuale <= SOGLIA_RICARICA) {
                pubblicaAvvisoRicarica(id, "CRITICO");
            } else if (percentuale <= SOGLIA_AVVISO) {
                pubblicaAvvisoRicarica(id, "AVVISO");
            }
        });
        pubblicaStatoCialde();
    }
    
    private void pubblicaAvvisoRicarica(int idCialda, String livelloAllarme) {
        try {
            String topic = "macchine/" + idMacchina + "/cialde/avviso/risposta";
            InfoCialda info = cialde.get(idCialda);
            
            Map<String, Object> avviso = Map.of(
                "idCialda", idCialda,
                "tipo", "ricarica_necessaria",
                "livello", livelloAllarme,
                "quantitaAttuale", info.quantitaAttuale,
                "quantitaMassima", info.quantitaMassima,
                "percentuale", (double) info.quantitaAttuale / info.quantitaMassima * 100,
                "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(avviso));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione avviso ricarica: " + e.getMessage());
        }
    }
    

    private void pubblicaStatoCialde() {
        try {
            String topic = "macchine/" + idMacchina + "/cialde/stato/risposta";
            Map<String, Object> stato = new ConcurrentHashMap<>();
            cialde.forEach((id, info) -> stato.put(String.valueOf(id), info.toMap()));
            mqttClient.publish(topic, gson.toJson(stato));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione stato cialde: " + e.getMessage());
        }
    }

    private void pubblicaAvvisoRicarica(int idCialda) {
        try {
            String topic = "macchine/" + idMacchina + "/cialde/avviso/risposta";
            Map<String, Object> avviso = Map.of(
                "idCialda", idCialda,
                "tipo", "ricarica_necessaria",
                "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(avviso));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione avviso ricarica: " + e.getMessage());
        }
    }

    private void pubblicaConfermaRicarica(int idCialda) {
        try {
            String topic = "macchine/" + idMacchina + "/cialde/ricarica/conferma";
            Map<String, Object> conferma = Map.of(
                "idCialda", idCialda,
                "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(conferma));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione conferma ricarica: " + e.getMessage());
        }
    }

    private static class InfoCialda {
        private int quantitaAttuale;
        private final int quantitaMassima;

        public InfoCialda(int quantitaAttuale, int quantitaMassima) {
            this.quantitaAttuale = quantitaAttuale;
            this.quantitaMassima = quantitaMassima;
        }

        public void decrementaQuantita() {
            if (quantitaAttuale > 0) quantitaAttuale--;
        }

        public void ricarica() {
            quantitaAttuale = quantitaMassima;
        }

        public boolean necessitaRicarica() {
            return quantitaAttuale < (quantitaMassima * SOGLIA_RICARICA);
        }

        public Map<String, Object> toMap() {
            return Map.of(
                "quantitaAttuale", quantitaAttuale,
                "quantitaMassima", quantitaMassima,
                "percentuale", (double) quantitaAttuale / quantitaMassima * 100,
                "necessitaRicarica", necessitaRicarica()
            );
        }
    }

    public static class RichiestaCialde {
        public int idCialda;
    }

    public Map<String, Object> ottieniStato() {
        Map<String, Object> statoCialde = new HashMap<>();
        cialde.forEach((id, info) -> 
            statoCialde.put(String.valueOf(id), Map.of(
                "quantitaAttuale", info.quantitaAttuale,
                "quantitaMassima", info.quantitaMassima,
                "necessitaRicarica", info.quantitaAttuale < info.quantitaMassima * 0.2
            ))
        );
        return statoCialde;
        }

    public void spegni() {
        mqttClient.disconnect();
    }
}