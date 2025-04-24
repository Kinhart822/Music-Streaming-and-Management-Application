import {fetchWithRefresh} from '/js/api/refresh.js';

document.addEventListener('DOMContentLoaded', async () => {
    const form = document.getElementById('profile-form');
    if (!form) {
        console.error('Profile form not found in DOM');
        return;
    }

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

    // Load initial profile data from API
    const loadProfile = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/account/profile/artist', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to fetch profile data');
            }

            const profileData = await response.json();
            console.log('Profile data:', profileData);

            // Populate form fields
            firstNameInput.value = profileData.firstName || '';
            lastNameInput.value = profileData.lastName || '';
            descriptionInput.value = profileData.description || '';
            genderInputs.forEach(input => {
                if (input.value.toLowerCase() === profileData.gender?.toLowerCase()) {
                    input.checked = true;
                }
            });
            // Convert dd/MM/yyyy to YYYY-MM-DD for <input type="date">
            if (profileData.birthDay) {
                const [day, month, year] = profileData.birthDay.split('/');
                dobInput.value = `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
            }
            phoneInput.value = profileData.phone || '';

            if (profileData.avatar) {
                avatarPreview.src = profileData.avatar;
                avatarPreview.style.display = 'block';
            }
            if (profileData.backgroundImage) {
                backgroundPreview.src = profileData.backgroundImage;
                backgroundPreview.style.display = 'block';
            }
        } catch (error) {
            console.error('Error loading profile:', error);
            alert('Failed to load profile data. Please try again.');
        }
    };

    // Load profile on a page load
    await loadProfile();

    // List of allowed image MIME types
    const allowedImageTypes = [
        'image/jpeg',
        'image/png',
        'image/gif',
        'image/webp',
        'image/bmp',
        'image/tiff',
        'image/svg+xml'
    ];

    // Avatar preview
    avatarInput?.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file && allowedImageTypes.includes(file.type)) {
            const reader = new FileReader();
            reader.onload = (event) => {
                avatarPreview.src = event.target.result;
                avatarPreview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        } else {
            alert('Please select a valid image file (JPEG, PNG, GIF, WEBP, BMP, TIFF, or SVG).');
            avatarInput.value = '';
        }
    });

    // Background preview
    backgroundInput?.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file && allowedImageTypes.includes(file.type)) {
            const reader = new FileReader();
            reader.onload = (event) => {
                backgroundPreview.src = event.target.result;
                backgroundPreview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        } else {
            alert('Please select a valid image file (JPEG, PNG, GIF, WEBP, BMP, TIFF, or SVG).');
            backgroundInput.value = '';
        }
    });

    // Form submit handler
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const firstName = firstNameInput.value.trim();
        const lastName = lastNameInput.value.trim();
        const description = descriptionInput.value.trim();
        const gender = document.querySelector('input[name="gender"]:checked');
        const dob = dobInput.value; // YYYY-MM-DD
        const phone = phoneInput.value.trim();

        // Validation
        if (!firstName || !lastName || !gender || !dob || !phone) {
            alert('Please fill out all required fields.');
            return;
        }

        if (!/^[0-9]{10,15}$/.test(phone)) {
            alert('Please enter a valid phone number (10-15 digits).');
            return;
        }

        // Validate description length (150 words)
        const wordCount = description.split(/\s+/).filter(word => word.length > 0).length;
        if (wordCount > 150) {
            alert('Description must not exceed 150 words.');
            return;
        }

        // Convert YYYY-MM-DD to dd/MM/yyyy
        let dateOfBirth = '';
        if (dob) {
            const date = new Date(dob);
            const day = String(date.getDate()).padStart(2, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const year = date.getFullYear();
            dateOfBirth = `${day}/${month}/${year}`;
        }

        // Prepare FormData for multipart/form-data
        const formData = new FormData();
        formData.append('firstName', firstName);
        formData.append('lastName', lastName);
        formData.append('description', description);
        formData.append('gender', gender.value); // Sends Male, Female, Other
        formData.append('dateOfBirth', dateOfBirth);
        formData.append('phone', phone);

        if (avatarInput.files[0]) {
            formData.append('avatar', avatarInput.files[0]);
        }
        if (backgroundInput.files[0]) {
            formData.append('backgroundImage', backgroundInput.files[0]);
        }

        // Log FormData entries for debugging
        console.log('FormData entries:');
        for (const [key, value] of formData.entries()) {
            console.log(`${key}:`, value);
        }

        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/account/updateArtist', {
                method: 'PUT',
                body: formData
            });

            console.log('Update response status:', response.status);

            if (!response.ok) {
                let errorData;
                try {
                    errorData = await response.json();
                } catch {
                    throw new Error(`HTTP error ${response.status}`);
                }
                throw new Error(errorData.message || 'Failed to update profile');
            }

            const data = await response.json();
            console.log('Update response data:', data);

            // Handle success response (accept multiple success codes)
            if (data.code === 'OK' || data.code === 'SUCCESS' || data.message === 'SUCCESS') {
                alert('Profile updated successfully!');
                window.location.href = 'artist_dashboard.html';
            } else {
                throw new Error(data.message || 'Unexpected response format');
            }
        } catch (error) {
            console.error('Error updating profile:', error);
            alert(`Failed to update profile: ${error.message || 'Please try again.'}`);
        }
    });
});