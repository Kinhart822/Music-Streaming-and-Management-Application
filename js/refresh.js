/**
 * Refresh access token using refreshToken and email
 * @param {string} email - User's email
 * @param {string} refreshToken - Current refresh token
 * @returns {Promise<string | null>} New access token or null if refresh fails
 */
const refreshAccessToken = async (email, refreshToken) => {
    try {
        console.log('[Refresh] Refreshing token for:', email);
        const response = await fetch('https://music-streaming-and-management.onrender.com/api/v1/auth/refresh', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({email, refreshToken})
        });

        console.log('[Refresh] Response status:', response.status);

        if (!response.ok) {
            if (response.status === 401) {
                console.error('[Refresh] Refresh token expired');
                clearCurrentUserData();
                showSessionExpiredAlert();
                return null;
            }
            console.error('[Refresh] Refresh failed with status:', response.status);
            throw new Error('Failed to refresh token');
        }

        const data = await response.json();
        console.log('[Refresh] Success response data:', data);
        const newAccessToken = data;

        // Store new access token
        sessionStorage.setItem(`user_${email}_accessToken`, newAccessToken);

        return newAccessToken;
    } catch (error) {
        console.error('[Refresh] Token refresh error:', error);
        // Clear sessionStorage for the current user and redirect to log in
        clearCurrentUserData();
        window.location.href = '../auth/login_register.html';
        throw error;
    }
};

/**
 * Fetch with automatic token refresh
 * @param {string} url - API endpoint
 * @param {RequestInit} options - Fetch options
 * @returns {Promise<Response>} Fetch response
 */
const fetchWithRefresh = async (url, options = {}) => {
    const currentUserEmail = sessionStorage.getItem('currentUserEmail');
    if (!currentUserEmail) {
        console.error('[Fetch] No current user email, redirecting to login');
        window.location.href = '../auth/login_register.html';
        throw new Error('No current user email available');
    }

    const accessToken = sessionStorage.getItem(`user_${currentUserEmail}_accessToken`);
    const refreshToken = sessionStorage.getItem(`user_${currentUserEmail}_refreshToken`);

    // Set default headers
    options.headers = {
        ...options.headers,
        'Accept': 'application/json',
    };
    if (!(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
    }

    // Add Authorization header if access token exists
    if (accessToken) {
        options.headers['Authorization'] = `Bearer ${accessToken}`;
    }

    console.log('[Fetch] Request:', {
        url,
        method: options.method || 'GET',
        headers: options.headers,
        body: options.body instanceof FormData ? '[FormData]' : options.body,
    });

    try {
        const response = await fetch(url, options);
        console.log('[Fetch] Response status:', response.status, 'URL:', url);

        // Handle token expiration (401, 403, 500)
        if (response.status === 401 || response.status === 403 || response.status === 500) {
            console.log('[Fetch] Token expired or unauthorized, attempting to refresh');

            if (!refreshToken) {
                console.error('[Fetch] No refresh token available');
                clearCurrentUserData();
                showSessionExpiredAlert();
                window.location.href = '../auth/login_register.html';
                throw new Error('No refresh token available');
            }

            const newAccessToken = await refreshAccessToken(currentUserEmail, refreshToken);
            if (!newAccessToken) {
                console.error('[Fetch] Refresh failed, redirecting to login');
                clearCurrentUserData();
                showSessionExpiredAlert();
                window.location.href = '../auth/login_register.html';
                throw new Error('Token refresh failed');
            }

            // Retry with new token
            options.headers['Authorization'] = `Bearer ${newAccessToken}`;
            console.log('[Fetch] Retrying with new token:', url);
            const retryResponse = await fetch(url, options);
            console.log('[Fetch] Retry response status:', retryResponse.status, 'URL:', url);

            // If retry fails, do not attempt another refresh
            if (retryResponse.status === 401 || retryResponse.status === 403 || retryResponse.status === 500) {
                console.error('[Fetch] Retry failed. Status:', retryResponse.status, 'URL:', retryResponse.url);
                try {
                    const errorText = await retryResponse.text();
                    console.error('[Fetch] Response body:', errorText);
                    clearCurrentUserData();
                    showSessionExpiredAlert();
                    window.location.href = '../auth/login_register.html';
                    throw new Error('Unauthorized after token refresh: ' + errorText);
                } catch (err) {
                    console.error('[Fetch] Error reading response body:', err);
                    throw new Error('Failed to process retry response');
                }
            }

            return retryResponse;
        }

        return response;
    } catch (error) {
        console.error('[Fetch] Error for URL:', url, 'Error:', error.message);
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

/**
 * Show session expired alert to the user
 */
const showSessionExpiredAlert = () => {
    alert('Session expired. Please sign in again.');
};

// Export fetchWithRefresh for use in other modules
export {fetchWithRefresh};