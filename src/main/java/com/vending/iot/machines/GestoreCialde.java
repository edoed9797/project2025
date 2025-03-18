package com.vending.iot.machines;

import com.google.gson.Gson;
import com.vending.iot.mqtt.MQTTBrokerManager;
import com.vending.iot.mqtt.MQTTClient;
import com.vending.ServiceRegistry;
import com.vending.core.models.Cialda;
import com.vending.core.models.Macchina;
import com.vending.core.models.QuantitaCialde;
import com.vending.core.repositories.MacchinaRepository;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GestoreCialde {
    private final int idMacchina;
    private Map<Integer, QuantitaCialde> cialde;
    private final MQTTClient mqttClient;
    private final Gson gson;
    private static final Logger logger = LoggerFactory.getLogger(GestoreCialde.class);
    private final MacchinaRepository macchinaRepository;
    
    private static final double SOGLIA_RICARICA = 0.1; // 10%
    private static final double SOGLIA_AVVISO = 0.3;   // 30%

    public GestoreCialde(int idMacchina) throws MqttException {
        this.macchinaRepository = ServiceRegistry.get(MacchinaRepository.class);
        this.idMacchina = idMacchina;
        this.cialde = new HashMap<>();
        this.gson = new Gson();
        this.mqttClient = new MQTTClient("cialde_" + idMacchina);
        
        // Inizializza lo stato delle cialde dal database
        Macchina macchina = macchinaRepository.findById(idMacchina);
        if (macchina != null && macchina.getCialde() != null) {
            for (QuantitaCialde qc : macchina.getCialde()) {
                cialde.put(qc.getCialdaId(), qc);
            }
            logger.info("Cialde caricate per macchina {}: {} tipi di cialda", idMacchina, cialde.size());
        } else {
            logger.warn("Nessuna cialda trovata per la macchina {}", idMacchina);
        }
        
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
        cialde.put(idCialda, new QuantitaCialde(idMacchina, idCialda, quantita, quantitaMassima));
        pubblicaStatoCialde();
    }
    
    /**
     * Verifica se ci sono cialde sufficienti per erogare una bevanda.
     * 
     * @param cialdeNecessarie Lista di cialde necessarie per la bevanda
     * @return true se tutte le cialde sono disponibili in quantità sufficiente
     */
    public boolean verificaDisponibilitaCialde(List<Cialda> cialdeNecessarie) {
        if (cialdeNecessarie == null || cialdeNecessarie.isEmpty()) {
            logger.warn("Nessuna cialda specificata per la verifica");
            return false;
        }

        // Conta quante cialde di ogni tipo sono necessarie
        Map<Integer, Integer> cialdeRichieste = new HashMap<>();
        for (Cialda cialda : cialdeNecessarie) {
            cialdeRichieste.merge(cialda.getId(), 1, Integer::sum);
        }

        // Verifica disponibilità
        for (Map.Entry<Integer, Integer> entry : cialdeRichieste.entrySet()) {
            int cialdaId = entry.getKey();
            int quantitaRichiesta = entry.getValue();
            
            QuantitaCialde qc = cialde.get(cialdaId);
            if (qc == null || qc.getQuantita() < quantitaRichiesta) {
                logger.warn("Cialda {} non disponibile in quantità sufficiente. Richiesta: {}, Disponibile: {}", 
                           cialdaId, quantitaRichiesta, (qc != null ? qc.getQuantita() : 0));
                return false;
            }
        }
        
        return true;
    }

    /**
     * Consuma le cialde necessarie per erogare una bevanda.
     * 
     * @param cialdeNecessarie Lista di cialde da consumare
     * @return true se l'operazione è riuscita
     */
    public boolean consumaCialde(List<Cialda> cialdeNecessarie) {
        if (!verificaDisponibilitaCialde(cialdeNecessarie)) {
            return false;
        }

        // Conta quante cialde di ogni tipo sono necessarie
        Map<Integer, Integer> cialdeRichieste = new HashMap<>();
        for (Cialda cialda : cialdeNecessarie) {
            cialdeRichieste.merge(cialda.getId(), 1, Integer::sum);
        }

        // Consuma le cialde
        for (Map.Entry<Integer, Integer> entry : cialdeRichieste.entrySet()) {
            int cialdaId = entry.getKey();
            int quantitaRichiesta = entry.getValue();
            
            QuantitaCialde qc = cialde.get(cialdaId);
            qc.setQuantita(qc.getQuantita() - quantitaRichiesta);
            logger.debug("Consumate {} cialde di tipo {}. Rimaste: {}", 
                        quantitaRichiesta, cialdaId, qc.getQuantita());
        }

        // Aggiorna il database
        aggiornaDatabase();
        
        // Verifica se è necessario segnalare necessità di rifornimento
        verificaStatoCialde();
        
        return true;
    }

    /**
     * Gestisce la ricarica delle cialde.
     * 
     * @param richiesta Richiesta di ricarica cialde
     * @return true se l'operazione è riuscita
     */
    public boolean gestisciRicaricaCialde(RichiestaCialde richiesta) {
        if (richiesta == null || richiesta.getQuantitaRichieste().isEmpty()) {
            // Se non ci sono richieste specifiche, ricarica tutte le cialde al massimo
            for (QuantitaCialde qc : cialde.values()) {
                qc.setQuantita(qc.getQuantitaMassima());
                logger.info("Cialda {} ricaricata alla capacità massima: {}", 
                           qc.getCialdaId(), qc.getQuantitaMassima());
            }
        } else {
            // Altrimenti, ricarica solo le cialde specificate
            for (Map.Entry<Integer, Integer> entry : richiesta.getQuantitaRichieste().entrySet()) {
                int cialdaId = entry.getKey();
                int quantitaRichiesta = entry.getValue();
                
                QuantitaCialde qc = cialde.get(cialdaId);
                if (qc != null) {
                    int nuovaQuantita = Math.min(qc.getQuantita() + quantitaRichiesta, qc.getQuantitaMassima());
                    qc.setQuantita(nuovaQuantita);
                    logger.info("Cialda {} ricaricata a {}", cialdaId, nuovaQuantita);
                } else {
                    logger.warn("Tentativo di ricaricare cialda {} non presente nella macchina {}", 
                              cialdaId, idMacchina);
                }
            }
        }

        // Aggiorna il database
        aggiornaDatabase();
        
        return true;
    }

    /**
     * Verifica lo stato delle cialde e segnala se ci sono cialde sotto soglia.
     * 
     * @return Map con gli stati delle cialde
     */
    public Map<String, Object> verificaStatoCialde() {
        Map<String, Object> stato = ottieniStato();

        // Verifica quali cialde sono sotto soglia
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cialdeStato = (List<Map<String, Object>>) stato.get("cialde");
        
        for (Map<String, Object> cialda : cialdeStato) {
            logger.debug("Cialda ID: {}, Nome: {}, Quantità: {}, Massimo: {}, Soglia: {}, SottoSoglia: {}",
                cialda.get("id"), cialda.get("nome"), cialda.get("quantita"), 
                cialda.get("massimo"), ((int)cialda.get("massimo") * 0.2), cialda.get("sottoSoglia"));
        }

        // Filtra le cialde che sono sotto soglia
        List<Map<String, Object>> cialdeSottoSoglia = cialdeStato.stream()
            .filter(c -> (boolean) c.get("sottoSoglia"))
            .collect(Collectors.toList());
        if (!cialdeSottoSoglia.isEmpty()) {
            logger.warn("Cialde sotto soglia trovate:");
            for (Map<String, Object> cialda : cialdeSottoSoglia) {
                logger.warn("  - {}: quantità {} (soglia minima: {})", 
                    cialda.get("nome"), cialda.get("quantita"), 
                    (int)((double)cialda.get("massimo") * 0.2));
            }
        }

        // Se ci sono cialde sotto soglia, pubblica un messaggio MQTT
        if (!cialdeSottoSoglia.isEmpty()) {
            try {
                String topic = "macchine/" + idMacchina + "/stato/aggiornamento";
                Map<String, Object> messaggio = new HashMap<>();
                messaggio.put("cialde", new HashMap<String, Object>() {{
                    put("sottoSoglia", true);
                    put("cialdeSottoSoglia", cialdeSottoSoglia);
                }});
                messaggio.put("timestamp", System.currentTimeMillis());

                // Ottieni l'istanza MQTTClient dal ServiceRegistry o altro meccanismo appropriato
                //MQTTClient mqttClient = MQTTBrokerManager.getInstance().getClient("gestore-cialde-" + idMacchina);
                if (mqttClient != null) {
                    mqttClient.publish(topic, new Gson().toJson(messaggio));
                    logger.warn("Segnalate {} tipi di cialde sotto soglia per la macchina {}", 
                                cialdeSottoSoglia.size(), idMacchina);
                } else {
                    logger.error("Impossibile ottenere client MQTT per la macchina {}", idMacchina);
                }
            } catch (Exception e) {
                logger.error("Errore nella pubblicazione dell'avviso cialde: {}", e.getMessage(), e);
            }
        }

        return stato;
    }
    
    
    private void pubblicaAvvisoRicarica(int idCialda, String livelloAllarme) {
        try {
            String topic = "macchine/" + idMacchina + "/cialde/avviso/risposta";
            QuantitaCialde info = cialde.get(idCialda);
            
            Map<String, Object> avviso = Map.of(
                "idCialda", idCialda,
                "tipo", "ricarica_necessaria",
                "livello", livelloAllarme,
                "quantitaAttuale", info.getQuantita(),
                "quantitaMassima", info.getQuantitaMassima(),
                "percentuale", (double) info.getQuantita() / info.getQuantitaMassima() * 100,
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
            Map<String, Object> stato = ottieniStato();
            mqttClient.publish(topic, gson.toJson(stato));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione stato cialde: " + e.getMessage());
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

    /**
     * Classe interna per rappresentare una richiesta di ricarica cialde.
     */
    public static class RichiestaCialde {
        private Map<Integer, Integer> quantitaRichieste = new HashMap<>();

        public void aggiungiRichiesta(int cialdaId, int quantita) {
            quantitaRichieste.put(cialdaId, quantita);
        }

        public Map<Integer, Integer> getQuantitaRichieste() {
            return quantitaRichieste;
        }
    }

    /**
     * Restituisce lo stato attuale delle cialde.
     * 
     * @return Mappa con lo stato delle cialde
     */
    public Map<String, Object> ottieniStato() {
        Map<String, Object> stato = new HashMap<>();
        List<Map<String, Object>> cialdeStato = new ArrayList<>();
        
        for (QuantitaCialde qc : cialde.values()) {
            Map<String, Object> cialdaStato = new HashMap<>();
            cialdaStato.put("id", qc.getCialdaId());
            cialdaStato.put("nome", qc.getNomeCialda());
            cialdaStato.put("tipo", qc.getTipoCialda());
            cialdaStato.put("quantita", qc.getQuantita());
            cialdaStato.put("massimo", qc.getQuantitaMassima());
            cialdaStato.put("percentuale", (double)qc.getQuantita() / qc.getQuantitaMassima() * 100);
            cialdaStato.put("sottoSoglia", qc.necessitaRifornimento());
            cialdeStato.add(cialdaStato);
        }
        
        stato.put("cialde", cialdeStato);
        stato.put("timestamp", System.currentTimeMillis());
        
        // Mappa per compatibilità con ManutenzioneService
        Map<Integer, Integer> quantita = new HashMap<>();
        for (QuantitaCialde qc : cialde.values()) {
            quantita.put(qc.getCialdaId(), qc.getQuantita());
        }
        stato.put("quantita", quantita);
        
        return stato;
    }
    
    /**
     * Aggiorna lo stato delle cialde nel database.
     */
    private void aggiornaDatabase() {
        try {
            Macchina macchina = macchinaRepository.findById(idMacchina);
            if (macchina != null) {
                List<QuantitaCialde> cialdeDB = macchina.getCialde();
                
                // Aggiorna le quantità nel DB con quelle in memoria
                for (QuantitaCialde qcDB : cialdeDB) {
                    QuantitaCialde qcMemoria = cialde.get(qcDB.getCialdaId());
                    if (qcMemoria != null) {
                        qcDB.setQuantita(qcMemoria.getQuantita());
                    }
                }
                
                macchinaRepository.update(macchina);
                logger.debug("Database aggiornato per le cialde della macchina {}", idMacchina);
            } else {
                logger.error("Impossibile aggiornare database: macchina {} non trovata", idMacchina);
            }
        } catch (Exception e) {
            logger.error("Errore nell'aggiornamento del database per le cialde della macchina {}: {}", 
                        idMacchina, e.getMessage(), e);
        }
    }

    public void spegni() {
        mqttClient.disconnect();
    }
}