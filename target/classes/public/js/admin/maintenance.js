/**
 * Gestione Manutenzioni
 * Gestisce le richieste di manutenzione e gli interventi tecnici
 */
import mqttClient from '../common/mqtt.js';
import Utils from '../common/utils.js';
import auth from '../common/authentication.js';

class MaintenanceManager {
    constructor() {
        // Verifica autenticazione
        auth.protectEmployeeRoute();

        this.initializeElements();
        this.initializeState();
        this.initialize();
    }

    /**
     * Inizializza gli elementi DOM
     */
    initializeElements() {
        this.elements = {
            maintenanceList: document.getElementById('maintenanceList'),
            completedList: document.getElementById('completedList'),
            filterStatus: document.getElementById('filterStatus'),
            filterTechnician: document.getElementById('filterTechnician'),
            filterPriority: document.getElementById('filterPriority'),
            searchInput: document.getElementById('searchMaintenance'),
            assignModal: document.getElementById('assignTechnicianModal'),
            completeModal: document.getElementById('completeMaintenanceModal'),
            pagination: document.getElementById('maintenancePagination'),
            summary: document.getElementById('maintenanceSummary')
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
            activeRequests: new Map(),
            completedRequests: new Map(),
            technicians: new Map(),
            currentPage: 1,
            itemsPerPage: 10,
            filters: {
                status: '',
                technician: '',
                priority: '',
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
                this.loadMaintenanceData(),
                this.initializeMQTTSubscriptions()
            ]);

            // Setup UI
            this.setupEventListeners();
            this.populateFilters();
            this.renderUI();

            console.log('Gestore manutenzioni inizializzato');
        } catch (error) {
            console.error('Errore inizializzazione:', error);
            Utils.showToast('Errore durante l\'inizializzazione', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Carica i dati delle manutenzioni e dei tecnici
     */
    async loadMaintenanceData() {
        try {
            const [active, completed, technicians] = await Promise.all([
                Utils.apiCall('/api/manutenzione/attive'),
                Utils.apiCall('/api/manutenzione/completate'),
                Utils.apiCall('/api/tecnici')
            ]);

            // Manutenzioni attive
            active.forEach(request => {
                this.state.activeRequests.set(request.id, this.normalizeRequest(request));
            });

            // Manutenzioni completate
            completed.forEach(request => {
                this.state.completedRequests.set(request.id, this.normalizeRequest(request));
            });

            // Tecnici
            technicians.forEach(tech => {
                this.state.technicians.set(tech.id, tech);
            });

        } catch (error) {
            console.error('Errore caricamento dati:', error);
            throw error;
        }
    }

    /**
     * Normalizza i dati di una richiesta
     */
    normalizeRequest(request) {
        return {
            id: request.id,
            macchinaId: request.macchinaId,
            tecnicoId: request.tecnicoId,
            stato: request.statoId,
            priorita: request.priorita,
            descrizione: request.descrizione,
            dataRichiesta: request.dataRichiesta,
            dataCompletamento: request.dataCompletamento,
            note: request.note || '',
            partiSostituite: request.partiSostituite || []
        };
    }

    /**
     * Configura le sottoscrizioni MQTT
     */
    async initializeMQTTSubscriptions() {
        const topics = [
            {
                topic: 'manutenzione/richieste/nuova',
                handler: (topic, message) => {
                    const request = JSON.parse(message);
                    this.handleNewRequest(request);
                }
            },
            {
                topic: 'manutenzione/stato/+',
                handler: (topic, message) => {
                    const requestId = parseInt(topic.split('/').pop());
                    this.handleStatusUpdate(requestId, JSON.parse(message));
                }
            },
            {
                topic: 'manutenzione/completata/+',
                handler: (topic, message) => {
                    const requestId = parseInt(topic.split('/').pop());
                    this.handleCompletion(requestId, JSON.parse(message));
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
        // Filtri
        ['Status', 'Technician', 'Priority'].forEach(filter => {
            const element = this.elements[`filter${filter}`];
            element?.addEventListener('change', () => {
                this.updateFilters(filter.toLowerCase());
            });
        });

        // Ricerca
        this.elements.searchInput?.addEventListener('input',
            Utils.debounce(() => this.updateFilters('search'), 300)
        );

        // Modali
        document.querySelectorAll('.modal-close').forEach(button => {
            button.addEventListener('click', () => this.closeModals());
        });

        // Handler globali per azioni manutenzione
        window.maintenanceManager = this;
        window.assignTechnician = (id) => this.openAssignModal(id);
        window.completeMaintenance = (id) => this.openCompleteModal(id);
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
        this.renderMaintenance();
    }

    /**
     * Gestisce una nuova richiesta di manutenzione
     */
    handleNewRequest(request) {
        const normalizedRequest = this.normalizeRequest(request);
        this.state.activeRequests.set(request.id, normalizedRequest);
        
        this.renderMaintenance();
        this.updateSummary();

        Utils.showToast(`
            Nuova richiesta di manutenzione per la macchina #${request.macchinaId}
            ${request.priorita === 'alta' ? ' - PRIORITÀ ALTA' : ''}
        `, 'info');
    }

    /**
     * Gestisce l'aggiornamento di stato di una richiesta
     */
    handleStatusUpdate(requestId, status) {
        const request = this.state.activeRequests.get(requestId);
        if (!request) return;

        request.stato = status.statoId;
        request.ultimoAggiornamento = new Date().toISOString();

        this.renderRequestCard(request);
        this.updateSummary();
    }

    /**
     * Gestisce il completamento di una richiesta
     */
    handleCompletion(requestId, completion) {
        const request = this.state.activeRequests.get(requestId);
        if (!request) return;

        const completedRequest = {
            ...request,
            stato: 'completata',
            dataCompletamento: completion.dataCompletamento,
            note: completion.note,
            partiSostituite: completion.partiSostituite
        };

        this.state.activeRequests.delete(requestId);
        this.state.completedRequests.set(requestId, completedRequest);

        this.renderMaintenance();
        this.updateSummary();

        Utils.showToast(`
            Manutenzione #${requestId} completata con successo
        `, 'success');
    }

    /**
     * Assegna un tecnico a una richiesta
     */
    async assignTechnician(requestId, technicianId) {
        try {
            Utils.toggleLoading(true);

            const response = await Utils.apiCall(`/api/manutenzione/${requestId}/assegna`, {
                method: 'PUT',
                body: JSON.stringify({ tecnicoId: technicianId })
            });

            const request = this.state.activeRequests.get(requestId);
            if (request) {
                request.tecnicoId = technicianId;
                request.stato = 'assegnata';
                request.ultimoAggiornamento = new Date().toISOString();
            }

            this.renderRequestCard(request);
            this.updateSummary();
            this.closeModals();

            Utils.showToast('Tecnico assegnato con successo', 'success');
        } catch (error) {
            console.error('Errore assegnazione tecnico:', error);
            Utils.showToast(error.message || 'Errore durante l\'assegnazione', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Completa una richiesta di manutenzione
     */
    async completeMaintenance(requestId, formData) {
        try {
            Utils.toggleLoading(true);

            const completionData = {
                note: formData.get('note'),
                partiSostituite: formData.get('partiSostituite')
                    .split(',')
                    .map(part => part.trim())
                    .filter(Boolean),
                dataCompletamento: new Date().toISOString()
            };

            await Utils.apiCall(`/api/manutenzione/${requestId}/completa`, {
                method: 'PUT',
                body: JSON.stringify(completionData)
            });

            // L'aggiornamento UI sarà gestito dal messaggio MQTT
            this.closeModals();
            
        } catch (error) {
            console.error('Errore completamento manutenzione:', error);
            Utils.showToast(error.message || 'Errore durante il completamento', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Filtra le richieste in base ai criteri
     */
    filterRequests(request) {
        const { status, technician, priority, search } = this.state.filters;

        if (status && request.stato !== status) return false;
        if (technician && request.tecnicoId !== parseInt(technician)) return false;
        if (priority && request.priorita !== priority) return false;

        if (search) {
            const searchTerm = search.toLowerCase();
            return request.id.toString().includes(searchTerm) ||
                   request.macchinaId.toString().includes(searchTerm) ||
                   request.descrizione.toLowerCase().includes(searchTerm);
        }

        return true;
    }

    /**
     * Renderizza l'interfaccia utente
     */
    renderUI() {
        this.renderMaintenance();
        this.updateSummary();
    }

    /**
     * Renderizza l'elenco delle manutenzioni
     */
    renderMaintenance() {
        const filteredRequests = Array.from(this.state.activeRequests.values())
            .filter(this.filterRequests.bind(this))
            .sort((a, b) => {
                // Prima per priorità
                const priorityOrder = { alta: 0, media: 1, bassa: 2 };
                if (a.priorita !== b.priorita) {
                    return priorityOrder[a.priorita] - priorityOrder[b.priorita];
                }
                // Poi per data richiesta
                return new Date(b.dataRichiesta) - new Date(a.dataRichiesta);
            });

        const start = (this.state.currentPage - 1) * this.state.itemsPerPage;
        const paginatedRequests = filteredRequests.slice(
            start,
            start + this.state.itemsPerPage
        );

        this.elements.maintenanceList.innerHTML = paginatedRequests
            .map(request => this.renderRequestCard(request))
            .join('');

        this.renderPagination(filteredRequests.length);
        this.renderCompletedRequests();
    }

    /**
     * Renderizza una card di richiesta manutenzione
     */
    renderRequestCard(request) {
        const technician = this.state.technicians.get(request.tecnicoId);
        const status = this.getStatusDetails(request.stato);

        return `
            <div class="maintenance-card ${status.class}" data-id="${request.id}">
                <div class="card-header">
                    <div class="header-left">
                        <h3>Richiesta #${request.id}</h3>
                        <span class="priority-badge ${request.priorita}">
                            ${this.getPriorityText(request.priorita)}
                        </span>
                    </div>
                    <div class="status-badge ${status.class}">
                        <i class="fas ${status.icon}"></i>
                        ${status.text}
                    </div>
                </div>

                <div class="card-body">
                    <div class="info-grid">
                        <div class="info-item">
                            <i class="fas fa-coffee"></i>
                            <span>Macchina #${request.macchinaId}</span>
                            </div>
                        <div class="info-item">
                            <i class="fas fa-calendar"></i>
                            <span>Richiesto: ${Utils.formatDate(request.dataRichiesta)}</span>
                        </div>
                        <div class="info-item">
                            <i class="fas fa-user-cog"></i>
                            <span>Tecnico: ${technician ? Utils.escapeHtml(technician.nome) : 'Non assegnato'}</span>
                        </div>
                    </div>

                    <div class="description">
                        <p>${Utils.escapeHtml(request.descrizione)}</p>
                    </div>

                    ${request.note ? `
                        <div class="notes">
                            <strong>Note:</strong>
                            <p>${Utils.escapeHtml(request.note)}</p>
                        </div>
                    ` : ''}
                </div>

                <div class="card-actions">
                    ${this.renderRequestActions(request)}
                </div>
            </div>
        `;
    }

    /**
     * Renderizza le azioni disponibili per una richiesta
     */
    renderRequestActions(request) {
        const actions = [];

        // Assegnazione tecnico
        if (!request.tecnicoId) {
            actions.push(`
                <button class="btn btn-primary" 
                        onclick="maintenanceManager.openAssignModal(${request.id})"
                        title="Assegna un tecnico">
                    <i class="fas fa-user-plus"></i>
                    <span>Assegna</span>
                </button>
            `);
        }

        // Completamento
        if (request.stato === 'in_corso') {
            actions.push(`
                <button class="btn btn-success"
                        onclick="maintenanceManager.openCompleteModal(${request.id})"
                        title="Completa manutenzione">
                    <i class="fas fa-check"></i>
                    <span>Completa</span>
                </button>
            `);
        }

        // Visualizzazione dettagli
        actions.push(`
            <a href="/admin/manutenzione/${request.id}/dettagli" 
               class="btn btn-info"
               title="Visualizza dettagli">
                <i class="fas fa-info-circle"></i>
                <span>Dettagli</span>
            </a>
        `);

        return actions.join('');
    }

    /**
     * Renderizza le manutenzioni completate
     */
    renderCompletedRequests() {
        const completedRequests = Array.from(this.state.completedRequests.values())
            .sort((a, b) => new Date(b.dataCompletamento) - new Date(a.dataCompletamento))
            .slice(0, 5);

        this.elements.completedList.innerHTML = completedRequests
            .map(request => this.renderCompletedCard(request))
            .join('');
    }

    /**
     * Renderizza una card di manutenzione completata
     */
    renderCompletedCard(request) {
        const technician = this.state.technicians.get(request.tecnicoId);
        
        return `
            <div class="completed-card">
                <div class="card-header">
                    <div class="header-info">
                        <h4>Manutenzione #${request.id}</h4>
                        <span class="completion-date">
                            ${Utils.formatDate(request.dataCompletamento)}
                        </span>
                    </div>
                </div>

                <div class="card-body">
                    <div class="info-grid">
                        <div class="info-item">
                            <i class="fas fa-coffee"></i>
                            <span>Macchina #${request.macchinaId}</span>
                        </div>
                        <div class="info-item">
                            <i class="fas fa-user-check"></i>
                            <span>Tecnico: ${technician ? Utils.escapeHtml(technician.nome) : 'N/D'}</span>
                        </div>
                    </div>
                    
                    ${request.partiSostituite.length > 0 ? `
                        <div class="parts-replaced">
                            <strong>Parti sostituite:</strong>
                            <ul>
                                ${request.partiSostituite.map(part => 
                                    `<li>${Utils.escapeHtml(part)}</li>`
                                ).join('')}
                            </ul>
                        </div>
                    ` : ''}

                    ${request.note ? `
                        <div class="notes">
                            <strong>Note:</strong>
                            <p>${Utils.escapeHtml(request.note)}</p>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    /**
     * Aggiorna il riepilogo delle manutenzioni
     */
    updateSummary() {
        const summary = Array.from(this.state.activeRequests.values()).reduce(
            (acc, request) => {
                acc.total++;
                acc[request.priorita]++;
                if (request.stato === 'in_corso') acc.inProgress++;
                if (!request.tecnicoId) acc.unassigned++;
                return acc;
            },
            { total: 0, alta: 0, media: 0, bassa: 0, inProgress: 0, unassigned: 0 }
        );

        this.elements.summary.innerHTML = `
            <div class="summary-grid">
                <div class="summary-card">
                    <div class="summary-icon total">
                        <i class="fas fa-tasks"></i>
                    </div>
                    <div class="summary-content">
                        <div class="summary-value">${summary.total}</div>
                        <div class="summary-label">Richieste Totali</div>
                    </div>
                </div>

                <div class="summary-card">
                    <div class="summary-icon ${summary.alta > 0 ? 'danger' : 'success'}">
                        <i class="fas fa-exclamation-triangle"></i>
                    </div>
                    <div class="summary-content">
                        <div class="summary-value">${summary.alta}</div>
                        <div class="summary-label">Priorità Alta</div>
                    </div>
                </div>

                <div class="summary-card">
                    <div class="summary-icon ${summary.unassigned > 0 ? 'warning' : 'success'}">
                        <i class="fas fa-user-clock"></i>
                    </div>
                    <div class="summary-content">
                        <div class="summary-value">${summary.unassigned}</div>
                        <div class="summary-label">Da Assegnare</div>
                    </div>
                </div>

                <div class="summary-card">
                    <div class="summary-icon progress">
                        <i class="fas fa-cogs"></i>
                    </div>
                    <div class="summary-content">
                        <div class="summary-value">${summary.inProgress}</div>
                        <div class="summary-label">In Corso</div>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Ottiene i dettagli di uno stato
     */
    getStatusDetails(stato) {
        return {
            'in_attesa': {
                class: 'status-waiting',
                icon: 'fa-clock',
                text: 'In Attesa'
            },
            'assegnata': {
                class: 'status-assigned',
                icon: 'fa-user-check',
                text: 'Assegnata'
            },
            'in_corso': {
                class: 'status-progress',
                icon: 'fa-cogs',
                text: 'In Corso'
            },
            'completata': {
                class: 'status-completed',
                icon: 'fa-check-circle',
                text: 'Completata'
            }
        }[stato] || {
            class: 'status-unknown',
            icon: 'fa-question-circle',
            text: 'Sconosciuto'
        };
    }

    /**
     * Ottiene il testo della priorità
     */
    getPriorityText(priority) {
        return {
            'alta': 'Alta Priorità',
            'media': 'Media Priorità',
            'bassa': 'Bassa Priorità'
        }[priority] || 'Priorità N/D';
    }

    /**
     * Apre il modale di assegnazione tecnico
     */
    openAssignModal(requestId) {
        try {
            const request = this.state.activeRequests.get(requestId);
            if (!request) {
                throw new Error('Richiesta non trovata');
            }

            // Tecnici disponibili (non assegnati ad altre richieste in corso)
            const assignedTechnicians = new Set(
                Array.from(this.state.activeRequests.values())
                    .filter(r => r.stato === 'in_corso')
                    .map(r => r.tecnicoId)
            );

            const availableTechnicians = Array.from(this.state.technicians.values())
                .filter(tech => !assignedTechnicians.has(tech.id))
                .sort((a, b) => a.nome.localeCompare(b.nome));

            if (availableTechnicians.length === 0) {
                throw new Error('Nessun tecnico disponibile al momento');
            }

            const modalContent = `
                <div class="modal-header">
                    <h3>Assegna Tecnico</h3>
                    <button class="btn-close" onclick="maintenanceManager.closeModals()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                
                <div class="modal-body">
                    <div class="request-info">
                        <p><strong>Richiesta #${request.id}</strong></p>
                        <p>Macchina #${request.macchinaId}</p>
                        <p class="priority-tag ${request.priorita}">
                            ${this.getPriorityText(request.priorita)}
                        </p>
                    </div>

                    <form id="assignForm" class="assign-form">
                        <div class="form-group">
                            <label for="technicianId">Seleziona Tecnico:</label>
                            <select id="technicianId" name="technicianId" class="form-select" required>
                                <option value="">Seleziona...</option>
                                ${availableTechnicians.map(tech => `
                                    <option value="${tech.id}">
                                        ${Utils.escapeHtml(tech.nome)} 
                                        ${tech.specializzazione ? `(${tech.specializzazione})` : ''}
                                    </option>
                                `).join('')}
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="note">Note per il tecnico (opzionale):</label>
                            <textarea id="note" name="note" class="form-textarea" 
                                    rows="3" maxlength="500"></textarea>
                        </div>

                        <div class="modal-actions">
                            <button type="button" class="btn btn-secondary" 
                                    onclick="maintenanceManager.closeModals()">
                                Annulla
                            </button>
                            <button type="submit" class="btn btn-primary">
                                Assegna
                            </button>
                        </div>
                    </form>
                </div>
            `;

            this.elements.assignModal.innerHTML = modalContent;
            this.elements.assignModal.classList.remove('hidden');

            // Form handler
            document.getElementById('assignForm').addEventListener('submit', async (e) => {
                e.preventDefault();
                const formData = new FormData(e.target);
                await this.assignTechnician(
                    requestId, 
                    parseInt(formData.get('technicianId')),
                    formData.get('note')
                );
            });

        } catch (error) {
            Utils.showToast(error.message, 'error');
        }
    }

    /**
     * Apre il modale di completamento manutenzione
     */
    openCompleteModal(requestId) {
        try {
            const request = this.state.activeRequests.get(requestId);
            if (!request) {
                throw new Error('Richiesta non trovata');
            }

            const modalContent = `
                <div class="modal-header">
                    <h3>Completa Manutenzione</h3>
                    <button class="btn-close" onclick="maintenanceManager.closeModals()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                
                <div class="modal-body">
                    <div class="request-info">
                        <p><strong>Richiesta #${request.id}</strong></p>
                        <p>Macchina #${request.macchinaId}</p>
                        <p>Tecnico: ${
                            this.state.technicians.get(request.tecnicoId)?.nome || 'N/D'
                        }</p>
                    </div>

                    <form id="completeForm" class="complete-form">
                        <div class="form-group">
                            <label for="esito">Esito Intervento:</label>
                            <select id="esito" name="esito" class="form-select" required>
                                <option value="risolto">Problema Risolto</option>
                                <option value="parziale">Risolto Parzialmente</option>
                                <option value="non_risolto">Non Risolvibile</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="note">Note Intervento:</label>
                            <textarea id="note" name="note" class="form-textarea" 
                                    rows="3" required maxlength="1000"></textarea>
                        </div>

                        <div class="form-group">
                            <label for="partiSostituite">Parti Sostituite:</label>
                            <input type="text" id="partiSostituite" name="partiSostituite" 
                                   class="form-input" placeholder="Separare con virgola">
                            <small class="form-help">
                                Inserire le parti sostituite separate da virgola
                            </small>
                        </div>

                        <div class="form-group">
                            <label>
                                <input type="checkbox" name="richiediVerifica" value="1">
                                Richiedi verifica supervisore
                            </label>
                        </div>

                        <div class="modal-actions">
                            <button type="button" class="btn btn-secondary" 
                                    onclick="maintenanceManager.closeModals()">
                                Annulla
                            </button>
                            <button type="submit" class="btn btn-primary">
                                Completa
                            </button>
                        </div>
                    </form>
                </div>
            `;

            this.elements.completeModal.innerHTML = modalContent;
            this.elements.completeModal.classList.remove('hidden');

            // Form handler
            document.getElementById('completeForm').addEventListener('submit', async (e) => {
                e.preventDefault();
                await this.completeMaintenance(requestId, new FormData(e.target));
            });

        } catch (error) {
            Utils.showToast(error.message, 'error');
        }
    }

    /**
     * Chiude tutti i modali
     */
    closeModals() {
        document.querySelectorAll('.modal').forEach(modal => {
            modal.classList.add('hidden');
            modal.innerHTML = ''; // Pulizia contenuto
        });
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
            <div class="pagination">
                <button class="btn btn-icon" 
                        onclick="maintenanceManager.changePage(1)"
                        ${this.state.currentPage === 1 ? 'disabled' : ''}
                        title="Prima pagina">
                    <i class="fas fa-angle-double-left"></i>
                </button>

                <button class="btn btn-icon" 
                        onclick="maintenanceManager.changePage(${this.state.currentPage - 1})"
                        ${this.state.currentPage === 1 ? 'disabled' : ''}
                        title="Pagina precedente">
                    <i class="fas fa-angle-left"></i>
                </button>

                <div class="page-numbers">
        `;

        // Logica per mostrare i numeri di pagina
        for (let i = 1; i <= totalPages; i++) {
            // Mostra sempre prima e ultima pagina
            // Mostra 2 pagine prima e dopo quella corrente
            if (
                i === 1 || i === totalPages ||
                (i >= this.state.currentPage - 2 && i <= this.state.currentPage + 2)
            ) {
                paginationHTML += `
                    <button class="btn btn-page ${i === this.state.currentPage ? 'active' : ''}"
                            onclick="maintenanceManager.changePage(${i})">
                        ${i}
                    </button>
                `;
            } else if (i === this.state.currentPage - 3 || i === this.state.currentPage + 3) {
                paginationHTML += '<span class="pagination-dots">...</span>';
            }
        }

        paginationHTML += `
                </div>

                <button class="btn btn-icon" 
                        onclick="maintenanceManager.changePage(${this.state.currentPage + 1})"
                        ${this.state.currentPage === totalPages ? 'disabled' : ''}
                        title="Pagina successiva">
                    <i class="fas fa-angle-right"></i>
                </button>

                <button class="btn btn-icon" 
                        onclick="maintenanceManager.changePage(${totalPages})"
                        ${this.state.currentPage === totalPages ? 'disabled' : ''}
                        title="Ultima pagina">
                    <i class="fas fa-angle-double-right"></i>
                </button>
            </div>

            <div class="pagination-info">
                Pagina ${this.state.currentPage} di ${totalPages}
            </div>
        `;

        this.elements.pagination.innerHTML = paginationHTML;
    }

    /**
     * Cambia pagina
     */
    changePage(page) {
        this.state.currentPage = page;
        this.renderMaintenance();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    /**
     * Pulisce le risorse
     */
    destroy() {
        // Rimuovi sottoscrizioni MQTT
        const topics = [
            'manutenzione/richieste/nuova',
            'manutenzione/stato/+',
            'manutenzione/completata/+'
        ];
        
        topics.forEach(topic => mqttClient.unsubscribe(topic));
        
        // Rimuovi handler globali
        delete window.maintenanceManager;
        delete window.assignTechnician;
        delete window.completeMaintenance;
    }
}

// Inizializzazione
let maintenanceManager = null;

document.addEventListener('DOMContentLoaded', () => {
    maintenanceManager = new MaintenanceManager();
});

export default MaintenanceManager;