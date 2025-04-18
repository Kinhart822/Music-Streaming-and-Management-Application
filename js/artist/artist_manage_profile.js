document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('profile-form');
    const avatarInput = document.getElementById('avatar');
    const avatarPreview = document.getElementById('avatar-preview');

    // Load saved avatar from localStorage
    const savedAvatar = localStorage.getItem('userAvatar');
    if (savedAvatar) {
        avatarPreview.src = savedAvatar;
        avatarPreview.style.display = 'block';
    }

    // Preview avatar when selected
    avatarInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (event) => {
                avatarPreview.src = event.target.result;
                avatarPreview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        }
    });

    // Handle form submission
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        // Validate inputs
        const firstName = document.getElementById('first-name').value.trim();
        const lastName = document.getElementById('last-name').value.trim();
        const gender = document.querySelector('input[name="gender"]:checked');
        const dob = document.getElementById('dob').value;
        const phone = document.getElementById('phone').value;

        if (!firstName || !lastName || !gender || !dob || !phone) {
            alert('Please fill out all required fields.');
            return;
        }

        if (!/^[0-9]{10,15}$/.test(phone)) {
            alert('Please enter a valid phone number (10-15 digits).');
            return;
        }

        // Save avatar to localStorage
        if (avatarInput.files[0]) {
            const reader = new FileReader();
            reader.onload = (event) => {
                localStorage.setItem('userAvatar', event.target.result);
                alert('Profile updated successfully!');
                window.location.href = 'artist_dashboard.html';
            };
            reader.readAsDataURL(avatarInput.files[0]);
        } else {
            alert('Profile updated successfully!');
            window.location.href = 'artist_dashboard.html';
        }
    });
});