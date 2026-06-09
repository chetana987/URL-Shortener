const API = {
    baseUrl: '',

    getToken() {
        return localStorage.getItem('token');
    },

    setToken(token) {
        localStorage.setItem('token', token);
    },

    getRefreshToken() {
        return localStorage.getItem('refreshToken');
    },

    setRefreshToken(token) {
        if (token) localStorage.setItem('refreshToken', token);
    },

    clearToken() {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    decodeToken(token) {
        try {
            const payload = token.split('.')[1];
            return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
        } catch {
            return null;
        }
    },

    isTokenExpiringSoon(token, marginSeconds = 60) {
        const decoded = this.decodeToken(token);
        if (!decoded || !decoded.exp) return false;
        return (decoded.exp * 1000) - Date.now() < marginSeconds * 1000;
    },

    getHeaders(multipart = false, skipAuth = false) {
        const headers = {};
        if (!multipart) {
            headers['Content-Type'] = 'application/json';
        }
        if (!skipAuth) {
            const token = this.getToken();
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        }
        return headers;
    },

    async refreshAccessToken() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) throw new Error('No refresh token');
        const response = await this.request('/api/v1/auth/refresh', {
            method: 'POST',
            body: JSON.stringify({ refreshToken }),
            skipAuth: true,
        });
        this.setToken(response.data.token);
        this.setRefreshToken(response.data.refreshToken);
        return response.data.token;
    },

    async request(endpoint, options = {}) {
        const skipAuth = options.skipAuth || false;
        delete options.skipAuth;

        if (!skipAuth && this.isAuthenticated() && this.isTokenExpiringSoon(this.getToken())) {
            try {
                await this.refreshAccessToken();
            } catch {
                this.clearToken();
            }
        }

        const config = {
            headers: this.getHeaders(false, skipAuth),
            ...options,
        };

        try {
            const response = await fetch(`${this.baseUrl}${endpoint}`, config);
            const data = await response.json();

            if (!response.ok) {
                const error = {
                    status: response.status,
                    message: data.message || 'An error occurred',
                    data: data,
                };
                if (response.status === 429) {
                    error.message = data.message || 'Too many requests. Please wait and try again.';
                }
                if (response.status === 401 && !skipAuth && this.getRefreshToken()) {
                    try {
                        await this.refreshAccessToken();
                        config.headers['Authorization'] = `Bearer ${this.getToken()}`;
                        const retryResponse = await fetch(`${this.baseUrl}${endpoint}`, config);
                        const retryData = await retryResponse.json();
                        if (!retryResponse.ok) {
                            const retryError = {
                                status: retryResponse.status,
                                message: retryData.message || 'An error occurred',
                                data: retryData,
                            };
                            if (retryResponse.status === 429) {
                                retryError.message = retryData.message || 'Too many requests. Please wait and try again.';
                            }
                            if (retryResponse.status === 401) this.clearToken();
                            throw retryError;
                        }
                        return retryData;
                    } catch (refreshError) {
                        if (refreshError.status) throw refreshError;
                        this.clearToken();
                        throw error;
                    }
                }
                throw error;
            }

            return data;
        } catch (error) {
            if (error.status) throw error;
            throw {
                status: 0,
                message: 'Network error. Please check your connection.',
                data: null,
            };
        }
    },

    async logout() {
        try {
            await this.request('/api/v1/auth/logout', { method: 'POST' });
        } catch {
            // Ignore server errors — clear locally regardless
        }
        this.clearToken();
    },

    /* Auth */
    async register(data) {
        return this.request('/api/v1/auth/register', {
            method: 'POST',
            body: JSON.stringify(data),
        });
    },

    async login(data) {
        return this.request('/api/v1/auth/login', {
            method: 'POST',
            body: JSON.stringify(data),
        });
    },

    async getCurrentUser() {
        return this.request('/api/v1/auth/me', { method: 'GET' });
    },

    async updateProfile(data) {
        return this.request('/api/v1/auth/profile', {
            method: 'PUT',
            body: JSON.stringify(data),
        });
    },

    async changePassword(data) {
        return this.request('/api/v1/auth/change-password', {
            method: 'POST',
            body: JSON.stringify(data),
        });
    },

    /* URL Management */
    async createShortUrl(data) {
        return this.request('/api/v1/urls', {
            method: 'POST',
            body: JSON.stringify(data),
        });
    },

    async getUrls(page = 0, size = 20) {
        return this.request(`/api/v1/urls?page=${page}&size=${size}`, { method: 'GET' });
    },

    async getInactiveUrls(page = 0, size = 20) {
        return this.request(`/api/v1/urls/inactive?page=${page}&size=${size}`, { method: 'GET' });
    },

    async getExpiredUrls(page = 0, size = 20) {
        return this.request(`/api/v1/urls/expired?page=${page}&size=${size}`, { method: 'GET' });
    },

    async getUrlDetails(shortCode) {
        return this.request(`/api/v1/urls/${shortCode}`, { method: 'GET' });
    },

    async updateUrl(shortCode, data) {
        return this.request(`/api/v1/urls/${shortCode}`, {
            method: 'PUT',
            body: JSON.stringify(data),
        });
    },

    async deactivateUrl(shortCode) {
        return this.request(`/api/v1/urls/${shortCode}`, { method: 'DELETE' });
    },

    async activateUrl(shortCode) {
        return this.request(`/api/v1/urls/${shortCode}/activate`, { method: 'POST' });
    },

    async extendExpiration(shortCode, days = 30) {
        return this.request(`/api/v1/urls/${shortCode}/extend?days=${days}`, { method: 'POST' });
    },

    /* Analytics */
    async getUrlStats(shortCode) {
        return this.request(`/api/v1/analytics/urls/${shortCode}`, { method: 'GET' });
    },

    async getClickHistory(shortCode, page = 0, size = 20) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/clicks?page=${page}&size=${size}`, { method: 'GET' });
    },

    async getTotalClicks(shortCode) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/total-clicks`, { method: 'GET' });
    },

    async getUniqueVisitors(shortCode) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/unique-visitors`, { method: 'GET' });
    },

    async getLastClickTime(shortCode) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/last-click`, { method: 'GET' });
    },

    async getTopBrowsers(shortCode, limit = 10) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/browsers?limit=${limit}`, { method: 'GET' });
    },

    async getTopCountries(shortCode, limit = 10) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/countries?limit=${limit}`, { method: 'GET' });
    },

    async getTopReferers(shortCode, limit = 10) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/referers?limit=${limit}`, { method: 'GET' });
    },

    async getDailyStats(shortCode, days = 30) {
        return this.request(`/api/v1/analytics/urls/${shortCode}/daily?days=${days}`, { method: 'GET' });
    },
};

/* Utility Functions */
const Utils = {
    showToast(message, type = 'success') {
        const container = document.getElementById('toastContainer');
        if (!container) return;

        const bgColor = type === 'success' ? '#00cec9' : type === 'error' ? '#fd79a8' : type === 'warning' ? '#fdcb6e' : '#6c5ce7';

        const toast = document.createElement('div');
        toast.className = 'toast align-items-center text-white border-0 show';
        toast.style.background = bgColor;
        toast.style.minWidth = '280px';
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;

        container.appendChild(toast);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    },

    showError(elementId, message) {
        const el = document.getElementById(elementId);
        if (el) {
            el.textContent = message;
            el.style.display = 'block';
        }
    },

    hideError(elementId) {
        const el = document.getElementById(elementId);
        if (el) el.style.display = 'none';
    },

    showLoading(elementId) {
        const el = document.getElementById(elementId);
        if (el) el.classList.add('show');
    },

    hideLoading(elementId) {
        const el = document.getElementById(elementId);
        if (el) el.classList.remove('show');
    },

    formatDate(dateStr) {
        if (!dateStr) return '—';
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    },

    timeAgo(dateStr) {
        if (!dateStr) return '—';
        const now = new Date();
        const date = new Date(dateStr);
        const diff = Math.floor((now - date) / 1000);

        if (diff < 60) return 'just now';
        if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
        if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
        if (diff < 2592000) return `${Math.floor(diff / 86400)}d ago`;
        return this.formatDate(dateStr);
    },

    copyToClipboard(text, btn) {
        navigator.clipboard.writeText(text).then(() => {
            const original = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-check"></i> Copied!';
            btn.classList.add('btn-success');
            btn.classList.remove('btn-outline-primary');
            setTimeout(() => {
                btn.innerHTML = original;
                btn.classList.remove('btn-success');
                btn.classList.add('btn-outline-primary');
            }, 2000);
        }).catch(() => {
            this.showToast('Failed to copy', 'error');
        });
    },

    generateQR(shortCode, containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;

        container.innerHTML = '';
        const img = document.createElement('img');
        img.src = `/api/v1/qr/${shortCode}`;
        img.alt = 'QR Code';
        img.className = 'img-fluid rounded';
        img.style.maxWidth = '200px';
        img.onerror = () => {
            container.innerHTML = '<p class="text-muted small">QR code unavailable</p>';
        };
        container.appendChild(img);
    },

    setLoading(btn, loading) {
        if (loading) {
            btn.classList.add('btn-loading');
            btn.disabled = true;
        } else {
            btn.classList.remove('btn-loading');
            btn.disabled = false;
        }
    },

    getUrlParams() {
        const params = {};
        const query = window.location.search.substring(1);
        query.split('&').forEach(pair => {
            const [key, val] = pair.split('=');
            if (key) params[decodeURIComponent(key)] = decodeURIComponent(val || '');
        });
        return params;
    },

    redirectToLogin() {
        if (!API.isAuthenticated() && !window.location.pathname.includes('login') && !window.location.pathname.includes('signup') && window.location.pathname !== '/') {
            window.location.href = '/app/login.html';
        }
    }
};

/* Auth guard — run on pages that require authentication */
document.addEventListener('DOMContentLoaded', () => {
    const protectedPages = ['/app/dashboard.html'];
    const currentPath = window.location.pathname;

    if (protectedPages.some(p => currentPath.endsWith(p) || currentPath === p)) {
        if (!API.isAuthenticated()) {
            window.location.href = '/app/login.html?redirect=' + encodeURIComponent(currentPath);
            return;
        }
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            API.logout().then(() => {
                Utils.showToast('Logged out successfully');
                setTimeout(() => window.location.href = '/', 500);
            });
        });
    }

    /* Update nav based on auth state */
    const authNav = document.getElementById('authNav');
    if (authNav) {
        if (API.isAuthenticated()) {
            const isDashboard = window.location.pathname === '/app/dashboard.html';
            authNav.innerHTML = `
                <li class="nav-item"><a class="nav-link${isDashboard ? ' active' : ''}" href="/app/dashboard.html"><i class="fas fa-chart-bar"></i> Dashboard</a></li>
                <li class="nav-item"><a class="nav-link" href="#" id="logoutBtn"><i class="fas fa-sign-out-alt"></i> Logout</a></li>
            `;
            document.getElementById('logoutBtn')?.addEventListener('click', (e) => {
                e.preventDefault();
                API.logout().then(() => {
                    Utils.showToast('Logged out successfully');
                    setTimeout(() => window.location.href = '/', 500);
                });
            });
        } else {
            authNav.innerHTML = `
                <li class="nav-item"><a class="nav-link" href="/app/login.html"><i class="fas fa-sign-in-alt"></i> Login</a></li>
                <li class="nav-item"><a class="nav-link" href="/app/signup.html"><i class="fas fa-user-plus"></i> Sign Up</a></li>
            `;
        }
    }
});
