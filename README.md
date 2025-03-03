# Sistema di Gestione Distributori Automatici di Bevande

## Descrizione del Progetto
Sistema cloud+IoT per la gestione di distributori automatici di bevande calde installati in istituti scolastici. Il progetto simula una rete di "macchinette del caffè" gestite da un'azienda fittizzia, con funzionalità sia a livello operativo (erogazione) che gestionale (amministrazione).


### Caratteristiche Principali
- **Gestione di macchine distribuite** in diversi istituti scolastici
- **Sistema a due livelli**: operativo (campo) e amministrativo (gestionale)
- **Monitoraggio remoto** dello stato di ciascuna macchina
- **Comunicazione via MQTT** tra dispositivi e sistema centrale
- **API REST** per l'interfacciamento con il frontend
- **Autenticazione utenti** con diversi livelli di permesso


## Architettura del Sistema
### Livello Operativo ("Campo")
- Simulazione di distributori automatici con capacità di:
  - Gestire l'erogazione di bevande a base di cialde
  - Monitorare livelli di scorte (cialde, bevande, ecc.)
  - Gestire transazioni economiche (inserimento denaro, resto)
  - Segnalare necessità di manutenzione o rifornimento
  - Comunicare con il sistema centrale tramite MQTT

### Livello Gestionale
- Interfaccia amministrativa per:
  - Visualizzazione stato di tutte le macchine
  - Gestione istituti e macchine (aggiunta/rimozione)
  - Invio tecnici per manutenzione
  - Monitoraggio ricavi
  - Gestione utenti (amministratori e impiegati)

### Componenti Software
1. **Backend**:
   - API REST per il livello gestionale
   - Gestisce database con informazioni su istituti, macchine, stato ecc..
   - Comunica con le macchine tramite broker MQTT

2. **Frontend**:
   - Web app per interazione con il backend
   - Interfacce diverse per amministratori e impiegati

3. **Sottosistema IoT**:
   - Componenti simulati per emulare sensori e attuatori
   - Comunicazione via MQTT con il backend
   - Microservizi indipendenti per ogni macchina


## Tecnologie Utilizzate
- **Backend**:
  - Java
  - Spark framework per API REST
  - MQTT e Paho MQTT per comunicazione con broker Mosquitto
  - Database per archiviazione dati

- **Frontend**:
  - HTML, CSS, JavaScript
  - Axios per interazioni con API REST
  - Paho MQTT per interazioni fra macchina e managment server

- **Sicurezza**:
  - Comunicazioni tramite TLS
  - Autenticazione username/password
  - JWT per gestione token di autenticazione


## Requisiti di Sviluppo
### Specifica
- Definizione dettagliata del dominio e casi d'uso
- Diagrammi UML (classi, casi d'uso) tramite PlantUML

### Progettazione
- Documentazione dettagliata delle classi da implementare
- Diagrammi UML (classi, package, sequenza)
- Definizione endpoint API REST e topic MQTT

### Implementazione
- Linguaggio principale: Java
- Possibilità di implementare microservizi in linguaggi diversi mantenendo compatibilità delle interfacce

### Testing
- Test di componenti e sottosistemi in isolamento
- Copertura di casistiche tipiche di utilizzo
- Dimostrazione finale di funzionamento

## Ambiente di Sviluppo
- Eclipse IDE come ambiente di sviluppo principale
- Broker MQTT (Mosquitto) per la comunicazione tra componenti
- Maven per la gestione delle dipendenze

## Struttura del Progetto

```
MQTT_20019309
├── .classpath
├── .project
├── pom.xml
├── README.md
├── .settings
│   └── [configurazioni Eclipse]
├── logs
├── src
│   └── main
│       ├── java
│       │   └── com
│       │       └── vending
│       │           │   config.properties
│       │           │   Main.java
│       │           │   ServiceRegistry.java
│       │           │
│       │           ├── api
│       │           │   ├── controllers
│       │           │   │   └── [controller per API REST]
│       │           │   ├── middleware
│       │           │   │   └── [middleware per autenticazione e logging]
│       │           │
│       │           ├── core
│       │           │   ├── models
│       │           │   │   └── [modelli di dominio]
│       │           │   ├── repositories
│       │           │   │   └── [accesso al database]
│       │           │   └── services
│       │           │       └── [logica di business]
│       │           │
│       │           ├── iot
│       │           │   ├── bridge
│       │           │   │   └── [connessione tra backend e mqtt]
│       │           │   ├── machines
│       │           │   │   └── [simulazione macchine]
│       │           │   ├── monitor
│       │           │   │   └── [monitoraggio stato macchine]
│       │           │   └── mqtt
│       │           │       └── [client mqtt e gestione messaggi]
│       │           │
│       │           ├── security
│       │           │   │   [utilità di sicurezza]
│       │           │   ├── auth
│       │           │   │   └── [autenticazione e autorizzazione]
│       │           │   ├── config
│       │           │   │   └── [configurazione sicurezza]
│       │           │   ├── encryption
│       │           │   │   └── [servizi di cifratura]
│       │           │   └── jwt
│       │           │       └── [gestione JWT]
│       │           │
│       │           └── utils
│       │               └── [utilità varie]
│       │
│       └── resources
│           │   config.properties
│           │   keystore.p12
│           │
│           └── public
│               │   [file HTML principali]
│               ├── css
│               │   └── [file CSS]
│               ├── img
│               │   └── [immagini]
│               ├── js
│               │   ├── admin
│               │   │   └── [script per interfaccia admin]
│               │   ├── client
│               │   │   └── [script per interfaccia cliente]
│               │   └── common
│               │       └── [script comuni]
│               └── pages
│                   ├── admin
│                   │   └── [pagine interfaccia admin]
│                   └── client
│                       └── [pagine interfaccia cliente]
└── target
```

## Note Aggiuntive
- I dispositivi IoT (macchine) sono simulati tramite processi indipendenti
- Ogni macchina è composta da diversi servizi interagenti (gestore cassa, erogazione, assistenza, frontend)
- Il codice è organizzato secondo il pattern MVC con separazione tra modelli, controller e servizi
- Il frontend è organizzato per ruoli (admin e client) con interfacce dedicate