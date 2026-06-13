import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

/**
 * API client for CodeSage backend
 */
const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

// Request interceptor for logging
api.interceptors.request.use(
    (config) => {
        console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
        return config;
    },
    (error) => {
        console.warn('[API] Request error:', error.message);
        return Promise.reject(error);
    }
);

// Response interceptor for error handling
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        console.warn('[API] Response unavailable, dashboard may use demo data:', error.response?.data || error.message);

        if (error.response) {
            // Server responded with error status
            const { status, data } = error.response;

            if (status === 404) {
                throw new Error('Resource not found');
            } else if (status === 500) {
                throw new Error('Server error. Please try again later.');
            } else if (data?.message) {
                throw new Error(data.message);
            }
        } else if (error.request) {
            // Request made but no response
            throw new Error('Unable to connect to server. Please check your connection.');
        }

        throw error;
    }
);

/**
 * Reviews API
 */
export const reviewsAPI = {
    /**
     * Get all reviews with pagination
     */
    getAll: async (page = 0, size = 10) => {
        const response = await api.get('/reviews', { params: { page, size } });
        return response.data;
    },

    /**
     * Get specific review by ID
     */
    getById: async (id) => {
        const response = await api.get(`/reviews/${id}`);
        return response.data;
    },

    /**
     * Get reviews for a specific repository
     */
    getByRepository: async (owner, name, page = 0, size = 10) => {
        const response = await api.get(`/reviews/repo/${owner}/${name}`, {
            params: { page, size },
        });
        return response.data;
    },

    /**
     * Get recent reviews (last 7 days)
     */
    getRecent: async () => {
        const response = await api.get('/reviews/recent');
        return response.data;
    },

    /**
     * Get dashboard statistics
     */
    getStats: async () => {
        const response = await api.get('/reviews/stats');
        return response.data;
    },

    /**
     * Health check
     */
    healthCheck: async () => {
        const response = await api.get('/reviews/health');
        return response.data;
    },
};

export default api;
