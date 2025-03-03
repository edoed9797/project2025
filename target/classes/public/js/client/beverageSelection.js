const API_BASE_URL = 'https://localhost:8443/api';
let currentMachineId = 1;
let currentCredit = 0;
let selectedBeverage = null;
let sugarLevel = 2;

const machineNumberElement = document.getElementById('machineNumber');
const statusIndicatorElement = document.getElementById('statusIndicator');
const beveragesListElement = document.getElementById('beveragesList');
const currentCreditElement = document.getElementById('currentCredit');
const dispenseButton = document.getElementById('dispenseButton');
const returnCreditButton = document.getElementById('returnCreditButton');
const dispensingOverlay = document.getElementById('dispensingOverlay');
const dispensingStatusElement = document.getElementById('dispensingStatus');

async function fetchMachineInfo(machineId) {
    try {
        const response = await axios.get(`${API_BASE_URL}/macchine/${machineId}`);
        const machine = response.data;
        machineNumberElement.textContent = `#${machine.id}`;
        updateMachineStatus(machine.statoId);
        fetchBeverages(machineId);
    } catch (error) {
        console.error('Errore nel recupero delle informazioni della macchina:', error);
        alert('Impossibile caricare le informazioni della macchina.');
    }
}

function updateMachineStatus(statusId) {
    const statusMap = {
        1: { text: 'Attiva', class: 'status-active' },
        2: { text: 'Manutenzione', class: 'status-maintenance' },
        3: { text: 'Fuori Servizio', class: 'status-error' },
        default: { text: 'Sconosciuto', class: 'status-unknown' }
    };

    const status = statusMap[statusId] || statusMap.default;
    statusIndicatorElement.className = `status-indicator ${status.class}`;
    statusIndicatorElement.querySelector('.status-text').textContent = status.text;
}

async function fetchBeverages(machineId) {
    try {
        const response = await axios.get(`${API_BASE_URL}/macchine/${machineId}/bevande`);
        const beverages = response.data;
        renderBeverages(beverages);
    } catch (error) {
        console.error('Errore nel recupero delle bevande:', error);
        alert('Impossibile caricare le bevande disponibili.');
    }
}

function renderBeverages(beverages) {
    beveragesListElement.innerHTML = beverages.map(beverage => `
        <div class="beverage-card ${beverage.disponibile ? '' : 'unavailable'}" 
             data-id="${beverage.id}" 
             data-price="${beverage.prezzo}" 
             onclick="selectBeverage(${beverage.id}, ${beverage.prezzo}, ${beverage.disponibile})">
            <h3 class="font-bold">${beverage.nome}</h3>
            <p class="text-gray-600">€${beverage.prezzo.toFixed(2)}</p>
            ${beverage.disponibile ? '' : '<p class="text-red-600">Esaurito</p>'}
        </div>
    `).join('');
}

function selectBeverage(beverageId, price, isAvailable) {
    if (!isAvailable) return;
    selectedBeverage = { id: beverageId, price };
    document.querySelectorAll('.beverage-card').forEach(card => {
        card.classList.remove('selected');
    });
    document.querySelector(`.beverage-card[data-id="${beverageId}"]`).classList.add('selected');
    dispenseButton.disabled = currentCredit < price;
}

document.querySelectorAll('.coin-btn').forEach(button => {
    button.addEventListener('click', () => {
        const value = parseFloat(button.getAttribute('data-value'));
        currentCredit += value;
        currentCreditElement.textContent = currentCredit.toFixed(2);
        if (selectedBeverage && currentCredit >= selectedBeverage.price) {
            dispenseButton.disabled = false;
        }
    });
});

dispenseButton.addEventListener('click', async () => {
    if (!selectedBeverage || currentCredit < selectedBeverage.price) return;
    dispensingOverlay.classList.remove('hidden');
    dispensingStatusElement.textContent = 'Preparazione bevanda...';
    const topic = `macchine/${currentMachineId}/bevande/richiesta`;
    const message = {
        bevandaId: selectedBeverage.id,
        importo: selectedBeverage.price,
        zucchero: sugarLevel,
        timestamp: Date.now()
    };
    try {
        await mqttClient.publish(topic, JSON.stringify(message));
        console.log('Richiesta di erogazione inviata:', message);
    } catch (error) {
        console.error('Errore durante l\'invio della richiesta di erogazione:', error);
        alert('Errore durante l\'invio della richiesta di erogazione.');
    }
});

mqttClient.subscribe(`macchine/${currentMachineId}/stato`, (topic, payload) => {
    const status = JSON.parse(payload);
    updateMachineStatus(status.statoId);
});

mqttClient.subscribe(`macchine/${currentMachineId}/bevande/erogazione`, (topic, payload) => {
    const result = JSON.parse(payload);
    if (result.success) {
        dispensingStatusElement.textContent = 'Bevanda erogata con successo!';
        currentCredit = 0;
        currentCreditElement.textContent = '0.00';
        selectedBeverage = null;
        setTimeout(() => dispensingOverlay.classList.add('hidden'), 2000);
    } else {
        dispensingStatusElement.textContent = 'Errore durante l\'erogazione.';
    }
});

returnCreditButton.addEventListener('click', async () => {
    const topic = `macchine/${currentMachineId}/cassa/restituzione`;
    try {
        await mqttClient.publish(topic, JSON.stringify({}));
        currentCredit = 0;
        currentCreditElement.textContent = '0.00';
        alert('Credito restituito con successo.');
    } catch (error) {
        console.error('Errore durante la restituzione del credito:', error);
        alert('Errore durante la restituzione del credito.');
    }
});

document.addEventListener('DOMContentLoaded', () => {
    fetchMachineInfo(currentMachineId);
    mqttClient.connect();
});