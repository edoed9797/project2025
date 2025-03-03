/**
 * Gestione Dashboard Amministrativa
 * Gestisce l'interfaccia utente della dashboard e la comunicazione con il backend
 */
import mqttClient from '../common/mqtt.js';
import Utils from '../common/utils.js';
import AuthService from '../common/authentication.js';

class DashboardManager {
    constructor() {
        // Inizializza elementi DOM
        this.elements = {
            // Statistiche
            activeMachines: document.getElementById('activeMachines'),
            maintenanceCount: document.getElementById('maintenanceCount'),
            todayRevenue: document.getElementById('todayRevenue'),
            todayDispensed: document.getElementById('todayDispensed'),
            
            // Chart
            revenueChart: document.getElementById('revenueChart'),
            chartPeriod: document.getElementById('chartPeriod'),
            
            // Status
            machineStatusList: document.getElementById('machineStatusList'),
            alertsList: document.getElementById('alertsList'),
            
            // Viste
            adminView: document.getElementById('adminView'),
            guestView: document.getElementById('guestView'),
            
            // Sidebar
            adminNav: document.getElementById('adminNav')
        };

        // Stato dell'applicazione
        this.state = {
            stats: {
                activeMachines: 0,
                maintenanceCount: 0,
                todayRevenue: 0,
                todayDispensed: 0
            },
            chart: null,
            machines: new Map(),
            alerts: [],
            mqttSubscriptions: new Set() // Per gestire le sottoscrizioni MQTT
        };

        // Inizializza
        this.initialize();
    }

    /**
     * Inizializza la dashboard
     */
    async initialize() {
        try {
            // Verifica autenticazione
            if (!await AuthService.checkAuthentication()) {
                this.showGuestView();
                return;
            }

            // Mostra vista admin
            this.showAdminView();

            // Carica dati iniziali
            await this.loadInitialData();

            // Inizializza chart
            this.initializeChart();

            // Setup MQTT
            await this.initializeMQTTSubscriptions();

            // Event listeners
            this.initializeEventListeners();

            console.log('Dashboard inizializzata con successo');

        } catch (error) {
            console.error('Errore inizializzazione dashboard:', error);
            Utils.showToast('Errore durante il caricamento della dashboard', 'error');
        }
    }

    /**
     * Carica i dati iniziali (statistiche, macchine, avvisi)
     */
    async loadInitialData() {
        try {
            const [stats, machines, alerts] = await Promise.all([
                Utils.apiCall('/api/dashboard/stats'),
                Utils.apiCall('/macchine'),
                Utils.apiCall('/api/dashboard/alerts')
            ]);

            // Aggiorna stato
            this.state.stats = stats;
            this.state.machines.clear();
            machines.forEach(machine => {
                this.state.machines.set(machine.id, machine);
            });
            this.state.alerts = alerts;

            // Aggiorna UI
            this.updateStatsUI();
            this.updateMachinesUI();
            this.updateAlertsUI();

        } catch (error) {
            console.error('Errore caricamento dati iniziali:', error);
            throw error;
        }
    }

    /**
     * Inizializza il grafico
     */
    initializeChart() {
        const ctx = this.elements.revenueChart.getContext('2d');
        
        this.state.chart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'Ricavi',
                    data: [],
                    borderColor: '#3b82f6',
                    tension: 0.4,
                    fill: true,
                    backgroundColor: 'rgba(59, 130, 246, 0.1)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            display: true,
                            drawBorder: false
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });

        // Carica dati iniziali
        this.updateChartData();
    }

    /**
     * Inizializza sottoscrizioni MQTT
     */
    async initializeMQTTSubscriptions() {
        try {
            // Sottoscrivi a stati macchine
            if (!this.state.mqttSubscriptions.has('macchine/+/stato')) {
                await mqttClient.subscribe('macchine/+/stato', (topic, message) => {
                    const machineId = parseInt(topic.split('/')[1]);
                    this.handleMachineStatusUpdate(machineId, JSON.parse(message));
                });
                this.state.mqttSubscriptions.add('macchine/+/stato');
            }

            // Sottoscrivi ad avvisi
            if (!this.state.mqttSubscriptions.has('system/alerts')) {
                await mqttClient.subscribe('system/alerts', (topic, message) => {
                    this.handleNewAlert(JSON.parse(message));
                });
                this.state.mqttSubscriptions.add('system/alerts');
            }

        } catch (error) {
            console.error('Errore sottoscrizioni MQTT:', error);
            throw error;
        }
    }

    /**
     * Inizializza event listeners
     */
    initializeEventListeners() {
        // Periodo chart
        this.elements.chartPeriod?.addEventListener('change', () => {
            this.updateChartData();
        });

        // Refresh stato macchine
        document.querySelector('.btn-refresh')?.addEventListener('click', () => {
            this.loadInitialData();
        });
    }

    /**
     * Gestisce l'aggiornamento dello stato di una macchina
     */
    handleMachineStatusUpdate(machineId, status) {
        const machine = this.state.machines.get(machineId);
        if (!machine) return;

        // Aggiorna stato
        machine.stato = status.stato;

        // Aggiorna conteggi
        this.updateMachinesCounts();

        // Aggiorna UI
        this.updateMachineStatusUI(machine);
    }

    /**
     * Gestisce un nuovo avviso
     */
    handleNewAlert(alert) {
        // Aggiungi alla lista
        this.state.alerts.unshift(alert);
        
        // Mantieni solo gli ultimi 10 avvisi
        if (this.state.alerts.length > 10) {
            this.state.alerts.pop();
        }

        // Aggiorna UI
        this.updateAlertsUI();
    }

    /**
     * Aggiorna l'UI delle statistiche
     */
    updateStatsUI() {
        if (!this.elements.activeMachines) return;

        this.elements.activeMachines.textContent = this.state.stats.activeMachines;
        this.elements.maintenanceCount.textContent = this.state.stats.maintenanceCount;
        this.elements.todayRevenue.textContent = Utils.formatPrice(this.state.stats.todayRevenue);
        this.elements.todayDispensed.textContent = this.state.stats.todayDispensed;
    }

    /**
     * Aggiorna i dati del grafico
     */
    async updateChartData() {
        try {
            const period = this.elements.chartPeriod?.value || 'day';
            const data = await Utils.apiCall(`/api/dashboard/revenue?period=${period}`);

            this.state.chart.data.labels = data.labels;
            this.state.chart.data.datasets[0].data = data.values;
            this.state.chart.update();

        } catch (error) {
            console.error('Errore aggiornamento grafico:', error);
            Utils.showToast('Errore aggiornamento grafico', 'error');
        }
    }

    /**
     * Aggiorna l'UI delle macchine
     */
    updateMachinesUI() {
        if (!this.elements.machineStatusList) return;

        this.elements.machineStatusList.innerHTML = Array.from(this.state.machines.values())
            .map(machine => this.renderMachineStatus(machine))
            .join('');

        this.updateMachinesCounts();
    }

    /**
     * Aggiorna i conteggi delle macchine
     */
    updateMachinesCounts() {
        const machines = Array.from(this.state.machines.values());
        
        this.state.stats.activeMachines = machines.filter(m => m.stato === 'active').length;
        this.state.stats.maintenanceCount = machines.filter(m => m.stato === 'maintenance').length;

        this.updateStatsUI();
    }

    /**
     * Renderizza lo stato di una macchina
     */
    renderMachineStatus(machine) {
        const status = this.getMachineStatus(machine.stato);
        
        return `
            <div class="status-item">
                <div class="status-item-icon ${status.class}">
                    <i class="${status.icon}"></i>
                </div>
                <div class="status-item-info">
                    <h4 class="status-item-title">Distributore #${machine.id}</h4>
                    <p class="status-item-subtitle">
                        ${machine.ubicazione} - ${machine.istitutoNome}
                    </p>
                </div>
                <div class="status-badge ${status.class}">
                    ${status.text}
                </div>
            </div>
        `;
    }

    /**
     * Aggiorna l'UI degli avvisi
     */
    updateAlertsUI() {
        if (!this.elements.alertsList) return;

        this.elements.alertsList.innerHTML = this.state.alerts
            .map(alert => this.renderAlert(alert))
            .join('');
    }

    /**
     * Renderizza un avviso
     */
    renderAlert(alert) {
        return `
            <div class="alert-item alert-${alert.type}">
                <div class="alert-icon">
                    <i class="${this.getAlertIcon(alert.type)}"></i>
                </div>
                <div class="alert-info">
                    <h4 class="alert-title">${alert.title}</h4>
                    <p class="alert-message">${alert.message}</p>
                    <span class="alert-time">${Utils.formatDate(alert.timestamp)}</span>
                </div>
            </div>
        `;
    }

    /**
     * Ottiene i dettagli dello stato di una macchina
     */
    getMachineStatus(stato) {
        const statusMap = {
            'active': {
                class: 'success',
                text: 'Attiva',
                icon: 'fas fa-check-circle'
            },
            'maintenance': {
                class: 'warning',
                text: 'In Manutenzione',
                icon: 'fas fa-tools'
            },
            'inactive': {
                class: 'danger',
                text: 'Inattiva',
                icon: 'fas fa-times-circle'
            }
        };

        return statusMap[stato] || {
            class: 'default',
            text: 'Sconosciuto',
            icon: 'fas fa-question-circle'
        };
    }

    /**
     * Ottiene l'icona per un tipo di avviso
     */
    getAlertIcon(type) {
        const icons = {
            'success': 'fas fa-check-circle',
            'warning': 'fas fa-exclamation-triangle',
            'error': 'fas fa-times-circle',
            'info': 'fas fa-info-circle'
        };

        return icons[type] || icons.info;
    }

    /**
     * Mostra la vista amministratore
     */
    showAdminView() {
        if (this.elements.guestView) {
            this.elements.guestView.classList.add('hidden');
        }
        if (this.elements.adminView) {
            this.elements.adminView.classList.remove('hidden');
        }
        if (this.elements.adminNav) {
            this.elements.adminNav.classList.remove('hidden');
        }
    }

    /**
     * Mostra la vista ospite
     */
    showGuestView() {
        if (this.elements.adminView) {
            this.elements.adminView.classList.add('hidden');
        }
        if (this.elements.adminNav) {
            this.elements.adminNav.classList.add('hidden');
        }
        if (this.elements.guestView) {
            this.elements.guestView.classList.remove('hidden');
        }
    }

    /**
     * Pulisce le risorse
     */
    destroy() {
        // Rimuovi sottoscrizioni MQTT
        this.state.mqttSubscriptions.forEach(topic => {
            mqttClient.unsubscribe(topic);
        });
        this.state.mqttSubscriptions.clear();

        // Distruggi chart
        if (this.state.chart) {
            this.state.chart.destroy();
        }

        // Pulisci stato
        this.state.machines.clear();
        this.state.alerts = [];
    }
}

// Inizializzazione quando il DOM è pronto
document.addEventListener('DOMContentLoaded', () => {
    const dashboard = new DashboardManager();

    // Cleanup quando si lascia la pagina
    window.addEventListener('unload', () => {
        dashboard.destroy();
    });
});

export default DashboardManager;