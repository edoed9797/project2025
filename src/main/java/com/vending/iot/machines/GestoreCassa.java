package com.vending.iot.machines;

import com.google.gson.Gson;
import com.vending.ServiceRegistry;
import com.vending.core.models.Macchina;
import com.vending.core.models.Ricavo;
import com.vending.core.models.Transazione;
import com.vending.core.repositories.MacchinaRepository;
import com.vending.core.repositories.RicavoRepository;
import com.vending.core.repositories.TransazioneRepository;
import com.vending.iot.mqtt.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private double  creditoAttuale;
    private double  cassaAttuale;
    private final double cassaMassima;
    private final MQTTClient mqttClient;
    private final Gson gson;
    private final MacchinaRepository macchinaRepository;
    private static final double SOGLIA_AVVISO_CASSA_PIENA = 0.8; // 80%

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
        this.gson = new Gson();
        this.mqttClient = new MQTTClient("cassa_" + idMacchina);
        this.idMacchina = idMacchina;
        this.cassaMassima = cassaMassima;
        this.macchinaRepository = ServiceRegistry.get(MacchinaRepository.class);
        
        Macchina macchina = macchinaRepository.findById(idMacchina);
        if (macchina != null) {
            this.cassaAttuale = macchina.getCassaAttuale();
            this.creditoAttuale = macchina.getCreditoAttuale();
        } else {
            this.cassaAttuale = 0.0;
            this.creditoAttuale = 0.0;
            logger.warn("Macchina {} non trovata nel database, inizializzata cassa a 0", idMacchina);
        }
        logger.info("Cassa per macchina {} configurata con successo", idMacchina);
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

        cassaAttuale = nuovoSaldo;
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
        double saldoAttuale = cassaAttuale;
        Map<String, Object> stato = new HashMap<>();
        stato.put("creditoAttuale", creditoAttuale);
        stato.put("cassaAttuale", saldoAttuale);
        stato.put("cassaMassima", cassaMassima);
        stato.put("percentualeOccupazione", (saldoAttuale / cassaMassima) * 100);
        stato.put("timestamp", System.currentTimeMillis());
        return stato;
    }

    /**
     * Gestisce l'inserimento di una moneta nella cassa.
     *
     * @param importo l'importo da inserire
     * @return true se l'inserimento è riuscito, false altrimenti
     */
    public boolean gestisciInserimentoMoneta(double importo) {
        if (importo <= 0) {
            pubblicaErrore("L'importo deve essere positivo");
            logger.warn("Tentativo di inserire importo non valido: {}", importo);
            return false;
        }

        if (puoAccettareImporto(importo)) {
        	 // Aggiorna il credito con arrotondamento a 2 decimali
            this.creditoAttuale = Math.round((this.creditoAttuale + importo) * 100.0) / 100.0;
            logger.info("Credito aggiornato a {} per la macchina {}", creditoAttuale, idMacchina);
            pubblicaStatoCredito();
            return true;
        } else {
            logger.warn("Impossibile accettare l'importo {} - cassa piena", importo);
            pubblicaErrore("Impossibile accettare l'importo - cassa piena");
            return false;
        }
    }
    

    /**
     * Processa il pagamento per l'acquisto di una bevanda.
     *
     * @param prezzo Prezzo della bevanda da acquistare
     * @param bevandaId ID della bevanda acquistata
     * @return true se il pagamento è stato processato con successo, false altrimenti
     */
    public boolean processaPagamento(double prezzo, int bevandaId) {
        if (prezzo <= 0) {
            logger.error("Tentativo di processare un pagamento con prezzo non valido: {}", prezzo);
            return false;
        }

        if (creditoAttuale >= prezzo) {
            // Aggiorna credito e cassa 
            this.creditoAttuale = Math.round((this.creditoAttuale - prezzo) * 100.0) / 100.0;
            this.cassaAttuale = Math.round((this.cassaAttuale + prezzo) * 100.0) / 100.0;
            
            try {
                // Registra la transazione nel database
                TransazioneRepository transazioneRepo = ServiceRegistry.get(TransazioneRepository.class);
                Transazione transazione = new Transazione();
                transazione.setMacchinaId(idMacchina);
                transazione.setBevandaId(bevandaId);
                transazione.setImporto(prezzo);
                transazione.setDataOra(LocalDateTime.now());
                
                // Salva la transazione nel database
                transazione = transazioneRepo.save(transazione);
                
                // Pubblica aggiornamenti tramite MQTT
                pubblicaStatoCredito();
                pubblicaStatoCassa();
                
                // Pubblica conferma della transazione
                String topicTransazione = "macchine/" + idMacchina + "/transazioni/completata";
                Map<String, Object> messaggioTransazione = new HashMap<>();
                messaggioTransazione.put("transazioneId", transazione.getId());
                messaggioTransazione.put("bevandaId", bevandaId);
                messaggioTransazione.put("importo", prezzo);
                messaggioTransazione.put("timestamp", System.currentTimeMillis());
                
                mqttClient.publish(topicTransazione, gson.toJson(messaggioTransazione));
                
                // Verifica se la cassa è vicina al riempimento
                if ((cassaAttuale / cassaMassima) > SOGLIA_AVVISO_CASSA_PIENA) {
                    pubblicaAvvisoCassaPiena();
                }

                logger.info("Pagamento processato con successo: {} per la macchina {}, transazione {}", 
                          prezzo, idMacchina, transazione.getId());
                
                aggiornaDatabase();
                return true;
            } catch (Exception e) {
                logger.error("Errore durante la registrazione della transazione: {}", e.getMessage(), e);
                // Ripristina lo stato precedente in caso di errore
                
                this.creditoAttuale = Math.round((this.creditoAttuale + prezzo) * 100.0) / 100.0;
                this.cassaAttuale = Math.round((this.cassaAttuale - prezzo) * 100.0) / 100.0;
                return false;
            }
        }

        logger.warn("Credito insufficiente per il pagamento: {} < {}", creditoAttuale, prezzo);
        return false;
    }
    
    /**
     * Gestisce l'operazione di svuotamento della cassa.
     */
    public double gestisciSvuotamentoCassa() {
        double importoSvuotato = cassaAttuale;
        cassaAttuale = 0.0;
        logger.info("Svuotamento cassa effettuato: {} per la macchina {}", importoSvuotato, idMacchina);
        
        // Registra il ricavo
        try {
            RicavoRepository ricavoRepo = ServiceRegistry.get(RicavoRepository.class);
            Ricavo ricavo = new Ricavo(idMacchina, importoSvuotato);
            ricavo.setDataOra(LocalDateTime.now());
            ricavoRepo.save(ricavo);

            // Pubblica conferma di svuotamento
            pubblicaConfermaSvuotamento(importoSvuotato);
            pubblicaStatoCassa();
            aggiornaDatabase();
            logger.info("Ricavo registrato per svuotamento cassa: {} per macchina {}", importoSvuotato, idMacchina);
            
            return importoSvuotato;
        } catch (Exception e) {
            logger.error("Errore durante la registrazione del ricavo per svuotamento cassa: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Gestisce la restituzione del credito all'utente.
     * 
     * @return importo restituito
     */
    public double gestisciRestituzioneCredito() {
        double importoRestituito = creditoAttuale;
        creditoAttuale = 0.0;
        logger.info("Restituzione credito: {} per la macchina {}", importoRestituito, idMacchina);
        
        // Aggiorna cassa solo se ci sono crediti da restituire
        if (importoRestituito > 0) {
            pubblicaRestituzione(importoRestituito);
            pubblicaStatoCredito();
        }
        
        return importoRestituito;
    }

    /**
     * Verifica se la cassa può accettare un determinato importo. Controlla che
     * l'importo non faccia superare la capacità massima della cassa.
     *
     * @param importo l'importo da verificare
     * @return true se l'importo può essere accettato, false altrimenti
     */
    public boolean puoAccettareImporto(double importo) {
        return (cassaAttuale + creditoAttuale + importo) <= cassaMassima;
    }
    
    /**
     * Aggiorna lo stato della cassa nel database.
     */
    private void aggiornaDatabase() {
        try {
            Macchina macchina = macchinaRepository.findById(idMacchina);
            if (macchina != null) {
                macchina.setCreditoAttuale(creditoAttuale);
                macchina.setCassaAttuale(cassaAttuale);
                macchinaRepository.update(macchina);
                logger.debug("Database aggiornato per macchina {}: Credito={}, Cassa={}", 
                		idMacchina, creditoAttuale, cassaAttuale);
            } else {
                logger.error("Impossibile aggiornare database: macchina {} non trovata", idMacchina);
            }
        } catch (Exception e) {
            logger.error("Errore nell'aggiornamento del database per la macchina {}: {}", 
            		idMacchina, e.getMessage(), e);
        }
    }
    
    /**
     * Pubblica lo stato attuale della cassa sul topic MQTT appropriato.
     */
    private void pubblicaStatoCassa() {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/stato/risposta";
            mqttClient.publish(topic, gson.toJson(ottieniStato()));
            logger.debug("Stato cassa pubblicato per macchina {}", idMacchina);
        } catch (Exception e) {
            logger.error("Errore pubblicazione stato cassa: {}", e.getMessage(), e);
        }
    }

    /**
     * Pubblica lo stato del credito attuale sul topic MQTT appropriato.
     */
    private void pubblicaStatoCredito() {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/credito/risposta";
            Map<String, Object> stato = new HashMap<>();
            stato.put("creditoAttuale", creditoAttuale);
            stato.put("timestamp", System.currentTimeMillis());
            
            mqttClient.publish(topic, gson.toJson(stato));
            logger.debug("Stato credito pubblicato: {} per macchina {}", creditoAttuale, idMacchina);
        } catch (Exception e) {
            logger.error("Errore pubblicazione stato credito: {}", e.getMessage(), e);
        }
    }

    /**
     * Pubblica un avviso quando la cassa supera la soglia di riempimento.
     */
    private void pubblicaAvvisoCassaPiena() {
        try {
            String topicAvviso = "macchine/" + idMacchina + "/cassa/avviso/cassaPiena";
            Map<String, Object> avviso = new HashMap<>();
            avviso.put("guasto", false);
            avviso.put("cassa", new HashMap<String, Object>() {{
                put("importo", cassaAttuale);
                put("massimo", cassaMassima);
                put("percentuale", (cassaAttuale / cassaMassima) * 100);
                put("piena", (cassaAttuale / cassaMassima) > SOGLIA_AVVISO_CASSA_PIENA);
            }});
            avviso.put("timestamp", System.currentTimeMillis());
            
            mqttClient.publish(topicAvviso, gson.toJson(avviso));
            logger.warn("Avviso cassa quasi piena pubblicato per la macchina {}", idMacchina);
        } catch (Exception e) {
            logger.error("Errore pubblicazione avviso cassa piena: {}", e.getMessage(), e);
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
            Map<String, Object> conferma = new HashMap<>();
            conferma.put("importo", importo);
            conferma.put("timestamp", System.currentTimeMillis());
            
            mqttClient.publish(topic, gson.toJson(conferma));
            logger.info("Conferma svuotamento cassa pubblicata: {} per macchina {}", importo, idMacchina);
        } catch (Exception e) {
            logger.error("Errore pubblicazione conferma svuotamento: {}", e.getMessage(), e);
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
            Map<String, Object> resto = new HashMap<>();
            resto.put("importo", importo);
            resto.put("timestamp", System.currentTimeMillis());
            
            mqttClient.publish(topic, gson.toJson(resto));
            logger.info("Conferma restituzione credito pubblicata: {} per macchina {}", importo, idMacchina);
        } catch (Exception e) {
            logger.error("Errore pubblicazione restituzione: {}", e.getMessage(), e);
        }
    }

    /**
     * Pubblica un messaggio di errore sul topic MQTT appropriato.
     *
     * @param messaggio il messaggio di errore da pubblicare
     */
    private void pubblicaErrore(String messaggio) {
        try {
            String topic = "macchine/" + idMacchina + "/cassa/errore/risposta";
            Map<String, Object> errore = new HashMap<>();
            errore.put("messaggio", messaggio);
            errore.put("timestamp", System.currentTimeMillis());
            
            mqttClient.publish(topic, gson.toJson(errore));
            logger.error("Errore cassa pubblicato: {}", messaggio);
        } catch (Exception e) {
            logger.error("Errore pubblicazione errore: {}", e.getMessage(), e);
        }
    }
    
    /**
    * Ottiene l'importo attuale in cassa.
    * 
    * @return importo attuale in cassa
    */
   public double ottieniStatoCassa() {
       return cassaAttuale;
   }

   /**
    * Ottiene il credito attualmente inserito.
    * 
    * @return credito attuale
    */
   public double ottieniCreditoAttuale() {
       return creditoAttuale;
   }

   /**
    * Verifica se c'è credito sufficiente per una bevanda.
    * 
    * @param importo importo da verificare
    * @return true se il credito è sufficiente
    */
   public boolean verificaCredisoSufficiente(double importo) {
	   double ca = creditoAttuale;
       return ca >= importo;
   }

   /**
    * Sottrae l'importo dal credito attuale.
    * 
    * @param importo importo da sottrarre
    * @return true se l'operazione è avvenuta con successo
    */
   public boolean sottraiCredito(double importo) {
	   double ca = creditoAttuale;
       if (importo <= 0 || importo > ca) {
           return false;
       }
       
       this.creditoAttuale = Math.round((this.creditoAttuale - importo) * 100.0) / 100.0;
       try {
           // Pubblica l'aggiornamento del credito
           String topic = "macchine/" + idMacchina + "/cassa/stato/risposta";
           mqttClient.publish(topic, gson.toJson(ottieniStato()));
       } catch (Exception e) {
           logger.error("Errore nella pubblicazione dell'aggiornamento credito: {}", e.getMessage());
       }
       return true;
   }

   /**
    * Aggiunge l'importo in cassa.
    * 
    * @param importo importo da aggiungere
    * @return true se l'operazione è avvenuta con successo
    */
   public boolean aggiungiInCassa(double importo) {
	   double ca = cassaAttuale;
       if (importo <= 0 || ( ca + importo) > cassaMassima) {
           return false;
       }
       this.creditoAttuale = Math.round((this.creditoAttuale - importo) * 100.0) / 100.0;
       try {
           // Pubblica l'aggiornamento dello stato cassa
           String topic = "macchine/" + idMacchina + "/cassa/stato/risposta";
           mqttClient.publish(topic, gson.toJson(ottieniStato()));
       } catch (Exception e) {
           logger.error("Errore nella pubblicazione dell'aggiornamento cassa: {}", e.getMessage());
       }
       return true;
   }

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
