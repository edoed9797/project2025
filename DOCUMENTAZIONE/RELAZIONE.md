# Analisi dettagliata di Main.java

## Introduzione

Il file `Main.java` rappresenta il punto di ingresso dell'applicazione per il sistema di gestione delle macchine distributrici di bevande. Questo componente svolge un ruolo cruciale nell'inizializzazione e configurazione dell'intero sistema, orchestrando l'avvio di tutti i servizi necessari per il funzionamento dell'applicazione.

## Struttura e funzionalità principali

La classe `Main` è strutturata in modo da gestire l'intero ciclo di vita dell'applicazione, dalla configurazione iniziale all'arresto controllato. Analizziamo nel dettaglio le sue componenti principali:

### Variabili e proprietà globali

All'inizio della classe vengono dichiarate diverse variabili statiche fondamentali:
- Un logger per registrare eventi e errori
- Un parser JSON (Gson) per la serializzazione/deserializzazione
- Un oggetto `Properties` per la configurazione
- Un client MQTT per la comunicazione con le macchine
- Una mappa per tenere traccia delle macchine attive

### Metodo `main`

Il metodo `main()` rappresenta l'entry point dell'applicazione e segue questi passaggi:

1. **Disabilitazione della validazione dei certificati**: Viene configurato il sistema per accettare connessioni SSL anche con certificati auto-firmati, utile in ambiente di sviluppo.

2. **Caricamento configurazione**: Viene invocato il metodo `initConfig()` che carica i parametri dal file `config.properties` e configura la porta del server e le dimensioni del thread pool.

3. **Inizializzazione servizi**: Il metodo `initServices()` registra tutti i repository e i servizi necessari nel `ServiceRegistry`, un componente che funziona come contenitore di dipendenze.

4. **Configurazione del server**: Il metodo `configureServer()` imposta il server HTTPS con il certificato SSL, configura le cartelle statiche e i middleware per CORS, autenticazione e logging.

5. **Inizializzazione MQTT**: Il metodo `initMQTT()` configura e avvia la connessione al broker MQTT per la comunicazione con le macchine distributrici.

6. **Inizializzazione macchine**: Il metodo `initMacchine()` recupera tutte le macchine dal database e crea istanze `MacchinaPrincipale` per ciascuna, aggiungendole alla mappa delle macchine attive.

7. **Configurazione delle rotte**: Il metodo `setupRoutes()` definisce tutti gli endpoint REST dell'API e configura i rispettivi controller.

8. **Apertura del dashboard**: Viene avviato il browser con l'URL della dashboard.

In caso di errori durante l'avvio, il sistema registra l'errore e termina con codice di uscita 1.

### Inizializzazione della configurazione

Il metodo `initConfig()` carica il file di configurazione `config.properties` e imposta:
- La porta del server (default 8443)
- Le dimensioni del thread pool (thread minimi, massimi e timeout)

### Inizializzazione dei servizi

Il metodo `initServices()` costruisce e registra tutti i componenti del sistema:
1. Inizializza tutti i repository per l'accesso ai dati
2. Registra i repository nel `ServiceRegistry`
3. Crea i servizi che implementano la logica di business
4. Crea i controller che gestiscono le richieste HTTP
5. Registra servizi e controller nel `ServiceRegistry`

Questo approccio garantisce che tutti i componenti siano correttamente collegati tra loro e disponibili quando necessario.

### Configurazione del server

Il metodo `configureServer()` configura il server web:
1. Imposta HTTPS con il keystore specificato
2. Configura la porta
3. Imposta la cartella per i file statici
4. Configura i middleware per CORS, autenticazione e logging
5. Configura la gestione degli errori e delle risposte predefinite

### Inizializzazione MQTT

Il metodo `initMQTT()` configura la connessione al broker MQTT:
1. Crea le opzioni di connessione con autenticazione
2. Configura SSL se necessario
3. Imposta il "Last Will and Testament" per segnalare disconnessioni impreviste
4. Inizializza e connette il client MQTT

### Inizializzazione delle macchine

Il metodo `initMacchine()` recupera tutte le macchine dal database e per ciascuna:
1. Crea un'istanza di `MacchinaPrincipale`
2. Configura i suoi gestori (cassa, bevande, cialde, manutenzione)
3. La aggiunge alla mappa delle macchine attive

### Configurazione delle routes API e topic mqtt

Il metodo `setupRoutes()` definisce tutti gli endpoint REST dell'API:
1. Endpoint per l'autenticazione (`/api/auth/login`, `/api/auth/register`, ecc.)
2. Endpoint pubblici per clienti (informazioni su macchine, bevande, istituti)
3. Endpoint protetti per amministratori (gestione istituti, macchine, bevande)
4. Endpoint per la manutenzione
5. Configura la gestione degli errori per ciascun endpoint

Inoltre, configura le sottoscrizioni MQTT per gestire la comunicazione con le macchine:
1. Topic per lo stato delle macchine
2. Topic per le operazioni utente (inserimento monete, erogazione bevande)
3. Topic per bevande e cialde
4. Topic per la manutenzione


### Topic MQTT e Chiamate REST Principali

## Topic per le Macchine
# Stato e Monitoraggio
- `macchine/+/stato` - Riceve e pubblica lo stato generale delle macchine
- `macchine/+/allarmi` - Segnalazioni di allarmi dalle macchine
- `macchine/+/manutenzione` - Informazioni relative alla manutenzione

# Gestione Cassa
- `macchine/+/cassa/stato` - Stato attuale della cassa (importi, percentuale occupazione)
- `macchine/+/cassa/credito` - Credito attualmente disponibile per l'utente
- `macchine/+/cassa/inserimento` - Richieste di inserimento monete
- `macchine/+/cassa/svuotamento` - Operazioni di svuotamento della cassa
- `macchine/+/cassa/cancella` - Richieste di restituzione del credito

# Gestione Bevande
- `macchine/+/bevande/lista` - Elenco bevande disponibili nella macchina
- `macchine/+/bevande/richiesta` - Richieste di erogazione di bevanda
- `macchine/+/bevande/aggiorna` - Aggiornamenti configurazione bevande
- `macchine/+/bevande/stato` - Stato dell'erogazione (preparazione, completata)
- `macchine/+/bevande/errore` - Errori durante l'erogazione
- `macchine/+/bevande/erogazione` - Conferma di erogazione avvenuta

# Gestione Cialde
- `macchine/+/cialde/stato` - Stato delle cialde (quantità disponibili)
- `macchine/+/cialde/ricarica` - Richieste di ricarica cialde
- `macchine/+/cialde/avviso` - Avvisi di scorte cialde in esaurimento
- `macchine/+/cialde/verifica` - Richieste di verifica livelli cialde

## Manutenzione
- `macchine/+/manutenzione/segnalazione` - Segnalazioni di problemi tecnici
- `macchine/+/manutenzione/risoluzione` - Conferme di problemi risolti
- `macchine/+/manutenzione/stato` - Stato manutenzione della macchina
- `macchine/+/manutenzione/urgente` - Segnalazioni di interventi urgenti

# Topic per il Monitoraggio
- `monitoraggio/+/statistiche` - Dati statistici sulle macchine
- `monitoraggio/+/alert` - Avvisi critici dal sistema di monitoraggio
- `monitoraggio/alert/+` - Gestione degli alert per una specifica macchina

# Topic per la Manutenzione
- `manutenzione/+/richieste` - Richieste di interventi tecnici
- `manutenzione/+/interventi` - Gestione degli interventi programmati
- `manutenzione/richieste/+` - Richieste di manutenzione per una specifica macchina


### Chiamate REST API Principali
# Autenticazione e Gestione Utenti
- `POST /api/auth/login` - Autenticazione utente (username, password)
- `POST /api/auth/register` - Registrazione nuovo utente
- `POST /api/auth/refresh` - Rinnovo token JWT
- `POST /api/auth/verify` - Verifica validità token
- `POST /api/auth/anonymous` - Ottenimento token anonimo per operazioni base

# Gestione Istituti
- `GET /api/istituti` - Elenco di tutti gli istituti
- `GET /api/istituti/:id` - Dettagli di un istituto specifico
- `POST /api/admin/istituti` - Creazione nuovo istituto (solo admin)
- `PUT /api/admin/istituti/:id` - Aggiornamento istituto (solo admin)
- `DELETE /api/admin/istituti/:id` - Eliminazione istituto (solo admin)

# Gestione Macchine
- `GET /api/macchine` - Elenco di tutte le macchine
- `GET /api/macchine/:id` - Dettagli di una macchina specifica
- `GET /api/macchine/istituto/:istitutoId` - Macchine in un istituto
- `GET /api/macchine/:id/stato` - Stato operativo di una macchina
- `GET /api/macchine/:id/bevande` - Bevande disponibili in una macchina
- `POST /api/admin/macchine` - Aggiunta nuova macchina (solo admin)
- `PUT /api/admin/macchine/:id/stato` - Aggiornamento stato macchina
- `DELETE /api/admin/macchine/:id` - Rimozione macchina (solo admin)

# Gestione Bevande
- `GET /api/bevande` - Elenco di tutte le bevande
- `GET /api/bevande/:id` - Dettagli di una bevanda specifica
- `GET /api/bevande/:id/disponibilita/:macchinaId` - Verifica disponibilità bevanda
- `POST /api/admin/bevande` - Creazione nuova bevanda (solo admin)
- `PUT /api/admin/bevande/:id` - Aggiornamento bevanda (solo admin)
- `DELETE /api/admin/bevande/:id` - Eliminazione bevanda (solo admin)
- `PUT /api/admin/bevande/:id/cialde` - Aggiunta cialda a bevanda
- `DELETE /api/admin/bevande/:id/cialde/:cialdaId` - Rimozione cialda da bevanda

# Operazioni Cliente
- `POST /api/macchine/:id/insertMoney` - Inserimento denaro
- `POST /api/macchine/:id/erogazione` - Richiesta erogazione bevanda
- `POST /api/macchine/:id/cancelInsert` - Annullamento e restituzione credito

# Manutenzione
- `GET /api/manutenzione` - Elenco di tutte le manutenzioni
- `GET /api/manutenzione/istituto/:istitutoId` - Manutenzioni di un istituto
- `GET /api/manutenzione/tecnico/:tecnicoId` - Manutenzioni assegnate a un tecnico
- `POST /api/manutenzione` - Avvio nuova manutenzione
- `PUT /api/manutenzione/:id/completa` - Completamento manutenzione
- `PUT /api/manutenzione/:id/fuori-servizio` - Impostazione fuori servizio

# Gestione transazioni e ricavi
- `GET /api/admin/transazioni` - Elenco transazioni (solo admin)
- `GET /api/admin/ricavi` - Dati sui ricavi (solo admin)
- `GET /api/admin/statistiche` - Statistiche generali (solo admin)

## Correlazione tra Topic MQTT e API REST

Il sistema integra le comunicazioni MQTT e REST in modo che:

1. I messaggi MQTT gestiscono comunicazioni in tempo reale tra macchine e sistema centrale
2. Le API REST forniscono accesso ai dati per l'interfaccia utente web e operazioni amministrative
3. I comandi REST possono generare messaggi MQTT quando necessario (es. API di erogazione genera messaggi su `macchine/+/bevande/richiesta`)
4. Gli eventi MQTT possono aggiornare lo stato del sistema che sarà poi accessibile tramite REST

Questa architettura ibrida permette comunicazioni in tempo reale tra macchine e sistema centrale tramite MQTT, mentre offre un'interfaccia strutturata e RESTful per le applicazioni client.

### Gestione dell'arresto

Il metodo `shutdown()` gestisce lo spegnimento controllato dell'applicazione:
1. Spegne tutte le macchine virtuali
2. Disconnette il client MQTT
3. Registra il completamento dello spegnimento

## Flusso di esecuzione

In sintesi, quando l'applicazione viene avviata:
1. Vengono caricate le configurazioni
2. Vengono inizializzati tutti i servizi e repository
3. Viene configurato il server web con HTTPS e middleware
4. Viene stabilita la connessione al broker MQTT
5. Vengono inizializzate le macchine virtuali
6. Vengono configurati gli endpoint REST e le sottoscrizioni MQTT
7. Viene aperto il browser con la dashboard
8. L'applicazione resta in ascolto di richieste HTTP e messaggi MQTT

In caso di arresto, viene eseguita la procedura di shutdown per garantire una terminazione pulita.

## Architettura a microservizi

Il `Main.java` implementa un'architettura a microservizi dove:
- Ogni macchina è un microservizio indipendente che comunica via MQTT
- I servizi di backend comunicano tramite REST API
- Il `ServiceRegistry` gestisce le dipendenze tra i componenti
- La configurazione è centralizzata ma i servizi funzionano in modo autonomo
- I componenti sono disaccoppiati e comunicano attraverso interfacce ben definite

## Sicurezza

Il sistema implementa diverse misure di sicurezza:
- HTTPS per le comunicazioni client-server
- Autenticazione JWT per proteggere gli endpoint sensibili
- Crittografia BCrypt per le password
- Connessione SSL al broker MQTT
- Gestione controllata degli errori per prevenire fughe di informazioni

## Conclusioni

Il file `Main.java` rappresenta il cuore dell'applicazione, orchestrando l'inizializzazione e la configurazione di tutti i componenti del sistema. La sua struttura modulare e ben organizzata garantisce che il sistema sia facilmente manutenibile ed estensibile, mentre l'architettura a microservizi assicura scalabilità e isolamento dei componenti.

L'implementazione dimostra un'attenta progettazione che bilancia funzionalità, sicurezza e prestazioni, rispettando i requisiti di un sistema di gestione delle macchine distributrici moderno e affidabile.