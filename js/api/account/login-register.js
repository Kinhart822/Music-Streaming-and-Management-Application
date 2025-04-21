// Toggle between sign-in and sign-up forms
const wrapper = document.querySelector('.wrapper');
const signUpLink = document.querySelector('.signup-link');
const signInLink = document.querySelector('.signin-link');

signUpLink.addEventListener('click', () => {
    wrapper.classList.add('animated-signin');
    wrapper.classList.remove('animated-signup');
});

signInLink.addEventListener('click', () => {
    wrapper.classList.add('animated-signup');
    wrapper.classList.remove('animated-signin');
});

// Handle a sign-up form
const signUpForm = document.getElementById('sign-up-form');
const signUpEmail = document.getElementById('sign-up-email');
const signUpPassword = document.getElementById('sign-up-password');
const signUpButton = document.getElementById('sign-up-button');
const signUpError = document.getElementById('sign-up-error');

// Debounce function to limit API calls
const debounce = (func, delay) => {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => func(...args), delay);
    };
};

// Check email existence
const checkEmailExistence = async (email) => {
    try {
        if (!email) {
            throw new Error('Email is empty');
        }
        console.log('Checking email:', email);
        const encodedEmail = encodeURIComponent(email);
        const url = `http://localhost:8080/api/v1/account/user/sign-up/check-email-existence?query=${encodedEmail}`;
        console.log('Fetching URL:', url);
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        console.log('Check email response status:', response.status);

        if (!response.ok) {
            throw new Error('Failed to check email');
        }

        const data = await response.json();
        console.log('Check email response data:', data);
        return data.emailExisted;
    } catch (error) {
        console.error('Check email error:', error);
        signUpError.textContent = 'Error checking email. Please try again.';
        signUpError.style.display = 'block';
        return true;
    }
};

// Validate form and enable/disable button
const validateForm = () => {
    const email = signUpEmail.value.trim();
    const password = signUpPassword.value.trim();
    const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    const isValidPassword = password.length >= 6; // Example validation
    signUpButton.disabled = !isValidEmail || !isValidPassword;
};

// Handle email input with debouncing
signUpEmail.addEventListener('input', debounce(async () => {
    const email = signUpEmail.value.trim();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        signUpError.style.display = 'none';
        signUpButton.disabled = true;
        return;
    }

    const emailExisted = await checkEmailExistence(email);
    if (emailExisted) {
        signUpError.textContent = 'Email already exists';
        signUpError.style.display = 'block';
        signUpButton.disabled = true;
    } else {
        signUpError.style.display = 'none';
        validateForm();
    }
}, 500));

// Validate password input
signUpPassword.addEventListener('input', validateForm);

// Handle sign-up form submission
signUpForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    signUpError.style.display = 'none';
    signUpError.textContent = '';

    const email = signUpEmail.value.trim();
    const password = signUpPassword.value.trim();

    try {
        console.log('Sending sign-up request to:', 'http://localhost:8080/api/v1/account/signUpArtist');
        console.log('Request body:', JSON.stringify({ email, password }));

        const response = await fetch('http://localhost:8080/api/v1/account/signUpArtist', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

        console.log('Sign-up response status:', response.status);

        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch {
                throw new Error(`HTTP error ${response.status}`);
            }
            throw new Error(errorData.message || 'Failed to create artist account');
        }

        const data = await response.json();
        console.log('Sign-up response data:', data);

        // Show success message and switch to sign-in form
        signUpError.style.color = 'green';
        signUpError.textContent = 'Artist account created successfully! Please sign in.';
        signUpError.style.display = 'block';
        setTimeout(() => {
            wrapper.classList.add('animated-signup');
            wrapper.classList.remove('animated-signin');
            signUpError.style.display = 'none';
            signUpError.textContent = '';
            signUpForm.reset();
            signUpButton.disabled = true;
        }, 2000);
    } catch (error) {
        console.error('Sign-up error:', error);
        signUpError.textContent = error.message === 'Email already exists' ? 'Email already exists' : 'Failed to create artist account. Please try again.';
        signUpError.style.display = 'block';
    }
});

// Handle sign-in form submission
const signInForm = document.getElementById('sign-in-form');
const signInError = document.getElementById('sign-in-error');

signInForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    signInError.style.display = 'none';
    signInError.textContent = '';

    const email = document.getElementById('sign-in-email').value.trim();
    const password = document.getElementById('sign-in-password').value.trim();

    try {
        console.log('Sending sign-in request to:', 'http://localhost:8080/api/v1/auth/sign-in');
        console.log('Request body:', JSON.stringify({ email, password }));

        const response = await fetch('http://localhost:8080/api/v1/auth/sign-in', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

        console.log('Sign-in response status:', response.status);

        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch {
                throw new Error(`HTTP error ${response.status}`);
            }
            throw new Error(errorData.message || 'Invalid email or password');
        }

        const data = await response.json();
        console.log('Sign-in response data:', data);
        const { accessToken, refreshToken, userType } = data;

        // Validate userType
        if (!userType) {
            throw new Error('Invalid response: userType missing');
        }

        // Store tokens and email in localStorage
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('email', email);

        // Redirect based on userType
        if (userType === 'ARTIST') {
            window.location.href = '../artist/artist_dashboard.html';
        } else if (userType === 'ADMIN') {
            window.location.href = '../admin/admin_dashboard.html';
        } else if (userType === 'USER') {
            signInError.textContent = 'Invalid user type';
            signInError.style.display = 'block';
        } else {
            throw new Error(`Unknown user type: ${userType}`);
        }
    } catch (error) {
        console.error('Sign-in error:', error);
        signInError.textContent = error.message || 'Unable to connect to the server. Please try again later.';
        signInError.style.display = 'block';
    }
});