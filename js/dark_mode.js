document.addEventListener('DOMContentLoaded', () => {
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

    // Initial state is already set in HTML inline script, just sync UI
    const isDarkMode = localStorage.getItem('darkMode') === 'true';
    updateDarkModeUI(isDarkMode);

    // Handle toggle click
    darkModeToggle?.addEventListener('click', () => {
        const isDark = !html.classList.contains('dark');
        localStorage.setItem('darkMode', isDark);
        updateDarkModeUI(isDark);
    });
});