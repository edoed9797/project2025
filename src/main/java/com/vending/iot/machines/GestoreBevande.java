package com.vending.iot.machines;

import com.google.gson.Gson;
import com.vending.iot.mqtt.MQTTClient;
import com.vending.Main;
import com.vending.ServiceRegistry;
import com.vending.core.models.Bevanda;
import com.vending.core.models.Cialda;
import com.vending.core.models.QuantitaCialde;
import com.vending.core.models.Transazione;
import com.vending.core.repositories.CialdaRepository;
import com.vending.core.repositories.TransazioneRepository;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GestoreBevande {

    private final int idMacchina;
    private final Map<Integer, Bevanda> bevande;
    private final AtomicBoolean inErogazione;
    private final MQTTClient mqttClient;
    private final Gson gson;
    private final GestoreCassa gestoreCassa;
    private final GestoreCialde gestoreCialde;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public GestoreBevande(int idMacchina, GestoreCassa gestoreCassa, GestoreCialde gestoreCialde) throws MqttException {
        this.idMacchina = idMacchina;
        this.bevande = new ConcurrentHashMap<>();
        this.inErogazione = new AtomicBoolean(false);
        this.gson = new Gson();
        this.mqttClient = new MQTTClient("bevande_" + idMacchina);
        this.gestoreCassa = gestoreCassa;
        this.gestoreCialde = gestoreCialde;

        inizializzaSottoscrizioni();
    }

    private void inizializzaSottoscrizioni() throws MqttException {
        String baseTopic = "macchine/" + idMacchina + "/bevande/";
        mqttClient.subscribe(baseTopic + "#", (topic, messaggio) -> {
            String azione = topic.substring(baseTopic.length());
            switch (azione) {
                case "richiesta":
                    gestisciRichiestaBevanda(gson.fromJson(messaggio, RichiestaBevanda.class));
                    break;
                case "aggiorna":
                    gestisciAggiornamentoBevanda(gson.fromJson(messaggio, AggiornamentoBevanda.class));
                    break;
            }
        });
    }

    public void aggiungiBevanda(Bevanda bevanda) {
        bevande.put(bevanda.getId(), bevanda);
        publishAggiornamentoBevande();
    }

    private void publishAggiornamentoBevande() {
        try {
            String topic = "macchine/" + idMacchina + "/bevande/lista";

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

            // Log dell'aggiornamento
            System.out.println("Aggiornamento bevande pubblicato con successo: "
                    + gson.toJson(dettagliAggiornamento));

        } catch (MqttException e) {
            System.err.println("Errore durante la pubblicazione dell'aggiornamento bevande: "
                    + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore imprevisto durante l'aggiornamento bevande: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

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
            Optional<QuantitaCialde> cialda = cialdaRepository.getQuantitaDisponibileByMacchina(idCialda, idMacchina);
            return cialda != null ? cialda.get().getQuantita() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void pubblicaAvvisoNessunaBevandaDisponibile() {
        try {
            String topicAvviso = "macchine/" + idMacchina + "/bevande/avviso";
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
            if (gestoreCassa.processaPagamento(bevanda.getPrezzo())) {
                // Simulazione erogazione
                pubblicaStato("preparazione");
                Thread.sleep(5000); // Simula tempo di preparazione

                // Consuma le cialde
                gestoreCialde.consumaCialde(bevanda.getCialde());

                pubblicaStato("completata");
                registraErogazione(bevanda);
                // Aggiorna il repository delle transazioni
                TransazioneRepository transazioneRepo = ServiceRegistry.get(TransazioneRepository.class);
                Optional<Transazione> transazione = transazioneRepo.findById(richiesta.transazioneId);
                if (transazione.isPresent()) {
                    Transazione t = transazione.get();
                    transazioneRepo.update(t);
                }
            } else {
                pubblicaErrore("Credito insufficiente");
            }
        } catch (Exception e) {
            pubblicaErrore("Errore durante l'erogazione: " + e.getMessage());
        } finally {
            inErogazione.set(false);
        }
    }

    private void gestisciAggiornamentoBevanda(AggiornamentoBevanda aggiornamento) {
        Bevanda bevanda = bevande.get(aggiornamento.idBevanda);
        if (bevanda != null) {
            bevanda.setPrezzo(aggiornamento.nuovoPrezzo);
            pubblicaAggiornamentoBevande();
        }
    }

    private void pubblicaStato(String stato) {
        try {
            String topic = "macchine/" + idMacchina + "/bevande/stato";
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
            String topic = "macchine/" + idMacchina + "/bevande/lista";
            mqttClient.publish(topic, gson.toJson(bevande));
        } catch (MqttException e) {
            System.err.println("Errore pubblicazione bevande: " + e.getMessage());
        }
    }

    private void pubblicaErrore(String messaggio) {
        try {
            String topic = "macchine/" + idMacchina + "/bevande/errore";
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
            String topic = "macchine/" + idMacchina + "/bevande/erogazione";
            int idT = transazioneRepo.getLastTransactionId() + 1;
            transazione.setId(idT);
            transazione.setMacchinaId(idMacchina);
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

    public Map<String, Object> ottieniStato() {
        List<Map<String, Object>> listaBevande = new ArrayList<>();

        for (Bevanda bevanda : bevande.values()) {
            Map<String, Object> infoBevanda = new HashMap<>();
            infoBevanda.put("id", bevanda.getId());
            infoBevanda.put("nome", bevanda.getNome());
            infoBevanda.put("prezzo", bevanda.getPrezzo());
            infoBevanda.put("disponibile", verificaDisponibilitaBevanda(bevanda));
            listaBevande.add(infoBevanda);
        }

        Map<String, Object> stato = new HashMap<>();
        stato.put("bevande", listaBevande);
        stato.put("inErogazione", inErogazione.get());

        return stato;
    }

    private static class RichiestaBevanda {
        public int idBevanda;
        public int transazioneId;
        public double importo;
    }

    private static class AggiornamentoBevanda {

        public int idBevanda;
        public double nuovoPrezzo;
    }

    public void spegni() {
        mqttClient.disconnect();
    }
}
