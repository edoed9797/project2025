# Possibili Scenari di Test:

Ecco una serie di scenari di test completi per verificare il corretto funzionamento del sistema.

## 1. Livello del Campo (Macchine)

### 1.1 Gestione Credito e Pagamenti
- **Inserimento credito**: Verifica che un utente possa inserire correttamente monete di diversi tagli.
- **Rifiuto credito eccedente**: Verifica che la macchina rifiuti il denaro quando la cassa è quasi piena.
- **Restituzione credito**: Verifica che premendo il pulsante "restituisci credito" il credito venga restituito correttamente.
- **Credito insufficiente**: Verifica che la macchina non eroghi bevande se il credito è insufficiente.

### 1.2 Erogazione Bevande
- **Erogazione bevanda semplice**: Verifica l'erogazione corretta di una bevanda che utilizza una sola cialda.
- **Erogazione bevanda complessa**: Verifica l'erogazione di bevande che utilizzano più cialde (es. caffè macchiato).
- **Erogazione con zucchero**: Verifica la corretta gestione dei livelli di zucchero.
- **Erogazione mancata per cialde esaurite**: Verifica che la macchina non eroghi quando le cialde sono esaurite.

### 1.3 Gestione Cialde
- **Monitoraggio cialde**: Verifica che il sistema tracci correttamente il livello di cialde disponibili.
- **Esaurimento cialde**: Verifica che la macchina segnali quando una tipologia di cialda è quasi esaurita.
- **Rifornimento cialde**: Verifica che la funzione di rifornimento cialde aggiorni correttamente i contatori.

### 1.4 Segnalazione Guasti
- **Segnalazione guasto generico**: Verifica che la macchina possa segnalare correttamente un guasto.
- **Stato fuori servizio**: Verifica che la macchina passi allo stato "fuori servizio" quando necessario.
- **Ripristino dopo guasto**: Verifica il corretto ripristino del funzionamento dopo la riparazione.

### 1.5 Comunicazione MQTT
- **Pubblicazione stato**: Verifica che la macchina pubblichi regolarmente il suo stato su MQTT.
- **Sottoscrizione a comandi**: Verifica che la macchina risponda correttamente ai comandi inviati tramite MQTT.
- **Gestione disconnessione**: Verifica il comportamento della macchina in caso di disconnessione dal broker MQTT.

## 2. Livello Gestionale

### 2.1 Autenticazione e Sicurezza
- **Login corretto**: Verifica che un utente possa autenticarsi con credenziali valide.
- **Login errato**: Verifica che venga rifiutato l'accesso con credenziali non valide.
- **Autorizzazione per ruolo**: Verifica che le autorizzazioni varino in base al ruolo (amministratore vs impiegato).
- **Scadenza token JWT**: Verifica che i token scaduti vengano rifiutati e sia necessario riautenticarsi.
- **Refresh token**: Verifica il funzionamento del rinnovo del token senza necessità di riautenticazione.

### 2.2 Gestione Istituti
- **Creazione istituto**: Verifica la creazione di un nuovo istituto.
- **Visualizzazione istituti**: Verifica la visualizzazione dell'elenco degli istituti.
- **Modifica istituto**: Verifica l'aggiornamento dei dati di un istituto.
- **Eliminazione istituto**: Verifica che un istituto possa essere eliminato solo se privo di macchine.

### 2.3 Gestione Macchine
- **Installazione macchina**: Verifica l'aggiunta di una nuova macchina a un istituto.
- **Visualizzazione macchine**: Verifica la visualizzazione dell'elenco macchine per istituto o globale.
- **Stato macchina**: Verifica la corretta visualizzazione dello stato di ogni macchina.
- **Rimozione macchina**: Verifica che una macchina possa essere rimossa da un istituto.

### 2.4 Gestione Manutenzione
- **Segnalazione problemi**: Verifica che i problemi vengano segnalati correttamente sul pannello di gestione.
- **Invio tecnico**: Verifica il processo di assegnazione di un tecnico a un problema.
- **Risoluzione problema**: Verifica il processo di completamento di una manutenzione.
- **Storico manutenzioni**: Verifica la visualizzazione dello storico delle manutenzioni per macchina.

### 2.5 Gestione Rifornimenti
- **Segnalazione necessità cialde**: Verifica che il sistema segnali quando una macchina necessita di rifornimento.
- **Assegnazione rifornimento**: Verifica il processo di assegnazione del rifornimento a un tecnico.
- **Conferma rifornimento**: Verifica la procedura di conferma dell'avvenuto rifornimento.

### 2.6 Gestione Ricavi
- **Visualizzazione ricavi**: Verifica la visualizzazione dei ricavi per macchina e per istituto.
- **Svuotamento cassa**: Verifica il processo di registrazione dello svuotamento cassa.
- **Report periodici**: Verifica la generazione di report sui ricavi per periodi definiti.

### 2.7 Gestione Bevande
- **Creazione bevanda**: Verifica l'aggiunta di una nuova bevanda al catalogo.
- **Modifica bevanda**: Verifica la modifica di una bevanda esistente.
- **Associazione cialde**: Verifica l'associazione corretta delle cialde a una bevanda.
- **Prezzo bevanda**: Verifica la corretta gestione dei prezzi delle bevande.

## 3. Test di Integrazione

### 3.1 Flusso Completo Client-Server
- **Erogazione e registrazione**: Verifica che l'erogazione di una bevanda venga correttamente registrata nel database.
- **Segnalazione e intervento**: Verifica il flusso completo dalla segnalazione di un guasto fino alla sua risoluzione.
- **Monitoraggio in tempo reale**: Verifica che il pannello di controllo mostri in tempo reale lo stato delle macchine.

### 3.2 Scalabilità
- **Gestione multiple macchine**: Verifica che il sistema gestisca correttamente numerose macchine simultaneamente.
- **Carico elevato**: Verifica il comportamento del sistema in condizioni di carico elevato (molte transazioni).

### 3.3 Resilienza
- **Perdita connessione**: Verifica il comportamento delle macchine in caso di perdita di connessione con il server.
- **Ripristino dopo crash**: Verifica il comportamento del sistema dopo un crash del server o di una macchina.
- **Consistenza dati**: Verifica che non ci siano perdite di dati in caso di problemi di connessione.

## 4. Test di Sicurezza

### 4.1 Protezione Endpoint
- **Accesso non autorizzato**: Verifica che gli endpoint protetti rifiutino accessi senza autenticazione.
- **Separazione privilegi**: Verifica che un impiegato non possa accedere a funzionalità riservate agli amministratori.

### 4.2 Sicurezza Dati
- **Protezione password**: Verifica che le password siano adeguatamente hash-ate nel database.
- **Cifratura comunicazioni**: Verifica che tutte le comunicazioni avvengano tramite TLS.
- **Validazione input**: Verifica la resistenza a injection SQL e XSS nei form di input.

## 5. Test di Interfaccia Utente

### 5.1 Dashboard Amministrativa
- **Responsività**: Verifica che l'interfaccia sia utilizzabile su diversi dispositivi.
- **Navigazione**: Verifica la facilità di navigazione tra le diverse sezioni.
- **Visualizzazione dati**: Verifica che i dati vengano presentati in modo chiaro e comprensibile.

### 5.2 Interfaccia Macchina
- **Usabilità**: Verifica che l'interfaccia della macchina sia intuitiva per gli utenti finali.
- **Feedback visivo**: Verifica che la macchina fornisca feedback adeguati durante le operazioni.
- **Gestione errori**: Verifica che i messaggi di errore siano chiari e informativi.