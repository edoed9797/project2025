// auth-utils.js

export class AuthService {
    static isAuthenticated() {
        return !!localStorage.getItem('authToken');
    }

    static getToken() {
        return localStorage.getItem('authToken');
    }

    static getUserRole() {
        return localStorage.getItem('userRole');
    }

    static logout() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userRole');
        window.location.href = '/login.html';
    }

    static async refreshToken() {
        try {
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`
                }
            });

            if (response.ok) {
                const { token } = await response.json();
                localStorage.setItem('authToken', token);
                return true;
            }
            return false;
        } catch (error) {
            return false;
        }
    }

    static async verifyAuth() {
        if (!this.isAuthenticated()) {
            window.location.href = '/login.html';
            return false;
        }

        try {
            const response = await fetch('/api/auth/verify', {
                headers: {
                    'Authorization': `Bearer ${this.getToken()}`
                }
            });

            if (!response.ok) {
                const refreshed = await this.refreshToken();
                if (!refreshed) {
                    this.logout();
                    return false;
                }
            }
            return true;
        } catch (error) {
            this.logout();
            return false;
        }
    }
}