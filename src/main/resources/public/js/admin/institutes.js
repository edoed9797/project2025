/**
 * Gestione Istituti
 * Gestisce le operazioni CRUD per gli istituti e le relative macchine associate
 */

import Utils from '../common/utils.js';
import auth from '../common/authentication.js';

class InstituteManager {
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
            institutesList: document.getElementById('institutesList'),
            addInstituteForm: document.getElementById('addInstituteForm'),
            editInstituteModal: document.getElementById('editInstituteModal'),
            searchInput: document.getElementById('searchInstitute'),
            pagination: document.getElementById('institutesPagination'),
            machinesSummary: document.getElementById('machinesSummary')
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
            institutes: new Map(),
            machines: new Map(),
            currentPage: 1,
            itemsPerPage: 10,
            filters: {
                search: ''
            }
        };
    }

    /**
     * Inizializza il gestore
     */
    async initialize() {
        try {
            Utils.toggleLoading(true);
            await this.loadInitialData();
            this.setupEventListeners();
            this.renderUI();
            console.log('Gestore istituti inizializzato');
        } catch (error) {
            console.error('Errore inizializzazione:', error);
            Utils.showToast('Errore durante l\'inizializzazione', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Carica i dati iniziali
     */
    async loadInitialData() {
        try {
            const [institutes, machines] = await Promise.all([
                Utils.apiCall('/api/istituti'),
                Utils.apiCall('/api/macchine')
            ]);

            institutes.forEach(institute => {
                this.state.institutes.set(institute.ID_Istituto, institute);
            });

            machines.forEach(summary => {
                this.state.machines.set(summary.ID_Istituto, summary);
            });

        } catch (error) {
            console.error('Errore caricamento dati:', error);
            throw error;
        }
    }

    /**
     * Configura i listener degli eventi
     */
    setupEventListeners() {
        // Form aggiunta istituto
        this.elements.addInstituteForm.addEventListener('submit', 
            this.handleAddInstitute.bind(this));

        // Ricerca
        this.elements.searchInput.addEventListener('input', 
            Utils.debounce(() => {
                this.state.filters.search = this.elements.searchInput.value;
                this.renderInstitutes();
            }, 300));

        // Gestione modali
        document.querySelectorAll('.modal-close').forEach(button => {
            button.addEventListener('click', () => this.closeModals());
        });

        // Prevenzione perdita modifiche
        window.addEventListener('beforeunload', (e) => {
            if (this.hasUnsavedChanges()) {
                e.preventDefault();
                e.returnValue = '';
            }
        });
    }

    /**
     * Gestisce l'aggiunta di un nuovo istituto
     */
    async handleAddInstitute(event) {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);

        try {
            Utils.toggleLoading(true, form);

            const instituteData = {
                id:formData.get('ID_Istituto'),
                nome: formData.get('Nome'),
                indirizzo: formData.get('Indirizzo')
            };

            this.validateInstituteData(instituteData);

            const newInstitute = await Utils.apiCall('/api/istituti', {
                method: 'POST',
                body: JSON.stringify(instituteData)
            });

            this.state.institutes.set(newInstitute.id, newInstitute);
            this.renderInstitutes();

            Utils.showToast('Istituto aggiunto con successo', 'success');
            form.reset();

        } catch (error) {
            console.error('Errore aggiunta istituto:', error);
            Utils.showToast(error.message || 'Errore durante l\'aggiunta', 'error');
        } finally {
            Utils.toggleLoading(false, form);
        }
    }

    /**
     * Gestisce la modifica di un istituto
     */
    async handleEditInstitute(instituteId, formData) {
        try {
            Utils.toggleLoading(true);

            const instituteData = {
                nome: formData.get('nome'),
                indirizzo: formData.get('indirizzo'),
                citta: formData.get('citta'),
                provincia: formData.get('provincia'),
                cap: formData.get('cap'),
                telefono: formData.get('telefono'),
                email: formData.get('email')
            };

            this.validateInstituteData(instituteData);

            const updatedInstitute = await Utils.apiCall(`/api/istituti/${instituteId}`, {
                method: 'PUT',
                body: JSON.stringify(instituteData)
            });

            this.state.institutes.set(instituteId, updatedInstitute);
            this.renderInstitutes();
            this.closeModals();

            Utils.showToast('Istituto aggiornato con successo', 'success');
        } catch (error) {
            console.error('Errore modifica istituto:', error);
            Utils.showToast(error.message || 'Errore durante la modifica', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Gestisce l'eliminazione di un istituto
     */
    async handleDeleteInstitute(instituteId) {
        try {
            const institute = this.state.institutes.get(instituteId);
            if (!institute) {
                throw new Error('Istituto non trovato');
            }

            const machineSummary = this.state.machines.get(instituteId);
            if (machineSummary?.totalMachines > 0) {
                throw new Error('Impossibile eliminare un istituto con macchine associate');
            }

            if (!confirm(`Sei sicuro di voler eliminare l'istituto "${institute.nome}"?`)) {
                return;
            }

            await Utils.apiCall(`/api/istituti/${instituteId}`, {
                method: 'DELETE'
            });

            this.state.institutes.delete(instituteId);
            this.state.machines.delete(instituteId);
            this.renderInstitutes();

            Utils.showToast('Istituto eliminato con successo', 'success');
        } catch (error) {
            console.error('Errore eliminazione istituto:', error);
            Utils.showToast(error.message || 'Errore durante l\'eliminazione', 'error');
        }
    }

    /**
     * Valida i dati di un istituto
     */
    validateInstituteData(data) {
        const errors = [];

        const validations = {
            nome: {
                condition: val => val?.trim().length >= 3,
                message: 'Il nome deve essere di almeno 3 caratteri'
            },
            indirizzo: {
                condition: val => val?.trim().length >= 5,
                message: 'L\'indirizzo deve essere di almeno 5 caratteri'
            },
            citta: {
                condition: val => val?.trim().length >= 2,
                message: 'La città deve essere specificata'
            },
            provincia: {
                condition: val => val?.trim().length === 2,
                message: 'La provincia deve essere di 2 caratteri'
            },
            cap: {
                condition: val => /^\d{5}$/.test(val),
                message: 'Il CAP deve essere di 5 cifre'
            },
            telefono: {
                condition: val => !val || /^\+?[\d\s-]{10,}$/.test(val),
                message: 'Il formato del telefono non è valido'
            },
            email: {
                condition: val => !val || Utils.validateEmail(val),
                message: 'Il formato dell\'email non è valido'
            }
        };

        Object.entries(validations).forEach(([field, validation]) => {
            if (!validation.condition(data[field])) {
                errors.push(validation.message);
            }
        });

        if (errors.length > 0) {
            throw new Error(errors.join('\n'));
        }
    }

    /**
     * Renderizza l'interfaccia utente
     */
    renderUI() {
        this.renderInstitutes();
        this.renderMachinesSummary();
    }

    /**
     * Renderizza l'elenco degli istituti
     */
    renderInstitutes() {
        const filteredInstitutes = Array.from(this.state.institutes.values())
            .filter(this.filterInstitute.bind(this))
            .sort((a, b) => a.nome.localeCompare(b.nome));

        const start = (this.state.currentPage - 1) * this.state.itemsPerPage;
        const paginatedInstitutes = filteredInstitutes.slice(
            start, 
            start + this.state.itemsPerPage
        );

        this.elements.institutesList.innerHTML = paginatedInstitutes
            .map(institute => this.renderInstituteCard(institute))
            .join('');

        this.renderPagination(filteredInstitutes.length);
    }

    /**
     * Renderizza una card istituto
     */
    renderInstituteCard(institute) {
        const machines = this.state.machines.get(institute.id) || { 
            totalMachines: 0, 
            activeMachines: 0 
        };

        return `
            <div class="institute-card" data-id="${institute.id}">
                <div class="card-header">
                    <h3>${Utils.escapeHtml(institute.nome)}</h3>
                    <div class="machines-count" title="Macchine attive / totali">
                        <i class="fas fa-coffee"></i>
                        ${machines.activeMachines}/${machines.totalMachines}
                    </div>
                </div>
                <div class="card-body">
                    <p>
                        <i class="fas fa-map-marker-alt"></i>
                        ${Utils.escapeHtml(institute.indirizzo)}<br>
                        ${Utils.escapeHtml(institute.cap)} ${Utils.escapeHtml(institute.citta)} (${Utils.escapeHtml(institute.provincia)})
                    </p>
                    ${institute.telefono ? `
                        <p>
                            <i class="fas fa-phone"></i>
                            ${Utils.escapeHtml(institute.telefono)}
                        </p>
                    ` : ''}
                    ${institute.email ? `
                        <p>
                            <i class="fas fa-envelope"></i>
                            ${Utils.escapeHtml(institute.email)}
                        </p>
                    ` : ''}
                </div>
                <div class="card-actions">
                    <button class="btn btn-edit" 
                            onclick="instituteManager.openEditModal(${institute.id})"
                            title="Modifica istituto">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-danger" 
                            onclick="instituteManager.handleDeleteInstitute(${institute.id})"
                            title="Elimina istituto"
                            ${machines.totalMachines > 0 ? 'disabled' : ''}>
                        <i class="fas fa-trash"></i>
                    </button>
                    <a href="/admin/machines?institute=${institute.id}" 
                       class="btn btn-primary"
                       title="Gestisci macchine">
                        <i class="fas fa-cog"></i>
                    </a>
                </div>
            </div>
        `;
    }

    /**
     * Filtra gli istituti in base ai criteri
     */
    filterInstitute(institute) {
        if (!this.state.filters.search) return true;

        const searchTerm = this.state.filters.search.toLowerCase();
        return (
            institute.nome.toLowerCase().includes(searchTerm) ||
            institute.citta.toLowerCase().includes(searchTerm) ||
            institute.indirizzo.toLowerCase().includes(searchTerm) ||
            institute.provincia.toLowerCase().includes(searchTerm)
        );
    }

    /**
     * Renderizza il riepilogo delle macchine
     */
    renderMachinesSummary() {
        const summary = Array.from(this.state.machines.values()).reduce(
            (acc, curr) => ({
                totalMachines: acc.totalMachines + curr.totalMachines,
                activeMachines: acc.activeMachines + curr.activeMachines
            }),
            { totalMachines: 0, activeMachines: 0 }
        );

        const activePercentage = summary.totalMachines > 0
            ? Math.round((summary.activeMachines / summary.totalMachines) * 100)
            : 0;

        this.elements.machinesSummary.innerHTML = `
            <div class="summary-grid">
                <div class="summary-card">
                    <div class="summary-icon">
                        <i class="fas fa-coffee"></i>
                    <div class="summary-content">
                        <div class="summary-value">${summary.totalMachines}</div>
                        <div class="summary-label">Macchine Totali</div>
                    </div>
                </div>
                <div class="summary-card">
                    <div class="summary-icon">
                        <i class="fas fa-check-circle"></i>
                    </div>
                    <div class="summary-content">
                        <div class="summary-value">${summary.activeMachines}</div>
                        <div class="summary-label">Macchine Attive</div>
                    </div>
                </div>
                <div class="summary-card">
                    <div class="summary-icon">
                        <i class="fas fa-percentage"></i>
                    </div>
                    <div class="summary-content">
                        <div class="summary-value">${activePercentage}%</div>
                        <div class="summary-label">Percentuale Attive</div>
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

        let paginationHTML = '';

        // Pulsante precedente
        paginationHTML += `
            <button class="btn btn-page" 
                    ${this.state.currentPage === 1 ? 'disabled' : ''}
                    onclick="instituteManager.changePage(${this.state.currentPage - 1})">
                <i class="fas fa-chevron-left"></i>
            </button>
        `;

        // Numeri pagina
        for (let i = 1; i <= totalPages; i++) {
            if (i === 1 || i === totalPages || 
                (i >= this.state.currentPage - 1 && i <= this.state.currentPage + 1)) {
                paginationHTML += `
                    <button class="btn btn-page ${i === this.state.currentPage ? 'active' : ''}"
                            onclick="instituteManager.changePage(${i})">
                        ${i}
                    </button>
                `;
            } else if (i === this.state.currentPage - 2 || i === this.state.currentPage + 2) {
                paginationHTML += '<span class="pagination-dots">...</span>';
            }
        }

        // Pulsante successivo
        paginationHTML += `
            <button class="btn btn-page" 
                    ${this.state.currentPage === totalPages ? 'disabled' : ''}
                    onclick="instituteManager.changePage(${this.state.currentPage + 1})">
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
        this.renderInstitutes();
        // Scroll to top
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    /**
     * Apre il modale di modifica
     */
    openEditModal(instituteId) {
        const institute = this.state.institutes.get(instituteId);
        if (!institute) return;

        const modalContent = `
            <form id="editInstituteForm" class="institute-form">
                <input type="hidden" name="id" value="${institute.id}">
                
                <div class="form-group">
                    <label for="nome">Nome Istituto</label>
                    <input type="text" id="nome" name="nome" 
                           value="${Utils.escapeHtml(institute.nome)}" required
                           minlength="3" maxlength="100">
                </div>

                <div class="form-group">
                    <label for="indirizzo">Indirizzo</label>
                    <input type="text" id="indirizzo" name="indirizzo" 
                           value="${Utils.escapeHtml(institute.indirizzo)}" required
                           minlength="5" maxlength="200">
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="citta">Città</label>
                        <input type="text" id="citta" name="citta" 
                               value="${Utils.escapeHtml(institute.citta)}" required
                               minlength="2" maxlength="100">
                    </div>
                    
                    <div class="form-group">
                        <label for="provincia">Provincia</label>
                        <input type="text" id="provincia" name="provincia" 
                               value="${Utils.escapeHtml(institute.provincia)}" required
                               pattern="[A-Za-z]{2}" maxlength="2"
                               oninput="this.value = this.value.toUpperCase()">
                    </div>

                    <div class="form-group">
                        <label for="cap">CAP</label>
                        <input type="text" id="cap" name="cap" 
                               value="${institute.cap}" required
                               pattern="\\d{5}" maxlength="5">
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="telefono">Telefono</label>
                        <input type="tel" id="telefono" name="telefono" 
                               value="${Utils.escapeHtml(institute.telefono || '')}"
                               pattern="\\+?[\\d\\s-]{10,}">
                    </div>

                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" 
                               value="${Utils.escapeHtml(institute.email || '')}">
                    </div>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" 
                            onclick="instituteManager.closeModals()">
                        Annulla
                    </button>
                    <button type="submit" class="btn btn-primary">
                        Salva Modifiche
                    </button>
                </div>
            </form>
        `;

        const modal = this.elements.editInstituteModal;
        modal.querySelector('.modal-content').innerHTML = modalContent;
        modal.querySelector('.modal-title').textContent = 'Modifica Istituto';
        modal.classList.remove('hidden');

        const form = document.getElementById('editInstituteForm');
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleEditInstitute(instituteId, new FormData(e.target));
        });
    }

    /**
     * Chiude tutti i modali
     */
    closeModals() {
        document.querySelectorAll('.modal').forEach(modal => {
            modal.classList.add('hidden');
        });
    }

    /**
     * Verifica la presenza di modifiche non salvate
     */
    hasUnsavedChanges() {
        const forms = document.querySelectorAll('form');
        return Array.from(forms).some(form => {
            const data = new FormData(form);
            return Array.from(data.entries()).some(([name, value]) => {
                const input = form.querySelector(`[name="${name}"]`);
                return input && input.defaultValue !== value;
            });
        });
    }
}

// Inizializzazione
let instituteManager = null;

document.addEventListener('DOMContentLoaded', () => {
    instituteManager = new InstituteManager();
    // Esponi l'istanza globalmente per i listener inline
    window.instituteManager = instituteManager;
});

export default InstituteManager;