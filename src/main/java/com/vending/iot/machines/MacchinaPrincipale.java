package com.vending.iot.machines;

import com.vending.core.models.Macchina;
import com.vending.core.models.Bevanda;
import com.vending.iot.mqtt.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import com.google.gson.Gson;
import java.util.Map;
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
    private final Gson gson;
    private final AtomicBoolean inErogazione;

    public MacchinaPrincipale(Macchina macchina) throws MqttException {
        this.id = macchina.getId();
        this.gson = new Gson();
        this.clientMqtt = new MQTTClient("macchina_" + id);
        this.gestoreCassa = new GestoreCassa(id, macchina.getCassaMassima());
        this.gestoreCialde = new GestoreCialde(id);
        this.gestoreBevande = new GestoreBevande(id, gestoreCassa, gestoreCialde);
        this.gestoreManutenzione = new GestoreManutenzione(id);
        this.inErogazione = new AtomicBoolean(false);

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
                case "stato":
                    pubblicaStatoMacchina();
                    break;
            }
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

    public void gestisciErogazioneBevanda(int bevandaId, int livelloZucchero) {
        try {
            // Verifica che la bevanda sia disponibile
            List<Bevanda> bevandeDisponibili = gestoreBevande.getBevandeMacchina(id);
            Bevanda bevandaRichiesta = null;

            for (Bevanda bevanda : bevandeDisponibili) {
                if (bevanda.getId() == bevandaId) {
                    bevandaRichiesta = bevanda;
                    break;
                }
            }

            if (bevandaRichiesta == null) {
                pubblicaErrore("bevanda_non_disponibile", "Bevanda non disponibile");
                return;
            }

            // Verifica che ci siano cialde sufficienti
            if (!gestoreCialde.verificaDisponibilitaCialde(bevandaRichiesta.getCialde())) {
                pubblicaErrore("cialde_insufficienti", "Cialde insufficienti per l'erogazione");
                return;
            }

            // Verifica che ci sia spazio sufficiente nella cassa
            if (!gestoreCassa.puoAccettareImporto(bevandaRichiesta.getPrezzo())) {
                pubblicaErrore("cassa_piena", "Non c'è spazio sufficiente nella cassa");
                return;
            }

            // Se tutti i controlli sono superati, procedi con l'erogazione
            pubblicaEvento("inizio_erogazione", "Preparazione bevanda in corso");

            // Simula tempo di preparazione
            Thread.sleep(5000);

            // Consuma le cialde
            gestoreCialde.consumaCialde(bevandaRichiesta.getCialde());

            // Aggiorna il saldo della cassa
            gestoreCassa.processaPagamento(bevandaRichiesta.getPrezzo());

            // Completa erogazione
            pubblicaEvento("fine_erogazione", "Bevanda pronta");

        } catch (Exception e) {
            pubblicaErrore("errore_erogazione", "Errore durante l'erogazione: " + e.getMessage());
        } finally {
            inErogazione.set(false);
        }
    }

    public void pubblicaStatoMacchina() {
        try {
            Map<String, Object> stato = new HashMap<>();
            stato.put("id", id);
            stato.put("statoCassa", gestoreCassa.ottieniStato());
            stato.put("statoBevande", gestoreBevande.ottieniStato());
            stato.put("statoCialde", gestoreCialde.ottieniStato());
            stato.put("statoManutenzione", gestoreManutenzione.ottieniStato());
            stato.put("inErogazione", inErogazione.get());

            clientMqtt.publish("macchine/" + id + "/stato", gson.toJson(stato));
        } catch (Exception e) {
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

            clientMqtt.publish("macchine/" + id + "/eventi", gson.toJson(evento));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pubblicaErrore(String codice, String messaggio) {
        pubblicaEvento("errore", Map.of(
                "codice", codice,
                "messaggio", messaggio
        ).toString());
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
