/**
 * Refresh access token using refreshToken and email
 * @param {string} email - User's email
 * @param {string} refreshToken - Current refresh token
 * @returns {Promise<string>} New access token
 */
const refreshAccessToken = async (email, refreshToken) => {
    try {
        console.log('[Refresh] Refreshing token for:', email);
        const response = await fetch('http://localhost:8080/api/v1/auth/refresh', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, refreshToken })
        });

        console.log('[Refresh] Response status:', response.status);

        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch {
                console.error('[Refresh] Failed to parse error response');
                throw new Error('Invalid refresh token');
            }
            console.error('[Refresh] Error response:', errorData);
            throw new Error(errorData.message || 'Invalid refresh token');
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

// Synchronization lock for refresh token
let isRefreshing = false;
let refreshPromise = null;

/**
 * Fetch with automatic token refresh
 * @param {string} url - API endpoint
 * @param {RequestInit} options - Fetch options
 * @returns {Promise<Response>} Fetch response
 */
const fetchWithRefresh = async (url, options = {}) => {
    const currentUserEmail = sessionStorage.getItem('currentUserEmail');
    if (!currentUserEmail) {
        console.error('[Fetch] No current user email found, redirecting to login');
        window.location.href = '../auth/login_register.html';
        throw new Error('No current user email available');
    }

    const accessToken = sessionStorage.getItem(`user_${currentUserEmail}_accessToken`);
    const refreshToken = sessionStorage.getItem(`user_${currentUserEmail}_refreshToken`);

    if (!accessToken || !refreshToken) {
        console.error('[Fetch] Missing tokens for user:', currentUserEmail, 'attempting refresh');
        try {
            const newAccessToken = await refreshAccessToken(currentUserEmail, refreshToken);
            sessionStorage.setItem(`user_${currentUserEmail}_accessToken`, newAccessToken);
            options.headers = {
                ...options.headers,
                'Authorization': `Bearer ${newAccessToken}`,
                'Accept': 'application/json'
            };
            if (!(options.body instanceof FormData)) {
                options.headers['Content-Type'] = 'application/json';
            }
        } catch (error) {
            console.error('[Fetch] Refresh failed for missing tokens:', error);
            clearCurrentUserData();
            window.location.href = '../auth/login_register.html';
            throw new Error('No tokens available after refresh attempt');
        }
    } else {
        // Add Authorization header, set Content-Type only if not FormData
        options.headers = {
            ...options.headers,
            'Authorization': `Bearer ${accessToken}`,
            'Accept': 'application/json'
        };
        if (!(options.body instanceof FormData)) {
            options.headers['Content-Type'] = 'application/json';
        }
    }

    try {
        console.log('[Fetch] Request:', {
            url,
            method: options.method || 'GET',
            headers: options.headers,
            body: options.body instanceof FormData ? '[FormData]' : options.body
        });

        const response = await fetch(url, options);
        console.log('[Fetch] Response status:', response.status, 'URL:', url);

        // Handle various error cases
        if (response.status === 401 || response.status === 403 || response.status === 500) {
            console.log('[Fetch] Token expired or unauthorized, attempting to refresh');

            // Synchronized refresh token handling
            if (!isRefreshing) {
                isRefreshing = true;
                refreshPromise = refreshAccessToken(currentUserEmail, refreshToken);
            }

            try {
                const newAccessToken = await refreshPromise;
                // Update Authorization header
                options.headers['Authorization'] = `Bearer ${newAccessToken}`;
                // Retry original request
                console.log('[Fetch] Retrying with new token:', url);
                const retryResponse = await fetch(url, options);
                console.log('[Fetch] Retry response status:', retryResponse.status, 'URL:', url);

                if (retryResponse.status === 401 || retryResponse.status === 403) {
                    console.error('[Fetch] Retry failed, redirecting to login');
                    clearCurrentUserData();
                    window.location.href = '../auth/login_register.html';
                    throw new Error('Unauthorized after token refresh');
                }

                return retryResponse;
            } catch (error) {
                console.error('[Fetch] Refresh failed:', error);
                clearCurrentUserData();
                window.location.href = '../auth/login_register.html';
                throw error;
            } finally {
                isRefreshing = false;
                refreshPromise = null;
            }
        }

        if (response.status === 500) {
            console.error('[Fetch] Server error (500) for URL:', url);
            throw new Error('Server error occurred');
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

// Export fetchWithRefresh for use in other modules
export { fetchWithRefresh };