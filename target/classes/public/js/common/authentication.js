/**
 * Authentication module for the vending machine management system
 */
import Utils from './utils.js';

class AuthenticationService {
    constructor() {
        this.baseUrl = '/auth';
        this.tokenKey = 'jwt_token';
        this.roleKey = 'user_role';
        this.userNameKey = 'user_name';
    }

    /**
     * Performs login
     * @param {string} username 
     * @param {string} password 
     * @returns {Promise<boolean>}
     */
    async login(username, password) {
        try {
            const response = await Utils.apiCall(`${this.baseUrl}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            if (response && response.token) {
                localStorage.setItem(this.tokenKey, response.token);
                localStorage.setItem(this.roleKey, response.ruolo);
                localStorage.setItem(this.userNameKey, username);
                localStorage.setItem(this.user_id, response.user_id);
                return true;
            }
            return false;
        } catch (error) {
            console.error('Login error:', error);
            throw new Error('Username o password non validi');
        }
    }

    /**
     * Performs logout
     */
    logout() {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.roleKey);
        localStorage.removeItem(this.userNameKey);
        localStorage.removeItem(this.user_id);
        window.location.href = '/index.html';
    }

    /**
     * Gets the stored username
     * @returns {string|null}
     */
    getUserName() {
        return localStorage.getItem(this.userNameKey);
    }
    
    
    
    /**
     * Verifies if user is authenticated
     * @returns {boolean}
     */
    isAuthenticated() {
        const token = localStorage.getItem('jwt_token');
        if (!token) return false;
        
        // Verifica che il token abbia il formato corretto (tre parti separate da punti)
	    if (!token.match(/^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]*$/)) {
	        console.log('Token format is invalid');
	        localStorage.removeItem('jwt_token'); // Rimuovi il token non valido
	        return false;
	    }
         try {
        // Divide il token nelle sue parti
        const parts = token.split('.');
        
        // Decodifica l'header e il payload
        const header = JSON.parse(atob(parts[0]));
        const payload = JSON.parse(atob(parts[1]));
        
        // Verifica la scadenza del token
        const now = Math.floor(Date.now() / 1000);
        if (payload.exp && payload.exp < now) {
            console.log('Token is expired');
            localStorage.removeItem('jwt_token');
            return false;
        }
        
        return true;
	    } catch (error) {
	        console.log('Token validation error:', error);
	        localStorage.removeItem('jwt_token'); // Rimuovi il token non valido
	        return false;
	    }
	}

    /**
     * Gets user role
     * @returns {string|null}
     */
    getUserRole() {
        const role = localStorage.getItem(this.roleKey);
        return role ? role.toLowerCase() : null;
    }

    /**
     * Verifies if user has admin role
     * @returns {boolean}
     */
    isAdmin() {
        const role = this.getUserRole();
        return role === 'amministratore' || role === 'admin';
    }

    /**
     * Verifies if user has employee role
     * @returns {boolean}
     */
    isEmployee() {
        const role = this.getUserRole();
        return role === 'operatore' || role === 'employee' || role === 'impiegato';
    }
    
    /**
     * Verifies if user has employee role
     * @returns {boolean}
     */
    isTec() {
        const role = this.getUserRole();
        return role === 'tecnico' || role === 'Tecnico';
    }

    /**
     * Gets authentication token
     * @returns {string|null}
     */
    getToken() {
        return localStorage.getItem(this.tokenKey);
    }

    /**
     * Refreshes authentication token
     * @returns {Promise<boolean>}
     */
    async refreshToken() {
        try {
            const currentToken = this.getToken();
            if (!currentToken) {
                throw new Error('No token to refresh');
            }

            const response = await Utils.apiCall(`${this.baseUrl}/refresh`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${currentToken}`
                }
            });

            if (response && response.token) {
                localStorage.setItem(this.tokenKey, response.token);
                return true;
            }
            return false;
        } catch (error) {
            console.error('Token refresh error:', error);
            this.logout();
            throw error;
        }
    }

    /**
     * Protects a route requiring authentication
     * @param {Function} roleCheck Optional role verification function
     */
    static protectRoute(roleCheck = null) {
        const auth = new AuthenticationService();
        
        if (!auth.isAuthenticated()) {
            auth.logout(); // Pulisce lo storage e reindirizza
            return;
        }

        if (roleCheck && !roleCheck(auth)) {
            Utils.showToast('Accesso non autorizzato', 'error');
            window.location.href = '/index.html';
            return;
        }
    }

    /**
     * Protects an admin route
     */
    static protectAdminRoute() {
        AuthenticationService.protectRoute(auth => auth.isAdmin());
    }

    /**
     * Protects an employee route
     */
    static protectEmployeeRoute() {
        AuthenticationService.protectRoute(auth => auth.isAdmin() || auth.isEmployee());
    }
}

// Create a singleton instance
const auth = new AuthenticationService();

// Export both class and singleton
export { AuthenticationService, auth as default };