/**
 * Gestione Cialde e Prodotti
 * Gestisce l'inventario e la configurazione delle cialde e dei prodotti.
 */
import mqttClient from '../common/mqtt.js';
import Utils from '../common/utils.js';
import auth from '../common/authentication.js';

class PodManager {
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
            // Cialde
            podsList: document.getElementById('podsList'),
            addPodForm: document.getElementById('addPodForm'),
            editPodModal: document.getElementById('editPodModal'),
            searchInput: document.getElementById('searchPod'),
            filterType: document.getElementById('filterType'),
            sortSelect: document.getElementById('sortSelect'),
            stockWarningLevel: document.getElementById('stockWarningLevel'),
            
            // Prodotti
            productsGrid: document.getElementById('productsGrid'),
            addProductForm: document.getElementById('addProductForm'),
            editProductModal: document.getElementById('editProductModal'),
            
            // Modali
            confirmModal: document.getElementById('confirmModal'),
            restockModal: document.getElementById('restockModal')
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
            pods: new Map(),
            products: new Map(),
            filters: {
                type: 'all',
                search: '',
                sort: 'name-asc'
            },
            stockThreshold: 20,
            loading: false,
            initialized: false
        };
    }

    /**
     * Inizializza il componente
     */
    async initialize() {
        try {
            Utils.toggleLoading(true);
            this.state.loading = true;
            
            // Carica dati iniziali
            await Promise.all([
                this.loadPods()
            ]);

            // Setup MQTT e event listeners
            await this.initializeMQTTSubscriptions();
            this.setupEventListeners();

            // Renderizza UI
            this.renderUI();

            this.state.initialized = true;
            console.log('Gestore cialde inizializzato');

        } catch (error) {
            console.error('Errore inizializzazione:', error);
            Utils.showToast('Errore durante l\'inizializzazione', 'error');
        } finally {
            this.state.loading = false;
            Utils.toggleLoading(false);
        }
    }

    /**
     * Carica l'elenco delle cialde
     */
    async loadPods() {
        try {
            const response = await Utils.apiCall('/api/cialde');
            
            this.state.pods.clear();
            response.forEach(pod => {
                this.state.pods.set(pod.id, this.normalizePodData(pod));
            });
        } catch (error) {
            console.error('Errore caricamento cialde:', error);
            throw error;
        }
    }

    /**
     * Normalizza i dati di una cialda
     */
    normalizePodData(pod) {
        return {
            id: pod.id,
            nome: pod.nome,
            tipo: pod.tipoCialda,
            quantitaAttuale: pod.quantitaAttuale || 0,
            quantitaMassima: pod.quantitaMassima || 100,
            prezzo: pod.prezzo || 0
        };
    }

    /**
     * Configura le sottoscrizioni MQTT
     */
    async initializeMQTTSubscriptions() {
        try {
            const topics = [
                // Aggiornamenti stock
                {
                    topic: 'cialde/+/stock',
                    handler: (topic, message) => {
                        const podId = parseInt(topic.split('/')[1]);
                        this.handleStockUpdate(podId, JSON.parse(message));
                    }
                },
                // Allarmi scorte
                {
                    topic: 'cialde/allarmi/scorte',
                    handler: (topic, message) => {
                        this.handleStockAlert(JSON.parse(message));
                    }
                }
            ];

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
        // Form cialde
        this.elements.addPodForm?.addEventListener('submit', (event) => {
            event.preventDefault();
            this.handleAddPod(new FormData(event.target));
        });

        // Form prodotti
        this.elements.addProductForm?.addEventListener('submit', (event) => {
            event.preventDefault();
            this.handleAddProduct(new FormData(event.target));
        });

        // Filtri e ordinamento
        this.elements.filterType?.addEventListener('change', () => {
            this.updateFilters('type');
        });

        this.elements.sortSelect?.addEventListener('change', () => {
            this.updateFilters('sort');
        });

        // Ricerca
        this.elements.searchInput?.addEventListener('input',
            Utils.debounce(() => this.updateFilters('search'), 300)
        );

        // Soglia scorte
        this.elements.stockWarningLevel?.addEventListener('change',
            Utils.debounce((event) => {
                const value = parseInt(event.target.value);
                if (!isNaN(value)) {
                    this.updateStockThreshold(value);
                }
            }, 500)
        );

        // Gestione modali
        document.querySelectorAll('.modal-close').forEach(button => {
            button.addEventListener('click', () => this.closeModals());
        });

        // Handler globali
        window.podManager = this;
        window.editPod = (id) => this.openEditPodModal(id);
        window.deletePod = (id) => this.handleDeletePod(id);
        window.restockPod = (id) => this.openRestockModal(id);
        window.editProduct = (id) => this.openEditProductModal(id);
        window.deleteProduct = (id) => this.handleDeleteProduct(id);
    }

    /**
     * Aggiorna i filtri e rigenera la lista
     */
    updateFilters(filterType) {
        const element = this.elements[`filter${filterType.charAt(0).toUpperCase() + filterType.slice(1)}`];
        if (!element) return;

        this.state.filters[filterType] = element.value;
        this.renderPods();
    }

    /**
     * Renderizza l'interfaccia utente
     */
    renderUI() {
        this.renderPods();
        this.renderProducts();
        this.updateStockIndicators();
    }

    /**
     * Renderizza l'elenco delle cialde
     */
    renderPods() {
        const filteredPods = Array.from(this.state.pods.values())
            .filter(this.filterPod.bind(this))
            .sort(this.sortPods.bind(this));

        this.elements.podsList.innerHTML = filteredPods
            .map(pod => this.renderPodCard(pod))
            .join('');
    }

    /**
     * Renderizza una card cialda
     */
    renderPodCard(pod) {
        const stockStatus = this.getStockStatus(pod);
        const stockPercentage = (pod.quantitaAttuale / pod.quantitaMassima * 100).toFixed(1);
        
        return `
            <div class="pod-card ${stockStatus.class}" data-id="${pod.id}">
                <div class="card-header">
                    <div class="header-content">
                        <h3>${Utils.escapeHtml(pod.nome)}</h3>
                        <span class="pod-type">${Utils.escapeHtml(pod.tipo)}</span>
                    </div>
                    <div class="stock-badge">
                        <i class="fas ${stockStatus.icon}"></i>
                        ${stockStatus.text}
                    </div>
                </div>

                <div class="card-body">
                    <div class="stock-container">
                        <div class="stock-bar-container">
                            <div class="stock-bar" 
                                style="width: ${stockPercentage}%"
                                title="Livello scorte: ${stockPercentage}%">
                            </div>
                        </div>
                        <div class="stock-info">
                            <span class="current">${pod.quantitaAttuale}</span>
                            <span class="separator">/</span>
                            <span class="max">${pod.quantitaMassima}</span>
                        </div>
                    </div>

                    <div class="pod-details">
                        <div class="detail-item">
                            <i class="fas fa-euro-sign"></i>
                            <span>${Utils.formatCurrency(pod.prezzo)}</span>
                        </div>
                        <div class="detail-item">
                            <i class="fas fa-exclamation-triangle"></i>
                            <span>Attuale: ${pod.quantitaAttuale}</span>
                        </div>
                        <div class="detail-item">
                            <i class="fas fa-clock"></i>
                            <span>Ultimo agg.: ${Utils.formatDate(pod.ultimoAggiornamento)}</span>
                        </div>
                    </div>
                </div>

                <div class="card-actions">
                    <button class="btn btn-edit" 
                            onclick="podManager.openEditPodModal(${pod.id})"
                            title="Modifica cialda">
                        <i class="fas fa-edit"></i>
                    </button>

                    <button class="btn btn-restock"
                            onclick="podManager.openRestockModal(${pod.id})"
                            title="Rifornisci scorte"
                            ${pod.quantitaAttuale >= pod.quantitaMassima ? 'disabled' : ''}>
                        <i class="fas fa-box"></i>
                    </button>

                    <button class="btn btn-danger"
                            onclick="podManager.handleDeletePod(${pod.id})"
                            title="Elimina cialda"
                            ${this.isPodInUse(pod.id) ? 'disabled' : ''}>
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Ottiene lo stato dello stock di una cialda
     */
    getStockStatus(pod) {
        const percentage = (pod.quantitaAttuale / pod.quantitaMassima) * 100;

        if (percentage <= 0.05) {
            return {
                class: 'stock-critical',
                icon: 'fa-exclamation-circle',
                text: 'Quasi esaurita'
            };
        }
        
        if (percentage <= this.state.stockThreshold) {
            return {
                class: 'stock-warning',
                icon: 'fa-exclamation-triangle',
                text: 'In esaurimento'
            };
        }

        return {
            class: 'stock-normal',
            icon: 'fa-check-circle',
            text: 'Disponibile'
        };
    }

    /**
     * Renderizza i prodotti
     */
    renderProducts() {
        const products = Array.from(this.state.products.values())
            .filter(product => product.attivo)
            .sort((a, b) => a.nome.localeCompare(b.nome));

        this.elements.productsGrid.innerHTML = products
            .map(product => this.renderProductCard(product))
            .join('');
    }

    /**
     * Renderizza una card prodotto
     */
    renderProductCard(product) {
        const podsInfo = product.cialde.map(pod => {
            const podData = this.state.pods.get(pod.id);
            return podData ? {
                nome: podData.nome,
                quantitaAttuale: pod.quantitaAttuale,
                disponibile: podData.quantitaAttuale >= pod.quantitaAttuale
            } : null;
        }).filter(Boolean);

        const isAvailable = podsInfo.every(pod => pod.disponibile);

        return `
            <div class="product-card ${isAvailable ? '' : 'unavailable'}" data-id="${product.id}">
                <div class="card-header">
                    <div class="header-content">
                        <h3>${Utils.escapeHtml(product.nome)}</h3>
                        <span class="price">${Utils.formatCurrency(product.prezzo)}</span>
                    </div>
                    <div class="availability-badge ${isAvailable ? 'available' : 'unavailable'}">
                        ${isAvailable ? 'Disponibile' : 'Non Disponibile'}
                    </div>
                </div>

                <div class="card-body">
                    <p class="description">
                        ${Utils.escapeHtml(product.descrizione)}
                    </p>
                    
                    <div class="pods-list">
                        <h4>Composizione:</h4>
                        <ul>
                            ${podsInfo.map(pod => `
                                <li class="${pod.disponibile ? 'available' : 'unavailable'}">
                                    <span class="pod-name">${Utils.escapeHtml(pod.nome)}</span>
                                    <span class="pod-quantity">x${pod.quantitaAttuale}</span>
                                </li>
                            `).join('')}
                        </ul>
                    </div>
                </div>

                <div class="card-actions">
                    <button class="btn btn-edit" 
                            onclick="podManager.openEditProductModal(${product.id})"
                            title="Modifica prodotto">
                        <i class="fas fa-edit"></i>
                    </button>

                    <button class="btn btn-danger"
                            onclick="podManager.handleDeleteProduct(${product.id})"
                            title="Elimina prodotto">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Gestisce l'aggiunta di una nuova cialda
     */
    async handleAddPod(formData) {
        try {
            Utils.toggleLoading(true);

            const podData = {
                nome: formData.get('nome'),
                tipo: formData.get('tipoCialda'),
                quantitaAttuale: parseInt(formData.get('quantitaAttuale')),
                quantitaMassima: parseInt(formData.get('quantitaMassima')),
                prezzo: parseFloat(formData.get('prezzo'))
            };

            // Validazione
            this.validatePodData(podData);

            const response = await Utils.apiCall('/api/cialde', {
                method: 'POST',
                body: JSON.stringify(podData)
            });

            this.state.pods.set(response.id, this.normalizePodData(response));
            this.renderUI();

            Utils.showToast('Cialda aggiunta con successo', 'success');
            this.elements.addPodForm.reset();

        } catch (error) {
            console.error('Errore aggiunta cialda:', error);
            Utils.showToast(error.message || 'Errore durante l\'aggiunta', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Gestisce l'aggiunta di un nuovo prodotto
     */
    async handleAddProduct(formData) {
        try {
            Utils.toggleLoading(true);

            const productData = {
                nome: formData.get('nome'),
                descrizione: formData.get('descrizione'),
                prezzo: parseFloat(formData.get('prezzo')),
                cialde: Array.from(formData.getAll('podIds')).map(podId => ({
                    id: parseInt(podId),
                    quantitaAttuale: parseInt(formData.get(`quantity_${podId}`))
                }))
            };

            // Validazione
            this.validateProductData(productData);

            const response = await Utils.apiCall('/api/prodotti', {
                method: 'POST',
                body: JSON.stringify(productData)
            });

            this.state.products.set(response.id, this.normalizeProductData(response));
            this.renderUI();

            Utils.showToast('Prodotto aggiunto con successo', 'success');
            this.elements.addProductForm.reset();

        } catch (error) {
            console.error('Errore aggiunta prodotto:', error);
            Utils.showToast(error.message || 'Errore durante l\'aggiunta', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Verifica se una cialda è utilizzata in qualche prodotto
     */
    isPodInUse(podId) {
        return Array.from(this.state.products.values())
            .some(product => 
                product.attivo && 
                product.cialde.some(pod => pod.id === podId)
            );
    }

    // /**
    //  * Aggiorna gli indicatori di stock
    //  */
    // updateStockIndicators() {
    //     let lowStock = 0;
    //     let criticalStock = 0;

    //     this.state.pods.forEach(pod => {
    //         if (pod.quantita <= pod.quantitaMinima) {
    //             criticalStock++;
    //         } else if ((pod.quantita / pod.quantitaMassima * 100) <= this.state.stockThreshold) {
    //             lowStock++;
    //         }
    //     });
    // }

    /**
     * Filtra le cialde in base ai criteri selezionati
     */
    filterPod(pod) {
        const { type, search } = this.state.filters;

        // Filtro per tipo
        if (type !== 'all' && pod.tipo !== type) {
            return false;
        }

        // Filtro per ricerca
        if (search) {
            const searchTerm = search.toLowerCase();
            return pod.nome.toLowerCase().includes(searchTerm) ||
                   pod.tipo.toLowerCase().includes(searchTerm);
        }

        return true;
    }

    /**
     * Ordina le cialde in base al criterio selezionato
     */
    sortPods(a, b) {
        const [field, direction] = this.state.filters.sort.split('-');
        const multiplier = direction === 'asc' ? 1 : -1;

        switch (field) {
            case 'nome':
                return multiplier * a.nome.localeCompare(b.nome);
            case 'quantitaAttuale':
                return multiplier * (a.quantitaAttuale - b.quantitaAttuale);
            case 'prezzo':
                return multiplier * (a.prezzo - b.prezzo);
            default:
                return 0;
        }
    }
/**
     * Gestisce il rifornimento di una cialda
     */
async handleRestock(podId, quantity) {
    try {
        Utils.toggleLoading(true);

        const pod = this.state.pods.get(podId);
        if (!pod) throw new Error('Cialda non trovata');

        const response = await Utils.apiCall(`/api/cialde/${podId}/rifornimento`, {
            method: 'POST',
            body: JSON.stringify({ quantitaAttuale: quantity })
        });

        pod.quantitaAttuale = response.quantitaAttuale;
        pod.ultimoAggiornamento = new Date().toISOString();

        this.renderPodCard(pod);
        this.updateStockIndicators();
        this.closeModals();

        Utils.showToast('Rifornimento completato con successo', 'success');
    } catch (error) {
        console.error('Errore rifornimento:', error);
        Utils.showToast(error.message || 'Errore durante il rifornimento', 'error');
    } finally {
        Utils.toggleLoading(false);
    }
}

/**
 * Apre il modale di modifica cialda
 */
openEditPodModal(podId) {
    try {
        const pod = this.state.pods.get(podId);
        if (!pod) throw new Error('Cialda non trovata');

        const modalContent = `
            <div class="modal-header">
                <h3>Modifica Cialda</h3>
                <button class="btn-close" onclick="podManager.closeModals()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="modal-body">
                <form id="editPodForm" class="form">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="nome">Nome</label>
                            <input type="text" id="nome" name="nome" 
                                   value="${Utils.escapeHtml(pod.nome)}"
                                   required minlength="3">
                        </div>
                        <div class="form-group">
                            <label for="tipo">Tipo</label>
                            <select id="tipo" name="tipo" required>
                                <option value="caffe" ${pod.tipo === 'caffe' ? 'selected' : ''}>Caffè</option>
                                <option value="te" ${pod.tipo === 'te' ? 'selected' : ''}>Tè</option>
                                <option value="cioccolata" ${pod.tipo === 'cioccolata' ? 'selected' : ''}>Cioccolata</option>
                                <option value="latte" ${pod.tipo === 'latte' ? 'selected' : ''}>Latte</option>
                            </select>
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="quantitaAttuale">Scorta attuale</label>
                            <input type="number" id="quantitaAttuale" name="quantitaAttuale"
                                   value="${pod.quantitaAttuale}"
                                   required min="1" max="${pod.quantitaMassima - 1}">
                        </div>
                        <div class="form-group">
                            <label for="quantitaMassima">Scorta massima</label>
                            <input type="number" id="quantitaMassima" name="quantitaMassima"
                                   value="${pod.quantitaMassima}"
                                   required min="1">
                        </div>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="prezzo">Prezzo (€)</label>
                            <input type="number" id="prezzo" name="prezzo"
                                   value="${pod.prezzo}"
                                   required min="0.01" step="0.01">
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="button" class="btn btn-secondary" 
                                onclick="podManager.closeModals()">
                            Annulla
                        </button>
                        <button type="submit" class="btn btn-primary">
                            Salva Modifiche
                        </button>
                    </div>
                </form>
            </div>
        `;

        this.elements.editPodModal.innerHTML = modalContent;
        this.elements.editPodModal.classList.remove('hidden');

        // Form handler
        document.getElementById('editPodForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleEditPod(podId, new FormData(e.target));
        });

    } catch (error) {
        Utils.showToast(error.message, 'error');
    }
}

/**
 * Apre il modale di modifica prodotto
 */
openEditProductModal(productId) {
    try {
        const product = this.state.products.get(productId);
        if (!product) throw new Error('Prodotto non trovato');

        const modalContent = `
            <div class="modal-header">
                <h3>Modifica Prodotto</h3>
                <button class="btn-close" onclick="podManager.closeModals()">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="modal-body">
                <form id="editProductForm" class="form">
                    <div class="form-group">
                        <label for="nome">Nome</label>
                        <input type="text" id="nome" name="nome" 
                               value="${Utils.escapeHtml(product.nome)}"
                               required minlength="3">
                    </div>

                    <div class="form-group">
                        <label for="descrizione">Descrizione</label>
                        <textarea id="descrizione" name="descrizione" 
                                required rows="3">${Utils.escapeHtml(product.descrizione)}</textarea>
                    </div>

                    <div class="form-group">
                        <label for="prezzo">Prezzo (€)</label>
                        <input type="number" id="prezzo" name="prezzo"
                               value="${product.prezzo}"
                               required min="0.01" step="0.01">
                    </div>

                    <div class="form-group">
                        <label>Composizione</label>
                        <div class="pods-grid">
                            ${Array.from(this.state.pods.values()).map(pod => `
                                <div class="pod-item">
                                    <div class="pod-check">
                                        <input type="checkbox" 
                                               id="pod_${pod.id}" 
                                               name="podIds" 
                                               value="${pod.id}"
                                               ${product.cialde.some(p => p.id === pod.id) ? 'checked' : ''}>
                                        <label for="pod_${pod.id}">
                                            ${Utils.escapeHtml(pod.nome)}
                                        </label>
                                    </div>
                                    <div class="pod-quantity">
                                        <input type="number" 
                                               name="quantity_${pod.id}" 
                                               value="${product.cialde.find(p => p.id === pod.id)?.quantitaAttuale || 1}"
                                               min="1" 
                                               max="${pod.quantitaMassima}"
                                               ${product.cialde.some(p => p.id === pod.id) ? '' : 'disabled'}>
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="button" class="btn btn-secondary" 
                                onclick="podManager.closeModals()">
                            Annulla
                        </button>
                        <button type="submit" class="btn btn-primary">
                            Salva Modifiche
                        </button>
                    </div>
                </form>
            </div>
        `;

        this.elements.editProductModal.innerHTML = modalContent;
        this.elements.editProductModal.classList.remove('hidden');

        // Handle pod checkboxes
        document.querySelectorAll('input[name="podIds"]').forEach(checkbox => {
            checkbox.addEventListener('change', (e) => {
                const quantityInput = document.querySelector(`input[name="quantity_${e.target.value}"]`);
                quantityInput.disabled = !e.target.checked;
                if (e.target.checked) {
                    quantityInput.focus();
                }
            });
        });

        // Form handler
        document.getElementById('editProductForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleEditProduct(productId, new FormData(e.target));
        });

    } catch (error) {
        Utils.showToast(error.message, 'error');
    }
}

/**
     * Apre il modale di rifornimento
     */
    openRestockModal(podId) {
        try {
            const pod = this.state.pods.get(podId);
            if (!pod) throw new Error('Cialda non trovata');

            const spazioDisponibile = pod.quantitaMassima - pod.quantitaAttuale;
            if (spazioDisponibile <= 0) {
                throw new Error('Scorta già al massimo');
            }

            const modalContent = `
                <div class="modal-header">
                    <h3>Rifornimento Cialda</h3>
                    <button class="btn-close" onclick="podManager.closeModals()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="pod-info">
                        <h4>${Utils.escapeHtml(pod.nome)}</h4>
                        <p>Scorta attuale: ${pod.quantitaAttuale} / ${pod.quantitaMassima}</p>
                        <p>Spazio disponibile: ${spazioDisponibile} unità</p>
                    </div>

                    <form id="restockForm" class="form">
                        <div class="form-group">
                            <label for="quantita">Quantità da aggiungere</label>
                            <input type="number" 
                                   id="quantita" 
                                   name="quantita"
                                   required
                                   min="1"
                                   max="${spazioDisponibile}"
                                   value="${Math.min(spazioDisponibile, 10)}">
                            <small class="form-help">
                                Massimo: ${spazioDisponibile} unità
                            </small>
                        </div>

                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" 
                                    onclick="podManager.closeModals()">
                                Annulla
                            </button>
                            <button type="submit" class="btn btn-primary">
                                Rifornisci
                            </button>
                        </div>
                    </form>
                </div>
            `;

            this.elements.restockModal.innerHTML = modalContent;
            this.elements.restockModal.classList.remove('hidden');

            // Form handler
            document.getElementById('restockForm').addEventListener('submit', async (e) => {
                e.preventDefault();
                const quantity = parseInt(e.target.quantita.value);
                await this.handleRestock(podId, quantity);
            });

        } catch (error) {
            Utils.showToast(error.message, 'error');
        }
    }

    /**
     * Gestisce la conferma di eliminazione
     */
    async confirmDelete(type, id, message) {
        return new Promise((resolve) => {
            const modalContent = `
                <div class="modal-header">
                    <h3>Conferma Eliminazione</h3>
                    <button class="btn-close" onclick="podManager.closeModals()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="confirm-message">
                        <i class="fas fa-exclamation-triangle"></i>
                        <p>${message}</p>
                    </div>
                    <div class="modal-actions">
                        <button class="btn btn-secondary" 
                                onclick="podManager.closeModals()">
                            Annulla
                        </button>
                        <button class="btn btn-danger" id="confirmBtn">
                            Elimina
                        </button>
                    </div>
                </div>
            `;

            this.elements.confirmModal.innerHTML = modalContent;
            this.elements.confirmModal.classList.remove('hidden');

            // Handler conferma
            document.getElementById('confirmBtn').addEventListener('click', () => {
                resolve(true);
                this.closeModals();
            });
        });
    }

    /**
     * Valida i dati di una cialda
     */
    validatePodData(data) {
        const errors = [];

        // Nome
        if (!data.nome?.trim() || data.nome.trim().length < 3) {
            errors.push('Il nome deve essere di almeno 3 caratteri');
        }


        // Quantità
        if (!Number.isInteger(data.quantitaAttuale) || data.quantitaAttuale < 0) {
            errors.push('La quantità deve essere un numero intero positivo');
        }

        // Quantità massima
        if (!Number.isInteger(data.quantitaMassima)) {
            errors.push('La quantità massima deve essere un numero intero');
        }

        // Prezzo
        if (typeof data.prezzo !== 'number') {
            errors.push('Errore valore prezzo');
        }

        if (errors.length > 0) {
            throw new Error(errors.join('\n'));
        }
    }

    /**
     * Valida i dati di un prodotto
     */
    validateProductData(data) {
        const errors = [];

        // Nome
        if (!data.nome?.trim() || data.nome.trim().length < 3) {
            errors.push('Il nome deve essere di almeno 3 caratteri');
        }

        // Descrizione
        if (!data.descrizione?.trim()) {
            errors.push('La descrizione è obbligatoria');
        }

        // Prezzo
        if (typeof data.prezzo !== 'number' || data.prezzo <= 0) {
            errors.push('Il prezzo deve essere maggiore di zero');
        }

        // Cialde
        if (!Array.isArray(data.cialde) || data.cialde.length === 0) {
            errors.push('Devi specificare almeno una cialda');
        } else {
            data.cialde.forEach((cialda, index) => {
                if (!this.state.pods.has(cialda.id)) {
                    errors.push(`Cialda ${cialda.id} non valida`);
                }
                if (!Number.isInteger(cialda.quantitaAttuale) || cialda.quantitaAttuale < 1) {
                    errors.push(`Quantità non valida per la cialda ${index + 1}`);
                }
            });
        }

        if (errors.length > 0) {
            throw new Error(errors.join('\n'));
        }
    }

    /**
     * Gestisce gli aggiornamenti MQTT di stock
     */
    handleStockUpdate(podId, data) {
        const pod = this.state.pods.get(parseInt(podId));
        if (!pod) return;

        pod.quantitaAttuale = data.quantitaAttuale;
        pod.ultimoAggiornamento = new Date().toISOString();

        this.renderPodCard(pod);
        this.updateStockIndicators();

        // Verifica scorta minima
        if (pod.quantitaAttuale <= 1) {
            Utils.showToast(`
                Attenzione: ${pod.nome} esaurita!
            `, 'warning');
        }
    }

    /**
     * Gestisce gli allarmi scorte
     */
    handleStockAlert(alert) {
        const pod = this.state.pods.get(alert.cialdaId);
        if (!pod) return;

        Utils.showToast(`
            ${alert.livello === 'critico' ? '⚠️' : '⚠'} 
            ${alert.messaggio} - ${pod.nome}
        `, alert.livello === 'critico' ? 'error' : 'warning');

        // Aggiorna UI se necessario
        this.updateStockIndicators();
    }

    /**
     * Chiude tutti i modali
     */
    closeModals() {
        document.querySelectorAll('.modal').forEach(modal => {
            modal.classList.add('hidden');
            // Pulizia contenuto per evitare memory leaks
            setTimeout(() => {
                if (modal.classList.contains('hidden')) {
                    modal.innerHTML = '';
                }
            }, 300);
        });
    }

    /**
     * Pulisce le risorse
     */
    destroy() {
        // Rimuovi sottoscrizioni MQTT
        mqttClient.unsubscribe('cialde/+/stock');
        mqttClient.unsubscribe('cialde/allarmi/scorte');

        // Rimuovi handler globali
        delete window.podManager;
        delete window.editPod;
        delete window.deletePod;
        delete window.restockPod;
        delete window.editProduct;
        delete window.deleteProduct;

        // Pulisci lo stato
        this.state.pods.clear();
        this.state.products.clear();
    }
}

// Inizializzazione
let podManager = null;

document.addEventListener('DOMContentLoaded', () => {
    podManager = new PodManager();

    // Cleanup alla chiusura
    window.addEventListener('unload', () => {
        if (podManager) {
            podManager.destroy();
        }
    });
});

export default PodManager;