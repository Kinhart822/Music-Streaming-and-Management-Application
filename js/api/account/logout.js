// Import fetchWithRefresh from Refresh.js
import { fetchWithRefresh } from '/js/api/refresh.js';

/**
 * Handle user logout by sending a request to the server and clearing localStorage
 */
const handleLogout = async () => {
    try {
        console.log('Initiating logout');
        const response = await fetchWithRefresh('http://localhost:8080/api/v1/auth/sign-out', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        console.log('Logout response status:', response.status);

        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch {
                throw new Error(`HTTP error ${response.status}`);
            }
            throw new Error(errorData.message || 'Failed to sign out');
        }

        const data = await response.json();
        console.log('Logout response data:', data);

        // Clear localStorage
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('email');
        console.log('localStorage cleared');

        // Redirect to login page
        window.location.href = '../auth/login_register.html';
    } catch (error) {
        console.error('Logout error:', error);
        // Clear localStorage and redirect even on error to ensure logout
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('email');
        window.location.href = '../auth/login_register.html';
    }
};

// Attach logout handler to the logout link
document.addEventListener('DOMContentLoaded', () => {
    const logoutLink = document.querySelector('.nav-link a.logout');
    if (logoutLink) {
        logoutLink.addEventListener('click', (e) => {
            e.preventDefault(); // Prevent default navigation
            handleLogout();
        });
    } else {
        console.error('Logout link not found in DOM');
    }
});