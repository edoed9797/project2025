package com.vending.iot.machines;

import com.google.gson.Gson;
import com.vending.iot.mqtt.MQTTClient;
import com.vending.Main;
import com.vending.ServiceRegistry;
import com.vending.core.models.Bevanda;
import com.vending.core.models.Cialda;
import com.vending.core.models.Macchina;
import com.vending.core.models.QuantitaCialde;
import com.vending.core.models.Transazione;
import com.vending.core.repositories.CialdaRepository;
import com.vending.core.repositories.MacchinaRepository;
import com.vending.core.repositories.TransazioneRepository;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class GestoreBevande {

    private final int macchinaId;
    private final Map<Integer, Bevanda> bevande;
    private final AtomicBoolean inErogazione;
    private final MQTTClient mqttClient;
    private final Gson gson;
    private final GestoreCassa gestoreCassa;
    private final GestoreCialde gestoreCialde;
    private final MacchinaRepository macchinaRepository;
    private final TransazioneRepository transazioneRepository;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    /**
     * Costruttore del gestore bevande con inizializzazione dati dal database.
     * 
     * @param macchinaId ID della macchina gestita
     */
    public GestoreBevande(int macchinaId, GestoreCassa gestoreCassa, GestoreCialde gestoreCialde) throws MqttException {
    	this.macchinaRepository = ServiceRegistry.get(MacchinaRepository.class);
    	this.transazioneRepository = ServiceRegistry.get(TransazioneRepository.class);
        this.macchinaId = macchinaId;
        this.bevande = new HashMap<>();
        this.inErogazione = new AtomicBoolean(false);
        this.gson = new Gson();
        this.mqttClient = new MQTTClient("bevande_" + macchinaId);
        this.gestoreCassa = gestoreCassa;
        this.gestoreCialde = gestoreCialde;
        
        // Inizializza la lista delle bevande dal database
        Macchina macchina = macchinaRepository.findById(macchinaId);
        if (macchina != null && macchina.getBevande() != null) {
            for (Bevanda bevanda : macchina.getBevande()) {
                this.bevande.put(bevanda.getId(), bevanda);
            }
            logger.info("Bevande caricate per macchina {}: {} tipi", macchinaId, bevande.size());
        } else {
            logger.warn("Nessuna bevanda trovata per la macchina {}", macchinaId);
        }
    
        inizializzaSottoscrizioni();
    }

    private void inizializzaSottoscrizioni() throws MqttException {
<<<<<<< HEAD
        String baseTopic = "macchine/" + macchinaId + "/bevande/";
=======
        String baseTopic = "macchine/" + idMacchina + "/bevande/";
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
        
        // Sottoscrizione per richieste di bevande
        mqttClient.subscribe(baseTopic + "richiesta", (topic, messaggio) -> {
            gestisciRichiestaBevanda(gson.fromJson(messaggio, RichiestaBevanda.class));
        });
        
        // Sottoscrizione per aggiornamento delle bevande
<<<<<<< HEAD
        /*mqttClient.subscribe(baseTopic + "aggiorna", (topic, messaggio) -> {
            gestisciAggiornamentoBevanda(gson.fromJson(messaggio, bevande));
        });*/
=======
        mqttClient.subscribe(baseTopic + "aggiorna", (topic, messaggio) -> {
            gestisciAggiornamentoBevanda(gson.fromJson(messaggio, AggiornamentoBevanda.class));
        });
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
        
        // Sottoscrizione per richieste stato delle bevande
        mqttClient.subscribe(baseTopic + "stato/richiesta", (topic, messaggio) -> {
            publishAggiornamentoBevande();
        });
    }
    
    public void aggiungiBevanda(Bevanda bevanda) {
        bevande.put(bevanda.getId(), bevanda);
        publishAggiornamentoBevande();
    }

    private void publishAggiornamentoBevande() {
        try {
<<<<<<< HEAD
            String topic = "macchine/" + macchinaId + "/bevande/lista/risposta";
=======
            String topic = "macchine/" + idMacchina + "/bevande/lista/risposta";
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26

            // Crea una mappa dettagliata con tutte le informazioni delle bevande
            Map<String, Object> dettagliAggiornamento = new HashMap<>();
            List<Map<String, Object>> listaBevande = new ArrayList<>();

            for (Bevanda bevanda : bevande.values()) {
                Map<String, Object> infoBevanda = new HashMap<>();
                try {
                    infoBevanda.put("id", bevanda.getId());
                    infoBevanda.put("nome", bevanda.getNome());
                    infoBevanda.put("prezzo", bevanda.getPrezzo());
                    infoBevanda.put("disponibile", verificaDisponibilitaBevanda(bevanda));

                    // Aggiunge informazioni sulle cialde necessarie
                    List<Map<String, Object>> cialde = new ArrayList<>();
                    for (Cialda cialda : bevanda.getCialde()) {
                        Map<String, Object> infoCialda = new HashMap<>();
                        infoCialda.put("id", cialda.getId());
                        infoCialda.put("tipo", cialda.getTipoCialda());
                        infoCialda.put("quantitaDisponibile", getQuantitaCialdaDisponibile(cialda.getId()));
                        cialde.add(infoCialda);
                    }
                    infoBevanda.put("cialde", cialde);

                    listaBevande.add(infoBevanda);
                } catch (NullPointerException e) {
                    System.err.println("Errore nell'elaborazione della bevanda ID "
                            + bevanda.getId() + ": " + e.getMessage());
                    // Continua con la prossima bevanda
                    continue;
                }
            }

            dettagliAggiornamento.put("bevande", listaBevande);
            dettagliAggiornamento.put("timestamp", System.currentTimeMillis());
            dettagliAggiornamento.put("totaleDisponibili",
                    listaBevande.stream().filter(b -> (boolean) b.get("disponibile")).count());

            // Verifica se ci sono bevande disponibili
            boolean almeno1BevandaDisponibile = listaBevande.stream()
                    .anyMatch(b -> (boolean) b.get("disponibile"));

            if (!almeno1BevandaDisponibile) {
                // Pubblica un avviso se non ci sono bevande disponibili
                pubblicaAvvisoNessunaBevandaDisponibile();
            }

            // Pubblica l'aggiornamento
            mqttClient.publish(topic, gson.toJson(dettagliAggiornamento));


        } catch (MqttException e) {
            System.err.println("Errore durante la pubblicazione dell'aggiornamento bevande: "
                    + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore imprevisto durante l'aggiornamento bevande: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

<<<<<<< HEAD
    /**
     * Trova una bevanda per ID.
     * 
     * @param bevandaId ID della bevanda
     * @return Optional contenente la bevanda, o empty se non trovata
     */
    public Optional<Bevanda> trovaBevanda(int bevandaId) {
        return Optional.ofNullable(bevande.get(bevandaId));
    }
    
=======

>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
    private boolean verificaDisponibilitaBevanda(Bevanda bevanda) {
        try {
            if (bevanda == null) {
                return false;
            }

            return bevanda.getCialde().stream()
                    .allMatch(cialda -> getQuantitaCialdaDisponibile(cialda.getId()) > 0);
        } catch (Exception e) {
            System.err.println("Errore verifica disponibilita'� bevanda ID "
                    + bevanda.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private int getQuantitaCialdaDisponibile(int idCialda) {
        try {
            CialdaRepository cialdaRepository = new CialdaRepository();
            Optional<QuantitaCialde> cialda = cialdaRepository.getQuantitaDisponibileByMacchina(idCialda, macchinaId);
            return cialda != null ? cialda.get().getQuantita() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void pubblicaAvvisoNessunaBevandaDisponibile() {
        try {
<<<<<<< HEAD
            String topicAvviso = "macchine/" + macchinaId + "/bevande/avviso/risposta";
=======
            String topicAvviso = "macchine/" + idMacchina + "/bevande/avviso/risposta";
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
            Map<String, Object> avviso = Map.of(
                    "tipo", "NESSUNA_BEVANDA_DISPONIBILE",
                    "messaggio", "Tutte le bevande sono momentaneamente non disponibili",
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topicAvviso, gson.toJson(avviso));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione avviso bevande non disponibili: "
                    + e.getMessage());
        }
    }
    
    
    private void gestisciRichiestaBevanda(RichiestaBevanda richiesta) {
        if (!inErogazione.compareAndSet(false, true)) {
            pubblicaErrore("Macchina occupata");
            return;
        }

        try {
            Bevanda bevanda = bevande.get(richiesta.idBevanda);
            if (bevanda == null) {
                pubblicaErrore("Bevanda non disponibile");
                return;
            }

            // Verifica disponibilita' cialde
            if (!gestoreCialde.verificaDisponibilitaCialde(bevanda.getCialde())) {
                pubblicaErrore("Cialde non sufficienti");
                return;
            }

            // Verifica pagamento
            if (gestoreCassa.processaPagamento(bevanda.getPrezzo(), bevanda.getId())) {
                // Simulazione erogazione
                pubblicaStato("preparazione");
                Thread.sleep(5000); // Simula tempo di preparazione

                // Consuma le cialde
                gestoreCialde.consumaCialde(bevanda.getCialde());

                pubblicaStato("completata");
                
            } else {
                pubblicaErrore("Credito insufficiente");
            }
        } catch (Exception e) {
            pubblicaErrore("Errore durante l'erogazione: " + e.getMessage());
        } finally {
            inErogazione.set(false);
        }
    }

    private boolean  gestisciAggiornamentoBevanda(List<Bevanda> nuoveBevande) {
        try {
            // Svuota la mappa esistente
            bevande.clear();
            
            // Popola con le nuove bevande
            for (Bevanda bevanda : nuoveBevande) {
                bevande.put(bevanda.getId(), bevanda);
            }
            
            // Aggiorna anche nel database
            Macchina macchina = macchinaRepository.findById(macchinaId);
            if (macchina != null) {
                // Estrai gli ID delle bevande
                List<Integer> bevandeIds = nuoveBevande.stream()
                                             .map(Bevanda::getId)
                                             .collect(Collectors.toList());
                
                // Usa il metodo corretto del repository per aggiornare le bevande nel database
                macchinaRepository.aggiornaBevande(macchina, bevandeIds);
                    
                logger.info("Bevande aggiornate per macchina {}: {} tipi", macchinaId, bevande.size());
                return true;
            } else {
                logger.error("Impossibile aggiornare bevande: macchina {} non trovata", macchinaId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Errore SQL nell'aggiornamento delle bevande per macchina {}: {}", 
                        macchinaId, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Errore nell'aggiornamento delle bevande per macchina {}: {}", 
                        macchinaId, e.getMessage(), e);
            return false;
        }
    }

    private void pubblicaStato(String stato) {
        try {
<<<<<<< HEAD
            String topic = "macchine/" + macchinaId + "/bevande/stato/risposta";
=======
            String topic = "macchine/" + idMacchina + "/bevande/stato/risposta";
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
            Map<String, Object> statoErogazione = Map.of(
                    "stato", stato,
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(statoErogazione));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione stato: " + e.getMessage());
        }
    }

    private void pubblicaAggiornamentoBevande() {
        try {
<<<<<<< HEAD
            String topic = "macchine/" + macchinaId + "/bevande/lista/risposta";
=======
            String topic = "macchine/" + idMacchina + "/bevande/lista/risposta";
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
            mqttClient.publish(topic, gson.toJson(bevande));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione bevande: " + e.getMessage());
        }
    }

    private void pubblicaErrore(String messaggio) {
        try {
<<<<<<< HEAD
            String topic = "macchine/" + macchinaId + "/bevande/errore/risposta";
=======
            String topic = "macchine/" + idMacchina + "/bevande/errore/risposta";
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
            Map<String, Object> errore = Map.of(
                    "messaggio", messaggio,
                    "timestamp", System.currentTimeMillis()
            );
            mqttClient.publish(topic, gson.toJson(errore));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione errore: " + e.getMessage());
        }
    }

    private void registraErogazione(Bevanda bevanda) {
        TransazioneRepository transazioneRepo = ServiceRegistry.get(TransazioneRepository.class);
        Transazione transazione = new Transazione();

        try {
<<<<<<< HEAD
            String topic = "macchine/" + macchinaId + "/bevande/erogazione/completata";
=======
            String topic = "macchine/" + idMacchina + "/bevande/erogazione/completata";
>>>>>>> 56a4bdcb35afaca3d0080370419ca274a4528a26
            int idT = transazioneRepo.getLastTransactionId() + 1;
            transazione.setId(idT);
            transazione.setMacchinaId(macchinaId);
            transazione.setBevandaId(bevanda.getId());
            transazione.setImporto(bevanda.getPrezzo());
            transazione.setDataOra(LocalDateTime.now());
            Map<String, Object> erogazione = Map.of(
                    "bevandaId", bevanda.getId(),
                    "nome", bevanda.getNome(),
                    "prezzo", bevanda.getPrezzo(),
                    "timestamp", System.currentTimeMillis(),
                    "transazioneId", transazione.getId()
            );
            mqttClient.publish(topic, gson.toJson(erogazione));
        } catch (MqttException e) {
            System.err.println("Errore registrazione erogazione: " + e.getMessage());
        }
    }

    public List<Bevanda> getBevandeMacchina(int macchinaId) {
        List<Bevanda> bevandeDisponibili = new ArrayList<>();

        // Itera su tutte le bevande gestite da questo gestore
        for (Bevanda bevanda : bevande.values()) {
            // Verifica se la bevanda è disponibile per questa macchina
            if (verificaDisponibilitaBevanda(bevanda)) {
                bevandeDisponibili.add(bevanda);
            }
        }

        return bevandeDisponibili;
    }

    /**
     * Restituisce lo stato attuale delle bevande.
     * 
     * @return Mappa con lo stato delle bevande
     */
    public Map<String, Object> ottieniStato() {
    	Macchina macchina = macchinaRepository.findById(macchinaId);
        Map<String, Object> stato = new HashMap<>();
        List<Map<String, Object>> bevandeStato = new ArrayList<>();
        int bevandeDisponibili = 0;
        
        for (Bevanda bevanda : bevande.values()) {
            Map<String, Object> bevandaStato = new HashMap<>();
            bevandaStato.put("id", bevanda.getId());
            bevandaStato.put("nome", bevanda.getNome());
            bevandaStato.put("prezzo", bevanda.getPrezzo());
            
            // Controlla la disponibilità di tutte le cialde necessarie
            boolean disponibile = true;
            List<Map<String, Object>> cialdeInfo = new ArrayList<>();
            
            for (Cialda cialda : bevanda.getCialde()) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", cialda.getId());
                info.put("nome", cialda.getNome());
                info.put("tipo", cialda.getTipoCialda());
                
                // Ottieni la quantità disponibile per questa cialda
                int quantitaDisponibile = 0;
                for (QuantitaCialde qc : macchina.getCialde()) {
                    if (qc.getCialdaId() == cialda.getId()) {
                        quantitaDisponibile = qc.getQuantita();
                        info.put("quantitaDisponibile", quantitaDisponibile);
                        break;
                    }
                }
                
                // Se anche solo una cialda non è disponibile, la bevanda non è disponibile
                if (quantitaDisponibile <= 0) {
                    disponibile = false;
                }
                
                cialdeInfo.add(info);
            }
            
            bevandaStato.put("cialde", cialdeInfo);
            bevandaStato.put("disponibile", disponibile);
            
            if (disponibile) {
                bevandeDisponibili++;
            }
            
            bevandeStato.add(bevandaStato);
        }
        
        stato.put("bevande", bevandeStato);
        stato.put("numeroBevande", bevande.size());
        stato.put("totaleDisponibili", bevandeDisponibili);
        stato.put("timestamp", System.currentTimeMillis());
        
        return stato;
    }
    /**
     * Ottiene le statistiche di vendita per le bevande.
     * 
     * @return Mappa con le statistiche di vendita
     */
    public Map<String, Object> ottieniStatistiche() {
        Map<String, Object> statistiche = new HashMap<>();
        List<Map<String, Object>> venditeBevande = new ArrayList<>();
        
        try {
            // Ottieni tutte le transazioni per questa macchina
            List<Transazione> transazioni = transazioneRepository.findByMacchinaId(macchinaId);
            
            // Calcola il numero di vendite per ogni bevanda
            Map<Integer, Long> conteggioVendite = transazioni.stream()
                .collect(Collectors.groupingBy(Transazione::getBevandaId, Collectors.counting()));
                
            // Calcola il ricavo totale per ogni bevanda
            Map<Integer, Double> ricavoPerBevanda = transazioni.stream()
                .collect(Collectors.groupingBy(Transazione::getBevandaId, 
                         Collectors.summingDouble(t -> t.getImporto() != null ? t.getImporto() : 0.0)));
                
            // Prepara le statistiche per ogni bevanda
            for (Bevanda bevanda : bevande.values()) {
                int bevandaId = bevanda.getId();
                Map<String, Object> statBevanda = new HashMap<>();
                statBevanda.put("id", bevandaId);
                statBevanda.put("nome", bevanda.getNome());
                statBevanda.put("quantitaVendute", conteggioVendite.getOrDefault(bevandaId, 0L));
                statBevanda.put("ricavoTotale", ricavoPerBevanda.getOrDefault(bevandaId, 0.0));
                
                venditeBevande.add(statBevanda);
            }
            
            statistiche.put("venditeBevande", venditeBevande);
            statistiche.put("totaleTransazioni", transazioni.size());
            statistiche.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("Errore nel recupero delle statistiche per macchina {}: {}", 
                        macchinaId, e.getMessage(), e);
            statistiche.put("errore", "Impossibile recuperare statistiche: " + e.getMessage());
        }
        
        return statistiche;
    }

    private static class RichiestaBevanda {
        public int idBevanda;
        public int transazioneId;
        public double importo;
    }

    public void spegni() {
        mqttClient.disconnect();
    }
}
