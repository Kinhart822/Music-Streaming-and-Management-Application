// Handle OTP verification and password reset
const wrapper = document.querySelector('.wrapper');
const otpForm = document.getElementById('otpForm');
const otpInput = document.getElementById('text');
const otpError = document.getElementById('error-message');

// Select a reset form by class since it lacks an ID
const resetForm = document.querySelector('.form-container.reset form');
const passwordInput = document.getElementById('password');
const confirmPasswordInput = document.getElementById('new-password');

// Create reset-error div dynamically
const resetError = document.createElement('div');
resetError.className = 'error-message';
resetError.style.color = 'red';
resetError.style.display = 'none';
resetError.style.marginBottom = '10px';

// Insert before the submitted button
if (resetForm) {
    resetForm.insertBefore(resetError, resetForm.querySelector('.btn'));
}

// Get sessionId and email from localStorage
const sessionId = localStorage.getItem('forgotPasswordSessionId');
const email = localStorage.getItem('forgotPasswordEmail');

// Validate session
if (!sessionId || !email) {
    if (otpError) {
        otpError.textContent = 'Invalid session. Please start over.';
        otpError.style.display = 'block';
    }
    if (otpForm) {
        const submitButton = otpForm.querySelector('button');
        if (submitButton) {
            submitButton.disabled = true;
        }
    }
    console.error('Missing sessionId or email in localStorage');
}

// Handle OTP form submission
if (otpForm) {
    otpForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (otpError) {
            otpError.style.display = 'none';
            otpError.textContent = '';
        }

        const otp = otpInput ? otpInput.value.trim() : '';
        if (!otp) {
            if (otpError) {
                otpError.textContent = 'OTP is required';
                otpError.style.display = 'block';
            }
            return;
        }

        try {
            console.log('Sending OTP check request:', { sessionId, otp });
            const response = await fetch('http://localhost:8080/api/v1/account/user/forgot-password/check-otp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ sessionId, otp })
            });

            console.log('OTP check response status:', response.status);

            if (!response.ok) {
                let errorData;
                try {
                    errorData = await response.json();
                } catch {
                    throw new Error(`HTTP error ${response.status}`);
                }
                throw new Error(errorData.message || 'Failed to verify OTP');
            }

            const data = await response.json();
            console.log('OTP check response data:', data);

            // Switch to a reset form with animation
            if (wrapper) {
                wrapper.classList.add('animated-reset');
                wrapper.classList.remove('animated-otp');
            }
        } catch (error) {
            console.error('OTP check error:', error);
            if (otpError) {
                otpError.textContent = error.message || 'Unable to verify OTP. Please try again.';
                otpError.style.display = 'block';
            }
        }
    });
} else {
    console.error('otpForm not found in DOM');
}

// Handle password reset form submission
if (resetForm) {
    resetForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        resetError.style.display = 'none';
        resetError.textContent = '';

        const password = passwordInput ? passwordInput.value.trim() : '';
        const confirmPassword = confirmPasswordInput ? confirmPasswordInput.value.trim() : '';

        if (password.length < 6) {
            resetError.textContent = 'Password must be at least 6 characters long';
            resetError.style.display = 'block';
            return;
        }

        if (password !== confirmPassword) {
            resetError.textContent = 'Passwords do not match';
            resetError.style.display = 'block';
            return;
        }

        try {
            console.log('Sending password reset request:', { sessionId, password });
            const response = await fetch('http://localhost:8080/api/v1/account/user/forgot-password/finish', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ sessionId, password })
            });

            console.log('Password reset response status:', response.status);

            if (!response.ok) {
                let errorData;
                try {
                    errorData = await response.json();
                } catch {
                    throw new Error(`HTTP error ${response.status}`);
                }
                throw new Error(errorData.message || 'Failed to reset password');
            }

            const data = await response.json();
            console.log('Password reset response data:', data);

            // Clear localStorage
            localStorage.removeItem('forgotPasswordSessionId');
            localStorage.removeItem('forgotPasswordEmail');

            // Show a success message and redirect to log in
            resetError.style.color = 'green';
            resetError.textContent = 'Password reset successfully! Redirecting to login...';
            resetError.style.display = 'block';
            setTimeout(() => {
                window.location.href = 'login_register.html';
            }, 2000);
        } catch (error) {
            console.error('Password reset error:', error);
            resetError.textContent = error.message || 'Unable to reset password. Please try again.';
            resetError.style.display = 'block';
        }
    });
} else {
    console.error('reset-form not found in DOM');
}