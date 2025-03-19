import Utils from '../common/utils.js';

class MachineSelectionManager {
    constructor() {
        this.elements = {
            machinesList: document.getElementById('machinesList'),
            institutesList: document.getElementById('institutesList'),
            userInfo: document.getElementById('userInfo'),
            logoutButton: document.querySelector('button[onclick="logout()"]')
        };

        this.state = {
            machines: new Map(),
            institutes: new Map(),
            isAuthenticated: false,
            token: null
        };

        this.initialize();
    }

    async initialize() {
        try {
            Utils.toggleLoading(true);
            
            // Verifica l'autenticazione e ottieni un token valido
            await this.checkAuthentication();
            
            // Aggiorna l'interfaccia utente in base allo stato di autenticazione
            this.updateUI();
            
            // Carica i dati
            await Promise.all([
                this.loadInstitutes(),
                this.loadMachines()
            ]);
            
            // Renderizza i dati
            this.renderMachines();
            this.renderInstitutes();
            
        } catch (error) {
            console.error('Initialization error:', error);
            Utils.showToast('Errore durante l\'inizializzazione. Riprova.', 'error');
            
            // Mostra contenuti vuoti
            this.renderEmptyState();
        } finally {
            Utils.toggleLoading(false);
        }
    }

    renderEmptyState() {
        // Mostra un messaggio quando non ci sono dati
        if (this.elements.machinesList) {
            this.elements.machinesList.innerHTML = `
                <div class="no-results">
                    <i class="fas fa-exclamation-circle"></i>
                    <p>Impossibile caricare i distributori. Riprova più tardi.</p>
                </div>
            `;
        }
        
        if (this.elements.institutesList) {
            this.elements.institutesList.innerHTML = `
                <div class="no-results">
                    <i class="fas fa-exclamation-circle"></i>
                    <p>Impossibile caricare gli istituti. Riprova più tardi.</p>
                </div>
            `;
        }
    }

    async checkAuthentication() {
        // Controlla se esiste già un token
        let token = localStorage.getItem('jwt_token');
        
        if (token) {
            // Verifica che il token esistente sia valido
            try {
                const response = await fetch('/api/auth/verify', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                if (response.ok) {
                    this.state.isAuthenticated = true;
                    this.state.token = token;
                    return token;
                }
            } catch (error) {
                console.warn('Token esistente non valido');
                localStorage.removeItem('jwt_token');
            }
        }
        
        // Se non c'è token o non è valido, richiedi un token anonimo
        return this.getAnonymousToken();
    }

    async getAnonymousToken() {
    try {
        const response = await fetch('/api/auth/anonymous', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // CORREZIONE: Il token è in data.jwt_token, non in data.token
        const token = data.jwt_token;
        
        if (!token) {
            console.error("Token non trovato nella risposta:", data);
            throw new Error("Token non trovato nella risposta del server");
        }
        
        // Salva il token anonimo
        localStorage.setItem('jwt_token', token);
        localStorage.setItem('userRole', 'anonymous');
        
        this.state.isAuthenticated = false;
        this.state.token = token; // Salva il token nello state
        
        console.log("Token salvato nello stato:", this.state.token);
        return token;
        
    } catch (error) {
        console.error('Errore durante la richiesta del token anonimo:', error);
        Utils.showToast('Errore di connessione al server', 'error');
        throw error;
    }
    }

    updateUI() {
        // Aggiorna l'UI in base allo stato di autenticazione
        if (this.elements.userInfo) {
            if (this.state.isAuthenticated) {
                const username = localStorage.getItem('username') || 'Utente';
                this.elements.userInfo.textContent = `Benvenuto, ${username}`;
            } else {
                this.elements.userInfo.textContent = 'Visitatore';
            }
        }
        
        // Aggiorna il pulsante di logout
        if (this.elements.logoutButton) {
            if (this.state.isAuthenticated) {
                this.elements.logoutButton.textContent = 'Logout';
                this.elements.logoutButton.onclick = () => this.logout();
            } else {
                this.elements.logoutButton.textContent = 'Login';
                this.elements.logoutButton.onclick = () => this.redirectToLogin();
            }
        }
    }

    logout() {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('username');
        localStorage.removeItem('userRole');
        window.location.href = '/index.html';
    }

    redirectToLogin() {
        window.location.href = '/index.html';
    }

    async loadMachines() {
    try {
        // Assicurati di usare il token corretto
        const token = localStorage.getItem('jwt_token');
        console.log("Usando token per richiesta macchine:", token);
        
        const response = await fetch('/api/macchine', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            // Se il token non è valido, richiedi un nuovo token anonimo
            if (response.status === 401) {
                console.warn("Token non valido per macchine, richiedo un nuovo token");
                const newToken = await this.getAnonymousToken();
                return this.retryLoadMachines(newToken);
            }
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const machines = await response.json();
        machines.forEach(machine => {
            this.state.machines.set(machine.id, machine);
        });
        
    } catch (error) {
        console.error('Failed to load machines:', error);
        Utils.showToast('Errore nel caricamento delle macchine', 'error');
        throw error;
    }
}


    async retryLoadMachines(token) {
    try {
        console.log("Ritentativo caricamento macchine con nuovo token:", token);
        
        const response = await fetch('/api/macchine', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });
        
        // Anche se lo status è 401, prova a leggere i dati se ci sono
        const responseText = await response.text();
        console.log("Risposta macchine:", response.status, responseText);
        
        if (responseText && responseText.length > 0) {
            try {
                // Prova a fare il parsing della risposta come JSON
                const machines = JSON.parse(responseText);
                if (Array.isArray(machines)) {
                    machines.forEach(machine => {
                        this.state.machines.set(machine.id, machine);
                    });
                    return; // Se il parsing ha avuto successo, esci dal metodo
                }
            } catch (parseError) {
                console.error("Errore parsing JSON:", parseError);
            }
        }
        
        // Se arriviamo qui, c'è stato un errore nel parsing o non ci sono dati
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
    } catch (error) {
        console.error('Failed to load machines after retry:', error);
        Utils.showToast('Errore nel caricamento delle macchine', 'error');
        throw error;
    }
}
    async loadInstitutes() {
    try {
        // Assicurati di usare il token corretto
        const token = this.state.token || localStorage.getItem('jwt_token');
        console.log("Usando token per richiesta istituti:", token);
        
        const response = await fetch('/api/istituti', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            // Se il token non è valido, richiedi un nuovo token anonimo
            if (response.status === 401) {
                console.warn("Token non valido per istituti, richiedo un nuovo token");
                const newToken = await this.getAnonymousToken();
                return this.retryLoadInstitutes(newToken);
            }
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const institutes = await response.json();
        institutes.forEach(institute => {
            this.state.institutes.set(institute.ID_istituto, institute);
        });

    } catch (error) {
        console.error('Failed to load institutes:', error);
        Utils.showToast('Errore nel caricamento degli istituti', 'error');
        throw error;
    }
}

    async retryLoadInstitutes(token) {
    try {
        console.log("Ritentativo caricamento istituti con nuovo token:", token);
        
        const response = await fetch('/api/istituti', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error("Risposta errore:", errorText);
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const institutes = await response.json();
        institutes.forEach(institute => {
            this.state.institutes.set(institute.ID_istituto, institute);
        });
        
    } catch (error) {
        console.error('Failed to load institutes after retry:', error);
        Utils.showToast('Errore nel caricamento degli istituti', 'error');
        throw error;
    }
}

    renderMachines() {
        if (!this.elements.machinesList) return;

        const machines = Array.from(this.state.machines.values());
        
        if (machines.length === 0) {
            this.elements.machinesList.innerHTML = `
                <div class="no-results">
                    <i class="fas fa-info-circle"></i>
                    <p>Nessun distributore disponibile al momento</p>
                </div>
            `;
            return;
        }

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
            <div id="machine-${machine.id}" class="machine-card" role="listitem" aria-label="Distributore ${machine.id}">
                <div class="card-header">
                    <h3>Distributore #${machine.id}</h3>
                    <span class="status-badge ${status.class}" title="${status.text}" aria-hidden="true"></span>
                </div>
                <div class="card-body">
                    <p class="institute"><i class="fas fa-building" aria-hidden="true"></i> ${instituteName}</p>
                    <p class="status-text">${status.text}</p>
                    <p class="address"><i class="fa fa-map-marker" aria-hidden="true"></i> ${instituteAddress}</p>
                    ${machine.statoDescrizione === 'Attiva' ? `
                        <button class="btn-select" onclick="window.selectMachine(${machine.id})">
                            Seleziona
                        </button>
                    ` : `
                        <button class="btn-disabled" disabled>
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

        const institutes = Array.from(this.state.institutes.values());
        
        if (institutes.length === 0) {
            this.elements.institutesList.innerHTML = `
                <div class="no-results">
                    <i class="fas fa-info-circle"></i>
                    <p>Nessun istituto disponibile al momento</p>
                </div>
            `;
            return;
        }

        this.elements.institutesList.innerHTML = institutes
            .map(institute => this.renderInstituteCard(institute))
            .join('');
    }

    renderInstituteCard(institute) {
        const machinesCount = Array.from(this.state.machines.values())
            .filter(machine => machine.istitutoId === institute.ID_istituto && machine.statoDescrizione === 'Attiva')
            .length;

        return `
            <div class="institute-card" role="listitem" aria-label="Istituto ${institute.nome}">
                <div class="card-header">
                    <h3>${institute.nome}</h3>
                </div>
                <div class="card-body">
                    <p class="address"><i class="fa fa-map-marker" aria-hidden="true"></i> ${institute.indirizzo}</p>
                    <p class="machines-count"><i class="fas fa-coffee" aria-hidden="true"></i> Macchine disponibili: ${machinesCount}</p>
                </div>
            </div>
        `;
    }

    selectMachine(machineId) {
        const machine = this.state.machines.get(parseInt(machineId));
        if (!machine || machine.statoDescrizione !== 'Attiva') {
            Utils.showToast('Questa macchina non è disponibile', 'warning');
            return;
        }

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

    destroy() {
        // Pulisci eventuali event listener o risorse
        console.log('MachineSelectionManager destroyed');
    }
}

// Funzione di debug per testare il token anonimo
async function testAnonymousToken() {
    try {
        const response = await fetch('/api/auth/anonymous', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            console.error("Errore nella richiesta:", response.status);
            return;
        }
        
        const data = await response.json();
        console.log("Token ricevuto:", data);
        
        // Test API macchine
        const machinesResponse = await fetch('/api/macchine', {
            headers: {
                'Authorization': `Bearer ${data.token}`
            }
        });
        
        console.log("Stato risposta macchine:", machinesResponse.status);
        if (machinesResponse.ok) {
            console.log("Macchine:", await machinesResponse.json());
        } else {
            console.error("Errore risposta:", await machinesResponse.text());
        }
    } catch (error) {
        console.error("Errore test:", error);
    }
}

// Funzioni globali
window.logout = function() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('username');
    localStorage.removeItem('user_name');
    localStorage.removeItem('user_role');
    window.location.href = '/index.html';
};

window.testToken = testAnonymousToken;

document.addEventListener('DOMContentLoaded', () => {
	 const token = localStorage.getItem('jwt_token');
    const manager = new MachineSelectionManager();
    
    window.addEventListener('unload', () => {
        manager.destroy();
    });
    
    // Esponi la funzione selectMachine globalmente
    window.selectMachine = (id) => manager.selectMachine(id);
});

export default MachineSelectionManager;