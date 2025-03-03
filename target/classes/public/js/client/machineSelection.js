

import Utils from '../common/utils.js';

class MachineSelectionManager {
    constructor() {
        this.elements = {
            searchInput: document.getElementById('searchMachine'),
            filterButtons: document.querySelectorAll('.filter-btn'),
            machinesList: document.getElementById('machinesList'),
            statusIndicator: document.getElementById('statusIndicator'),
            institutesList: document.getElementById('institutesList')
        };

        this.state = {
            machines: new Map(),
            institutes: new Map(),
            currentFilter: 'all'
        };

        this.initialize();
    }

    async initialize() {
        try {
            Utils.toggleLoading(true);
            await this.ensureAuthToken();

            // Carica prima gli istituti, poi le macchine
            await this.loadInstitutes();
            await this.loadMachines();

            // Renderizza le macchine e gli istituti
            this.renderMachines();
            this.renderInstitutes();
        } catch (error) {
            console.error('Initialization error:', error);
            Utils.showToast('Error during initialization. Please try again.', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    async ensureAuthToken() {
        let token = localStorage.getItem('jwt_token');
        if (!token) {
            token = this.generateAnonymousToken();
            localStorage.setItem('jwt_token', token);
            localStorage.setItem('userRole', 'anonymous');
        }
        return token;
    }

    generateAnonymousToken() {
        const random = Math.random().toString(36).substring(2);
        const timestamp = Date.now().toString(36);
        return `anonymous_${random}_${timestamp}`;
    }

    async loadMachines() {
        try {
            const response = await fetch('/api/macchine', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${await this.ensureAuthToken()}`
                }
            });
            
            if (!response.ok) {
                localStorage.removeItem('authToken');
                const newToken = await this.ensureAuthToken();
                
                const retryResponse = await fetch('/api/macchine', {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${newToken}`
                    }
                });
                
                if (!retryResponse.ok) {
                    throw new Error(`HTTP error! status: ${retryResponse.status}`);
                }
                
                const machines = await retryResponse.json();
                machines.forEach(machine => {
                    this.state.machines.set(machine.id, machine);
                });
            } else {
                const machines = await response.json();
                machines.forEach(machine => {
                    this.state.machines.set(machine.id, machine);
                });
            }
            
        } catch (error) {
            console.error('Failed to load machines:', error);
            Utils.showToast('Error loading machines', 'error');
        }
    }

    async loadInstitutes() {
        try {
            const response = await fetch('/api/istituti', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${await this.ensureAuthToken()}`
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const institutes = await response.json();
            institutes.forEach(institute => {
                this.state.institutes.set(institute.ID_istituto, institute);
            });

        } catch (error) {
            console.error('Failed to load institutes:', error);
            Utils.showToast('Error loading institutes', 'error');
        }
    }

    updateMachineStatus(machineId, status) {
        const machine = this.state.machines.get(machineId);
        if (!machine) return;

        machine.statoDescrizione = status.statoDescrizione;
        this.updateMachineUI(machine);
    }

    updateMachineUI(machine) {
        const card = document.getElementById(`machine-${machine.id}`);
        if (!card) return;

        const status = this.getMachineStatus(machine.statoDescrizione);
        
        // Aggiorna il pallino dello stato
        card.querySelector('.status-badge')
            .className = `status-badge ${status.class}`;
        
        // Aggiorna il testo dello stato
        card.querySelector('.status-text').textContent = status.text;

        // Aggiorna il pulsante
        const button = card.querySelector('.btn-select');
        if (machine.statoDescrizione === 'Attiva') {
            button.removeAttribute('disabled');
            button.textContent = 'Seleziona';
        } else {
            button.setAttribute('disabled', 'disabled');
            button.textContent = 'Non disponibile';
        }
    }

    renderMachines(machines = Array.from(this.state.machines.values())) {
        if (!this.elements.machinesList) return;

        this.elements.machinesList.innerHTML = machines
            .map(machine => {
                const institute = this.getInstituteForMachine(machine);
                return this.renderMachineCard(machine, institute);
            })
            .join('');
    }

    renderMachineCard(machine, institute) {
        const status = this.getMachineStatus(machine.statoDescrizione);
        
        // Se l'istituto non è trovato, usa valori di default
        const instituteName = institute ? institute.nome : 'N/A';
        const instituteAddress = institute ? institute.indirizzo : 'N/A';

        return `
            <div id="machine-${machine.id}" class="machine-card">
                <div class="card-header">
                    <h3>Distributore #${machine.id}</h3>
                    <span class="status-badge ${status.class}"></span>
                </div>
                <div class="card-body">
                    <p class="institute"><i class="fas fa-building"></i> ${instituteName}</p>
                    <p class="status-text">${status.text}</p>
                    <p class="address"><i class="fa fa-map-marker" aria-hidden="true"></i> ${instituteAddress}</p>
                    ${machine.statoDescrizione === 'Attiva' ? `
                        <button class="btn-select" onclick="window.selectMachine(${machine.id})">
                            Seleziona
                        </button>
                    ` : `
                        <button disabled>
                            Non disponibile
                        </button>
                    `}
                </div>
            </div>
        `;
    }

    getInstituteForMachine(machine) {
        // Verifica che machine.istitutoId sia definito e sia una chiave valida nel Map degli istituti
        if (machine.istitutoId && this.state.institutes.has(machine.istitutoId)) {
            return this.state.institutes.get(machine.istitutoId);
        }
        return null; // Restituisci null se l'istituto non è trovato
    }

    renderInstitutes() {
        if (!this.elements.institutesList) {
            console.error('Elemento institutesList non trovato!');
            return;
        }

        this.elements.institutesList.innerHTML = Array.from(this.state.institutes.values())
            .map(institute => this.renderInstituteCard(institute))
            .join('');
    }

    renderInstituteCard(institute) {
        const machinesCount = Array.from(this.state.machines.values())
            .filter(machine => machine.istitutoId === institute.ID_istituto && machine.statoDescrizione === 'Attiva')
            .length;

        return `
            <div class="institute-card">
                <div class="card-header">
                    <h3>${institute.nome}</h3>
                </div>
                <div class="card-body">
                    <p class="address"><i class="fa fa-map-marker" aria-hidden="true"></i> ${institute.indirizzo}</p>
                    <p class="machines-count"><i class="fas fa-coffee"></i> Macchine disponibili: ${machinesCount}</p>
                </div>
            </div>
        `;
    }

    selectMachine(machineId) {
        const machine = this.state.machines.get(machineId);
        if (!machine || machine.statoDescrizione !== 'Attiva') return;

        sessionStorage.setItem('selectedMachine', machineId);
        window.location.href = `/pages/client/beverageInterface.html?machine=${machineId}`;
    }

    getMachineStatus(statoDescrizione) {
        const statusMap = {
            'Attiva': { 
                class: 'status-available',
                text: 'Disponibile'
            },
            'In manutenzione': {
                class: 'status-maintenance',
                text: 'In Manutenzione'
            },
            'Fuori servizio': {
                class: 'status-inactive',
                text: 'Fuori Servizio'
            }
        };

        return statusMap[statoDescrizione] || { 
            class: 'status-unknown',
            text: 'Stato Sconosciuto'
        };
    }
}
document.addEventListener('DOMContentLoaded', () => {
    const manager = new MachineSelectionManager();

    window.addEventListener('unload', () => {
        manager.destroy();
    });

    window.selectMachine = (id) => manager.selectMachine(id);
});

export default MachineSelectionManager;