/**
 * Modulo delle funzioni di utilità per l'applicazione.
 * Fornisce funzionalità comuni come chiamate API, gestione token,
 * formattazione e validazione.
 */

class Utils {
    // Configurazione base
    static API_BASE_URL = '/api';
    static TOKEN_KEY = 'jwt_token';
    static REFRESH_TOKEN_KEY = 'refresh_token';

    /**
     * Esegue una chiamata API
     * @param {string} endpoint - Endpoint API
     * @param {Object} options - Opzioni della richiesta
     * @returns {Promise<any>} Response data
     */
    static async apiCall(endpoint, options = {}) {
        try {
            // Prepara l'URL
            const url = `${this.API_BASE_URL}${endpoint}`;
            

            // Configura le opzioni di default
            const defaultOptions = {
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'same-origin'
            };

            // Aggiunge il token di autenticazione se presente
            const token = this.getToken();
            if (token) {
                defaultOptions.headers['Authorization'] = `Bearer ${token}`;
            }

            // Unisce le opzioni
            const fetchOptions = {
                ...defaultOptions,
                ...options,
                headers: {
                    ...defaultOptions.headers,
                    ...options.headers
                }
            };

            // Esegue la richiesta
            const response = await fetch(url, fetchOptions);
/*
            // Gestisce errori HTTP
            if (!response.ok) {
                // Gestisce token scaduto
                if (response.status === 401) {
                    const refreshed = await this.refreshToken();
                    if (refreshed) {
                        // Riprova la chiamata con il nuovo token
                        return this.apiCall(endpoint, options);
                    }
                    // Se il refresh fallisce, logout
                    throw new Error('Sessione richiesta');
                }
                
                const error = await response.json();
                throw new Error(error.message || 'Errore nella richiesta');
            }
*/
            // Gestisce risposte vuote
            if (response.status === 204) {
                return null;
            }

            // Restituisce i dati
            return await response.json();

        } catch (error) {
            console.error('API call error:', error);
            throw error;
        }
    }
    
    static async apiCall2(endpoint, options = {}) {
        try {
            // Prepara l'URL
            const url = `${endpoint}`;
            

            // Configura le opzioni di default
            const defaultOptions = {
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'same-origin'
            };

            // Aggiunge il token di autenticazione se presente
            const token = this.getToken();
            console.log(token);
            if (token) {
                defaultOptions.headers['Authorization'] = `Bearer ${token}`;
            }

            // Unisce le opzioni
            const fetchOptions = {
                ...defaultOptions,
                ...options,
                headers: {
                    ...defaultOptions.headers,
                    ...options.headers
                }
            };

            // Esegue la richiesta
            const response = await fetch(url, fetchOptions);
/*
            // Gestisce errori HTTP
            if (!response.ok) {
                // Gestisce token scaduto
                if (response.status === 401) {
                    const refreshed = await this.refreshToken();
                    if (refreshed) {
                        // Riprova la chiamata con il nuovo token
                        return this.apiCall(endpoint, options);
                    }
                    // Se il refresh fallisce, logout
                    throw new Error('Sessione richiesta');
                }
                
                const error = await response.json();
                throw new Error(error.message || 'Errore nella richiesta');
            }
*/
            // Gestisce risposte vuote
            if (response.status === 204) {
                return null;
            }

            // Restituisce i dati
            return await response.json();

        } catch (error) {
            console.error('API call error:', error);
            throw error;
        }
    }
    /**
     * Creates a debounced version of a function that delays its execution
     * until after `delay` milliseconds have elapsed since the last time it was called.
     * 
     * @param {Function} func - The function to debounce
     * @param {number} delay - The delay in milliseconds
     * @returns {Function} - The debounced function
     */
    static debounce(func, delay = 300) {
        let timeoutId;
        
        return function (...args) {
            // Clear any existing timeout
            if (timeoutId) {
                clearTimeout(timeoutId);
            }
            
            // Set up new timeout
            timeoutId = setTimeout(() => {
                func.apply(this, args);
            }, delay);
        };
    }
    /**
     * Esegue il refresh del token
     * @returns {Promise<boolean>} Success status
     */
    static async refreshToken() {
        try {
            const refreshToken = this.getRefreshToken();
            if (!refreshToken) {
                return false;
            }

            const response = await fetch(`${this.API_BASE_URL}/auth/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${refreshToken}`
                }
            });

            if (!response.ok) {
                return false;
            }

            const { token, refreshToken: newRefreshToken } = await response.json();
            
            this.setToken(token);
            this.setRefreshToken(newRefreshToken);
            
            return true;

        } catch (error) {
            console.error('Token refresh error:', error);
            return false;
        }
    }

    /**
     * Gestisce il token di autenticazione
     */
    static getToken() {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    static setToken(token) {
        localStorage.setItem(this.TOKEN_KEY, token);
    }

    static getRefreshToken() {
        return localStorage.getItem(this.REFRESH_TOKEN_KEY);
    }

    static setRefreshToken(token) {
        localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
    }

    static clearTokens() {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    }

    /**
     * Gestione logout
     */
    /*static logout() {
        this.clearTokens();
        window.location.href = '/index.html';
    }*/

    /**
     * Mostra un messaggio toast
     * @param {string} message - Messaggio da mostrare
     * @param {string} type - Tipo di toast (success, error, warning, info)
     * @param {number} duration - Durata in ms
     */
    static showToast(message, type = 'info', duration = 3000) {
        const container = document.getElementById('toastContainer') || 
            this.createToastContainer();

        const toast = document.createElement('div');
        toast.className = `toast toast-${type} fade-in`;
        
        const icon = this.getToastIcon(type);
        
        toast.innerHTML = `
            <i class="${icon}"></i>
            <span class="toast-message">${message}</span>
        `;

        container.appendChild(toast);

        // Animazione di entrata
        setTimeout(() => toast.classList.add('show'), 100);

        // Rimozione automatica
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, duration);
    }

    /**
     * Crea il container per i toast se non esiste
     */
    static createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
        return container;
    }

    /**
     * Restituisce l'icona per il tipo di toast
     */
    static getToastIcon(type) {
        const icons = {
            success: 'fas fa-check-circle',
            error: 'fas fa-exclamation-circle',
            warning: 'fas fa-exclamation-triangle',
            info: 'fas fa-info-circle'
        };
        return icons[type] || icons.info;
    }

    /**
     * Formatta un prezzo in Euro
     */
    static formatPrice(amount) {
        return new Intl.NumberFormat('it-IT', {
            style: 'currency',
            currency: 'EUR'
        }).format(amount);
    }

    /**
     * Formatta una data in formato italiano
     */
    static formatDate(date) {
        return new Intl.DateTimeFormat('it-IT', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }).format(new Date(date));
    }

    /**
     * Mostra/Nasconde l'overlay di caricamento
     */
    static toggleLoading(show) {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.classList.toggle('hidden', !show);
        }
    }

    /**
     * Valida una email
     */
    static validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    /**
     * Valida una password
     */
    static validatePassword(password) {
        // Almeno 8 caratteri, una maiuscola, una minuscola, un numero
        const re = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$/;
        return re.test(password);
    }

    /**
     * Sanifica input HTML
     */
    static sanitizeHTML(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
}

export default Utils;