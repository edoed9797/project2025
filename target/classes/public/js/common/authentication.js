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
				console.log(response);
                localStorage.setItem(this.tokenKey, response.token);
                localStorage.setItem(this.roleKey, response.ruolo);
                localStorage.setItem(this.userNameKey, username);
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
        const token = this.getToken();
        if (!token) return false;
        
        try {
            // Verifica semplice della scadenza del token JWT
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expirationTime = payload.exp * 1000; // Converti in millisecondi
            return Date.now() < expirationTime;
        } catch (e) {
            console.error('Token validation error:', e);
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