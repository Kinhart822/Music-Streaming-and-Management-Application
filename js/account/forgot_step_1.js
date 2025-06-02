// Handle forgot password form submission
const form = document.getElementById('forgot-password-form');
const emailInput = document.getElementById('email');
const errorMessage = document.getElementById('error-message');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorMessage.style.display = 'none';
    errorMessage.textContent = '';

    const email = emailInput.value.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errorMessage.textContent = 'Invalid email format';
        errorMessage.style.display = 'block';
        return;
    }

    try {
        const sessionId = crypto.randomUUID();
        console.log('Sending forgot password request for:', { email, sessionId });
        const response = await fetch('https://music-streaming-and-management.onrender.com/api/v1/account/user/forgot-password/begin', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, sessionId })
        });

        console.log('Forgot password response status:', response.status);

        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch {
                throw new Error(`HTTP error ${response.status}`);
            }
            throw new Error(errorData.message || 'Failed to send OTP');
        }

        const data = await response.json();
        console.log('Forgot password response data:', data);

        // Store sessionId and email in localStorage
        localStorage.setItem('forgotPasswordSessionId', sessionId);
        localStorage.setItem('forgotPasswordEmail', email);

        // Redirect to otp_reset.html
        window.location.href = '../auth/otp_reset.html';
    } catch (error) {
        console.error('Forgot password error:', error);
        errorMessage.textContent = error.message || 'Unable to send OTP. Please try again.';
        errorMessage.style.display = 'block';
    }
});