package com.vending.iot.machines;

import com.vending.core.models.Macchina;
import com.vending.core.models.QuantitaCialde;
import com.vending.core.repositories.MacchinaRepository;
import com.vending.ServiceRegistry;
import com.vending.core.models.Bevanda;
import com.vending.iot.mqtt.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MacchinaPrincipale {

    private final int id;
    public final GestoreCassa gestoreCassa;
    public final GestoreBevande gestoreBevande;
    public final GestoreCialde gestoreCialde;
    public final GestoreManutenzione gestoreManutenzione;
    private final MQTTClient clientMqtt;
    private final MacchinaRepository macchinaRepository;
    private final Gson gson;
    private final AtomicBoolean inErogazione;
<<<<<<< HEAD
    private static final Logger logger = LoggerFactory.getLogger(MacchinaPrincipale.class);
=======
    private long ultimaPubblicazioneStato = 0;
    private static final long INTERVALLO_MINIMO_PUBBLICAZIONE = 5000;
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26

    public MacchinaPrincipale(Macchina macchina) throws MqttException {
        this.id = macchina.getId();
        this.gson = new Gson();
        this.clientMqtt = new MQTTClient("macchina_" + id);
        this.gestoreCassa = new GestoreCassa(id, macchina.getCassaMassima());
        this.gestoreCialde = new GestoreCialde(id);
        this.gestoreBevande = new GestoreBevande(id, gestoreCassa, gestoreCialde);
        this.gestoreManutenzione = new GestoreManutenzione(id);
        this.inErogazione = new AtomicBoolean(false);
        this.macchinaRepository = ServiceRegistry.get(MacchinaRepository.class);

        inizializzaMacchina(macchina);
        configuraSottoscrizioni();
    }

    private void inizializzaMacchina(Macchina macchina) {
        // Inizializza bevande e cialde
        macchina.getBevande().forEach(gestoreBevande::aggiungiBevanda);
        macchina.getCialde().forEach(cialda
                -> gestoreCialde.inizializzaCialda(
                        cialda.getCialdaId(),
                        cialda.getQuantita(),
                        cialda.getQuantitaMassima()
                )
        );
        
        
        // Imposta stato cassa
        gestoreCassa.impostaSaldoCassa(macchina.getCassaAttuale());
        pubblicaStatoMacchina();
    }

    private void configuraSottoscrizioni() throws MqttException {
        String topicBase = "macchine/" + id + "/";

        // Comandi generali
        clientMqtt.subscribe(topicBase + "comandi/#", (topic, messaggio) -> {
            String comando = topic.substring((topicBase + "comandi/").length());
            switch (comando) {
                case "spegnimento":
                    eseguiSpegnimento();
                    break;
                case "riavvio":
                    eseguiRiavvio();
                    break;
                case "stato/richiesta":
                    pubblicaStatoMacchina();
                    break;
            }
        });

<<<<<<< HEAD
        // Richieste di stato
=======
     // Richieste di stato
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
        clientMqtt.subscribe(topicBase + "stato/richiesta", (topic, messaggio) -> {
            pubblicaStatoMacchina();
        });
        
        // Operazioni cliente
        clientMqtt.subscribe(topicBase + "operazioni/#", (topic, messaggio) -> {
            String operazione = topic.substring((topicBase + "operazioni/").length());
            Map<String, Object> dati = gson.fromJson(messaggio, Map.class);

            switch (operazione) {
                case "inserimentoCredito":
                    try {
                        double importo = ((Number) dati.get("importo")).doubleValue();
                        gestoreCassa.gestisciInserimentoMoneta(importo);
                    } catch (Exception e) {
                        pubblicaErrore("errore_inserimento_credito", "Errore nell'inserimento del credito");
                    }
                    break;

                case "richiestaBevanda":
                    if (!inErogazione.compareAndSet(false, true)) {
                        pubblicaErrore("macchina_occupata", "Erogazione già in corso");
                        return;
                    }

                    try {
                        int bevandaId = ((Number) dati.get("bevandaId")).intValue();
                        int livelloZucchero = ((Number) dati.get("livelloZucchero")).intValue();
                        gestisciErogazioneBevanda(bevandaId, livelloZucchero);
                    } catch (Exception e) {
                        pubblicaErrore("errore_erogazione", "Errore nell'erogazione della bevanda");
                        inErogazione.set(false);
                    }
                    break;

                case "richiestaResto":
                    gestoreCassa.gestisciRestituzioneCredito();
                    break;
            }
        });
    }
    
    /**
     * Eroga una bevanda e registra la transazione.
     * Nota: Questo metodo si assume che le verifiche di credito e disponibilità cialde 
     * siano già state effettuate.
     * 
     * @param bevandaId ID della bevanda da erogare
     * @param livelloZucchero Livello di zucchero richiesto
     * @return true se l'erogazione ha successo
     */
    
    public boolean gestisciErogazioneBevanda(int bevandaId, int livelloZucchero) {
        try {
            // Verifica che la bevanda sia disponibile
        	Optional<Bevanda> bevandaOpt = gestoreBevande.trovaBevanda(bevandaId);
            if (bevandaOpt.isEmpty()) {
                logger.warn("Tentativo di erogare bevanda non disponibile: {}", bevandaId);
                return false;
            }
            Bevanda bevandaRichiesta = bevandaOpt.get();
            
            // Verifica che ci siano cialde sufficienti
            if (!gestoreCialde.verificaDisponibilitaCialde(bevandaRichiesta.getCialde())) {
                pubblicaErrore("cialde_insufficienti", "Cialde insufficienti per l'erogazione");
                return false;
            }

            // Verifica che ci sia spazio sufficiente nella cassa
            if (!gestoreCassa.puoAccettareImporto(bevandaRichiesta.getPrezzo())) {
                pubblicaErrore("cassa_piena", "Non c'è spazio sufficiente nella cassa");
                return false;
            }
<<<<<<< HEAD
            
            // Aggiorna il saldo della cassa
            gestoreCassa.processaPagamento(bevandaRichiesta.getPrezzo(), bevandaRichiesta.getId());
            
=======

            // Se tutti i controlli sono superati, procedi con l'erogazione
            pubblicaEvento("inizio_erogazione", "Preparazione bevanda in corso");

            // Simula tempo di preparazione
            Thread.sleep(7500);

>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
            // Consuma le cialde
            gestoreCialde.consumaCialde(bevandaRichiesta.getCialde());
            
            // Simula tempo di preparazione
            Thread.sleep(7500);

<<<<<<< HEAD
            // NOVITÀ: Sincronizza lo stato della macchina nel database
            sincronizzaStatoMacchina();
=======
            // Aggiorna il saldo della cassa
            gestoreCassa.processaPagamento(bevandaRichiesta.getPrezzo(), bevandaRichiesta.getId());

            // Completa erogazione
            pubblicaEvento("fine_erogazione", "Bevanda pronta");
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26

            // Pubblica aggiornamento di stato
            pubblicaStatoMacchina();
            
            logger.info("Erogazione bevanda {} completata con successo nella macchina {}", bevandaId, id);
            return true;
        } catch (Exception e) {
            pubblicaErrore("errore_erogazione", "Errore durante l'erogazione: " + e.getMessage());
            return false;
        } finally {
            inErogazione.set(false);
        }
    }
<<<<<<< HEAD
    
    
=======

>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
    /**
    * Pubblica lo stato corrente della macchina sul topic MQTT appropriato.
    * Questa funzione raccoglie lo stato da tutti i componenti e lo pubblica
    * come risposta su un topic specifico.
    */
    public void pubblicaStatoMacchina() {
	    try {
	        // Raccogli lo stato completo della macchina
	        Map<String, Object> statoCompletaMacchina = new HashMap<>();
	        
	        // Aggiungi informazioni di base della macchina
	        statoCompletaMacchina.put("id", id);
	        statoCompletaMacchina.put("timestamp", System.currentTimeMillis());
	        statoCompletaMacchina.put("inErogazione", inErogazione.get());
	        
	        // Aggiungi lo stato dei componenti
	        statoCompletaMacchina.put("cassa", gestoreCassa.ottieniStato());
	        statoCompletaMacchina.put("bevande", gestoreBevande.ottieniStato());
	        statoCompletaMacchina.put("cialde", gestoreCialde.ottieniStato());
	        statoCompletaMacchina.put("manutenzione", gestoreManutenzione.ottieniStato());
	        
	        // Converti lo stato in JSON
	        String statoJson = gson.toJson(statoCompletaMacchina);
	        
	        // Pubblica sul topic specifico per le risposte di stato
	        String topic = "macchine/" + id + "/stato/risposta";
	        clientMqtt.publish(topic, statoJson);
	        
	        // Log dell'operazione (usa il logger se disponibile)
	        System.out.println("Stato macchina " + id + " pubblicato");
	    } catch (Exception e) {
	        // Log dell'errore
	        System.err.println("Errore durante la pubblicazione dello stato della macchina " + 
	                    id + ": " + e.getMessage());
	        e.printStackTrace();
	    }
	}
    private void pubblicaEvento(String tipo, String messaggio) {
        try {
            Map<String, Object> evento = Map.of(
                    "tipo", tipo,
                    "messaggio", messaggio,
                    "timestamp", System.currentTimeMillis()
            );

            clientMqtt.publish("macchine/" + id + "/eventi/notifica", gson.toJson(evento));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sincronizza lo stato della macchina con il database dopo modifiche.
     * Questo metodo aggiorna i dati della macchina nel database in base allo stato
     * corrente memorizzato nei gestori.
     */
    private void sincronizzaStatoMacchina() {
        try {
            // Recupera lo stato attuale dal database
            Macchina macchinaDB = macchinaRepository.findById(id);
            if (macchinaDB == null) {
                logger.error("Impossibile sincronizzare stato: macchina {} non trovata nel database", id);
                return;
            }

            // Aggiorna lo stato della cassa
            Map<String, Object> statoCassa = gestoreCassa.ottieniStato();
            macchinaDB.setCreditoAttuale((Double) statoCassa.get("creditoAttuale"));
            macchinaDB.setCassaAttuale((Double) statoCassa.get("cassaAttuale"));

            // Aggiorna le quantità delle cialde
            List<QuantitaCialde> cialdeDB = macchinaDB.getCialde();
            Map<String, Object> statoCialde = gestoreCialde.ottieniStato();
            
            // Il formato esatto dipende dall'implementazione di gestoreCialde.ottieniStato()
            // Assumiamo che restituisca una mappa con le quantità di cialde
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> quantitaCialde = (Map<Integer, Integer>) statoCialde.get("quantita");
            
            if (quantitaCialde != null) {
                for (QuantitaCialde cialda : cialdeDB) {
                    Integer nuovaQuantita = quantitaCialde.get(cialda.getCialdaId());
                    if (nuovaQuantita != null) {
                        cialda.setQuantita(nuovaQuantita);
                    }
                }
            }

            // Aggiorna la macchina nel database
            macchinaRepository.update(macchinaDB);
            
            logger.info("Aggionamento stato della macchina #{} effettuato con successo", id);
        } catch (Exception e) {
            logger.error("Errore durante la sincronizzazione dello stato della macchina {}: {}", 
                        id, e.getMessage(), e);
        }
    }

    private void pubblicaErrore(String codice, String messaggio) {
        try {
            Map<String, Object> errore = Map.of(
                    "codice", codice, 
                    "messaggio", messaggio,
                    "timestamp", System.currentTimeMillis()
            );
            
            clientMqtt.publish("macchine/" + id + "/errori/notifica", gson.toJson(errore));
        } catch (Exception e) {
            e.printStackTrace(); 
        }
    }
    
    private int estraiMacchinaIdDaTopic(String topic) {
        try {
            String[] parts = topic.split("/");
<<<<<<< HEAD
            // Verifica che il topic abbia la struttura corretta
            if (parts.length >= 2 && parts[0].equals("macchine")) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    logger.error("ID macchina non numerico nel topic: {}", parts[1]);
                    throw new IllegalArgumentException("ID macchina non numerico: " + parts[1]);
                }
            } else {
                logger.error("Formato del topic non valido: {}", topic);
                throw new IllegalArgumentException("Formato del topic non valido: " + topic);
            }
        } catch (Exception e) {
            logger.error("Errore nell'estrazione dell'ID macchina dal topic: {}", topic, e);
=======
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            } else {
                throw new IllegalArgumentException("Format del topic non valido: " + topic);
            }
        } catch (Exception e) {
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
            throw new IllegalArgumentException("Errore nell'estrazione dell'ID macchina dal topic: " + topic, e);
        }
    }
    
    

    public void eseguiSpegnimento() {
        try {
            pubblicaEvento("spegnimento", "Spegnimento macchina in corso");
            gestoreCassa.spegni();
            gestoreBevande.spegni();
            gestoreCialde.spegni();
            gestoreManutenzione.spegni();
            clientMqtt.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void eseguiRiavvio() {
        try {
            pubblicaEvento("riavvio", "Riavvio macchina in corso");
            eseguiSpegnimento();
            Thread.sleep(5000);
            inizializzaMacchina(new Macchina());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
