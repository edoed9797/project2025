package com.vending;

import static spark.Spark.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import com.google.gson.Gson;
import com.vending.core.models.*;
import com.vending.core.repositories.*;
import com.vending.api.controllers.*;
import com.vending.api.middleware.*;
import com.vending.core.services.*;
import com.vending.iot.machines.*;
import com.vending.iot.mqtt.MQTTBrokerManager;
import com.vending.iot.mqtt.MQTTClient;
import com.vending.iot.mqtt.MQTTConfig;
import com.vending.iot.mqtt.MQTTWebSocketBridge;
import com.vending.security.auth.*;
import com.vending.security.jwt.JWTService;
import com.vending.utils.config.ConfigUtil;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Gson gson = new Gson();
    private static Properties config;
    private static MQTTClient mqttClient;
    private static MQTTWebSocketBridge mqttWebSocketBridge;
    private static final Map<Integer, MacchinaPrincipale> macchineAttive = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
        	disableCertificateValidation();
        	
            // Carica la configurazione
            initConfig();

            // Inizializza i servizi
            initServices();

            // Configura il server Spark
            configureServer();

            // Inizializza MQTT
            initMQTT();

            // Inizializza le macchine
            initMacchine();

            // Configura i controller e le route
            setupRoutes();

            logger.info("Applicazione avviata con successo sulla porta {}", config.getProperty("server.port"));

            // Apri il browser con la dashboard
            openDashboard(config.getProperty("server.port", "8443"));

        } catch (Exception e) {
            logger.error("Errore durante l'avvio dell'applicazione", e);
            System.exit(1);
        }
    }

    private static void initConfig() throws Exception {
        // Carica il file di configurazione
        config = ConfigUtil.loadProperties("config.properties");

        // Imposta la porta del server
        port(Integer.parseInt(config.getProperty("server.port", "8443")));

        // Configurazione thread pool
        int maxThreads = Integer.parseInt(config.getProperty("server.maxThreads", "100"));
        int minThreads = Integer.parseInt(config.getProperty("server.minThreads", "2"));
        int timeOutMillis = Integer.parseInt(config.getProperty("server.timeOutMillis", "30000"));
        threadPool(maxThreads, minThreads, timeOutMillis);
    }

    private static void initServices() {
        // Inizializza i repository
        CialdaRepository cialdaRepository = new CialdaRepository();
        BevandaRepository bevandaRepository = new BevandaRepository(cialdaRepository);
        UtenteRepository utenteRepository = new UtenteRepository();
        MacchinaRepository macchinaRepository = new MacchinaRepository();
        ManutenzioneRepository manutenzioneRepository = new ManutenzioneRepository();
        RicavoRepository ricavoRepository = new RicavoRepository();
        TransazioneRepository transazioneRepository = new TransazioneRepository();
        IstitutoRepository istitutoRepository = new IstitutoRepository(macchinaRepository);
        AdminLoginRepository adminLoginRepository = new AdminLoginRepository(utenteRepository);

        // Registra i repository nel ServiceRegistry
        ServiceRegistry.register("cialdaRepository", cialdaRepository);
        ServiceRegistry.register("utenteRepository", utenteRepository);
        ServiceRegistry.register("macchinaRepository", macchinaRepository);
        ServiceRegistry.register("bevandaRepository", bevandaRepository);
        ServiceRegistry.register("manutenzioneRepository", manutenzioneRepository);
        ServiceRegistry.register("ricavoRepository", ricavoRepository);
        ServiceRegistry.register("transazioneRepository", transazioneRepository);
        ServiceRegistry.register("istitutoRepository", istitutoRepository);
        ServiceRegistry.register("adminLoginRepository", adminLoginRepository);

        // Inizializza i servizi
        JWTService jwtService = new JWTService();
        PasswordService passwordService = new PasswordService();
        AuthenticationService authService = new AuthenticationService(utenteRepository, passwordService, jwtService);
        BevandaService bevandaService = new BevandaService(bevandaRepository, cialdaRepository);
        UtenteService utenteService = new UtenteService(utenteRepository);
        MacchinaService macchinaService = new MacchinaService(macchinaRepository, ricavoRepository, transazioneRepository);
        IstitutoService istitutoService = new IstitutoService(istitutoRepository, macchinaRepository);
        ManutenzioneService manutenzioneService = new ManutenzioneService(manutenzioneRepository, macchinaRepository, transazioneRepository, bevandaRepository);
        RicavoService ricavoService = new RicavoService(ricavoRepository);
        TransazioneService transazioneService = new TransazioneService(transazioneRepository);
        MacchinaController macchinaController = new MacchinaController(macchinaRepository, bevandaRepository);
        AdminLoginService adminLoginService = new AdminLoginService(adminLoginRepository, utenteRepository);

        // Registra i servizi
        ServiceRegistry.register("jwtService", jwtService);
        ServiceRegistry.register("authService", authService);
        ServiceRegistry.register("bevandaService", bevandaService);
        ServiceRegistry.register("utenteService", utenteService);
        ServiceRegistry.register("macchinaService", macchinaService);
        ServiceRegistry.register("istitutoService", istitutoService);
        ServiceRegistry.register("manutenzioneService", manutenzioneService);
        ServiceRegistry.register("ricavoService", ricavoService);
        ServiceRegistry.register("transazioneService", transazioneService);
        ServiceRegistry.register("adminLoginService", adminLoginService);
        ServiceRegistry.register("macchinaController", macchinaController);
        
        TransazioneController transazioneController = new TransazioneController(
        	    ServiceRegistry.get(TransazioneService.class)
        	);
        ServiceRegistry.register("transazioneController", transazioneController);
        
    }

    private static void configureServer() {
    	// Carica la configurazione del server
        String keystorePath = config.getProperty("server.keystore.path", "C:\\Users\\Ed\\eclipse-workspace\\mqtt_20019309\\src\\main\\resources\\keystore.p12");
        String keystorePassword = config.getProperty("server.keystore.password", "Pissir2024!");
        int port = Integer.parseInt(config.getProperty("server.port", "8443"));
        
        // Configura HTTPS
        secure(keystorePath, keystorePassword, null, null);
        // Imposta la porta del server
        port(port);
        
        // Configura la cartella dei file statici
        staticFiles.location("/public");
        
        staticFiles.header("Access-Control-Allow-Origin", "*");
        staticFiles.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        staticFiles.header("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Configura CORS 
        CORSMiddleware corsMiddleware = new CORSMiddleware();
        before((request, response) -> corsMiddleware.applicaCORS(request, response));

        // Gestione OPTIONS per CORS
        options("/*", (request, response) -> {
            corsMiddleware.applicaCORS(request, response);
            return "OK";
        });

        // Configurazione autenticazione
        AuthController authController = new AuthController(ServiceRegistry.get(AdminLoginService.class),
                ServiceRegistry.get(UtenteService.class));
        before("/api/*", authController::verificaToken);

        // Configurazione parser JSON
        after((request, response) -> {
            response.type("application/json");
        });

        // Gestione errori
        exception(Exception.class, (e, request, response) -> {
            logger.error("Errore non gestito", e);
            response.status(500);
            response.body(gson.toJson(new ErrorResponse("Errore interno del server")));
        });

        notFound((request, response) -> {
            response.type("application/json");
            return gson.toJson(new ErrorResponse("Risorsa non trovata"));
        });
    }

    private static void initMQTT() throws Exception {
        /* // Configurazione opzioni MQTT
        MqttConnectOptions options = new MqttConnectOptions();
        
        // Configurazione base
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        
        // Configurazione autenticazione
        options.setUserName(config.getProperty("mqtt.client.username"));
        options.setPassword(config.getProperty("mqtt.client.password").toCharArray());
        
        // Configurazione SSL
        try {
            System.setProperty("javax.net.ssl.trustStore", MQTTConfig.TRUSTSTORE_PATH);
            System.setProperty("javax.net.ssl.trustStorePassword", MQTTConfig.TRUSTSTORE_PASSWORD);
            System.setProperty("javax.net.ssl.trustStoreType", MQTTConfig.TRUSTSTORE_TYPE);
            
            Properties sslProperties = new Properties();
            sslProperties.setProperty("com.ibm.ssl.protocol", "TLSv1.2");
            options.setSSLProperties(sslProperties);
        } catch (Exception e) {
            logger.error("Errore nella configurazione SSL: {}", e.getMessage());
            throw new MqttException(new RuntimeException("Errore nella configurazione SSL", e));
        }
        
        // Configurazione Last Will and Testament (LWT)
        options.setWill("system/server/status", "offline".getBytes(), 1, true);

        // Inizializza e connetti il client MQTT principale
        mqttClient = new MQTTClient("server");
        mqttClient.connect(options);
        logger.info("Client MQTT inizializzato e connesso");
        
        // Inizializza e avvia il bridge WebSocket-MQTT
        MQTTWebSocketBridge mqttBridge = null;
        try {
            mqttBridge = new MQTTWebSocketBridge();
            mqttBridge.start();
            logger.info("MQTT WebSocket Bridge avviato con successo");
            
            // Registra il bridge nel ServiceRegistry per recuperarlo in seguito
            ServiceRegistry.register("mqttBridge", mqttBridge);
        } catch (MqttException e) {
            logger.error("Errore durante l'avvio del MQTT WebSocket Bridge: {}", e.getMessage());
            // Non blocchiamo l'inizializzazione del sistema se il bridge fallisce
            if (mqttBridge != null) {
                try {
                    mqttBridge.stop();
                } catch (Exception ex) {
                    logger.error("Errore durante il cleanup del bridge fallito: {}", ex.getMessage());
                }
            }
        }
        
        // Configura le sottoscrizioni ai topic MQTT
        setupMQTTTopics();
        
        mqttWebSocketBridge = new MQTTWebSocketBridge();
        mqttWebSocketBridge.start();
        logger.info("MQTT WebSocket Bridge avviato");*/
    	
    	 try {
    	        // Inizializza il broker manager invece di creare un client MQTT direttamente
    	        MQTTBrokerManager brokerManager = MQTTBrokerManager.getInstance();
    	        
    	        // Ottieni un client MQTT tramite il manager
    	        mqttClient = new MQTTClient("server");
    	        
    	        logger.info("MQTTBrokerManager inizializzato e client server connesso");
    	    } catch (Exception e) {
    	        logger.error("Errore durante l'inizializzazione del broker MQTT", e);
    	        throw new RuntimeException("Errore durante l'inizializzazione del broker MQTT", e);
    	    }
    	}

    private static void initMacchine() {
        try {
            MacchinaRepository macchinaRepo = ServiceRegistry.get(MacchinaRepository.class);
            var macchine = macchinaRepo.findAll();

            for (Macchina macchina : macchine) {
                try {
                    MacchinaPrincipale macchinaPrincipale = new MacchinaPrincipale(macchina);
                    macchineAttive.put(macchina.getId(), macchinaPrincipale);
                    logger.info("Macchina {} inizializzata con successo", macchina.getId());
                } catch (Exception e) {
                    logger.error("Errore nell'inizializzazione della macchina {}: {}",
                            macchina.getId(), e.getMessage());
                }
            }

            logger.info("Inizializzate {} macchine", macchineAttive.size());
        } catch (Exception e) {
            logger.error("Errore durante l'inizializzazione delle macchine", e);
        }
    }

    private static void setupRoutes() throws MqttException {
        // Middleware per autenticazione e logging
        AuthMiddleware authMiddleware = new AuthMiddleware();
        LogMiddleware logMiddleware = new LogMiddleware();
        CORSMiddleware corsMiddleware = new CORSMiddleware();

        // Controller e servizi
        IstitutoController istitutoController = new IstitutoController(
                ServiceRegistry.get(IstitutoRepository.class),
                ServiceRegistry.get(MacchinaRepository.class)
        );

        MacchinaController macchinaController = new MacchinaController(
                ServiceRegistry.get(MacchinaRepository.class),
                ServiceRegistry.get(BevandaRepository.class)
        );

        AuthController authController = new AuthController(ServiceRegistry.get(AdminLoginService.class),
                ServiceRegistry.get(UtenteService.class)
        );

        ManutenzioneController manutenzioneController = new ManutenzioneController(
                ServiceRegistry.get(ManutenzioneRepository.class)
        );

        BevandaController bevandaController = new BevandaController(
                ServiceRegistry.get(BevandaService.class)
        );
        
        UtenteController utenteController = new UtenteController(
        		ServiceRegistry.get(UtenteRepository.class)
        );
        
        // Register controllers in the ServiceRegistry
        ServiceRegistry.register("istitutoController", istitutoController);
        ServiceRegistry.register("macchinaController", macchinaController);
        ServiceRegistry.register("bevandaController", bevandaController);
        ServiceRegistry.register("manutenzioneController", manutenzioneController);
        ServiceRegistry.register("utenteController", utenteController);

        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        // Configurazione CORS e logging globale
        before((request, response) -> corsMiddleware.applicaCORS(request, response));
        after((request, response) -> logMiddleware.logRequest(request, response));
        
        // Autenticazione
        path("/api/auth", () -> {
            post("/login", authController::login);
            post("/register", authController::registrazione);
            post("/refresh", authController::refreshToken);
            post("/verify", authController::verificaToken);
            post("/anonymous", authController::getAnonymousToken);
        });

        // Root path e redirect alla homepage
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        // Accesso alle risorse
        get("/api/macchine", ServiceRegistry.get(MacchinaController.class)::getAll);
        get("/api/macchine/:id", ServiceRegistry.get(MacchinaController.class)::getById);
        get("/api/macchine/istituto/:istitutoId", ServiceRegistry.get(MacchinaController.class)::getByIstituto);
        get("/api/macchine/:id/stato", ServiceRegistry.get(MacchinaController.class)::getStatoMacchina);
        get("/api/macchine/:id/bevande", (req, res) -> {
            int macchinaId = Integer.parseInt(req.params(":id"));
            return macchinaController.getBevandeMacchina(macchinaId, res);
        });

        get("/api/istituti", ServiceRegistry.get(IstitutoController.class)::getAll);
        get("/api/istituti/:id", ServiceRegistry.get(IstitutoController.class)::getById);
        get("api/istituti/:istitutoId/macchine", ServiceRegistry.get(MacchinaController.class)::getByIstituto);

        get("/api/bevande", bevandaController::getAll);
        get("/api/bevande/:id", bevandaController::getById);
        get("/api/bevande/:id/disponibilita/:macchinaId", (req, res) -> {
            int bevandaId = Integer.parseInt(req.params(":id"));
            int macchinaId = Integer.parseInt(req.params(":macchinaId"));
            return macchinaController.verificaDisponibilitaBevanda(bevandaId, macchinaId, res);
        });

        // Rotte per la gestione del denaro e erogazione bevande
        path("/api/macchine/:id", () -> {
            // Inserimento denaro
            post("/insertMoney", (req, res) -> {
                int macchinaId = Integer.parseInt(req.params(":id"));
                double importo = Double.parseDouble(req.queryParams("importo"));

                // Recupera la macchina e verifica che sia attiva
                Macchina macchina = ServiceRegistry.get(MacchinaRepository.class).findById(macchinaId);
                if (macchina == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Macchina non trovata"));
                }

                if (macchina.getStatoId() != 1) { // 1 = Attiva
                    res.status(400);
                    return gson.toJson(Map.of("error", "Macchina non disponibile"));
                }

                // Crea un'istanza del GestoreCassa per questa macchina
                GestoreCassa gestoreCassa = new GestoreCassa(macchinaId, macchina.getCassaMassima());

                // Tenta di inserire il denaro
                boolean inserimentoRiuscito = gestoreCassa.gestisciInserimentoMoneta(importo);

                if (inserimentoRiuscito) {
                    return gson.toJson(Map.of(
                            "success", true,
                            "message", "Denaro inserito con successo",
                            "creditoAttuale", gestoreCassa.ottieniStato().get("creditoAttuale")
                    ));
                } else {
                    res.status(400);
                    return gson.toJson(Map.of("error", "Impossibile accettare il denaro"));
                }
            });

            // Erogazione bevanda
            post("/erogazione", (req, res) -> {
                int macchinaId = Integer.parseInt(req.params(":id"));
                Map<String, Object> body = gson.fromJson(req.body(), Map.class);
                int bevandaId = ((Number) body.get("bevandaId")).intValue();
                double importo = ((Number) body.get("importo")).doubleValue();

                // Recupera la macchina
                MacchinaPrincipale macchina = macchineAttive.get(macchinaId);
                if (macchina == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Macchina non trovata"));
                }

                // Recupera la bevanda per verificare il prezzo
                BevandaRepository bevandaRepo = ServiceRegistry.get(BevandaRepository.class);
                Optional<Bevanda> bevanda = bevandaRepo.findById(bevandaId);
                if (!bevanda.isPresent()) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Bevanda non trovata"));
                }

                // Crea la transazione
                TransazioneRepository transazioneRepo = ServiceRegistry.get(TransazioneRepository.class);
                Transazione transazione = new Transazione();
                int idT = transazioneRepo.getLastTransactionId() + 1;
                transazione.setId(idT);
                transazione.setMacchinaId(macchinaId);
                transazione.setBevandaId(bevandaId);
                transazione.setImporto(importo);
                transazione.setDataOra(LocalDateTime.now());

                // Pubblica la richiesta sul topic MQTT appropriato
                String topic = "macchine/" + macchinaId + "/bevande/richiesta";
                Map<String, Object> message = Map.of(
                        "bevandaId", bevandaId,
                        "importo", importo,
                        "timestamp", System.currentTimeMillis(),
                        "transazioneId", transazione.getId()
                );

                try {
                    mqttClient.publish(topic, gson.toJson(message));
                    transazione = transazioneRepo.save(transazione);

                    return gson.toJson(Map.of(
                            "success", true,
                            "message", "Richiesta erogazione inviata",
                            "transazioneId", transazione.getId()
                    ));
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of("error", "Errore durante il salvataggio della transazione: " + e.getMessage()));
                }
            });

            // Annulla inserimento e restituisci credito
            post("/cancelInsert", (req, res) -> {
                int macchinaId = Integer.parseInt(req.params(":id"));

                // Recupera la macchina
                Macchina macchina = ServiceRegistry.get(MacchinaRepository.class).findById(macchinaId);
                if (macchina == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Macchina non trovata"));
                }

                // Crea un'istanza del GestoreCassa
                GestoreCassa gestoreCassa = new GestoreCassa(macchinaId, macchina.getCassaMassima());

                // Gestisci la restituzione del credito
                gestoreCassa.gestisciRestituzioneCredito();

                return gson.toJson(Map.of(
                        "success", true,
                        "message", "Credito restituito"
                ));
            });

            // Verifica stato macchina
            get("/stato", (req, res) -> {
                int macchinaId = Integer.parseInt(req.params(":id"));

                // Recupera la macchina
                MacchinaPrincipale macchina = macchineAttive.get(macchinaId);
                if (macchina == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Macchina non trovata"));
                }

                // Ottieni lo stato completo della macchina
                Map<String, Object> stato = new HashMap<>();
                stato.put("statoMacchina", ServiceRegistry.get(MacchinaRepository.class)
                        .findById(macchinaId));
                stato.put("statoCassa", macchina.gestoreCassa.ottieniStato());
                stato.put("statoBevande", macchina.gestoreBevande.ottieniStato());
                stato.put("statoCialde", macchina.gestoreCialde.ottieniStato());

                return gson.toJson(stato);
            });
        });
        
        // Operazioni admin
        path("/api/admin", () -> {
            before("/*", authMiddleware::autenticazione);
            
            // Gestione Utenti
            path("/utenti", () -> {
                get("", (req, res) -> ServiceRegistry.get(UtenteController.class).getAll(req, res)); //Recuperare tutti gli utenti
                get("/:id", (req, res) -> ServiceRegistry.get(UtenteController.class).getById(req, res)); // Recuperare un utente per ID
                get("/username/:username", (req, res) -> ServiceRegistry.get(UtenteController.class).getByUsername(req, res)); // Recuperare un utente per username
                get("/ruolo/:ruolo", (req, res) -> ServiceRegistry.get(UtenteController.class).getByRuolo(req, res)); //Recuperare utenti per ruolo
                post("", (req, res) -> ServiceRegistry.get(UtenteController.class).create(req, res)); // Creare un nuovo utente
                put("/:id", (req, res) -> ServiceRegistry.get(UtenteController.class).update(req, res)); // Aggiornare un utente
                delete("/:id", (req, res) -> ServiceRegistry.get(UtenteController.class).delete(req, res)); // Eliminare un utente
            });
            
            // Gestione istituti
            post("/istituti", (req, res) -> {
                //authMiddleware.autorizzazioneAdmin(req, res);
                return istitutoController.create(req, res);
            });
            put("/istituti/:id", (req, res) -> {
                authMiddleware.autorizzazioneAdmin(req, res);
                return istitutoController.update(req, res);
            });
            delete("/istituti/:id", (req, res) -> {
                authMiddleware.autorizzazioneAdmin(req, res);
                return istitutoController.delete(req, res);
            });

            // Gestione macchine
            post("/macchine", (req, res) -> {
                authMiddleware.autorizzazioneAdmin(req, res);
                return macchinaController.create(req, res);
            });
            put("/macchine/:id/bevande", (req, res) -> {
                return ServiceRegistry.get(MacchinaController.class).updateBevandeMacchina(req, res);
            });
            put("/macchine/:id/stato", macchinaController::updateStato);
            delete("/macchine/:id", (req, res) -> {
                authMiddleware.autorizzazioneAdmin(req, res);
                return macchinaController.delete(req, res);
            });

            // Gestione bevande
            path("/bevande", () -> {
                post("", (req, res) -> {
                    authMiddleware.autorizzazioneAdmin(req, res);
                    return bevandaController.create(req, res);
                });
                put("/:id", (req, res) -> {
                    authMiddleware.autorizzazioneAdmin(req, res);
                    return bevandaController.update(req, res);
                });
                delete("/:id", (req, res) -> {
                    authMiddleware.autorizzazioneAdmin(req, res);
                    return bevandaController.delete(req, res);
                });
                put("/:id/cialde", (req, res) -> {
                    authMiddleware.autorizzazioneAdmin(req, res);
                    return bevandaController.aggiungiCialda(req, res);
                });
                delete("/:id/cialde/:cialdaId", (req, res) -> {
                    authMiddleware.autorizzazioneAdmin(req, res);
                    return bevandaController.rimuoviCialda(req, res);
                });
            });
            
            path("/transazioni", () -> {
                get("", (req, res) -> {
                    // Recupera tutte le transazioni
                    return ServiceRegistry.get(TransazioneController.class).getAllTransazioni(req, res);
                });
                
                get("/:id", (req, res) -> {
                    // Recupera una transazione specifica
                    return ServiceRegistry.get(TransazioneController.class).getTransazioneById(req, res);
                });
                
                get("/macchina/:macchinaId", (req, res) -> {
                    // Recupera transazioni di una macchina
                    return ServiceRegistry.get(TransazioneController.class).getTransazioniByMacchina(req, res);
                });
                
                get("/recenti", (req, res) -> {
                    // Recupera le transazioni più recenti
                    return ServiceRegistry.get(TransazioneController.class).getTransazioniRecenti(req, res);
                });
                
                post("", (req, res) -> {
                    // Crea una nuova transazione
                    return ServiceRegistry.get(TransazioneController.class).createTransazione(req, res);
                });
            });
        });
        
        // Manutenzioni
        path("/api/manutenzione", () -> {
        	
        	// Verifica autenticazione
        	before("/*", authMiddleware::autenticazione);
        	// Ottieni tutte le manutenzioni
        	get("", manutenzioneController::getManutenzioni);
        	});

        	// Ottieni manutenzioni per istituto
        	get("/istituto/:istitutoId", (req, res) -> {
        		try {
        			int istitutoId = Integer.parseInt(req.params(":istitutoId"));
        			List<Manutenzione> manutenzioni = (List<Manutenzione>) manutenzioneController.getManutenzioniIstituto(req, res);
        			res.type("application/json");
        			return gson.toJson(manutenzioni);
        		} catch (NumberFormatException e) {
        			res.status(400);
        			return gson.toJson(Map.of("error", "ID istituto non valido"));
        		} catch (Exception e) {
        			res.status(500);
        			return gson.toJson(Map.of("error", "Errore durante il recupero delle manutenzioni per istituto: " + e.getMessage()));
        		}
        	});
        	
        	// Ottieni manutenzioni per tecnico
        	get("/tecnico/:tecnicoId", (req, res) -> {
        		try {
        			int tecnicoId = Integer.parseInt(req.params(":tecnicoId"));
        			List<Manutenzione> manutenzioni = (List<Manutenzione>) manutenzioneController.getManutenzioniTecnico(req, res);
        			res.type("application/json");
        			return gson.toJson(manutenzioni);
        		} catch (NumberFormatException e) {
        			res.status(400);
        			return gson.toJson(Map.of("error", "ID tecnico non valido"));
        		} catch (Exception e) {
        			res.status(500);
        			return gson.toJson(Map.of("error", "Errore durante il recupero delle manutenzioni per tecnico: " + e.getMessage()));
        		}
        	});

        	// Inizia una nuova manutenzione
        	post("", (req, res) -> {
        		try {
        			Map<String, Object> body = gson.fromJson(req.body(), Map.class);
        			int macchinaId = Integer.parseInt(body.get("macchinaId").toString());
        			String tipoIntervento = (String) body.get("tipoIntervento");
        			String descrizione = (String) body.get("descrizione");
        			String urgenza = (String) body.get("urgenza");

        			Manutenzione manutenzione = (Manutenzione) manutenzioneController.iniziaManutenzione(req, res);
        			res.type("application/json");
        			return gson.toJson(manutenzione);
        		} catch (NumberFormatException e) {
        			res.status(400);
        			return gson.toJson(Map.of("error", "ID macchina non valido"));
        		} catch (Exception e) {
        			res.status(500);
        			return gson.toJson(Map.of("error", "Errore durante l'inizio della manutenzione: " + e.getMessage()));
        		}
        	});

        	// Completa una manutenzione esistente
        	put("/:id/completa", (req, res) -> {
        		try {
        			int manutenzioneId = Integer.parseInt(req.params(":id"));
        			Map<String, Object> body = gson.fromJson(req.body(), Map.class);
        			String note = (String) body.get("note");
        			String tecnico = (String) body.get("tecnico");

        			Manutenzione manutenzione = (Manutenzione) manutenzioneController.completaManutenzione(req, res);
        			res.type("application/json");
        			return gson.toJson(manutenzione);
        		} catch (NumberFormatException e) {
        			res.status(400);
        			return gson.toJson(Map.of("error", "ID manutenzione non valido"));
        		} catch (Exception e) {
        			res.status(500);
        			return gson.toJson(Map.of("error", "Errore durante il completamento della manutenzione: " + e.getMessage()));
        		}
        	});

        	// Imposta una manutenzione come fuori servizio
        	put("/:id/fuori-servizio", (req, res) -> {
        		try {
        			int manutenzioneId = Integer.parseInt(req.params(":id"));
        			Manutenzione manutenzione = (Manutenzione) manutenzioneController.setFuoriServizio(req, res);
        			res.type("application/json");
        			return gson.toJson(manutenzione);
        		} catch (NumberFormatException e) {
        			res.status(400);
        			return gson.toJson(Map.of("error", "ID manutenzione non valido"));
        		} catch (Exception e) {
        			res.status(500);
        			return gson.toJson(Map.of("error", "Errore durante l'impostazione della manutenzione come fuori servizio: " + e.getMessage()));
        		}
        	});

        // Setup MQTT topics
        setupMQTTTopics();

        // Rotta per lo spegnimento controllato
        get("/spegni", (req, res) -> {
            logger.info("Richiesta di spegnimento ricevuta");
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    shutdown();
                    System.exit(0);
                } catch (InterruptedException e) {
                    logger.error("Errore durante lo spegnimento: {}", e.getMessage());
                }
            }).start();
            res.type("application/json");
            
            return gson.toJson(Map.of("message", "Spegnimento del sistema in corso...", "status", "success"));
        });

        // Gestione errori
        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.type("application/json");
            res.body("{\"errore\": \"" + e.getMessage() + "\"}");
            logMiddleware.logError(req, e);
        });

        notFound((req, res) -> {
            res.type("application/json");
            res.status(404);
            return "{\"errore\": \"Risorsa non trovata\"}";
        });

        internalServerError((req, res) -> {
            res.type("application/json");
            return "{\"errore\": \"Errore interno del server\"}";
        });
    }

    private static void setupMQTTTopics() throws MqttException {

        // Topic per lo stato delle macchine
        mqttClient.subscribe("macchine/+/stato", (topic, message) -> {
            logger.debug("Ricevuto messaggio stato: {} - {}", topic, message);
            ServiceRegistry.get(ManutenzioneService.class).processaMacchinaStato(topic, message);

            String[] parts = topic.split("/");
            if (parts.length >= 2) {
                try {
                    int macchinaId = Integer.parseInt(parts[1]);
                    MacchinaPrincipale macchina = macchineAttive.get(macchinaId);
                    if (macchina != null) {
                        macchina.pubblicaStatoMacchina();
                    }
                } catch (NumberFormatException e) {
                    logger.error("ID macchina non valido nel topic: {}", topic);
                }
            }
        });

        // Topic per le operazioni utente
        mqttClient.subscribe("macchine/+/cassa/#", (topic, message) -> {
        	logger.info("RICEVUTO MESSAGGIO CASSA: Topic={}, Payload={}", topic, message);
            String[] parts = topic.split("/");
            //CHECK PER ZUCCHERO DA FARE
            if (parts.length >= 4) {
                try {
                    int macchinaId = Integer.parseInt(parts[1]);
                    String operazione = parts[3];
                    MacchinaPrincipale macchina = macchineAttive.get(macchinaId);

                    if (macchina != null) {
                        switch (operazione) {
                            case "inserimentoCredito":
                                double importo = Double.parseDouble(message);
                                boolean successo = macchina.gestoreCassa.gestisciInserimentoMoneta(importo);
                                if (successo) {
                                    logger.info("Credito inserito con successo nella macchina {}: {}", macchinaId, importo);
                                } else {
                                    logger.warn("Impossibile inserire credito nella macchina {}: {}", macchinaId, importo);
                                }
                                break;

                            case "richiestaBevanda":
                                int bevandaId = Integer.parseInt(message);
                                macchina.gestisciErogazioneBevanda(bevandaId, 0); // Livello zucchero default
                                logger.info("Richiesta bevanda {} nella macchina {}", bevandaId, macchinaId);
                                break;

                            case "richiestaResto":
                                macchina.gestoreCassa.gestisciRestituzioneCredito();
                                logger.info("Resto richiesto nella macchina {}", macchinaId);
                                break;

                            default:
                                logger.warn("Operazione non riconosciuta: {}", operazione);
                                break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Errore nell'elaborazione dell'operazione: {}", e.getMessage());
                }
            }
        });

        // Topic per le bevande
        mqttClient.subscribe("macchine/+/bevande/#", (topic, message) -> {
            logger.debug("Ricevuto messaggio bevande: {} - {}", topic, message);
            String[] parts = topic.split("/");
            if (parts.length >= 4) {
                try {
                    int macchinaId = Integer.parseInt(parts[1]);
                    int zucchero = Integer.parseInt(parts[2]);
                    String azione = parts[3];
                    MacchinaPrincipale macchina = macchineAttive.get(macchinaId);

                    if (macchina != null) {
                        switch (azione) {
                            case "aggiorna":
                                // Gestisci l'aggiornamento della bevanda
                                macchina.pubblicaStatoMacchina();
                                break;
                            default:
                                logger.warn("Azione bevanda non riconosciuta: {}", azione);
                                break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Errore nell'elaborazione della bevanda: {}", e.getMessage());
                }
            }
        });
        
     // Topic per lo stato della cassa
        mqttClient.subscribe("macchine/+/cassa/stato", (topic, message) -> {
            logger.debug("Ricevuta richiesta stato cassa: {} - {}", topic, message);
            String[] parts = topic.split("/");
            if (parts.length >= 3) {
                try {
                    int macchinaId = Integer.parseInt(parts[1]);
                    MacchinaPrincipale macchina = macchineAttive.get(macchinaId);
                    
                    if (macchina != null) {
                        // Ottieni lo stato della cassa
                        Map<String, Object> statoCassa = macchina.gestoreCassa.ottieniStato();
                        
                        // Pubblica lo stato come risposta
                        String statoTopic = "macchine/" + macchinaId + "/cassa/stato/risposta";
                        mqttClient.publish(statoTopic, gson.toJson(statoCassa));
                        logger.debug("Stato cassa pubblicato per macchina {}: {}", macchinaId, statoCassa);
                    } else {
                        logger.warn("Macchina {} non trovata per richiesta stato cassa", macchinaId);
                    }
                } catch (NumberFormatException e) {
                    logger.error("ID macchina non valido nel topic stato cassa: {}", topic);
                } catch (Exception e) {
                    logger.error("Errore nell'elaborazione dello stato cassa: {}", e.getMessage());
                }
            }
        });

        // Topic per lo stato delle bevande
        mqttClient.subscribe("macchine/+/bevande/stato", (topic, message) -> {
            logger.debug("Ricevuta richiesta stato bevande: {} - {}", topic, message);
            String[] parts = topic.split("/");
            if (parts.length >= 3) {
                try {
                    int macchinaId = Integer.parseInt(parts[1]);
                    MacchinaPrincipale macchina = macchineAttive.get(macchinaId);
                    
                    if (macchina != null) {
                        // Ottieni lo stato delle bevande
                        Map<String, Object> statoBevande = macchina.gestoreBevande.ottieniStato();
                        
                        // Pubblica lo stato come risposta
                        String statoTopic = "macchine/" + macchinaId + "/bevande/stato/risposta";
                        mqttClient.publish(statoTopic, gson.toJson(statoBevande));
                        logger.debug("Stato bevande pubblicato per macchina {}", macchinaId);
                    } else {
                        logger.warn("Macchina {} non trovata per richiesta stato bevande", macchinaId);
                    }
                } catch (NumberFormatException e) {
                    logger.error("ID macchina non valido nel topic stato bevande: {}", topic);
                } catch (Exception e) {
                    logger.error("Errore nell'elaborazione dello stato bevande: {}", e.getMessage());
                }
            }
        });

        // Topic per le cialde
        mqttClient.subscribe("macchine/+/cialde/#", (topic, message) -> {
            logger.debug("Ricevuto messaggio cialde: {} - {}", topic, message);
            String[] parts = topic.split("/");
            if (parts.length >= 4) {
                try {
                    int macchinaId = Integer.parseInt(parts[1]);
                    String azione = parts[3];
                    MacchinaPrincipale macchina = macchineAttive.get(macchinaId);

                    if (macchina != null) {
                        switch (azione) {
                            case "ricarica":
                                // Gestisci la ricarica delle cialde
                                macchina.gestoreCialde.gestisciRicaricaCialde(new GestoreCialde.RichiestaCialde());
                                break;
                            case "verifica":
                                // Verifica lo stato delle cialde
                                macchina.gestoreCialde.verificaStatoCialde();
                                break;
                            default:
                                logger.warn("Azione cialde non riconosciuta: {}", azione);
                                break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Errore nell'elaborazione delle cialde: {}", e.getMessage());
                }
            }
        });

        // Topic per la manutenzione
       /* mqttClient.subscribe("macchine/+/manutenzione/#", (topic, message) -> {
            logger.debug("Ricevuto messaggio manutenzione: {} - {}", topic, message);
            String[] parts = topic.split("/");
            if (parts.length >= 4) {
                try {
                    int macchinaId = Integer.parseInt(parts[1]);
                    String azione = parts[3];
                    MacchinaPrincipale macchina = macchineAttive.get(macchinaId);

                    if (macchina != null) {
                        switch (azione) {
                            case "segnalazione":
                                // Gestisci la segnalazione di un problema
                                macchina.gestoreManutenzione.segnalaProblema("tipo_problema", "descrizione_problema", new HashMap<>());
                                break;
                            case "risoluzione":
                                // Gestisci la risoluzione di un problema
                                macchina.gestoreManutenzione.risolviProblema("id_problema", "descrizione_risoluzione", "tecnico");
                                break;
                            case "verifica":
                                // Verifica lo stato della manutenzione
                                macchina.gestoreManutenzione.verificaStato();
                                break;
                            default:
                                logger.warn("Azione manutenzione non riconosciuta: {}", azione);
                                break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Errore nell'elaborazione della manutenzione: {}", e.getMessage());
                }
            }
        });*/

    }

    private static void openDashboard(String port) {
        try {
            //Thread.sleep(1500);
            String url = "https://localhost:" + port + "/index.html";
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                String[] browsers = {"google-chrome", "firefox", "mozilla", "epiphany",
                    "konqueror", "netscape", "opera", "links", "lynx"};

                StringBuffer cmd = new StringBuffer();
                for (int i = 0; i < browsers.length; i++) {
                    if (i == 0) {
                        cmd.append(String.format("%s \"%s\"", browsers[i], url));
                    } else {
                        cmd.append(String.format(" || %s \"%s\"", browsers[i], url));
                    }
                }

                Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd.toString()});
            }

            logger.info("Dashboard aperta nel browser: {}", url);
        } catch (Exception e) {
            logger.error("Impossibile aprire il browser automaticamente", e);
            logger.info("Per accedere alla dashboard, apri manualmente: https://localhost:{}/index.html", port);
        }
    }

    private static class ErrorResponse {

        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
    
    public static void disableCertificateValidation() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        logger.info("Avvio procedura di spegnimento...");

        macchineAttive.values().forEach(macchina -> {
            try {
                macchina.eseguiSpegnimento();
            } catch (Exception e) {
                logger.error("Errore durante lo spegnimento della macchina", e);
            }
        });

        macchineAttive.clear();

        try {
            // Disconnetti tutti i client MQTT tramite il manager
            MQTTBrokerManager.getInstance().disconnectAll();
        } catch (Exception e) {
            logger.error("Errore durante la disconnessione dei client MQTT", e);
        }

        logger.info("Spegnimento completato");
    }
}
