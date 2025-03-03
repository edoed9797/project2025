/**
 * Gestione Report e Statistiche
 * Gestisce la generazione e visualizzazione di report e statistiche del sistema
 */
import Utils from '../common/utils.js';
import auth from '../common/authentication.js';

class ReportManager {
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
            revenueChart: document.getElementById('revenueChart'),
            productChart: document.getElementById('productChart'),
            maintenanceChart: document.getElementById('maintenanceChart'),
            statsGrid: document.getElementById('statsGrid'),
            dateRangeStart: document.getElementById('dateRangeStart'),
            dateRangeEnd: document.getElementById('dateRangeEnd'),
            instituteFilter: document.getElementById('instituteFilter'),
            exportButton: document.getElementById('exportButton'),
            printButton: document.getElementById('printButton'),
            reportFilters: document.getElementById('reportFilters')
        };

        // Verifica presenza elementi richiesti
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
        const currentDate = new Date();
        const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);

        this.state = {
            dateRange: {
                start: firstDayOfMonth,
                end: currentDate
            },
            selectedInstitute: 'all',
            reports: {
                revenue: null,
                products: null,
                maintenance: null,
                stats: null
            },
            charts: new Map()
        };
    }

    /**
     * Inizializza il componente
     */
    async initialize() {
        try {
            Utils.toggleLoading(true);

            // Inizializzazioni parallele
            await Promise.all([
                this.initializeFilters(),
                this.loadReportData()
            ]);

            this.initializeCharts();
            this.setupEventListeners();
            this.renderUI();

            console.log('Gestore report inizializzato');
        } catch (error) {
            console.error('Errore inizializzazione:', error);
            Utils.showToast('Errore durante l\'inizializzazione dei report', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Inizializza i filtri
     */
    async initializeFilters() {
        try {
            // Setup date picker
            flatpickr(this.elements.dateRangeStart, {
                defaultDate: this.state.dateRange.start,
                maxDate: 'today',
                locale: 'it',
                onChange: ([date]) => this.updateDateRange('start', date)
            });

            flatpickr(this.elements.dateRangeEnd, {
                defaultDate: this.state.dateRange.end,
                maxDate: 'today',
                locale: 'it',
                onChange: ([date]) => this.updateDateRange('end', date)
            });

            // Carica e popola select istituti
            const institutes = await Utils.apiCall('/api/istituti');

            this.elements.instituteFilter.innerHTML = `
                <option value="all">Tutti gli istituti</option>
                ${institutes.map(institute => `
                    <option value="${institute.id}">
                        ${Utils.escapeHtml(institute.nome)}
                    </option>
                `).join('')}
            `;

        } catch (error) {
            console.error('Errore inizializzazione filtri:', error);
            throw error;
        }
    }

    /**
     * Configura i listener degli eventi
     */
    setupEventListeners() {
        // Cambio istituto
        this.elements.instituteFilter.addEventListener('change', async (e) => {
            this.state.selectedInstitute = e.target.value;
            await this.refreshReports();
        });

        // Export
        this.elements.exportButton.addEventListener('click', () => {
            this.exportReports();
        });

        // Print
        this.elements.printButton.addEventListener('click', () => {
            this.printReports();
        });
    }

    /**
     * Aggiorna il range date
     */
    async updateDateRange(type, date) {
        if (!date) return;

        this.state.dateRange[type] = date;

        // Validazione range
        if (this.state.dateRange.start > this.state.dateRange.end) {
            if (type === 'start') {
                this.state.dateRange.end = new Date(date);
            } else {
                this.state.dateRange.start = new Date(date);
            }
        }

        await this.refreshReports();
    }

    /**
     * Carica i dati dei report
     */
    async loadReportData() {
        try {
            const [revenue, products, maintenance, stats] = await Promise.all([
                this.fetchRevenueData(),
                this.fetchProductData(),
                this.fetchMaintenanceData(),
                this.fetchStatisticsData()
            ]);

            this.state.reports = {
                revenue: this.normalizeRevenueData(revenue),
                products: this.normalizeProductData(products),
                maintenance: this.normalizeMaintenanceData(maintenance),
                stats: this.normalizeStatisticsData(stats)
            };

        } catch (error) {
            console.error('Errore caricamento dati report:', error);
            throw error;
        }
    }
    /**
     * Recupera i dati dei ricavi
     */
    async fetchRevenueData() {
        const params = new URLSearchParams({
            dataInizio: this.state.dateRange.start.toISOString(),
            dataFine: this.state.dateRange.end.toISOString(),
            istitutoId: this.state.selectedInstitute
        });

        return await Utils.apiCall(`/api/reports/ricavi?${params}`);
    }

    /**
     * Recupera i dati dei prodotti
     */
    async fetchProductData() {
        const params = new URLSearchParams({
            dataInizio: this.state.dateRange.start.toISOString(),
            dataFine: this.state.dateRange.end.toISOString(),
            istitutoId: this.state.selectedInstitute
        });

        return await Utils.apiCall(`/api/reports/prodotti?${params}`);
    }

    /**
     * Recupera i dati delle manutenzioni
     */
    async fetchMaintenanceData() {
        const params = new URLSearchParams({
            dataInizio: this.state.dateRange.start.toISOString(),
            dataFine: this.state.dateRange.end.toISOString(),
            istitutoId: this.state.selectedInstitute
        });

        return await Utils.apiCall(`/api/reports/manutenzioni?${params}`);
    }

    /**
     * Recupera i dati delle statistiche
     */
    async fetchStatisticsData() {
        const params = new URLSearchParams({
            dataInizio: this.state.dateRange.start.toISOString(),
            dataFine: this.state.dateRange.end.toISOString(),
            istitutoId: this.state.selectedInstitute
        });

        return await Utils.apiCall(`/api/reports/statistiche?${params}`);
    }

    /**
     * Normalizza i dati dei ricavi
     */
    normalizeRevenueData(data) {
        return {
            giornalieri: data.giornalieri.map(item => ({
                data: new Date(item.data),
                importo: parseFloat(item.importo),
                transazioni: parseInt(item.transazioni)
            })),
            totale: parseFloat(data.totale),
            mediaGiornaliera: parseFloat(data.mediaGiornaliera)
        };
    }

    /**
     * Normalizza i dati dei prodotti
     */
    normalizeProductData(data) {
        return {
            prodotti: data.prodotti.map(item => ({
                id: item.id,
                nome: item.nome,
                quantita: parseInt(item.quantita),
                ricavo: parseFloat(item.ricavo)
            })),
            totaleVendite: parseInt(data.totaleVendite),
            prodottoPiuVenduto: data.prodottoPiuVenduto,
            prodottoMenoVenduto: data.prodottoMenoVenduto
        };
    }

    /**
     * Normalizza i dati delle manutenzioni
     */
    normalizeMaintenanceData(data) {
        return {
            perTipo: {
                preventiva: parseInt(data.perTipo.preventiva),
                correttiva: parseInt(data.perTipo.correttiva),
                emergenza: parseInt(data.perTipo.emergenza)
            },
            perMacchina: Object.entries(data.perMacchina).reduce((acc, [id, info]) => {
                acc[id] = {
                    conteggio: parseInt(info.conteggio),
                    tempoMedio: parseFloat(info.tempoMedio)
                };
                return acc;
            }, {})
        };
    }

    /**
     * Normalizza i dati delle statistiche
     */
    normalizeStatisticsData(data) {
        return {
            ricaviTotali: parseFloat(data.ricaviTotali),
            totaleTransazioni: parseInt(data.totaleTransazioni),
            mediaTransazione: parseFloat(data.mediaTransazione),
            tassoManutenzione: parseFloat(data.tassoManutenzione),
            prodottoPiuVenduto: data.prodottoPiuVenduto,
            prodottoMenoVenduto: data.prodottoMenoVenduto,
            efficienza: parseFloat(data.efficienza)
        };
    }

    /**
     * Inizializza i grafici
     */
    initializeCharts() {
        // Distruggi grafici esistenti se presenti
        this.destroyCharts();

        // Configura e crea nuovi grafici
        this.initializeRevenueChart();
        this.initializeProductChart();
        this.initializeMaintenanceChart();
    }

    /**
     * Inizializza il grafico dei ricavi
     */
    initializeRevenueChart() {
        if (!this.elements.revenueChart) return;

        const ctx = this.elements.revenueChart.getContext('2d');
        const data = this.prepareRevenueChartData();

        this.state.charts.set('revenue', new Chart(ctx, {
            type: 'line',
            data: data,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: 'Andamento Ricavi'
                    },
                    legend: {
                        position: 'bottom'
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false,
                        callbacks: {
                            label: (context) => {
                                const value = context.raw;
                                return `Ricavi: ${Utils.formatCurrency(value)}`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        type: 'time',
                        time: {
                            unit: 'day',
                            displayFormats: {
                                day: 'dd/MM'
                            }
                        },
                        title: {
                            display: true,
                            text: 'Data'
                        }
                    },
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Ricavi (€)'
                        },
                        ticks: {
                            callback: value => Utils.formatCurrency(value)
                        }
                    }
                }
            }
        }));
    }

    /**
     * Inizializza il grafico dei prodotti
     */
    initializeProductChart() {
        if (!this.elements.productChart) return;

        const ctx = this.elements.productChart.getContext('2d');
        const data = this.prepareProductChartData();

        this.state.charts.set('products', new Chart(ctx, {
            type: 'bar',
            data: data,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: 'Vendite per Prodotto'
                    },
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const value = context.raw;
                                return `Vendite: ${value}`;
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Quantità Vendute'
                        },
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        }));
    }
    /**
         * Inizializza il grafico delle manutenzioni
         */
    initializeMaintenanceChart() {
        if (!this.elements.maintenanceChart) return;

        const ctx = this.elements.maintenanceChart.getContext('2d');
        const data = this.prepareMaintenanceChartData();

        this.state.charts.set('maintenance', new Chart(ctx, {
            type: 'doughnut',
            data: data,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: 'Manutenzioni per Tipo'
                    },
                    legend: {
                        position: 'bottom'
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const value = context.raw;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((value / total) * 100).toFixed(1);
                                return `${context.label}: ${value} (${percentage}%)`;
                            }
                        }
                    }
                }
            }
        }));
    }

    /**
     * Prepara i dati per il grafico dei ricavi
     */
    prepareRevenueChartData() {
        const data = this.state.reports.revenue?.giornalieri || [];
        return {
            labels: data.map(d => d.data),
            datasets: [{
                label: 'Ricavi Giornalieri',
                data: data.map(d => d.importo),
                borderColor: '#2563eb',
                backgroundColor: 'rgba(37, 99, 235, 0.1)',
                borderWidth: 2,
                tension: 0.4,
                fill: true
            }]
        };
    }

    /**
     * Prepara i dati per il grafico dei prodotti
     */
    prepareProductChartData() {
        const data = this.state.reports.products?.prodotti || [];
        const sortedData = [...data].sort((a, b) => b.quantita - a.quantita);

        return {
            labels: sortedData.map(p => p.nome),
            datasets: [{
                data: sortedData.map(p => p.quantita),
                backgroundColor: '#10b981',
                borderColor: '#059669',
                borderWidth: 1,
                borderRadius: 4
            }]
        };
    }

    /**
     * Prepara i dati per il grafico delle manutenzioni
     */
    prepareMaintenanceChartData() {
        const data = this.state.reports.maintenance?.perTipo || {
            preventiva: 0,
            correttiva: 0,
            emergenza: 0
        };

        return {
            labels: ['Preventiva', 'Correttiva', 'Emergenza'],
            datasets: [{
                data: [
                    data.preventiva,
                    data.correttiva,
                    data.emergenza
                ],
                backgroundColor: [
                    '#10b981', // verde
                    '#f59e0b', // giallo
                    '#ef4444'  // rosso
                ],
                borderWidth: 1
            }]
        };
    }

    /**
     * Aggiorna tutti i report
     */
    async refreshReports() {
        try {
            Utils.toggleLoading(true);

            await this.loadReportData();
            this.updateCharts();
            this.updateStatistics();

        } catch (error) {
            console.error('Errore aggiornamento report:', error);
            Utils.showToast('Errore durante l\'aggiornamento dei report', 'error');
        } finally {
            Utils.toggleLoading(false);
        }
    }

    /**
     * Aggiorna i grafici con i nuovi dati
     */
    updateCharts() {
        this.state.charts.forEach((chart, type) => {
            switch (type) {
                case 'revenue':
                    chart.data = this.prepareRevenueChartData();
                    break;
                case 'products':
                    chart.data = this.prepareProductChartData();
                    break;
                case 'maintenance':
                    chart.data = this.prepareMaintenanceChartData();
                    break;
            }
            chart.update('none'); // aggiornamento senza animazione
        });
    }

    /**
     * Aggiorna il pannello statistiche
     */
    updateStatistics() {
        const stats = this.state.reports.stats || {
            ricaviTotali: 0,
            totaleTransazioni: 0,
            mediaTransazione: 0,
            tassoManutenzione: 0,
            efficienza: 0
        };

        this.elements.statsGrid.innerHTML = this.createStatisticsHTML(stats);
    }

    /**
     * Crea l'HTML per le statistiche
     */
    createStatisticsHTML(stats) {
        return `
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-icon revenue">
                    <i class="fas fa-euro-sign"></i>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${Utils.formatCurrency(stats.ricaviTotali)}</div>
                    <div class="stat-label">Ricavi Totali</div>
                </div>
            </div>

            <div class="stat-card">
                <div class="stat-icon transactions">
                    <i class="fas fa-shopping-cart"></i>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.totaleTransazioni.toLocaleString()}</div>
                    <div class="stat-label">Transazioni</div>
                </div>
            </div>

            <div class="stat-card">
                <div class="stat-icon average">
                    <i class="fas fa-calculator"></i>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${Utils.formatCurrency(stats.mediaTransazione)}</div>
                    <div class="stat-label">Media Transazione</div>
                </div>
            </div>

            <div class="stat-card">
                <div class="stat-icon maintenance">
                    <i class="fas fa-tools"></i>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.tassoManutenzione.toFixed(1)}%</div>
                    <div class="stat-label">Tasso Manutenzione</div>
                </div>
            </div>

            <div class="stat-card">
                <div class="stat-icon efficiency">
                    <i class="fas fa-chart-line"></i>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.efficienza.toFixed(1)}%</div>
                    <div class="stat-label">Efficienza</div>
                </div>
            </div>
        </div>
    `;
    }

    /**
     * Esporta i report in formato PDF
     */
    async exportReports() {
        try {
            Utils.toggleLoading(true, this.elements.exportButton);

            const response = await Utils.apiCall('/api/reports/export', {
                method: 'POST',
                body: JSON.stringify({
                    dateRange: this.state.dateRange,
                    istitutoId: this.state.selectedInstitute,
                    data: this.state.reports
                })
            });

            if (!response.url) throw new Error('URL download non valido');

            // Download del file
            const link = document.createElement('a');
            link.href = response.url;
            link.download = `report_${Utils.formatDateForFilename(new Date())}.pdf`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            Utils.showToast('Report esportato con successo', 'success');

        } catch (error) {
            console.error('Errore esportazione report:', error);
            Utils.showToast('Errore durante l\'esportazione del report', 'error');
        } finally {
            Utils.toggleLoading(false, this.elements.exportButton);
        }
    }
    /**
         * Stampa i report
         */
    printReports() {
        // Crea una nuova finestra per la stampa
        const printWindow = window.open('', '_blank');
        if (!printWindow) {
            Utils.showToast('Errore: Popup bloccati. Abilita i popup per stampare.', 'error');
            return;
        }

        const content = this.createPrintContent();

        printWindow.document.write(content);
        printWindow.document.close();

        // Attendi il caricamento dei contenuti
        printWindow.onload = () => {
            try {
                // Aggiorna i grafici nella finestra di stampa
                this.updatePrintCharts(printWindow);

                // Stampa dopo un breve ritardo per permettere il rendering dei grafici
                setTimeout(() => {
                    printWindow.print();
                    printWindow.onafterprint = () => printWindow.close();
                }, 500);

            } catch (error) {
                console.error('Errore durante la stampa:', error);
                printWindow.close();
                Utils.showToast('Errore durante la preparazione della stampa', 'error');
            }
        };
    }

    /**
     * Crea il contenuto HTML per la stampa
     */
    createPrintContent() {
        const dateRange = {
            start: this.state.dateRange.start.toLocaleDateString('it-IT'),
            end: this.state.dateRange.end.toLocaleDateString('it-IT')
        };

        const stats = this.state.reports.stats;
        const institute = this.elements.instituteFilter.options[
            this.elements.instituteFilter.selectedIndex
        ].text;

        return `
        <!DOCTYPE html>
        <html lang="it">
        <head>
            <meta charset="UTF-8">
            <title>Report Sistema Distributori</title>
            <style>
                ${this.getPrintStyles()}
            </style>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        </head>
        <body>
            <div class="print-container">
                <div class="print-header">
                    <h1>Report Sistema Distributori</h1>
                    <div class="print-meta">
                        <p>Periodo: ${dateRange.start} - ${dateRange.end}</p>
                        <p>Istituto: ${Utils.escapeHtml(institute)}</p>
                        <p>Generato il: ${new Date().toLocaleString('it-IT')}</p>
                    </div>
                </div>

                <div class="print-section">
                    <h2>Riepilogo Statistiche</h2>
                    ${this.createPrintableStats(stats)}
                </div>

                <div class="print-section charts-grid">
                    <div class="chart-container">
                        <h3>Andamento Ricavi</h3>
                        <canvas id="printRevenueChart"></canvas>
                    </div>
                    
                    <div class="chart-container">
                        <h3>Vendite per Prodotto</h3>
                        <canvas id="printProductChart"></canvas>
                    </div>
                    
                    <div class="chart-container">
                        <h3>Manutenzioni per Tipo</h3>
                        <canvas id="printMaintenanceChart"></canvas>
                    </div>
                </div>

                <div class="print-section">
                    <h2>Dettaglio Manutenzioni</h2>
                    ${this.createPrintableMaintenanceDetails()}
                </div>
            </div>
        </body>
        </html>
    `;
    }

    /**
     * Crea statistiche stampabili
     */
    createPrintableStats(stats) {
        if (!stats) return '<p>Nessuna statistica disponibile</p>';

        return `
        <div class="stats-grid">
            <div class="stat-item">
                <div class="stat-label">Ricavi Totali</div>
                <div class="stat-value">${Utils.formatCurrency(stats.ricaviTotali)}</div>
            </div>
            <div class="stat-item">
                <div class="stat-label">Transazioni</div>
                <div class="stat-value">${stats.totaleTransazioni.toLocaleString()}</div>
            </div>
            <div class="stat-item">
                <div class="stat-label">Media Transazione</div>
                <div class="stat-value">${Utils.formatCurrency(stats.mediaTransazione)}</div>
            </div>
            <div class="stat-item">
                <div class="stat-label">Tasso Manutenzione</div>
                <div class="stat-value">${stats.tassoManutenzione.toFixed(1)}%</div>
            </div>
            <div class="stat-item">
                <div class="stat-label">Efficienza Sistema</div>
                <div class="stat-value">${stats.efficienza.toFixed(1)}%</div>
            </div>
        </div>
    `;
    }

    /**
     * Crea dettagli manutenzione stampabili
     */
    createPrintableMaintenanceDetails() {
        const maintenance = this.state.reports.maintenance;
        if (!maintenance) return '<p>Nessun dato sulle manutenzioni disponibile</p>';

        return `
        <div class="maintenance-details">
            <div class="maintenance-summary">
                <h3>Riepilogo per Tipo</h3>
                <table>
                    <tr>
                        <th>Tipo</th>
                        <th>Interventi</th>
                    </tr>
                    <tr>
                        <td>Preventiva</td>
                        <td>${maintenance.perTipo.preventiva}</td>
                    </tr>
                    <tr>
                        <td>Correttiva</td>
                        <td>${maintenance.perTipo.correttiva}</td>
                    </tr>
                    <tr>
                        <td>Emergenza</td>
                        <td>${maintenance.perTipo.emergenza}</td>
                    </tr>
                </table>
            </div>

            <div class="maintenance-machines">
                <h3>Dettaglio per Macchina</h3>
                <table>
                    <tr>
                        <th>Macchina</th>
                        <th>Interventi</th>
                        <th>Tempo Medio (ore)</th>
                    </tr>
                    ${Object.entries(maintenance.perMacchina)
                .map(([id, data]) => `
                            <tr>
                                <td>Macchina #${id}</td>
                                <td>${data.conteggio}</td>
                                <td>${data.tempoMedio.toFixed(1)}</td>
                            </tr>
                        `).join('')}
                </table>
            </div>
        </div>
    `;
    }

    /**
     * Stili CSS per la stampa
     */
    getPrintStyles() {
        return `
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 2cm;
        }

        .print-container {
            max-width: 100%;
            margin: 0 auto;
        }

        .print-header {
            text-align: center;
            margin-bottom: 2rem;
            padding-bottom: 1rem;
            border-bottom: 2px solid #eee;
        }

        .print-meta {
            color: #666;
            font-size: 0.9rem;
        }

        .print-section {
            margin-bottom: 2rem;
            page-break-inside: avoid;
        }

        h1, h2, h3 {
            color: #2563eb;
            margin-bottom: 1rem;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1rem;
            margin-bottom: 2rem;
        }

        .stat-item {
            padding: 1rem;
            border: 1px solid #e5e7eb;
            border-radius: 0.5rem;
        }

        .stat-label {
            font-weight: bold;
            color: #666;
            margin-bottom: 0.5rem;
        }

        .stat-value {
            font-size: 1.25rem;
            color: #2563eb;
        }

        .charts-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 2rem;
        }

        .chart-container {
            height: 300px;
            margin-bottom: 2rem;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin: 1rem 0;
        }

        th, td {
            padding: 0.75rem;
            border: 1px solid #e5e7eb;
            text-align: left;
        }

        th {
            background-color: #f9fafb;
            font-weight: bold;
        }

        tr:nth-child(even) {
            background-color: #f9fafb;
        }

        @media print {
            body {
                padding: 0;
            }

            .print-container {
                width: 100%;
            }

            .chart-container {
                page-break-inside: avoid;
            }
        }
    `;
    }

    /**
     * Aggiorna i grafici nella finestra di stampa
     */
    updatePrintCharts(printWindow) {
        // Revenue Chart
        const revenueCtx = printWindow.document.getElementById('printRevenueChart');
        new Chart(revenueCtx, {
            type: 'line',
            data: this.prepareRevenueChartData(),
            options: {
                ...this.state.charts.get('revenue').options,
                animation: false
            }
        });

        // Product Chart
        const productCtx = printWindow.document.getElementById('printProductChart');
        new Chart(productCtx, {
            type: 'bar',
            data: this.prepareProductChartData(),
            options: {
                ...this.state.charts.get('products').options,
                animation: false
            }
        });

        // Maintenance Chart
        const maintenanceCtx = printWindow.document.getElementById('printMaintenanceChart');
        new Chart(maintenanceCtx, {
            type: 'doughnut',
            data: this.prepareMaintenanceChartData(),
            options: {
                ...this.state.charts.get('maintenance').options,
                animation: false
            }
        });
    }

    /**
     * Distrugge i grafici esistenti
     */
    destroyCharts() {
        this.state.charts.forEach(chart => chart.destroy());
        this.state.charts.clear();
    }

    /**
     * Pulisce le risorse
     */
    destroy() {
        this.destroyCharts();

        // Rimuovi event listeners
        this.elements.exportButton?.removeEventListener('click', this.exportReports);
        this.elements.printButton?.removeEventListener('click', this.printReports);
        this.elements.instituteFilter?.removeEventListener('change', this.refreshReports);
    }
}

// Inizializzazione
let reportManager = null;

document.addEventListener('DOMContentLoaded', () => {
    reportManager = new ReportManager();

    // Cleanup quando si lascia la pagina
    window.addEventListener('unload', () => {
        reportManager?.destroy();
    });
});

export default ReportManager;