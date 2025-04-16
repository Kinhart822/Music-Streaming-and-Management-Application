document.addEventListener('DOMContentLoaded', () => {
  let darkModeToggle = document.getElementById('darkModeToggle');
  let body = document.body;
  let moonIcon = document.querySelector('.moon-icon');
  let sunIcon = document.querySelector('.sun-icon');
  let toggleSwitch = document.querySelector('.switch');

  // Load dark mode state from localStorage
  const isDarkMode = localStorage.getItem('darkMode') === 'true';
  if (isDarkMode) {
      body.classList.add('dark');
      moonIcon.style.display = 'none';
      sunIcon.style.display = 'block';
      toggleSwitch.style.left = '28px';
  } else {
      body.classList.remove('dark');
      moonIcon.style.display = 'block';
      sunIcon.style.display = 'none';
      toggleSwitch.style.left = '2px';
  }

  // Toggle dark mode on click
  darkModeToggle.addEventListener('click', () => {
      body.classList.toggle('dark');
      const isDark = body.classList.contains('dark');
      localStorage.setItem('darkMode', isDark);

      if (isDark) {
          moonIcon.style.display = 'none';
          sunIcon.style.display = 'block';
          toggleSwitch.style.left = '28px';
      } else {
          moonIcon.style.display = 'block';
          sunIcon.style.display = 'none';
          toggleSwitch.style.left = '2px';
      }
  });
});
