/**
 * Gestione Macchine Distributrici
 * Gestisce le operazioni CRUD e il monitoraggio delle macchine distributrici
 */
import mqttClient from '../common/mqtt.js';
import Utils from '../common/utils.js';
import auth from '../common/authentication.js';

class MachineManager {
    constructor() {
        // Verifica autenticazione
        auth.protectAdminRoute();

        this.initializeElements();
        this.initializeState();
        this.initialize();
    }

    /**
     * Inizializza gli elementi DOM
     */
    initializeElements() {
        this.elements = {
            machinesList: document.getElementById('machinesList'),
            addMachineForm: document.getElementById('addMachineForm'),
            editMachineModal: document.getElementById('editMachineModal'),
            searchInput: document.getElementById('searchMachine'),
            filterInstitute: document.getElementById('filterInstitute'),
            filterStatus: document.getElementById('filterStatus'),
            pagination: document.getElementById('pagination'),
            summary: document.getElementById('machineSummary')
        };

        // Verifica elementi richiesti
        Object.entries(this.elements).forEach(([key, element]) => {
            if (!element) {
                throw new Error(`Elemento DOM non trovato: ${key}`);
            }
        });
    }

    /**
     * Inizializza lo stato dell'applicazione
     */
    initializeState() {
        this.state = {
            machines: new Map(),
            institutes: new Map(),
            currentPage: 1,
            itemsPerPage: 10,
            filters: {
                institute: '',
                status: '',
                search: ''
            }
        };
    }

    /**
     * Inizializza il componente
     */
    async initialize() {
        try {
            Utils.toggleLoading(true);
            
            // Carica dati iniziali
            await Promise.all([
                this.loadInstitutes(),
                this.loadMachines()
            ]);

            // Setup MQTT e listeners
            await this.initializeMQTTSubscriptions();
            this.setupEventListeners();
            
            // Inizializza UI
            this.populateFilters();
            this.renderUI();

            console.log('Gestore macchine inizializzato');
        } catch (error) {
            console.error('Errore inizializzazione:', error);
            Utils.showToast('Errore durante l\'inizializzazione', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Carica le macchine dal backend
     */
    async loadMachines() {
        try {
            const machines = await Utils.apiCall('/api/macchine');
            machines.forEach(machine => {
                this.state.machines.set(machine.id, this.normalizeMachineData(machine));
            });
        } catch (error) {
            console.error('Errore caricamento macchine:', error);
            throw error;
        }
    }

    /**
     * Carica gli istituti dal backend
     */
    async loadInstitutes() {
        try {
            const institutes = await Utils.apiCall('/api/istituti');
            institutes.forEach(institute => {
                this.state.institutes.set(institute.id, institute);
            });
        } catch (error) {
            console.error('Errore caricamento istituti:', error);
            throw error;
        }
    }

    /**
     * Normalizza i dati della macchina
     */
    normalizeMachineData(machine) {
        return {
            id: machine.id,
            istitutoId: machine.istitutoId,
            stato: machine.statoId,
            cassaAttuale: machine.cassaAttuale || 0,
            cassaMassima: machine.cassaMassima,
            cialdaAttuale: machine.cialdaAttuale || 0,
            cialdaMassima: machine.cialdaMassima,
            manutenzione: machine.manutenzione || null,
            ultimaModifica: machine.ultimaModifica
        };
    }

    /**
     * Configura le sottoscrizioni MQTT
     */
    async initializeMQTTSubscriptions() {
        const topics = [
            {
                topic: 'macchine/+/stato',
                handler: (topic, message) => {
                    const machineId = parseInt(topic.split('/')[1]);
                    this.handleMachineStatusUpdate(machineId, JSON.parse(message));
                }
            },
            {
                topic: 'macchine/+/allarmi',
                handler: (topic, message) => {
                    const machineId = parseInt(topic.split('/')[1]);
                    this.handleMachineAlarm(machineId, JSON.parse(message));
                }
            },
            {
                topic: 'macchine/+/manutenzione',
                handler: (topic, message) => {
                    const machineId = parseInt(topic.split('/')[1]);
                    this.handleMaintenanceUpdate(machineId, JSON.parse(message));
                }
            }
        ];

        try {
            await Promise.all(
                topics.map(({ topic, handler }) => 
                    mqttClient.subscribe(topic, handler)
                )
            );
        } catch (error) {
            console.error('Errore sottoscrizione MQTT:', error);
            throw error;
        }
    }

    /**
     * Configura i listener degli eventi
     */
    setupEventListeners() {
        // Form aggiunta macchina
        this.elements.addMachineForm?.addEventListener('submit', 
            this.handleAddMachine.bind(this));

        // Filtri e ricerca
        this.elements.filterInstitute?.addEventListener('change', 
            () => this.updateFilters('institute'));
            
        this.elements.filterStatus?.addEventListener('change',
            () => this.updateFilters('status'));

        this.elements.searchInput?.addEventListener('input',
            Utils.debounce(() => this.updateFilters('search'), 300));

        // Gestione modali
        document.querySelectorAll('.modal-close').forEach(button => {
            button.addEventListener('click', () => this.closeModals());
        });

        // Handler globali per azioni macchina
        window.editMachine = (id) => this.openEditModal(id);
        window.deleteMachine = (id) => this.handleDeleteMachine(id);
    }

    /**
     * Aggiorna i filtri e rigenera la lista
     */
    updateFilters(filterType) {
        const newValue = filterType === 'search' 
            ? this.elements.searchInput.value
            : this.elements[`filter${filterType.charAt(0).toUpperCase() + filterType.slice(1)}`].value;

        this.state.filters[filterType] = newValue;
        this.state.currentPage = 1;
        this.renderMachines();
    }

    /**
     * Gestisce l'aggiunta di una nuova macchina
     */
    async handleAddMachine(event) {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);

        try {
            Utils.toggleLoading(true, form);

            const machineData = {
                istitutoId: parseInt(formData.get('istituto')),
                cassaMassima: parseFloat(formData.get('cassaMassima')),
                cialdaMassima: parseInt(formData.get('cialdaMassima'))
            };

            // Validazione
            this.validateMachineData(machineData);

            const newMachine = await Utils.apiCall('/api/macchine', {
                method: 'POST',
                body: JSON.stringify(machineData)
            });

            this.state.machines.set(newMachine.id, this.normalizeMachineData(newMachine));
            this.renderMachines();

            Utils.showToast('Macchina aggiunta con successo', 'success');
            form.reset();

        } catch (error) {
            console.error('Errore aggiunta macchina:', error);
            Utils.showToast(error.message || 'Errore durante l\'aggiunta', 'error');
        } finally {
            Utils.toggleLoading(false, form);
        }
    }

    /**
     * Gestisce la modifica di una macchina
     */
    async handleEditMachine(machineId, formData) {
        try {
            Utils.toggleLoading(true);

            const machineData = {
                cassaMassima: parseFloat(formData.get('cassaMassima')),
                cialdaMassima: parseInt(formData.get('cialdaMassima')),
                statoId: parseInt(formData.get('stato'))
            };

            this.validateMachineData(machineData);

            const updatedMachine = await Utils.apiCall(`/api/macchine/${machineId}`, {
                method: 'PUT',
                body: JSON.stringify(machineData)
            });

            this.state.machines.set(machineId, this.normalizeMachineData(updatedMachine));
            this.renderMachines();
            this.closeModals();

            Utils.showToast('Macchina aggiornata con successo', 'success');
        } catch (error) {
            console.error('Errore modifica macchina:', error);
            Utils.showToast(error.message || 'Errore durante la modifica', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Gestisce l'eliminazione di una macchina
     */
    async handleDeleteMachine(machineId) {
        const machine = this.state.machines.get(machineId);
        if (!machine) return;

        const institute = this.state.institutes.get(machine.istitutoId);
        const confirmMessage = `Sei sicuro di voler eliminare la macchina #${machineId}${
            institute ? ` dall'istituto "${institute.nome}"` : ''
        }?`;

        if (!confirm(confirmMessage)) return;

        try {
            Utils.toggleLoading(true);

            await Utils.apiCall(`/api/macchine/${machineId}`, {
                method: 'DELETE'
            });

            this.state.machines.delete(machineId);
            this.renderMachines();

            Utils.showToast('Macchina eliminata con successo', 'success');
        } catch (error) {
            console.error('Errore eliminazione macchina:', error);
            Utils.showToast(error.message || 'Errore durante l\'eliminazione', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Gestisce l'aggiornamento dello stato di una macchina via MQTT
     */
    handleMachineStatusUpdate(machineId, status) {
        const machine = this.state.machines.get(machineId);
        if (!machine) return;

        machine.stato = status.statoId;
        machine.ultimaModifica = new Date().toISOString();

        this.renderMachineCard(machine);
        this.updateMachineSummary();
    }

    /**
     * Gestisce gli allarmi di una macchina
     */
    handleMachineAlarm(machineId, alarm) {
        const machine = this.state.machines.get(machineId);
        if (!machine) return;

        const institute = this.state.institutes.get(machine.istitutoId);
        const location = institute ? ` presso ${institute.nome}` : '';
        
        Utils.showToast(
            `Allarme macchina #${machineId}${location}: ${alarm.messaggio}`,
            'warning'
        );

        this.renderMachineCard(machine);
    }

    /**
     * Gestisce gli aggiornamenti di manutenzione
     */
    handleMaintenanceUpdate(machineId, update) {
        const machine = this.state.machines.get(machineId);
        if (!machine) return;

        machine.manutenzione = update;
        machine.ultimaModifica = new Date().toISOString();

        this.renderMachineCard(machine);
        this.updateMachineSummary();
    }

    /**
     * Valida i dati di una macchina
     */
    validateMachineData(data) {
        const errors = [];

        // Valida istituto se presente
        if (data.istitutoId !== undefined) {
            if (!this.state.institutes.has(data.istitutoId)) {
                errors.push('Istituto non valido');
            }
        }

        // Valida cassa massima
        if (data.cassaMassima !== undefined) {
            if (isNaN(data.cassaMassima) || data.cassaMassima <= 0) {
                errors.push('La cassa massima deve essere un numero positivo');
            }
        }

        // Valida capacità cialde
        if (data.cialdaMassima !== undefined) {
            if (!Number.isInteger(data.cialdaMassima) || data.cialdaMassima <= 0) {
                errors.push('La capacità cialde deve essere un numero intero positivo');
            }
        }

        // Valida stato se presente
        if (data.statoId !== undefined) {
            if (![1, 2, 3].includes(data.statoId)) {
                errors.push('Stato non valido');
            }
        }

        if (errors.length > 0) {
            throw new Error(errors.join('\n'));
        }
    }

    /**
     * Renderizza l'interfaccia utente
     */
    renderUI() {
        this.renderMachines();
        this.updateMachineSummary();
    }

    /**
     * Renderizza l'elenco delle macchine
     */
    renderMachines() {
        const filteredMachines = Array.from(this.state.machines.values())
            .filter(this.filterMachine.bind(this))
            .sort((a, b) => b.ultimaModifica.localeCompare(a.ultimaModifica));

        const start = (this.state.currentPage - 1) * this.state.itemsPerPage;
        const paginatedMachines = filteredMachines.slice(
            start, 
            start + this.state.itemsPerPage);

            this.elements.machinesList.innerHTML = paginatedMachines
                .map(machine => this.renderMachineCard(machine))
                .join('');
    
            this.renderPagination(filteredMachines.length);
        }
    
        /**
         * Filtra le macchine in base ai criteri selezionati
         */
        filterMachine(machine) {
            const { institute, status, search } = this.state.filters;
    
            // Filtro istituto
            if (institute && machine.istitutoId !== parseInt(institute)) {
                return false;
            }
    
            // Filtro stato
            if (status && machine.stato !== parseInt(status)) {
                return false;
            }
    
            // Filtro ricerca
            if (search) {
                const searchTerm = search.toLowerCase();
                const institute = this.state.institutes.get(machine.istitutoId);
                return machine.id.toString().includes(searchTerm) ||
                       (institute && institute.nome.toLowerCase().includes(searchTerm));
            }
    
            return true;
        }
    
        /**
         * Renderizza una singola card macchina
         */
        renderMachineCard(machine) {
            const institute = this.state.institutes.get(machine.istitutoId);
            const status = this.getMachineStatus(machine.stato);
    
            return `
                <div class="machine-card" data-id="${machine.id}">
                    <div class="card-header">
                        <div class="header-title">
                            <h3>Distributore #${machine.id}</h3>
                            <span class="status-badge ${status.class}">
                                <i class="fas ${status.icon}"></i>
                                ${status.text}
                            </span>
                        </div>
                        ${institute ? `
                            <div class="header-subtitle">
                                <i class="fas fa-building"></i>
                                ${Utils.escapeHtml(institute.nome)}
                            </div>
                        ` : ''}
                    </div>
                    
                    <div class="card-body">
                        <div class="stats-grid">
                            <div class="stat-item">
                                <div class="stat-label">
                                    <i class="fas fa-euro-sign"></i> Cassa
                                </div>
                                <div class="stat-value">
                                    ${Utils.formatCurrency(machine.cassaAttuale)} / 
                                    ${Utils.formatCurrency(machine.cassaMassima)}
                                </div>
                                <div class="stat-bar">
                                    <div class="stat-progress" style="width: ${
                                        (machine.cassaAttuale / machine.cassaMassima * 100).toFixed(1)
                                    }%"></div>
                                </div>
                            </div>
                            
                            <div class="stat-item">
                                <div class="stat-label">
                                    <i class="fas fa-box"></i> Cialde
                                </div>
                                <div class="stat-value">
                                    ${machine.cialdaAttuale} / ${machine.cialdaMassima}
                                </div>
                                <div class="stat-bar">
                                    <div class="stat-progress" style="width: ${
                                        (machine.cialdaAttuale / machine.cialdaMassima * 100).toFixed(1)
                                    }%"></div>
                                </div>
                            </div>
                        </div>
    
                        ${machine.manutenzione ? `
                            <div class="maintenance-info">
                                <i class="fas fa-tools"></i>
                                Ultima manutenzione: ${Utils.formatDate(machine.manutenzione.data)}
                                <br>
                                <small>${Utils.escapeHtml(machine.manutenzione.note || '')}</small>
                            </div>
                        ` : ''}
                    </div>
    
                    <div class="card-actions">
                        <button class="btn btn-edit" 
                                onclick="machineManager.openEditModal(${machine.id})"
                                title="Modifica macchina">
                            <i class="fas fa-edit"></i>
                        </button>
    
                        <button class="btn btn-danger"
                                onclick="machineManager.handleDeleteMachine(${machine.id})"
                                title="Elimina macchina"
                                ${machine.stato !== 3 ? 'disabled' : ''}>
                            <i class="fas fa-trash"></i>
                        </button>
    
                        <button class="btn btn-secondary"
                                onclick="machineManager.handleMaintenanceRequest(${machine.id})"
                                title="Richiedi manutenzione"
                                ${machine.stato === 2 ? 'disabled' : ''}>
                            <i class="fas fa-tools"></i>
                        </button>
    
                        <a href="/admin/machines/${machine.id}/details" 
                           class="btn btn-primary"
                           title="Visualizza dettagli">
                            <i class="fas fa-info-circle"></i>
                        </a>
                    </div>
                </div>
            `;
        }
    
        /**
         * Ottiene lo stato formattato di una macchina
         */
        getMachineStatus(stato) {
            const statusMap = {
                1: { 
                    class: 'status-active',
                    icon: 'fa-check-circle',
                    text: 'Attiva'
                },
                2: {
                    class: 'status-maintenance',
                    icon: 'fa-tools',
                    text: 'In Manutenzione'
                },
                3: {
                    class: 'status-inactive',
                    icon: 'fa-times-circle',
                    text: 'Fuori Servizio'
                }
            };
    
            return statusMap[stato] || {
                class: 'status-unknown',
                icon: 'fa-question-circle',
                text: 'Stato Sconosciuto'
            };
        }
    
        /**
         * Aggiorna il riepilogo delle macchine
         */
        updateMachineSummary() {
            const summary = Array.from(this.state.machines.values()).reduce(
                (acc, machine) => {
                    acc.total++;
                    if (machine.stato === 1) acc.active++;
                    if (machine.stato === 2) acc.maintenance++;
                    if (machine.cassaAttuale >= machine.cassaMassima * 0.9) acc.cashAlert++;
                    if (machine.cialdaAttuale <= machine.cialdaMassima * 0.1) acc.podsAlert++;
                    return acc;
                },
                { total: 0, active: 0, maintenance: 0, cashAlert: 0, podsAlert: 0 }
            );
    
            this.elements.summary.innerHTML = `
                <div class="summary-grid">
                    <div class="summary-card">
                        <div class="summary-icon active">
                            <i class="fas fa-check-circle"></i>
                        </div>
                        <div class="summary-content">
                            <div class="summary-value">${summary.active}/${summary.total}</div>
                            <div class="summary-label">Macchine Attive</div>
                        </div>
                    </div>
    
                    <div class="summary-card">
                        <div class="summary-icon warning">
                            <i class="fas fa-tools"></i>
                        </div>
                        <div class="summary-content">
                            <div class="summary-value">${summary.maintenance}</div>
                            <div class="summary-label">In Manutenzione</div>
                        </div>
                    </div>
    
                    <div class="summary-card">
                        <div class="summary-icon ${summary.cashAlert > 0 ? 'danger' : 'success'}">
                            <i class="fas fa-euro-sign"></i>
                        </div>
                        <div class="summary-content">
                            <div class="summary-value">${summary.cashAlert}</div>
                            <div class="summary-label">Cassa da Svuotare</div>
                        </div>
                    </div>
    
                    <div class="summary-card">
                        <div class="summary-icon ${summary.podsAlert > 0 ? 'warning' : 'success'}">
                            <i class="fas fa-box"></i>
                        </div>
                        <div class="summary-content">
                            <div class="summary-value">${summary.podsAlert}</div>
                            <div class="summary-label">Cialde in Esaurimento</div>
                        </div>
                    </div>
                </div>
            `;
        }
    
        /**
         * Renderizza la paginazione
         */
        renderPagination(totalItems) {
            const totalPages = Math.ceil(totalItems / this.state.itemsPerPage);
            if (totalPages <= 1) {
                this.elements.pagination.innerHTML = '';
                return;
            }
    
            let paginationHTML = `
                <button class="btn btn-page" 
                        onclick="machineManager.changePage(${this.state.currentPage - 1})"
                        ${this.state.currentPage === 1 ? 'disabled' : ''}>
                    <i class="fas fa-chevron-left"></i>
                </button>
            `;
    
            for (let i = 1; i <= totalPages; i++) {
                if (i === 1 || i === totalPages || 
                    (i >= this.state.currentPage - 2 && i <= this.state.currentPage + 2)) {
                    paginationHTML += `
                        <button class="btn btn-page ${i === this.state.currentPage ? 'active' : ''}"
                                onclick="machineManager.changePage(${i})">
                            ${i}
                        </button>
                    `;
                } else if (i === this.state.currentPage - 3 || i === this.state.currentPage + 3) {
                    paginationHTML += '<span class="pagination-dots">...</span>';
                }
            }
    
            paginationHTML += `
                <button class="btn btn-page" 
                        onclick="machineManager.changePage(${this.state.currentPage + 1})"
                        ${this.state.currentPage === totalPages ? 'disabled' : ''}>
                    <i class="fas fa-chevron-right"></i>
                </button>
            `;
    
            this.elements.pagination.innerHTML = paginationHTML;
        }
    
        /**
         * Cambia pagina
         */
        changePage(page) {
            this.state.currentPage = page;
            this.renderMachines();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    
        /**
         * Popola i filtri con i dati disponibili
         */
        populateFilters() {
            // Select istituti
            let instituteOptions = '<option value="">Tutti gli istituti</option>';
            Array.from(this.state.institutes.values())
                .sort((a, b) => a.nome.localeCompare(b.nome))
                .forEach(institute => {
                    instituteOptions += `
                        <option value="${institute.id}">
                            ${Utils.escapeHtml(institute.nome)}
                        </option>
                    `;
                });
            this.elements.filterInstitute.innerHTML = instituteOptions;
    
            // Select stati
            this.elements.filterStatus.innerHTML = `
                <option value="">Tutti gli stati</option>
                <option value="1">Attive</option>
                <option value="2">In Manutenzione</option>
                <option value="3">Fuori Servizio</option>
            `;
        }
    
        /**
         * Pulisce le risorse
         */
        destroy() {
            // Rimuovi sottoscrizioni MQTT
            const topics = [
                'macchine/+/stato',
                'macchine/+/allarmi',
                'macchine/+/manutenzione'
            ];
            
            topics.forEach(topic => mqttClient.unsubscribe(topic));
            
            // Rimuovi handler globali
            delete window.machineManager;
            delete window.editMachine;
            delete window.deleteMachine;
        }
    }
    
    // Inizializzazione
    let machineManager = null;
    
    document.addEventListener('DOMContentLoaded', () => {
        machineManager = new MachineManager();
        window.machineManager = machineManager;
    });
    
    export default MachineManager;