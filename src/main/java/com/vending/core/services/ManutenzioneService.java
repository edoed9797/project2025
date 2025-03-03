package com.vending.core.services;

import com.google.gson.JsonElement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.vending.core.models.*;
import com.vending.core.repositories.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servizio che gestisce la logica di business relativa alle manutenzioni delle macchine distributrici.
 * Gestisce il ciclo di vita delle manutenzioni, dalla segnalazione al completamento,
 * integrando i dati delle macchine e dei tecnici assegnati.
 */
public class ManutenzioneService {
    private final ManutenzioneRepository manutenzioneRepository;
    private final MacchinaRepository macchinaRepository;
    private final TransazioneRepository transazioneRepository;
    private final BevandaRepository bevandaRepository;
    

    /**
     * Costruisce un nuovo servizio manutenzioni.
     *
     * @param manutenzioneRepository repository per l'accesso ai dati delle manutenzioni
     * @param macchinaRepository repository per l'accesso ai dati delle macchine
     */
    public ManutenzioneService(ManutenzioneRepository manutenzioneRepository, MacchinaRepository macchinaRepository, TransazioneRepository transazioneRepository, BevandaRepository bevandaRepository) {
        this.manutenzioneRepository = manutenzioneRepository;
        this.macchinaRepository = macchinaRepository;
        this.transazioneRepository = transazioneRepository;
        this.bevandaRepository = bevandaRepository;
    }

    /**
     * Recupera tutte le manutenzioni attive nel sistema.
     * Una manutenzione è considerata attiva se non ha una data di completamento.
     *
     * @return lista delle manutenzioni attive
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public List<Manutenzione> getManutenzioniAttive() {
        try {
            return manutenzioneRepository.findAll().stream()
                    .filter(m -> !m.isCompletata())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Errore nel recupero delle manutenzioni attive", e);
        }
    }

    /**
     * Recupera tutte le manutenzioni di una specifica macchina.
     *
     * @param macchinaId ID della macchina
     * @return lista delle manutenzioni della macchina
     * @throws IllegalArgumentException se la macchina non esiste
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public List<Manutenzione> getManutenzioniMacchina(int macchinaId) {
        Macchina macchina = validaMacchina(macchinaId);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina con id: " + macchinaId + " non trovata");
        }
        return manutenzioneRepository.findByMacchinaId(macchinaId);
    }

    /**
     * Recupera lo stato dettagliato di manutenzione di una macchina.
     *
     * @param macchinaId ID della macchina
     * @return mappa contenente lo stato e le statistiche di manutenzione
     * @throws IllegalArgumentException se la macchina non esiste
     * @throws RuntimeException se si verifica un errore durante il recupero
     */
    public Map<String, Object> getStatoManutenzione(int macchinaId) {
        try {
            Macchina macchina = validaMacchina(macchinaId);
            List<Manutenzione> manutenzioni = manutenzioneRepository.findByMacchinaId(macchinaId);
            
            List<Manutenzione> attive = manutenzioni.stream()
                    .filter(m -> !m.isCompletata())
                    .collect(Collectors.toList());
            
            List<Manutenzione> completate = manutenzioni.stream()
                    .filter(Manutenzione::isCompletata)
                    .collect(Collectors.toList());

            Map<String, Object> stato = new HashMap<>();
            stato.put("statoId", macchina.getStatoId());
            stato.put("statoDescrizione", macchina.getStatoDescrizione());
            stato.put("manutenzioniAttive", attive);
            stato.put("numeroManutenzioniAttive", attive.size());
            
            completate.stream()
                    .max((a, b) -> a.getDataCompletamento().compareTo(b.getDataCompletamento()))
                    .ifPresent(m -> stato.put("ultimaManutenzione", m));

            return stato;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Errore nel recupero dello stato manutenzione", e);
        }
    }

    /**
     * Avvia una nuova manutenzione per una macchina.
     *
     * @param macchinaId ID della macchina
     * @param tecnicoId ID del tecnico assegnato
     * @param tipoIntervento Tipo di intervento
     * @param descrizione Descrizione della manutenzione
     * @param urgenza Urgenza della manutenzione (BASSA, MEDIA, ALTA)
     * @return la manutenzione creata
     * @throws IllegalArgumentException se i parametri non sono validi
     * @throws RuntimeException se si verifica un errore durante la creazione
     */
    public Manutenzione avviaManutenzione(int macchinaId, String tipoIntervento, int tecnicoId, String descrizione, String urgenza) {
        try {
            Macchina macchina = validaMacchina(macchinaId);
            
            Manutenzione manutenzione = new Manutenzione(macchinaId, tipoIntervento, descrizione, urgenza);
            manutenzione.setTecnicoId(tecnicoId);
            manutenzione.setDataRichiesta(LocalDateTime.now());
            
            macchina.setStatoId(2); // In manutenzione
            macchinaRepository.update(macchina);
            
            return manutenzioneRepository.save(manutenzione);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Errore nell'avvio della manutenzione", e);
        }
    }

    /**
     * Completa una manutenzione esistente.
     *
     * @param manutenzioneId ID della manutenzione
     * @param note note di completamento
     * @return la manutenzione completata
     * @throws IllegalArgumentException se la manutenzione non esiste
     * @throws IllegalStateException se la manutenzione è già completata
     * @throws RuntimeException se si verifica un errore durante il completamento
     */
    public Manutenzione completaManutenzione(int manutenzioneId, String note) {
        try {
            Optional<Manutenzione> optManutenzione = manutenzioneRepository.findById(manutenzioneId);
            if (!optManutenzione.isPresent()) {
                throw new IllegalArgumentException("Manutenzione non trovata: " + manutenzioneId);
            }

            Manutenzione manutenzione = optManutenzione.get();
            if (manutenzione.isCompletata()) {
                throw new IllegalStateException("Manutenzione già completata");
            }

            Manutenzione manutenzioneCompletata = manutenzioneRepository.completaManutenzione(manutenzione);

            // Verifica altre manutenzioni attive
            List<Manutenzione> altreAttive = getManutenzioniMacchina(manutenzione.getMacchinaId())
                    .stream()
                    .filter(m -> !m.isCompletata())
                    .collect(Collectors.toList());

            if (altreAttive.isEmpty()) {
                Macchina macchina = validaMacchina(manutenzione.getMacchinaId());
                macchina.setStatoId(1); // Attiva
                macchinaRepository.update(macchina);
            }

            return manutenzioneCompletata;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Errore nel completamento della manutenzione", e);
        }
    }

    /**
     * Imposta una macchina come fuori servizio.
     *
     * @param macchinaId ID della macchina
     * @return true se l'operazione ha successo
     * @throws IllegalArgumentException se la macchina non esiste
     * @throws RuntimeException se si verifica un errore durante l'operazione
     */
    public boolean setFuoriServizio(int macchinaId) {
        Macchina macchina = validaMacchina(macchinaId);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina con id: " + macchinaId + " non trovata");
        }
        return manutenzioneRepository.setFuoriServizio(macchinaId);
    }

    /**
     * Valida l'esistenza di una macchina.
     *
     * @param macchinaId ID della macchina da validare
     * @return la macchina se esiste
     * @throws IllegalArgumentException se la macchina non esiste
     */
    private Macchina validaMacchina(int macchinaId) {
        Macchina macchina = macchinaRepository.findById(macchinaId);
        if (macchina == null) {
            throw new IllegalArgumentException("Macchina non trovata: " + macchinaId);
        }
        return macchina;
    }

    /**
     * Processa i messaggi relativi allo stato di una macchina e avvia le manutenzioni necessarie.
     * Gestisce gli stati: guasto, rifornimento cialde necessario, svuotamento cassa necessario.
     *
     * @param topic topic MQTT del messaggio
     * @param message contenuto del messaggio in formato JSON
     * @throws IllegalArgumentException se il messaggio non è valido
     * @throws RuntimeException se si verifica un errore durante l'elaborazione
     */
    public void processaMacchinaStato(String topic, String message) {
        try {
            // Estrai l'ID della macchina dal topic (formato: macchine/{id}/stato)
            int macchinaId = estraiMacchinaIdDaTopic(topic);
            JsonObject statoMacchina = JsonParser.parseString(message).getAsJsonObject();
            
            // Valida la macchina
            Macchina macchina = validaMacchina(macchinaId);
            
            // Processa i diversi tipi di stato
            if (statoMacchina.has("guasto") && statoMacchina.get("guasto").getAsBoolean()) {
                // Avvia manutenzione per guasto
                gestisciGuasto(macchina, statoMacchina);
            }
            
            if (statoMacchina.has("cialde")) {
                // Verifica necessità rifornimento cialde
                gestisciStatoCialde(macchina, statoMacchina.getAsJsonObject("cialde"));
            }
            
            if (statoMacchina.has("cassa")) {
                // Verifica necessità svuotamento cassa
                gestisciStatoCassa(macchina, statoMacchina.getAsJsonObject("cassa"));
            }
            
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Formato messaggio non valido: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Errore nell'elaborazione dello stato macchina: " + e.getMessage());
        }
    }

    private void gestisciGuasto(Macchina macchina, JsonObject statoMacchina) {
        // Se non ci sono già manutenzioni attive per guasto
        List<Manutenzione> manutenzioniAttive = getManutenzioniMacchina(macchina.getId())
                .stream()
                .filter(m -> !m.isCompletata())
                .collect(Collectors.toList());
                
        if (manutenzioniAttive.isEmpty()) {
            String descrizioneGuasto = statoMacchina.has("descrizioneGuasto") ? 
                    statoMacchina.get("descrizioneGuasto").getAsString() : 
                    "Guasto generico";
                    
            avviaManutenzione(
                macchina.getId(),
                "GUASTO", // Tipo di intervento
                0, // Tecnico da assegnare
                "Guasto rilevato: " + descrizioneGuasto,
                "ALTA" // Urgenza alta per guasti
            );
        }
    }

    private void gestisciStatoCialde(Macchina macchina, JsonObject statoCialde) {
        boolean rifornimentoNecessario = false;
        StringBuilder noteCialde = new StringBuilder("Rifornimento cialde necessario: ");
        
        // Verifica soglie cialde per ogni tipo
        for (Map.Entry<String, JsonElement> entry : statoCialde.entrySet()) {
            JsonObject cialda = entry.getValue().getAsJsonObject();
            int quantita = cialda.get("quantita").getAsInt();
            int soglia = cialda.get("soglia").getAsInt();
            
            if (quantita <= soglia) {
                rifornimentoNecessario = true;
                noteCialde.append(entry.getKey())
                        .append(" (")
                        .append(quantita)
                        .append("/")
                        .append(cialda.get("massimo").getAsInt())
                        .append("), ");
            }
        }
        
        if (rifornimentoNecessario) {
            // Se non ci sono già manutenzioni attive per rifornimento
            List<Manutenzione> manutenzioniAttive = getManutenzioniMacchina(macchina.getId())
                    .stream()
                    .filter(m -> !m.isCompletata())
                    .collect(Collectors.toList());
                    
            if (manutenzioniAttive.isEmpty()) {
                avviaManutenzione(
                    macchina.getId(),
                    "RIFORNIMENTO_CIALDE", // Tipo di intervento
                    0, // Tecnico da assegnare
                    noteCialde.toString().replaceAll(", $", ""),
                    "MEDIA" // Urgenza media per rifornimento cialde
                );
            }
        }
    }
    
    public Map<Integer, Integer> calcolaQuantitaCialdeNecessarie(int macchinaId) {
        // Recupera statistiche di consumo
        List<Transazione> ultimiConsumi = transazioneRepository.findByMacchinaId(macchinaId);
        Map<Integer, Long> conteggioConsumiBevanda = ultimiConsumi.stream()
            .collect(Collectors.groupingBy(Transazione::getBevandaId, Collectors.counting()));
            
        // Recupera cialde attuali
        Macchina macchina = macchinaRepository.findById(macchinaId);
        List<QuantitaCialde> cialdeAttuali = macchina.getCialde();
        
        // Calcola necessità per ciascuna cialda
        Map<Integer, Integer> necessitaCialde = new HashMap<>();
        
        // Per ogni bevanda popolare, calcola quante cialde servono
        for (Map.Entry<Integer, Long> entry : conteggioConsumiBevanda.entrySet()) {
            int bevandaId = entry.getKey();
            long frequenzaConsumo = entry.getValue();
            
            Optional<Bevanda> bevanda = bevandaRepository.findById(bevandaId);
            if (bevanda.isPresent()) {
                for (Cialda cialda : bevanda.get().getCialde()) {
                    int cialdaId = cialda.getId();
                    
                    // Trova la quantità attuale
                    Optional<QuantitaCialde> quantitaCialda = cialdeAttuali.stream()
                        .filter(qc -> qc.getCialdaId() == cialdaId)
                        .findFirst();
                        
                    if (quantitaCialda.isPresent()) {
                        int quantitaAttuale = quantitaCialda.get().getQuantita();
                        int quantitaMassima = quantitaCialda.get().getQuantitaMassima();
                        
                        // Calcola quantità da rifornire basandosi sulla popolarità
                        int daRifornire = (int) ((quantitaMassima - quantitaAttuale) * 
                                              Math.min(1.0, frequenzaConsumo / 10.0));
                                              
                        // Aggiorna la mappa di necessità
                        necessitaCialde.merge(cialdaId, daRifornire, Integer::sum);
                    }
                }
            }
        }
        
        return necessitaCialde;
    }

    private void gestisciStatoCassa(Macchina macchina, JsonObject statoCassa) {
        double importoAttuale = statoCassa.get("importo").getAsDouble();
        double importoMassimo = statoCassa.get("massimo").getAsDouble();
        
        // Se la cassa ha raggiunto l'80% della capacità
        if (importoAttuale >= (importoMassimo * 0.8)) {
            // Se non ci sono già manutenzioni attive per svuotamento cassa
            List<Manutenzione> manutenzioniAttive = getManutenzioniMacchina(macchina.getId())
                    .stream()
                    .filter(m -> !m.isCompletata())
                    .collect(Collectors.toList());
                    
            if (manutenzioniAttive.isEmpty()) {
                avviaManutenzione(
                    macchina.getId(),
                    "SVUOTAMENTO_CASSA", // Tipo di intervento
                    0, // Tecnico da assegnare
                    String.format("Svuotamento cassa necessario: %.2f/%.2f euro", importoAttuale, importoMassimo),
                    "ALTA" // Urgenza alta per svuotamento cassa
                );
            }
        }
    }

    private int estraiMacchinaIdDaTopic(String topic) {
        try {
            String[] parts = topic.split("/");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Topic non valido: " + topic);
        }
    }
}