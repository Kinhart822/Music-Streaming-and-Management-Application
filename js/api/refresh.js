/**
 * Refresh access token using refreshToken and email
 * @param {string} email - User's email
 * @param {string} refreshToken - Current refresh token
 * @returns {Promise<string>} New access token
 */
const refreshAccessToken = async (email, refreshToken) => {
    try {
        console.log('Refreshing token for:', email);
        const response = await fetch('http://localhost:8080/api/v1/auth/refresh', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({email, refreshToken})
        });

        console.log('Refresh response status:', response.status);

        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch {
                throw new Error('Invalid refresh token');
            }
            throw new Error(errorData.message || 'Invalid refresh token');
        }

        const data = await response.json();
        console.log('Refresh response data:', data);
        const newAccessToken = data;

        // Store new tokens
        localStorage.setItem('accessToken', newAccessToken);

        return newAccessToken;
    } catch (error) {
        console.error('Refresh token error:', error);
        // Clear tokens and redirect to log in
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('email');
        window.location.href = '../auth/login_register.html';
        throw error;
    }
};

/**
 * Fetch with automatic token refresh on 401 Unauthorized
 * @param {string} url - API endpoint
 * @param {RequestInit} options - Fetch options
 * @returns {Promise<Response>} Fetch response
 */
const fetchWithRefresh = async (url, options = {}) => {
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');
    const email = localStorage.getItem('email');

    // Add Authorization header, set Content-Type only if not FormData
    options.headers = {
        ...options.headers,
        'Authorization': `Bearer ${accessToken}`,
        'Accept': 'application/json'
    };
    if (!(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
    }

    try {
        const response = await fetch(url, options);
        console.log('Fetch response status:', response.status);

        if (!accessToken || !refreshToken || !email) {
            window.location.href = '../auth/login_register.html';
            throw new Error('No tokens or email available');
        }

        if (response.status === 401 || response.status === 500) {
            // Token expired, try refreshing
            const newAccessToken = await refreshAccessToken(email, refreshToken);
            // Update Authorization header with new token
            options.headers['Authorization'] = `Bearer ${newAccessToken}`;
            // Retry original request
            return await fetch(url, options);
        }

        return response;
    } catch (error) {
        console.error('Fetch with refresh error:', error);
        throw error;
    }
};

// Export fetchWithRefresh for use in other modules
export { fetchWithRefresh };