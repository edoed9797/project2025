/**
 * Modulo per la gestione delle comunicazioni MQTT del sistema
 * Utilizza Paho MQTT Client per la comunicazione
 */

class MQTTManager {
    constructor(config = {}) {
        // Configurazione di default
        this.config = {
            host: config.host || 'localhost',
            port: config.port || 8883,
            path: config.path || '/mqtt',
            clientId: config.clientId || 'client_' + Math.random().toString(16).substr(2, 8),
            username: config.username || '20019309',
            password: config.password || 'Pissir2024!',
            useSSL: config.useSSL !== undefined ? config.useSSL : true,
            keepaliveInterval: config.keepaliveInterval || 30,
            connectTimeout: config.connectTimeout || 10,
            reconnectDelay: config.reconnectDelay || 5000,
            maxReconnectAttempts: config.maxReconnectAttempts || 5
        };

        // Stato della connessione
        this.connected = false;
        this.reconnectCount = 0;
        this.client = null;

        // Gestione topic e callback
        this.subscriptions = new Map();
        this.messageHandlers = new Map();
        this.errorHandlers = new Set();
        this.connectionHandlers = new Set();

        // Buffer messaggi offline
        this.offlineMessageQueue = [];

        // Bind dei metodi
        this._handleConnect = this._handleConnect.bind(this);
        this._handleLostConnection = this._handleLostConnection.bind(this);
        this._handleMessage = this._handleMessage.bind(this);
        this._handleError = this._handleError.bind(this);
    }

    /**
     * Inizializza la connessione MQTT
     * @returns {Promise} Promise che si risolve quando la connessione è stabilita
     */
    async connect() {
        return new Promise((resolve, reject) => {
            try {
                // Crea un nuovo client MQTT
                this.client = new Paho.MQTT.Client(
                    this.config.host,
                    this.config.port,
                    this.config.path,
                    this.config.clientId
                );

                // Configura i callback
                this.client.onConnectionLost = this._handleLostConnection;
                this.client.onMessageArrived = this._handleMessage;

                // Opzioni di connessione
                const options = {
                    useSSL: this.config.useSSL,
                    userName: this.config.username,
                    password: this.config.password,
                    keepAliveInterval: this.config.keepaliveInterval,
                    connectTimeout: this.config.connectTimeout,
                    onSuccess: () => {
                        this._handleConnect();
                        resolve();
                    },
                    onFailure: (error) => {
                        this._handleError(error);
                        reject(error);
                    }
                };

                // Connetti al broker
                this.client.connect(options);

            } catch (error) {
                reject(error);
            }
        });
    }

    /**
     * Gestisce la connessione stabilita
     * @private
     */
    _handleConnect() {
        this.connected = true;
        this.reconnectCount = 0;
        console.log('Connessione MQTT stabilita');

        // Ripristina le sottoscrizioni
        this.subscriptions.forEach((qos, topic) => {
            this._resubscribe(topic, qos);
        });

        // Invia i messaggi in coda
        while (this.offlineMessageQueue.length > 0) {
            const { topic, message, options } = this.offlineMessageQueue.shift();
            this.publish(topic, message, options);
        }

        // Notifica i listener
        this.connectionHandlers.forEach(handler => {
            handler({ connected: true });
        });
    }

    /**
     * Gestisce la perdita di connessione
     * @private
     * @param {Object} responseObject Oggetto di risposta con i dettagli della disconnessione
     */
    _handleLostConnection(responseObject) {
        this.connected = false;
        console.warn('Connessione MQTT persa:', responseObject.errorMessage);

        // Notifica i listener
        this.connectionHandlers.forEach(handler => {
            handler({ connected: false, error: responseObject });
        });

        // Tenta la riconnessione se non è una disconnessione volontaria
        if (responseObject.errorCode !== 0) {
            this._attemptReconnect();
        }
    }

    /**
     * Tenta la riconnessione al broker
     * @private
     */
    async _attemptReconnect() {
        if (this.reconnectCount >= this.config.maxReconnectAttempts) {
            console.error('Numero massimo di tentativi di riconnessione raggiunto');
            return;
        }

        this.reconnectCount++;
        console.log(`Tentativo di riconnessione ${this.reconnectCount}/${this.config.maxReconnectAttempts}`);

        try {
            await new Promise(resolve => setTimeout(resolve, this.config.reconnectDelay));
            await this.connect();
        } catch (error) {
            console.error('Tentativo di riconnessione fallito:', error);
            this._attemptReconnect();
        }
    }

    /**
     * Gestisce i messaggi in arrivo
     * @private
     * @param {Paho.MQTT.Message} message Messaggio MQTT ricevuto
     */
    _handleMessage(message) {
        const topic = message.destinationName;
        const payload = message.payloadString;

        // Cerca handler specifici per il topic
        this.messageHandlers.forEach((handler, pattern) => {
            if (this._matchTopic(topic, pattern)) {
                try {
                    handler(topic, payload);
                } catch (error) {
                    console.error(`Errore nell'handler del topic ${topic}:`, error);
                }
            }
        });
    }

    /**
     * Verifica se un topic corrisponde a un pattern
     * @private
     * @param {string} topic Topic da verificare
     * @param {string} pattern Pattern con cui confrontare
     * @returns {boolean} true se il topic corrisponde al pattern
     */
    _matchTopic(topic, pattern) {
        const topicParts = topic.split('/');
        const patternParts = pattern.split('/');

        if (topicParts.length !== patternParts.length) {
            return false;
        }

        return patternParts.every((part, i) => {
            return part === '+' || part === '#' || part === topicParts[i];
        });
    }

    /**
     * Gestisce gli errori
     * @private
     * @param {Error} error Errore da gestire
     */
    _handleError(error) {
        console.error('Errore MQTT:', error);
        this.errorHandlers.forEach(handler => {
            handler(error);
        });
    }

    /**
     * Sottoscrive a un topic
     * @param {string} topic Topic da sottoscrivere
     * @param {Function} handler Handler per i messaggi
     * @param {Object} options Opzioni di sottoscrizione
     * @returns {Promise} Promise che si risolve alla sottoscrizione completata
     */
    async subscribe(topic, handler, options = { qos: 0 }) {
        return new Promise((resolve, reject) => {
            try {
                this.subscriptions.set(topic, options.qos);
                this.messageHandlers.set(topic, handler);

                if (this.connected) {
                    this.client.subscribe(topic, {
                        qos: options.qos,
                        onSuccess: () => {
                            console.log(`Sottoscritto al topic: ${topic}`);
                            resolve();
                        },
                        onFailure: (error) => {
                            console.error(`Errore sottoscrizione al topic ${topic}:`, error);
                            reject(error);
                        }
                    });
                } else {
                    resolve(); // Verrà sottoscritto alla riconnessione
                }
            } catch (error) {
                reject(error);
            }
        });
    }

    /**
     * Ripristina una sottoscrizione
     * @private
     * @param {string} topic Topic da risottoscrivere
     * @param {number} qos QoS della sottoscrizione
     */
    _resubscribe(topic, qos) {
        this.client.subscribe(topic, {
            qos,
            onSuccess: () => console.log(`Risottoscritto al topic: ${topic}`),
            onFailure: (error) => console.error(`Errore risottoscrizione al topic ${topic}:`, error)
        });
    }

    /**
     * Pubblica un messaggio
     * @param {string} topic Topic su cui pubblicare
     * @param {string|Object} message Messaggio da pubblicare
     * @param {Object} options Opzioni di pubblicazione
     * @returns {Promise} Promise che si risolve alla pubblicazione completata
     */
    async publish(topic, message, options = { qos: 0, retained: false }) {
        return new Promise((resolve, reject) => {
            try {
                const payload = typeof message === 'string' ? message : JSON.stringify(message);
                const mqttMessage = new Paho.MQTT.Message(payload);
                mqttMessage.destinationName = topic;
                mqttMessage.qos = options.qos;
                mqttMessage.retained = options.retained;

                if (this.connected) {
                    this.client.send(mqttMessage);
                    resolve();
                } else {
                    // Salva il messaggio per l'invio alla riconnessione
                    this.offlineMessageQueue.push({ topic, message: payload, options });
                    resolve();
                }
            } catch (error) {
                reject(error);
            }
        });
    }

    /**
     * Cancella la sottoscrizione a un topic
     * @param {string} topic Topic da cui cancellare la sottoscrizione
     * @returns {Promise} Promise che si risolve alla cancellazione completata
     */
    async unsubscribe(topic) {
        return new Promise((resolve, reject) => {
            try {
                if (this.connected) {
                    this.client.unsubscribe(topic, {
                        onSuccess: () => {
                            this.subscriptions.delete(topic);
                            this.messageHandlers.delete(topic);
                            console.log(`Sottoscrizione cancellata per il topic: ${topic}`);
                            resolve();
                        },
                        onFailure: reject
                    });
                } else {
                    this.subscriptions.delete(topic);
                    this.messageHandlers.delete(topic);
                    resolve();
                }
            } catch (error) {
                reject(error);
            }
        });
    }

    /**
     * Aggiunge un handler per gli eventi di connessione
     * @param {Function} handler Handler da aggiungere
     */
    onConnection(handler) {
        this.connectionHandlers.add(handler);
    }

    /**
     * Rimuove un handler per gli eventi di connessione
     * @param {Function} handler Handler da rimuovere
     */
    offConnection(handler) {
        this.connectionHandlers.delete(handler);
    }

    /**
     * Aggiunge un handler per gli errori
     * @param {Function} handler Handler da aggiungere
     */
    onError(handler) {
        this.errorHandlers.add(handler);
    }

    /**
     * Rimuove un handler per gli errori
     * @param {Function} handler Handler da rimuovere
     */
    offError(handler) {
        this.errorHandlers.delete(handler);
    }

    /**
     * Disconnette dal broker MQTT
     */
    disconnect() {
        if (this.connected) {
            this.client.disconnect();
            this.connected = false;
        }
    }
}

// Crea un'istanza del gestore MQTT per l'uso comune
const mqttClient = new MQTTManager({
    host: 'localhost',
    port: 8883,
    useSSL: true,
    username: '20019309',
    password: 'Pissir2024!'
});

// Esporta sia la classe che l'istanza singleton
export { MQTTManager, mqttClient };
