const darkModeToggle = document.getElementById('darkModeToggle');
const html = document.documentElement;
const moonIcon = document.querySelector('.moon-icon');
const sunIcon = document.querySelector('.sun-icon');
const toggleSwitch = document.querySelector('.switch');

// Function to update UI based on dark mode state
const updateDarkModeUI = (isDark) => {
    if (isDark) {
        html.classList.add('dark');
        moonIcon.style.display = 'none';
        sunIcon.style.display = 'block';
        toggleSwitch.style.left = '28px';
    } else {
        html.classList.remove('dark');
        moonIcon.style.display = 'block';
        sunIcon.style.display = 'none';
        toggleSwitch.style.left = '2px';
    }
};

// Get current user email from sessionStorage
const getCurrentUserEmail = () => {
    return sessionStorage.getItem('currentUserEmail');
};

// Get dark mode state for the current user
const getDarkModeState = () => {
    const email = getCurrentUserEmail();
    if (email) {
        // Get user-specific dark mode setting
        return localStorage.getItem(`darkMode_${email}`) === 'true';
    } else {
        // Fallback to default dark mode setting if no user is logged in
        return localStorage.getItem('darkMode_default') === 'true';
    }
};

// Set dark mode state for the current user
const setDarkModeState = (isDark) => {
    const email = getCurrentUserEmail();
    if (email) {
        // Store user-specific dark mode setting
        localStorage.setItem(`darkMode_${email}`, isDark);
    } else {
        // Store default dark mode setting if no user is logged in
        localStorage.setItem('darkMode_default', isDark);
    }
};

// Initialize dark mode state when page loads
document.addEventListener('DOMContentLoaded', () => {
    const isDarkMode = getDarkModeState();
    updateDarkModeUI(isDarkMode);
});

// Handle toggle click
darkModeToggle?.addEventListener('click', () => {
    const isDark = !html.classList.contains('dark');
    setDarkModeState(isDark);
    updateDarkModeUI(isDark);
});