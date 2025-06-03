// Import fetchWithRefresh from Refresh.js
import { fetchWithRefresh } from '../refresh.js';

/**
 * Handle user logout by sending a request to the server and clearing sessionStorage
 */
const handleLogout = async () => {
    try {
        console.log('Initiating logout');
        const currentUserEmail = sessionStorage.getItem('currentUserEmail');
        if (!currentUserEmail) {
            console.warn('No user is currently logged in');
            window.location.href = '../auth/login_register.html';
            return;
        }

        const accessToken = sessionStorage.getItem(`user_${currentUserEmail}_accessToken`);
        const response = await fetchWithRefresh('http://localhost:8080/api/v1/auth/sign-out', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': `Bearer ${accessToken}` // Thêm token vào header nếu API yêu cầu
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

        // Clear sessionStorage for the current user
        clearCurrentUserData();
        console.log('sessionStorage cleared for user:', currentUserEmail);

        // Redirect to login page
        window.location.href = '../auth/login_register.html';
    } catch (error) {
        console.error('Logout error:', error);
        // Clear sessionStorage and redirect even on error to ensure logout
        clearCurrentUserData();
        window.location.href = '../auth/login_register.html';
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