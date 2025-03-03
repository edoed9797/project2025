/**
 * Gestione Pagamenti
 * Gestisce l'interfaccia di pagamento e le transazioni
 */

import mqttClient from '../common/mqtt.js';
import Utils from '../common/utils.js';

class PaymentManager {
    constructor() {
        // Recupera ID macchina
        this.machineId = new URLSearchParams(window.location.search).get('machine');

        // Elementi DOM
        this.elements = {
            currentCredit: document.getElementById('currentCredit'),
            productPrice: document.getElementById('productPrice'),
            coinButtons: document.querySelectorAll('.coin-button'),
            dispenseButton: document.getElementById('dispenseButton'),
            returnCreditButton: document.getElementById('returnCreditButton')
        };

        // Stato
        this.state = {
            credit: 0,
            selectedProduct: null,
            customization: null,
            transactionInProgress: false
        };

        // Inizializzazione
        this.initialize();
    }

    /**
     * Inizializza il gestore pagamenti
     */
    async initialize() {
        try {
            // Sottoscrivi agli aggiornamenti MQTT
            await this.subscribeMQTTTopics();

            // Inizializza gli event listeners
            this.initializeEventListeners();

            console.log('Gestore pagamenti inizializzato');
        } catch (error) {
            console.error('Errore inizializzazione:', error);
            Utils.showToast('Errore durante l\'inizializzazione del pagamento', 'error');
        }
    }

    /**
     * Sottoscrivi ai topic MQTT
     */
    async subscribeMQTTTopics() {
        try {
            // Conferma inserimento monete
            await mqttClient.subscribe(`machines/${this.machineId}/payment/coin-accepted`, (topic, message) => {
                this.handleCoinAccepted(JSON.parse(message));
            });

            // Stato transazione
            await mqttClient.subscribe(`machines/${this.machineId}/payment/transaction-status`, (topic, message) => {
                this.handleTransactionStatus(JSON.parse(message));
            });

        } catch (error) {
            console.error('Errore sottoscrizione MQTT:', error);
            throw error;
        }
    }

    /**
     * Inizializza i listener degli eventi
     */
    initializeEventListeners() {
        // Pulsanti monete
        this.elements.coinButtons.forEach(button => {
            button.addEventListener('click', () => {
                if (this.state.transactionInProgress) return;
                const value = parseFloat(button.dataset.value);
                this.insertCoin(value);
            });
        });

        // Pulsante erogazione
        this.elements.dispenseButton.addEventListener('click', () => {
            if (this.canDispense()) {
                this.startTransaction();
            }
        });

        // Pulsante restituzione credito
        this.elements.returnCreditButton.addEventListener('click', () => {
            if (!this.state.transactionInProgress && this.state.credit > 0) {
                this.returnCredit();
            }
        });

        // Eventi selezione prodotto
        window.addEventListener('beverageSelected', (e) => {
            this.handleProductSelected(e.detail.beverage);
        });

        window.addEventListener('customizationChanged', (e) => {
            this.handleCustomizationChanged(e.detail);
        });

        window.addEventListener('selectionReset', () => {
            this.resetPayment();
        });
    }

    /**
     * Gestisce l'accettazione di una moneta
     */
    handleCoinAccepted(data) {
        if (data.accepted) {
            this.state.credit += data.value;
            this.updateCreditDisplay();
            Utils.showToast(`Moneta da €${data.value.toFixed(2)} accettata`, 'success');
        } else {
            Utils.showToast('Moneta non accettata', 'error');
        }
    }

    /**
     * Gestisce gli aggiornamenti di stato della transazione
     */
    handleTransactionStatus(status) {
        switch (status.state) {
            case 'completed':
                this.state.transactionInProgress = false;
                this.state.credit = 0;
                this.updateUI();
                break;

            case 'failed':
                this.state.transactionInProgress = false;
                Utils.showToast('Transazione fallita: ' + status.message, 'error');
                this.updateUI();
                break;

            case 'processing':
                // Aggiorna UI con stato processamento
                break;
        }
    }

    /**
     * Gestisce la selezione di un prodotto
     */
    handleProductSelected(product) {
        this.state.selectedProduct = product;
        this.updateUI();
    }

    /**
     * Gestisce il cambio delle personalizzazioni
     */
    handleCustomizationChanged(data) {
        this.state.selectedProduct = data.beverage;
        this.state.customization = data.customization;
        this.updateUI();
    }

    /**
     * Inserisce una moneta
     */
    async insertCoin(value) {
        try {
            await mqttClient.publish(
                `machines/${this.machineId}/payment/insert-coin`,
                JSON.stringify({ value })
            );
        } catch (error) {
            console.error('Errore inserimento moneta:', error);
            Utils.showToast('Errore durante l\'inserimento della moneta', 'error');
        }
    }

    /**
     * Avvia una transazione
     */
    async startTransaction() {
        try {
            this.state.transactionInProgress = true;
            this.updateUI();

            const transactionData = {
                productId: this.state.selectedProduct.id,
                customization: this.state.customization,
                amount: this.calculateTotal(),
                credit: this.state.credit
            };

            await mqttClient.publish(
                `machines/${this.machineId}/payment/start-transaction`,
                JSON.stringify(transactionData)
            );

            // Avvia l'erogazione
            window.startDispensing();

        } catch (error) {
            console.error('Errore avvio transazione:', error);
            Utils.showToast('Errore durante l\'avvio della transazione', 'error');
            this.state.transactionInProgress = false;
            this.updateUI();
        }
    }

    /**
     * Restituisce il credito residuo
     */
    async returnCredit() {
        try {
            await mqttClient.publish(
                `machines/${this.machineId}/payment/return-credit`,
                JSON.stringify({ amount: this.state.credit })
            );

            Utils.showToast(`Credito di €${this.state.credit.toFixed(2)} restituito`, 'success');
            this.state.credit = 0;
            this.updateUI();

        } catch (error) {
            console.error('Errore restituzione credito:', error);
            Utils.showToast('Errore durante la restituzione del credito', 'error');
        }
    }

    /**
     * Calcola il totale con extra
     */
    calculateTotal() {
        let total = this.state.selectedProduct.price;

        // Aggiungi prezzo extra
        if (this.state.customization && this.state.customization.extras) {
            this.state.customization.extras.forEach(extraId => {
                const extra = this.state.selectedProduct.extras.find(e => e.id === extraId);
                if (extra) {
                    total += extra.price;
                }
            });
        }

        return total;
    }

    /**
     * Verifica se è possibile erogare
     */
    canDispense() {
        return !this.state.transactionInProgress &&
               this.state.selectedProduct &&
               this.state.credit >= this.calculateTotal();
    }

    /**
     * Aggiorna la visualizzazione del credito
     */
    updateCreditDisplay() {
        this.elements.currentCredit.textContent = Utils.formatCurrency(this.state.credit);
    }

    /**
     * Aggiorna l'interfaccia utente
     */
    updateUI() {
        // Aggiorna display credito
        this.updateCreditDisplay();

        // Aggiorna prezzo prodotto
        if (this.state.selectedProduct) {
            const total = this.calculateTotal();
            this.elements.productPrice.textContent = total.toFixed(2);
        } else {
            this.elements.productPrice.textContent = '0.00';
        }

        // Aggiorna stato pulsanti
        this.elements.dispenseButton.disabled = !this.canDispense();
        this.elements.returnCreditButton.disabled = 
            this.state.transactionInProgress || this.state.credit <= 0;

        // Aggiorna stato monete
        this.elements.coinButtons.forEach(button => {
            button.disabled = this.state.transactionInProgress;
        });
    }

    /**
     * Resetta lo stato del pagamento
     */
    resetPayment() {
        if (this.state.credit > 0) {
            this.returnCredit();
        }

        this.state.selectedProduct = null;
        this.state.customization = null;
        this.updateUI();
    }

    /**
     * Pulisce e libera risorse
     */
    destroy() {
        // Cancella sottoscrizioni MQTT
        mqttClient.unsubscribe(`machines/${this.machineId}/payment/coin-accepted`);
        mqttClient.unsubscribe(`machines/${this.machineId}/payment/transaction-status`);
    }
}

// Inizializza il gestore quando il DOM è pronto
document.addEventListener('DOMContentLoaded', () => {
    const paymentManager = new PaymentManager();

    // Gestione pulizia quando si lascia la pagina
    window.addEventListener('unload', () => {
        paymentManager.destroy();
    });
});

export default PaymentManager;