package com.vending.iot.machines;

import com.google.gson.Gson;
import com.vending.ServiceRegistry;
import com.vending.core.models.Ricavo;
import com.vending.core.repositories.RicavoRepository;
import com.vending.iot.mqtt.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Classe che rappresenta il gestore della cassa di una macchina distributrice.
 * Gestisce l'inserimento di monete, lo svuotamento della cassa e la
 * restituzione del credito.
 *
 * Un'operazione atomica è un'operazione che viene eseguita in un singolo passo
 * indivisibile. Questo significa che, in un contesto multi-thread, nessun
 * thread può osservare uno stato intermedio durante l'esecuzione
 * dell'operazione.
 */
public class GestoreCassa {

    private static final Logger logger = LoggerFactory.getLogger(GestoreCassa.class);
    private final int idMacchina;
    private final AtomicReference<Double> creditoAttuale;
    private final AtomicReference<Double> cassaAttuale;
    private final double cassaMassima;
    private final MQTTClient mqttClient;
    private final Gson gson;
    private static final double SOGLIA_AVVISO_CASSA_PIENA = 0.9; // 90%

    /**
     * Costruttore del gestore cassa.
     *
     * @param idMacchina ID della macchina distributrice
     * @param cassaMassima capacità massima della cassa
     * @throws MqttException se si verificano errori nella connessione MQTT
     */
    public GestoreCassa(int idMacchina, double cassaMassima) throws MqttException {
        if (cassaMassima <= 0) {
            throw new IllegalArgumentException("La capacità massima della cassa deve essere positiva");
        }

        this.idMacchina = idMacchina;
        this.cassaMassima = cassaMassima;
        this.creditoAttuale = new AtomicReference<>(0.0);
        this.cassaAttuale = new AtomicReference<>(0.0);
        this.gson = new Gson();
        this.mqttClient = new MQTTClient("cassa_" + idMacchina);

        logger.info("Inizializzazione GestoreCassa per macchina {}", idMacchina);
        inizializzaSottoscrizioni();
    }

    /**
     * Imposta il saldo corrente della cassa. Viene utilizzato principalmente
     * per inizializzare o sincronizzare lo stato della cassa.
     *
     * @param nuovoSaldo il nuovo saldo da impostare
     * @throws IllegalArgumentException se il saldo è negativo o supera la
     * capacità massima
     */
    public void impostaSaldoCassa(double nuovoSaldo) {
        if (nuovoSaldo < 0) {
            logger.error("Tentativo di impostare un saldo negativo: {}", nuovoSaldo);
            throw new IllegalArgumentException("Il saldo della cassa non può essere negativo");
        }
        if (nuovoSaldo > cassaMassima) {
            logger.error("Tentativo di impostare un saldo superiore alla capacità massima: {} > {}",
                    nuovoSaldo, cassaMassima);
            throw new IllegalArgumentException("Il saldo supera la capacità massima della cassa");
        }

        cassaAttuale.set(nuovoSaldo);
        logger.info("Saldo cassa impostato a {} per la macchina {}", nuovoSaldo, idMacchina);
        pubblicaStatoCassa();

        // Verifica se necessario pubblicare l'avviso di cassa quasi piena
        if ((nuovoSaldo / cassaMassima) > SOGLIA_AVVISO_CASSA_PIENA) {
            pubblicaAvvisoCassaPiena();
        }
    }

    /**
     * Restituisce lo stato attuale della cassa, inclusi credito, saldo e
     * percentuale di occupazione.
     *
     * @return una mappa contenente: creditoAttuale, cassaAttuale, cassaMassima
     * e percentualeOccupazione
     */
    public Map<String, Object> ottieniStato() {
        double saldoAttuale = cassaAttuale.get();
        return Map.of(
                "creditoAttuale", creditoAttuale.get(),
                "cassaAttuale", saldoAttuale,
                "cassaMassima", cassaMassima,
                "percentualeOccupazione", (saldoAttuale / cassaMassima) * 100
        );
    }

    /**
     * Inizializza le sottoscrizioni MQTT per gestire le operazioni della cassa.
     * Gestisce le operazioni di inserimento monete, svuotamento cassa e
     * restituzione credito.
     *
     * @throws MqttException se si verificano errori nella sottoscrizione ai
     * topic MQTT
     */
    private void inizializzaSottoscrizioni() throws MqttException {
        String baseTopic = "macchine/" + idMacchina + "/cassa/";
        mqttClient.subscribe(baseTopic + "#", (topic, messaggio) -> {
            logger.debug("Ricevuto messaggio sul topic {}: {}", topic, messaggio);
            String azione = topic.substring(baseTopic.length());
            double importo = gson.fromJson(messaggio, double.class);
            try {
                switch (azione) {
                    case "inserimento":
                        gestisciInserimentoMoneta(importo);
                        break;
                    case "svuotamento":
                        gestisciSvuotamentoCassa();
                        break;
                    case "cancella":
                        gestisciRestituzioneCredito();
                        break;
                    default:
                        logger.warn("Azione non riconosciuta: {}", azione);
                }
            } catch (Exception e) {
                logger.error("Errore durante l'elaborazione del messaggio", e);
                pubblicaErrore("Errore durante l'elaborazione dell'operazione: " + e.getMessage());
            }
        });
        logger.info("Sottoscrizioni inizializzate per la macchina {}", idMacchina);
    }

    /**
     * Gestisce l'inserimento di una moneta nella cassa. Verifica se l'importo
     * può essere accettato e aggiorna il credito attuale.
     *
     * @param operazione l'operazione di inserimento moneta contenente l'importo
     */
    public boolean gestisciInserimentoMoneta(double importo) {
        logger.debug("Gestione inserimento moneta: {}", importo);
        if (importo <= 0) {
            pubblicaErrore("L'importo deve essere positivo");
            return false;
        }

        if (puoAccettareImporto(importo)) {
            creditoAttuale.updateAndGet(credito -> credito + importo);
            logger.info("Credito aggiornato a {} per la macchina {}", creditoAttuale.get(), idMacchina);
            pubblicaStatoCredito();
            return true;
        } else {
            logger.warn("Impossibile accettare l'importo {} - cassa piena", importo);
            pubblicaErrore("Impossibile accettare l'importo - cassa piena");
            return false;
        }
    }

    /**
     * Gestisce l'operazione di svuotamento della cassa. Azzera il saldo attuale
     * e pubblica la conferma dell'operazione.
     */
    public void gestisciSvuotamentoCassa() {
        double importoSvuotato = cassaAttuale.getAndSet(0.0);
        logger.info("Svuotamento cassa effettuato: {} per la macchina {}", importoSvuotato, idMacchina);
        // Registra il ricavo
        try {
            RicavoRepository ricavoRepo = ServiceRegistry.get(RicavoRepository.class);
            Ricavo ricavo = new Ricavo(idMacchina, importoSvuotato);
            ricavo.setDataOra(LocalDateTime.now());
            ricavoRepo.save(ricavo);

            // Richiedi manutenzione per conferma svuotamento
            GestoreManutenzione gestoreManutenzione = new GestoreManutenzione(idMacchina);
            gestoreManutenzione.richiestaSvuotamentoCassa(idMacchina, importoSvuotato);

        } catch (Exception e) {
            logger.error("Errore durante la registrazione del ricavo per svuotamento cassa: {}", e.getMessage());
        }
        pubblicaConfermaSvuotamento(importoSvuotato);
        pubblicaStatoCassa();
    }

    /**
     * Gestisce la restituzione del credito all'utente. Azzera il credito
     * attuale e pubblica la conferma dell'operazione.
     */
    public void gestisciRestituzioneCredito() {
        double importoRestituito = creditoAttuale.getAndSet(0.0);
        logger.info("Restituzione credito: {} per la macchina {}", importoRestituito, idMacchina);
        pubblicaRestituzione(importoRestituito);
        pubblicaStatoCredito();
    }

    /**
     * Verifica se la cassa può accettare un determinato importo. Controlla che
     * l'importo non faccia superare la capacità massima della cassa.
     *
     * @param importo l'importo da verificare
     * @return true se l'importo può essere accettato, false altrimenti
     */
    public boolean puoAccettareImporto(double importo) {
        return (cassaAttuale.get() + creditoAttuale.get() + importo) <= cassaMassima;
    }

    /**
     * Pubblica lo stato attuale della cassa sul topic MQTT appropriato. Include
     * informazioni su saldo attuale, capacità massima e percentuale di
     * occupazione.
     */
    private void pubblicaStatoCassa() {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/stato";
            Map<String, Object> stato = Map.of(
                    "cassaAttuale", cassaAttuale.get(),
                    "cassaMassima", cassaMassima,
                    "percentualeOccupazione", (cassaAttuale.get() / cassaMassima) * 100,
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(stato));
        } catch (MqttException e) {
            logger.error("Errore pubblicazione stato cassa", e);
        }
    }

    /**
     * Pubblica lo stato del credito attuale sul topic MQTT appropriato.
     */
    private void pubblicaStatoCredito() {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/credito";
            Map<String, Object> stato = Map.of(
                    "creditoAttuale", creditoAttuale.get(),
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(stato));
        } catch (MqttException e) {
            logger.error("Errore pubblicazione stato credito", e);
        }
    }

    /**
     * Pubblica un avviso quando la cassa supera la soglia di riempimento.
     * L'avviso viene inviato quando la cassa supera il 90% della sua capacità.
     */
    private void pubblicaAvvisoCassaPiena() {
        try {
            String topicAvviso = "macchine/" + idMacchina + "/cassa/avviso";
            Map<String, Object> avviso = Map.of(
                    "tipo", "CASSA_QUASI_PIENA",
                    "messaggio", "La cassa ha superato il 90% della capacità",
                    "percentualeOccupazione", (cassaAttuale.get() / cassaMassima) * 100,
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topicAvviso, gson.toJson(avviso));
            logger.warn("Avviso cassa quasi piena pubblicato per la macchina {}", idMacchina);
        } catch (MqttException e) {
            logger.error("Errore pubblicazione avviso cassa piena", e);
        }
    }

    /**
     * Pubblica la conferma di svuotamento della cassa con l'importo svuotato.
     *
     * @param importo l'importo che è stato prelevato dalla cassa
     */
    private void pubblicaConfermaSvuotamento(double importo) {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/svuotamento/conferma";
            Map<String, Object> conferma = Map.of(
                    "importo", importo,
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(conferma));
        } catch (MqttException e) {
            logger.error("Errore pubblicazione conferma svuotamento", e);
        }
    }

    /**
     * Pubblica la conferma di restituzione del credito all'utente.
     *
     * @param importo l'importo che è stato restituito all'utente
     */
    private void pubblicaRestituzione(double importo) {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/resto/conferma";
            Map<String, Object> resto = Map.of(
                    "importo", importo,
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(resto));
        } catch (MqttException e) {
            logger.error("Errore pubblicazione restituzione", e);
        }
    }

    /**
     * Pubblica un messaggio di errore sul topic MQTT appropriato.
     *
     * @param messaggio il messaggio di errore da pubblicare
     */
    private void pubblicaErrore(String messaggio) {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/errore";
            Map<String, Object> errore = Map.of(
                    "messaggio", messaggio,
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(errore));
            logger.error("Errore cassa: {}", messaggio);
        } catch (MqttException e) {
            logger.error("Errore pubblicazione errore", e);
        }
    }

    /**
     * Processa un pagamento, verificando la disponibilità del credito e
     * aggiornando i saldi. Se il pagamento va a buon fine, aggiorna sia il
     * credito che la cassa e pubblica gli aggiornamenti.
     *
     * @param prezzo l'importo da addebitare
     * @return true se il pagamento è stato processato con successo, false
     * altrimenti
     */
    public boolean processaPagamento(double prezzo) {
        if (prezzo <= 0) {
            logger.error("Tentativo di processare un pagamento con prezzo non valido: {}", prezzo);
            return false;
        }

        if (creditoAttuale.get() >= prezzo) {
            creditoAttuale.updateAndGet(credito -> credito - prezzo);
            cassaAttuale.updateAndGet(cassa -> cassa + prezzo);

            try {
                pubblicaStatoCredito();
                pubblicaStatoCassa();

                if ((cassaAttuale.get() / cassaMassima) > SOGLIA_AVVISO_CASSA_PIENA) {
                    pubblicaAvvisoCassaPiena();
                }

                logger.info("Pagamento processato con successo: {} per la macchina {}", prezzo, idMacchina);
                return true;
            } catch (Exception e) {
                logger.error("Errore durante la pubblicazione degli aggiornamenti del pagamento", e);
                return true; // Il pagamento è comunque avvenuto con successo
            }
        }

        logger.warn("Credito insufficiente per il pagamento: {} < {}", creditoAttuale.get(), prezzo);
        return false;
    }

    // /**
    //  * Classe interna che rappresenta un'operazione di inserimento moneta.
    //  */
    // static class OperazioneMoneta {
    //         public double importo;
    // 		public double getImporto() {
    // 			return importo;
    // 		}
    // 		public void setImporto(double importo) {
    // 			this.importo = importo;
    // 		}
    //     }
    /**
     * Spegne il gestore cassa, disconnettendo il client MQTT. Da chiamare
     * quando la macchina viene spenta o riavviata.
     */
    public void spegni() {
        try {
            mqttClient.disconnect();
            logger.info("GestoreCassa spento per la macchina {}", idMacchina);
        } catch (Exception e) {
            logger.error("Errore durante lo spegnimento del GestoreCassa", e);
        }
    }
}
