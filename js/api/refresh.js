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
            body: JSON.stringify({ email, refreshToken })
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

        // Store new access token
        sessionStorage.setItem(`user_${email}_accessToken`, newAccessToken);

        return newAccessToken;
    } catch (error) {
        console.error('Refresh token error:', error);
        // Clear sessionStorage for the current user and redirect to login
        clearCurrentUserData();
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
    const currentUserEmail = sessionStorage.getItem('currentUserEmail');
    if (!currentUserEmail) {
        console.error('No current user email found, redirecting to login');
        window.location.href = '../auth/login_register.html';
        throw new Error('No current user email available');
    }

    const accessToken = sessionStorage.getItem(`user_${currentUserEmail}_accessToken`);
    const refreshToken = sessionStorage.getItem(`user_${currentUserEmail}_refreshToken`);

    if (!accessToken || !refreshToken) {
        console.error('Missing tokens for user:', currentUserEmail, 'redirecting to login');
        clearCurrentUserData();
        window.location.href = '../auth/login_register.html';
        throw new Error('No tokens available');
    }

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
        console.log('Fetching:', url, 'Options:', {
            method: options.method || 'GET',
            headers: options.headers,
            body: options.body instanceof FormData ? '[FormData]' : options.body
        });
        const response = await fetch(url, options);
        console.log('Fetch response status:', response.status, 'URL:', url);

        if (response.status === 401 || response.status === 403) {
            console.log('Token expired or unauthorized, attempting to refresh');
            // Try refreshing the token
            const newAccessToken = await refreshAccessToken(currentUserEmail, refreshToken);
            // Update Authorization header
            options.headers['Authorization'] = `Bearer ${newAccessToken}`;
            // Retry original request
            console.log('Retrying with new token:', url);
            const retryResponse = await fetch(url, options);
            console.log('Retry response status:', retryResponse.status, 'URL:', url);

            if (retryResponse.status === 401 || retryResponse.status === 403) {
                console.error('Retry failed, redirecting to login');
                clearCurrentUserData();
                window.location.href = '../auth/login_register.html';
                throw new Error('Unauthorized after token refresh');
            }
            return retryResponse;
        }

        if (response.status === 500) {
            console.error('Server error (500) for URL:', url);
            throw new Error('Server error occurred');
        }

        return response;
    } catch (error) {
        console.error('Fetch error for URL:', url, 'Error:', error.message);
        throw error;
    }
};

/**
 * Clear sessionStorage data for the current user
 */
function clearCurrentUserData() {
    const currentUserEmail = sessionStorage.getItem('currentUserEmail');
    if (currentUserEmail) {
        sessionStorage.removeItem(`user_${currentUserEmail}_accessToken`);
        sessionStorage.removeItem(`user_${currentUserEmail}_refreshToken`);
        sessionStorage.removeItem(`user_${currentUserEmail}_userType`);
        sessionStorage.removeItem('currentUserEmail');
    }
}

// Export fetchWithRefresh for use in other modules
export { fetchWithRefresh };