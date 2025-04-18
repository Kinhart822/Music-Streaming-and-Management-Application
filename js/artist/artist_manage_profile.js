document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('profile-form');
    if (!form) return;

    const avatarInput = document.getElementById('avatar');
    const avatarPreview = document.getElementById('avatar-preview');
    const backgroundInput = document.getElementById('background');
    const backgroundPreview = document.getElementById('background-preview');
    const firstNameInput = document.getElementById('first-name');
    const lastNameInput = document.getElementById('last-name');
    const descriptionInput = document.getElementById('description');
    const genderInputs = document.querySelectorAll('input[name="gender"]');
    const dobInput = document.getElementById('dob');
    const phoneInput = document.getElementById('phone');

    const savedProfile = JSON.parse(localStorage.getItem('userProfile')) || {};
    const savedAvatar = localStorage.getItem('userAvatar');

    // Load profile data
    if (savedProfile) {
        firstNameInput.value = savedProfile.firstName || '';
        lastNameInput.value = savedProfile.lastName || '';
        descriptionInput.value = Array.isArray(savedProfile.description)
            ? savedProfile.description.join('\n\n')
            : '';
        genderInputs.forEach(input => {
            if (input.value === savedProfile.gender) input.checked = true;
        });
        dobInput.value = savedProfile.dob || '';
        phoneInput.value = savedProfile.phone || '';

        if (savedProfile.avatar) {
            avatarPreview.src = savedProfile.avatar;
            avatarPreview.style.display = 'block';
        } else if (savedAvatar) {
            avatarPreview.src = savedAvatar;
            avatarPreview.style.display = 'block';
        }

        if (savedProfile.background) {
            backgroundPreview.src = savedProfile.background;
            backgroundPreview.style.display = 'block';
        }
    }

    // Avatar preview
    avatarInput?.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file && ['image/jpeg', 'image/png'].includes(file.type)) {
            const reader = new FileReader();
            reader.onload = (event) => {
                avatarPreview.src = event.target.result;
                avatarPreview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        } else {
            alert('Please select a valid image file (JPG or PNG).');
            avatarInput.value = '';
        }
    });

    // Background preview
    backgroundInput?.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file && ['image/jpeg', 'image/png'].includes(file.type)) {
            const reader = new FileReader();
            reader.onload = (event) => {
                backgroundPreview.src = event.target.result;
                backgroundPreview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        } else {
            alert('Please select a valid image file (JPG or PNG).');
            backgroundInput.value = '';
        }
    });

    // Form submit handler
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const firstName = firstNameInput.value.trim();
        const lastName = lastNameInput.value.trim();
        const descriptionText = descriptionInput.value.trim();
        const gender = document.querySelector('input[name="gender"]:checked');
        const dob = dobInput.value;
        const phone = phoneInput.value;

        if (!firstName || !lastName || !gender || !dob || !phone) {
            alert('Please fill out all required fields.');
            return;
        }

        if (!/^[0-9]{10,15}$/.test(phone)) {
            alert('Please enter a valid phone number (10-15 digits).');
            return;
        }

        const description = descriptionText
            ? descriptionText.split(/\n\n+/).map(p => p.trim()).filter(p => p)
            : [];

        const profileData = {
            firstName,
            lastName,
            description,
            gender: gender.value,
            dob,
            phone,
            avatar: savedProfile.avatar || '',
            background: savedProfile.background || ''
        };

        // Handle image uploads
        if (avatarInput.files[0]) {
            const avatarReader = new FileReader();
            await new Promise(resolve => {
                avatarReader.onload = (event) => {
                    profileData.avatar = event.target.result;
                    localStorage.setItem('userAvatar', event.target.result); // Save avatar separately too
                    resolve();
                };
                avatarReader.readAsDataURL(avatarInput.files[0]);
            });
        }

        if (backgroundInput.files[0]) {
            const bgReader = new FileReader();
            await new Promise(resolve => {
                bgReader.onload = (event) => {
                    profileData.background = event.target.result;
                    resolve();
                };
                bgReader.readAsDataURL(backgroundInput.files[0]);
            });
        }

        try {
            localStorage.setItem('userProfile', JSON.stringify(profileData));
            const updateEvent = new CustomEvent('profileUpdated', { detail: profileData });
            window.dispatchEvent(updateEvent);
            alert('Profile updated successfully!');
            window.location.href = 'artist_dashboard.html';
        } catch (err) {
            alert('Failed to save profile: Storage quota exceeded.');
        }
    });
});
