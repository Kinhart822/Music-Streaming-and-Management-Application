// Hàm hiển thị thông báo
function showNotification(message, type = 'error') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type} show`;
    setTimeout(() => {
        notification.classList.remove('show');
        notification.style.display = 'none';
    }, 3000);
}

// Kiểm tra người dùng đã đăng nhập khi tải trang
document.addEventListener('DOMContentLoaded', () => {
    const currentUserEmail = sessionStorage.getItem('currentUserEmail');
    if (currentUserEmail) {
        const userType = sessionStorage.getItem(`user_${currentUserEmail}_userType`);
        if (userType === 'ARTIST') {
            window.location.href = '../artist/artist_dashboard.html';
        } else if (userType === 'ADMIN') {
            window.location.href = '../admin/admin_dashboard.html';
        }
    }
});

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
        const encodedEmail = encodeURIComponent(email);
        const url = `http://localhost:8080/api/v1/account/user/sign-up/check-email-existence?query=${encodedEmail}`;
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to check email');
        }

        const data = await response.json();
        return data.emailExisted;
    } catch (error) {
        showNotification('Error checking email. Please try again.', 'error');
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
        const response = await fetch('http://localhost:8080/api/v1/account/signUpArtist', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

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
        signUpError.style.color = 'green';
        signUpError.textContent = 'Artist account created successfully! Please sign in.';
        signUpError.style.display = 'block';
        showNotification('Artist account created successfully!', 'success');
        setTimeout(() => {
            wrapper.classList.add('animated-signup');
            wrapper.classList.remove('animated-signin');
            signUpError.style.display = 'none';
            signUpError.textContent = '';
            signUpForm.reset();
            signUpButton.disabled = true;
        }, 2000);
    } catch (error) {
        signUpError.textContent = error.message === 'Email already exists' ? 'Email already exists' : 'Failed to create artist account. Please try again.';
        signUpError.style.display = 'block';
        showNotification(error.message || 'Failed to create artist account.', 'error');
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
        const response = await fetch('http://localhost:8080/api/v1/auth/sign-in', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

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
        const { email: userEmail, userType, accessToken, refreshToken } = data;

        // Validate userType
        if (!userType) {
            throw new Error('Invalid response: userType missing');
        }

        // Lưu thông tin người dùng
        handleLoginSuccess(userEmail, userType, accessToken, refreshToken);

        showNotification('Login successful!', 'success');

        // Redirect based on userType
        if (userType === 'ARTIST') {
            window.location.href = '../artist/artist_dashboard.html';
        } else if (userType === 'ADMIN') {
            window.location.href = '../admin/admin_dashboard.html';
        } else if (userType === 'USER') {
            signInError.textContent = 'Invalid user type';
            signInError.style.display = 'block';
            showNotification('Invalid user type', 'error');
        } else {
            throw new Error(`Unknown user type: ${userType}`);
        }
    } catch (error) {
        signInError.textContent = error.message || 'Unable to connect to the server. Please try again later.';
        signInError.style.display = 'block';
        showNotification(error.message || 'Unable to connect to the server.', 'error');
    }
});

// Lưu thông tin đăng nhập
function handleLoginSuccess(email, userType, accessToken, refreshToken) {
    // Lưu token theo email
    const accessTokenKey = `user_${email}_accessToken`;
    const refreshTokenKey = `user_${email}_refreshToken`;
    const userTypeKey = `user_${email}_userType`;
    sessionStorage.setItem(accessTokenKey, accessToken);
    sessionStorage.setItem(refreshTokenKey, refreshToken);
    sessionStorage.setItem(userTypeKey, userType);

    // Lưu email người dùng hiện tại
    sessionStorage.setItem('currentUserEmail', email);
}