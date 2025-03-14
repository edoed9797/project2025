package com.vending.iot.machines;

import com.google.gson.Gson;
import com.vending.Main;
import com.vending.ServiceRegistry;
import com.vending.core.models.Macchina;
import com.vending.core.models.Manutenzione;
import com.vending.core.models.Ricavo;
import com.vending.core.repositories.MacchinaRepository;
import com.vending.core.repositories.ManutenzioneRepository;
import com.vending.core.repositories.RicavoRepository;
import com.vending.iot.mqtt.MQTTClient;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GestoreManutenzione {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private final int idMacchina;
    private final MQTTClient mqttClient;
    private final Map<String, String> problemiAttivi; // Mappa per tenere traccia dei problemi attivi
    private final Gson gson;
    private final ManutenzioneRepository manutenzioneRepo;

    public GestoreManutenzione(int idMacchina) throws MqttException {
        this.idMacchina = idMacchina;
        this.problemiAttivi = new HashMap<>();
        this.gson = new Gson();
        this.mqttClient = new MQTTClient("manutenzione_" + idMacchina);
        this.manutenzioneRepo = ServiceRegistry.get(ManutenzioneRepository.class);
        inizializzaSottoscrizioni();
    }

    private void inizializzaSottoscrizioni() throws MqttException {
        String baseTopic = "macchine/" + idMacchina + "/manutenzione/";
        
        // Sottoscrizione per segnalazioni
        mqttClient.subscribe(baseTopic + "segnalazione/richiesta", (topic, messaggio) -> {
            gestisciSegnalazione(gson.fromJson(messaggio, SegnalazioneProblema.class));
        });
        
        // Sottoscrizione per risoluzioni
        mqttClient.subscribe(baseTopic + "risoluzione/richiesta", (topic, messaggio) -> {
            gestisciRisoluzione(gson.fromJson(messaggio, RisoluzioneProblema.class));
        });
        
        // Sottoscrizione per verifiche di stato
        mqttClient.subscribe(baseTopic + "verifica/richiesta", (topic, messaggio) -> {
            verificaStato();
        });
        
        // Sottoscrizione per richieste di stato
        mqttClient.subscribe(baseTopic + "stato/richiesta", (topic, messaggio) -> {
            pubblicaStatoManutenzione();
        });
    }

    public void segnalaProblema(String tipo, String descrizione, Map<String, Object> dettagliAggiuntivi) {
        try {
            SegnalazioneProblema segnalazione = new SegnalazioneProblema();
            segnalazione.tipo = tipo;
            segnalazione.descrizione = descrizione;
            segnalazione.dettagli = dettagliAggiuntivi;

            String topic = "macchine/" + idMacchina + "/manutenzione/segnalazione/notifica";
            mqttClient.publish(topic, gson.toJson(segnalazione));

            String descProblema = "Problema " + segnalazione.tipo + ": " + segnalazione.descrizione;
            problemiAttivi.put(String.valueOf(segnalazione.timestamp), descProblema);
            pubblicaStatoManutenzione();

            // Registra la manutenzione nel database
            registraManutenzione(tipo, descrizione, dettagliAggiuntivi);

        } catch (MqttException e) {
            logger.error("Errore durante la segnalazione del problema: {}", e.getMessage());
        }
    }

    private void registraManutenzione(String tipoIntervento, String descrizione, Map<String, Object> dettagli) {
        try {
            Manutenzione manutenzione = new Manutenzione();
            manutenzione.setMacchinaId(idMacchina);
            manutenzione.setTipoIntervento(tipoIntervento);
            manutenzione.setDescrizione(descrizione);
            manutenzione.setDataRichiesta(LocalDateTime.now());
            manutenzione.setStato("IN_ATTESA");
            manutenzione.setUrgenza((String) dettagli.getOrDefault("urgenza", "MEDIA"));
            manutenzione.setNote((String) dettagli.get("note"));

            manutenzioneRepo.save(manutenzione);
            logger.info("Registrata nuova manutenzione per la macchina {}: {}", idMacchina, tipoIntervento);

        } catch (Exception e) {
            logger.error("Errore durante la registrazione della manutenzione: {}", e.getMessage());
        }
    }

    public void risolviProblema(String idProblema, String descrizioneRisoluzione, String tecnico) {
        try {
            RisoluzioneProblema risoluzione = new RisoluzioneProblema();
            risoluzione.idProblema = idProblema;
            risoluzione.descrizioneRisoluzione = descrizioneRisoluzione;
            risoluzione.tecnico = tecnico;
            risoluzione.timestampRisoluzione = System.currentTimeMillis();

            String topic = "macchine/" + idMacchina + "/manutenzione/risoluzione/completata";
            mqttClient.publish(topic, gson.toJson(risoluzione));

            problemiAttivi.remove(idProblema);
            pubblicaStatoManutenzione();

            // Aggiorna la manutenzione nel database
            aggiornaManutenzione(idProblema, descrizioneRisoluzione, tecnico);

        } catch (MqttException e) {
            logger.error("Errore durante la risoluzione del problema: {}", e.getMessage());
        }
    }
    
    private void aggiornaManutenzione(String idProblema, String descrizioneRisoluzione, String tecnico) {
        try {
            // Trova la manutenzione per ID
            Optional<Manutenzione> optionalManutenzione = manutenzioneRepo.findById(Integer.parseInt(idProblema));

            // Se la manutenzione è presente, aggiornala
            if (optionalManutenzione.isPresent()) {
                Manutenzione manutenzione = optionalManutenzione.get(); // Estrai l'oggetto Manutenzione dall'Optional
                manutenzione.setStato("COMPLETATA");
                manutenzione.setDataCompletamento(LocalDateTime.now());
                manutenzione.setNote(descrizioneRisoluzione);
                manutenzione.setTecnicoId(Integer.parseInt(tecnico)); // Supponendo che tecnico sia l'ID del tecnico

                // Aggiorna la manutenzione nel repository
                manutenzioneRepo.update(manutenzione);
                logger.info("Manutenzione {} completata dal tecnico {}", idProblema, tecnico);
            } else {
                logger.warn("Manutenzione con ID {} non trovata", idProblema);
            }
        } catch (Exception e) {
            logger.error("Errore durante l'aggiornamento della manutenzione: {}", e.getMessage());
        }
    }
    private void gestisciSegnalazione(SegnalazioneProblema segnalazione) {
        problemiAttivi.put(String.valueOf(segnalazione.timestamp), segnalazione.descrizione);
        pubblicaStatoManutenzione();
    }

    private void gestisciRisoluzione(RisoluzioneProblema risoluzione) {
        problemiAttivi.remove(risoluzione.idProblema);
        pubblicaStatoManutenzione();
    }

    public Map<String, Object> getStatoManutenzione() {
        return Map.of(
                "problemiAttivi", problemiAttivi,
                "ultimoControllo", System.currentTimeMillis(),
                "richiedeIntervento", !problemiAttivi.isEmpty(),
                "numeroProblemi", problemiAttivi.size()
        );
    }

    private void pubblicaStatoManutenzione() {
        try {
            String topic = "macchine/" + idMacchina + "/manutenzione/stato/risposta";
            Map<String, Object> stato = getStatoManutenzione();
            mqttClient.publish(topic, gson.toJson(stato));

            if ((boolean) stato.get("richiedeIntervento")) {
                String topicAvviso = "macchine/" + idMacchina + "/manutenzione/avviso/notifica";
                Map<String, Object> avviso = Map.of(
                        "tipo", "RICHIESTA_INTERVENTO",
                        "numeroProblemi", stato.get("numeroProblemi"),
                        "ultimoControllo", stato.get("ultimoControllo"),
                        "timestamp", System.currentTimeMillis()
                );
                mqttClient.publish(topicAvviso, gson.toJson(avviso));
            }
        } catch (MqttException e) {
            logger.error("Errore durante la pubblicazione dello stato: {}", e.getMessage());
        }
    }

    public void verificaStato() {
        boolean interventoUrgente = !problemiAttivi.isEmpty();

        if (interventoUrgente) {
            try {
                String topic = "macchine/" + idMacchina + "/manutenzione/urgente/notifica";
                Map<String, Object> avviso = Map.of(
                        "tipo", "INTERVENTO_URGENTE",
                        "timestamp", System.currentTimeMillis(),
                        "problemi", problemiAttivi
                );
                mqttClient.publish(topic, gson.toJson(avviso));
            } catch (MqttException e) {
                logger.error("Errore durante la pubblicazione dell'avviso urgente: {}", e.getMessage());
            }
        }
    }

    public Map<String, Object> ottieniStato() {
        return Map.of(
                "problemiAttivi", problemiAttivi,
                "ultimoControllo", System.currentTimeMillis(),
                "richiedeIntervento", !problemiAttivi.isEmpty(),
                "numeroProblemi", problemiAttivi.size()
        );
    }

    public void richiestaManutenzioneCialde(int macchinaId, String messaggio) {
        try {
            Map<String, Object> dettagli = Map.of(
                    "tipoIntervento", "RIFORNIMENTO_CIALDE",
                    "urgenza", "MEDIA",
                    "stato", "IN_ATTESA"
            );

            segnalaProblema("CIALDE_ESAURITE", messaggio, dettagli);

            // Aggiorna stato macchina a "In manutenzione"
            MacchinaRepository macchinaRepo = ServiceRegistry.get(MacchinaRepository.class);
            Macchina macchina = macchinaRepo.findById(macchinaId);
            if (macchina != null) {
                macchina.setStatoId(2); // 2 = In manutenzione
                macchinaRepo.update(macchina);
            }

        } catch (Exception e) {
            logger.error("Errore nella richiesta manutenzione cialde: {}", e.getMessage());
        }
    }

    public void richiestaSvuotamentoCassa(int macchinaId, double importo) {
        try {
            Map<String, Object> dettagli = Map.of(
                    "tipoIntervento", "SVUOTAMENTO_CASSA",
                    "urgenza", "ALTA",
                    "importo", importo
            );

            segnalaProblema("CASSA_PIENA",
                    "Necessario svuotamento cassa - Importo: " + importo + "€",
                    dettagli);

            // Registra il ricavo
            registraRicavo(macchinaId, importo);

        } catch (Exception e) {
            logger.error("Errore nella richiesta svuotamento cassa: {}", e.getMessage());
        }
    }

    private void registraRicavo(int macchinaId, double importo) {
        try {
            RicavoRepository ricavoRepo = ServiceRegistry.get(RicavoRepository.class);

            Ricavo ricavo = new Ricavo();
            ricavo.setMacchinaId(macchinaId);
            ricavo.setImporto(importo);
            ricavo.setDataOra(LocalDateTime.now());

            ricavoRepo.save(ricavo);
            logger.info("Registrato ricavo di {}€ per la macchina {}", importo, macchinaId);

        } catch (Exception e) {
            logger.error("Errore nella registrazione del ricavo: {}", e.getMessage());
        }
    }

    public void spegni() {
        mqttClient.disconnect();
    }

    private static class SegnalazioneProblema {
        String tipo;
        String descrizione;
        Map<String, Object> dettagli;
        long timestamp;

        public SegnalazioneProblema() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class RisoluzioneProblema {
        String idProblema;
        String descrizioneRisoluzione;
        String tecnico;
        long timestampRisoluzione;
    }
}