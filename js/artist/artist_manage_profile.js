import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";

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
    const submitButton = form.querySelector('button[type="submit"]');

    // Show spinner
    const showSpinner = () => {
        const spinner = document.createElement('div');
        spinner.className = 'spinner';
        spinner.style.margin = '20px auto';
        form.appendChild(spinner);
        return spinner;
    };

    // Hide spinner
    const hideSpinner = (spinner) => {
        if (spinner && spinner.parentNode) {
            spinner.parentNode.removeChild(spinner);
        }
    };

    // Load initial profile data from API
    const loadProfile = async () => {
        const spinner = showSpinner();
        try {
            const response = await fetchWithRefresh('https://music-streaming-and-management.onrender.com/api/v1/account/profile/artist', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch profile data: ${response.status} - ${errorText}`);
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
            if (profileData.birthDay) {
                const [day, month, year] = profileData.birthDay.split('/');
                dobInput.value = `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
            }
            phoneInput.value = profileData.phone || '';

            if (profileData.avatar) {
                avatarPreview.src = profileData.avatar;
                avatarPreview.style.display = 'block';
            }
            if (profileData.image) {
                backgroundPreview.src = profileData.image;
                backgroundPreview.style.display = 'block';
            }
        } catch (error) {
            console.error('Error loading profile:', error);
            showNotification('Failed to load profile data. Please try again.', true);
            
        } finally {
            hideSpinner(spinner);
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
            showNotification('Please select a valid image file (JPEG, PNG, GIF, WEBP, BMP, TIFF, or SVG).', true);
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
            showNotification('Please select a valid image file (JPEG, PNG, GIF, WEBP, BMP, TIFF, or SVG).', true);
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
        const dob = dobInput.value;
        const phone = phoneInput.value.trim();

        // Validation
        if (!firstName || !lastName || !gender || !dob || !phone) {
            showNotification('Please fill out all required fields.', true);
            return;
        }

        if (!/^[0-9]{10,15}$/.test(phone)) {
            showNotification('Please enter a valid phone number (10-15 digits).', true);
            return;
        }

        const wordCount = description.split(/\s+/).filter(word => word.length > 0).length;
        if (wordCount > 150) {
            showNotification('Description must not exceed 150 words.', true);
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

        // Prepare FormData
        const formData = new FormData();
        formData.append('firstName', firstName);
        formData.append('lastName', lastName);
        formData.append('description', description);
        formData.append('gender', gender.value);
        formData.append('dateOfBirth', dateOfBirth);
        formData.append('phone', phone);

        if (avatarInput.files[0]) {
            formData.append('avatar', avatarInput.files[0]);
        }
        if (backgroundInput.files[0]) {
            formData.append('backgroundImage', backgroundInput.files[0]);
        }

        try {
            submitButton.disabled = true;
            submitButton.textContent = 'Updating...';
            const response = await fetchWithRefresh('https://music-streaming-and-management.onrender.com/api/v1/account/updateArtist', {
                method: 'PUT',
                body: formData
            });

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
            if (data.code === 'OK' || data.code === 'SUCCESS' || data.message === 'SUCCESS') {
                showNotification('Profile updated successfully!');
                // Dispatch profile updated event
                const profileUpdatedEvent = new CustomEvent('profileUpdated', {
                    detail: {
                        firstName,
                        lastName,
                        description,
                        gender: gender.value,
                        dateOfBirth,
                        phone,
                        avatar: avatarPreview.src,
                        image: backgroundPreview.src
                    }
                });
                window.dispatchEvent(profileUpdatedEvent);
                setTimeout(() => {
                    window.location.href = 'https://683dafb14818b60008040e18--msma-system.netlify.app/artist/artist_dashboard.html';
                }, 1000);
            } else {
                throw new Error(data.message || 'Unexpected response format');
            }
        } catch (error) {
            console.error('Error updating profile:', error);
            showNotification(`Failed to update profile: ${error.message || 'Please try again.'}`, true);
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = 'Update Profile';
        }
    });
});