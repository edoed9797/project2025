# Specifica di Dominio e Casi d'Uso

## Diagramma delle Classi del Dominio

Questo diagramma rappresenta le principali entità del sistema e le loro relazioni:

```plantuml
@startuml "Modello del Dominio"
skinparam classAttributeIconSize 0

class Istituto {
  -id: Integer
  -nome: String
  -indirizzo: String
  +getMacchine(): List<Macchina>
}

class Macchina {
  -id: Integer
  -statoId: Integer
  -cassaAttuale: Double
  -cassaMassima: Double
  -creditoAttuale: Double
  +erogaBevanda(bevandaId: Integer): Boolean
  +inserisciCredito(importo: Double): Boolean
  +restituisciCredito(): Double
  +verificaDisponibilitaBevanda(bevandaId: Integer): Boolean
}

class StatoMacchina {
  -id: Integer
  -descrizione: String
}

class Bevanda {
  -id: Integer
  -nome: String
  -prezzo: Double
  +isDisponibile(cialde: List<QuantitaCialde>): Boolean
  +getCialde(): List<Cialda>
}

class Cialda {
  -id: Integer
  -nome: String
  -tipoCialda: String
}

class QuantitaCialde {
  -id: Integer
  -cialdaId: Integer
  -macchinaId: Integer
  -quantita: Integer
  -quantitaMassima: Integer
  +necessitaRifornimento(): Boolean
  +decrementaQuantita(): void
  +rifornisci(): void
}

class Manutenzione {
  -id: Integer
  -macchinaId: Integer
  -tipoIntervento: String
  -descrizione: String
  -dataRichiesta: LocalDateTime
  -dataCompletamento: LocalDateTime
  -stato: String
  -urgenza: String
  -tecnicoId: Integer
  -note: String
  +isCompletata(): Boolean
}

class Transazione {
  -id: Integer
  -macchinaId: Integer
  -bevandaId: Integer
  -importo: Double
  -dataOra: LocalDateTime
}

class Ricavo {
  -id: Integer
  -macchinaId: Integer
  -importo: Double
  -dataOra: LocalDateTime
}

class Utente {
  -id: Integer
  -nome: String
  -ruolo: String
  -username: String
  -passwordHash: String
  +verificaPassword(password: String): Boolean
  +isAmministratore(): Boolean
  +isTecnico(): Boolean
  +isImpiegato(): Boolean
}

Istituto "1" -- "0..*" Macchina : contiene >
Macchina "1" -- "1" StatoMacchina : ha >
Macchina "1" -- "0..*" QuantitaCialde : contiene >
Macchina "1" -- "0..*" Manutenzione : richiede >
Macchina "1" -- "0..*" Transazione : registra >
Macchina "1" -- "0..*" Ricavo : genera >
Macchina "1" -- "0..*" Bevanda : eroga >
Bevanda "1" -- "1..*" Cialda : richiede >
Manutenzione "*" -- "0..1" Utente : assegnata a >
@enduml
```

### Descrizione delle Entità del Dominio
- **Istituto**: Rappresenta la scuola dove sono installate le macchine distributrici.
- **Macchina**: Rappresenta il distributore automatico di bevande calde con le sue funzionalità.
- **StatoMacchina**: Definisce lo stato operativo della macchina (Attiva, In Manutenzione, Fuori Servizio).
- **Bevanda**: Rappresenta una bevanda erogabile dalla macchina.
- **Cialda**: Rappresenta le cialde utilizzate per preparare le bevande.
- **QuantitaCialde**: Rappresenta la quantità di cialde di un certo tipo disponibili in una specifica macchina.
- **Manutenzione**: Registra le attività di manutenzione richieste o completate.
- **Transazione**: Registra ogni erogazione di bevanda effettuata.
- **Ricavo**: Registra gli importi prelevati dalle casse delle macchine.
- **Utente**: Rappresenta un utente del sistema gestionale con specifici ruoli e permessi.


## Diagramma dei Casi d'Uso
I casi d'uso sono divisi in due parti: livello del "campo" per le funzionalità delle macchine e livello "gestionale" per l'amministrazione.

### Casi d'Uso - Livello del "Campo"
```plantuml
@startuml "Casi d'Uso Livello Campo"
left to right direction
skinparam packageStyle rectangle

actor Cliente as CL
actor "Tecnico" as T
actor "Sistema IoT" as IOT

rectangle "Livello del Campo" {
  usecase "Inserire denaro" as UC1
  usecase "Selezionare bevanda" as UC2
  usecase "Recuperare credito residuo" as UC3
  usecase "Erogare bevanda" as UC4
  usecase "Monitorare livello cialde" as UC5
  usecase "Monitorare cassa" as UC6
  usecase "Segnalare guasti" as UC7
  usecase "Ricaricare cialde" as UC8
  usecase "Svuotare cassa" as UC9
  usecase "Risolvere guasti" as UC10
}

CL --> UC1
CL --> UC2
CL --> UC3
IOT --> UC4
IOT --> UC5
IOT --> UC6
IOT --> UC7
T --> UC8
T --> UC9
T --> UC10

UC2 ..> UC4 : <<include>>
UC5 ..> UC7 : <<extend>>
UC6 ..> UC7 : <<extend>>
@enduml
```


### Descrizione Casi d'Uso - Livello del "Campo"
1. **Inserire denaro**: Il cliente inserisce monete nella macchina per accumulare credito.
   - L'importo viene aggiunto al credito attuale se c'è spazio in cassa.
   - Se la cassa è vicina alla capacità massima, il denaro potrebbe essere rifiutato.

2. **Selezionare bevanda**: Il cliente sceglie una bevanda tra quelle disponibili.
   - Il sistema verifica la disponibilità delle cialde necessarie.
   - Il sistema verifica che il credito sia sufficiente.

3. **Recuperare credito residuo**: Il cliente può richiedere la restituzione del credito non utilizzato.

4. **Erogare bevanda**: Il sistema eroga la bevanda selezionata.
   - Consuma le cialde necessarie.
   - Decrementa il credito.
   - Incrementa la cassa.
   - Registra la transazione.

5. **Monitorare livello cialde**: Il sistema controlla costantemente le quantità di cialde disponibili.
   - Se una tipologia di cialda scende sotto la soglia minima, si genera una segnalazione.

6. **Monitorare cassa**: Il sistema controlla il livello di riempimento della cassa.
   - Se la cassa supera una soglia di riempimento (es. 80%), viene generata una segnalazione.

7. **Segnalare guasti**: Il sistema può segnalare guasti o esigenze di manutenzione.
   - Può essere attivato automaticamente dal monitoraggio o manualmente.

8. **Ricaricare cialde**: Il tecnico rifornisce le cialde esaurite o in esaurimento.

9. **Svuotare cassa**: Il tecnico rimuove il denaro dalla cassa quando è piena.
   - L'importo rimosso viene registrato come ricavo.

10. **Risolvere guasti**: Il tecnico interviene per risolvere i problemi tecnici segnalati.


### Casi d'Uso - Livello "Gestionale"
```plantuml
@startuml "Casi d'Uso Livello Gestionale"
left to right direction
skinparam packageStyle rectangle

actor "Amministratore" as A
actor "Impiegato" as I

rectangle "Livello Gestionale" {
  usecase "Gestire istituti" as UC11
  usecase "Gestire macchine" as UC12
  usecase "Visualizzare stato macchine" as UC13
  usecase "Gestire utenti" as UC14
  usecase "Inviare tecnici" as UC15
  usecase "Visualizzare ricavi" as UC16
  usecase "Gestire manutenzioni" as UC17
  usecase "Autenticarsi" as UC18
  usecase "Visualizzare statistiche" as UC19
  usecase "Gestire bevande" as UC20
}

A --> UC11
A --> UC12
A --> UC13
A --> UC14
A --> UC15
A --> UC16
A --> UC17
A --> UC18
A --> UC19
A --> UC20

I --> UC13
I --> UC15
I --> UC16
I --> UC17
I --> UC18
I --> UC19

UC15 ..> UC17 : <<include>>
UC11 ..> UC18 : <<include>>
UC12 ..> UC18 : <<include>>
UC13 ..> UC18 : <<include>>
UC14 ..> UC18 : <<include>>
UC15 ..> UC18 : <<include>>
UC16 ..> UC18 : <<include>>
UC17 ..> UC18 : <<include>>
UC19 ..> UC18 : <<include>>
UC20 ..> UC18 : <<include>>
@enduml
```


### Descrizione Casi d'Uso - Livello "Gestionale"
11. **Gestire istituti**: L'amministratore può aggiungere, modificare o rimuovere istituti.
    - Un istituto può essere rimosso solo se non ha macchine associate.

12. **Gestire macchine**: L'amministratore può installare, configurare o ritirare macchine.
    - Include l'assegnazione delle macchine agli istituti.
    - Include la configurazione delle bevande e cialde disponibili per ogni macchina.

13. **Visualizzare stato macchine**: Permette di monitorare lo stato di tutte le macchine.
    - Visualizza lo stato operativo (attiva, in manutenzione, fuori servizio).
    - Mostra i livelli di cialde e lo stato della cassa.

14. **Gestire utenti**: L'amministratore può creare, modificare ed eliminare account utente.
    - Assegna ruoli specifici (amministratore, impiegato, tecnico).

15. **Inviare tecnici**: Assegna interventi di manutenzione ai tecnici disponibili.
    - Include la prioritizzazione degli interventi in base all'urgenza.

16. **Visualizzare ricavi**: Permette di consultare i ricavi generati dalle macchine.
    - Filtrabile per periodo, istituto o macchina.

17. **Gestire manutenzioni**: Permette di tracciare e gestire le richieste di manutenzione.
    - Include la registrazione dell'esito degli interventi.

18. **Autenticarsi**: Tutti gli utenti devono autenticarsi per accedere al sistema gestionale.
    - Verifica credenziali e assegna i permessi in base al ruolo.

19. **Visualizzare statistiche**: Permette di analizzare dati su consumi, guasti e ricavi.
    - Include report e grafici per supportare le decisioni gestionali.

20. **Gestire bevande**: L'amministratore può configurare le bevande disponibili.
    - Include la definizione delle cialde necessarie per ogni bevanda.
    - Include l'impostazione dei prezzi.


## Diagramma di Sequenza - Erogazione Bevanda
Per illustrare meglio l'interazione tra componenti, ecco un diagramma di sequenza per l'erogazione di una bevanda:

```plantuml
@startuml "Sequenza Erogazione Bevanda"
actor Cliente
participant "Interfaccia Macchina" as UI
participant "Gestore Cassa" as GC
participant "Gestore Bevande" as GB
participant "Gestore Cialde" as GCi
participant "MQTT Bridge" as MQTT
participant "Management Service" as MS

Cliente -> UI: Inserisce denaro
UI -> GC: gestisciInserimentoMoneta(importo)
GC -> UI: Conferma inserimento e mostra credito

Cliente -> UI: Seleziona bevanda
UI -> GB: richiestaBevanda(bevandaId)
GB -> GCi: verificaDisponibilitaCialde()
GCi --> GB: cialde disponibili
GB -> GC: verificaCredito(bevanda.prezzo)
GC --> GB: credito sufficiente

GB -> GCi: consumaCialde(bevanda.cialde)
GB -> GC: processaPagamento(bevanda.prezzo)
GC --> GB: pagamento confermato

GB -> MQTT: pubblicaErogazione(bevandaId, transazioneId)
MQTT -> MS: registraTransazione()
GB -> UI: confermaErogazione()
UI -> Cliente: Eroga bevanda

@enduml
```


## Diagramma di Sequenza - Segnalazione e Intervento di Manutenzione
```plantuml
@startuml "Sequenza Manutenzione"
participant "Macchina" as M
participant "Gestore Manutenzione" as GM
participant "MQTT Bridge" as MQTT
participant "Management Service" as MS
actor "Impiegato" as I
actor "Tecnico" as T

M -> GM: segnalaProblema(tipo, descrizione)
GM -> MQTT: pubblicaSegnalazione()
MQTT -> MS: registraManutenzione()

I -> MS: visualizzaManutenzioniPendenti()
MS --> I: listaManutenzioni
I -> MS: assegnaManutenzione(tecnicoId)
MS --> T: notificaAssegnazione

T -> M: intervieneSulPosto()
T -> M: risolveProblema()
T -> GM: comunicaRisoluzione(note)
GM -> MQTT: pubblicaRisoluzione()
MQTT -> MS: aggiornaManutenzione(completata)

@enduml
```