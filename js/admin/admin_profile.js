document.addEventListener('DOMContentLoaded', () => {
    // Profile elements
    const profile = document.querySelector('.profile');
    const profileModal = document.querySelector('.profile-modal');
    const closeModal = document.querySelector('.close-modal');
    const profileIconImg = document.getElementById('profile-icon-img');
    const profileModalImg = document.getElementById('profile-modal-img');
    const profileBackgroundImg = document.getElementById('profile-background-img');
    const profileFullname = document.getElementById('profile-fullname');
    const profileDescription = document.getElementById('profile-description');
    const profileGender = document.getElementById('profile-gender');
    const profileDob = document.getElementById('profile-dob');
    const profilePhone = document.getElementById('profile-phone');

    // Load saved profile data for header and modal
    const savedProfile = JSON.parse(localStorage.getItem('userProfile')) || {};
    if (savedProfile) {
        if (profileIconImg) profileIconImg.src = savedProfile.avatar || 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg';
        if (profileModalImg) profileModalImg.src = savedProfile.avatar || 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg';
        if (profileBackgroundImg && savedProfile.background) profileBackgroundImg.src = savedProfile.background;
        if (profileFullname) profileFullname.textContent = `${savedProfile.firstName || ''} ${savedProfile.lastName || ''}`.trim() || 'Unknown User';
        if (profileDescription) {
            const descriptionArray = savedProfile.description && Array.isArray(savedProfile.description) ? savedProfile.description : ['No description available'];
            profileDescription.innerHTML = descriptionArray.map(p => `<p>${p}</p>`).join('');
        }
        if (profileGender) profileGender.textContent = savedProfile.gender || 'Not specified';
        if (profileDob) profileDob.textContent = savedProfile.dob || 'Not specified';
        if (profilePhone) profilePhone.textContent = savedProfile.phone || 'Not specified';
    }

    // Toggle profile modal
    if (profile) {
        profile.onclick = function (event) {
            profileModal.classList.toggle('active');
            event.stopPropagation();
        };
    }

    // Close profile modal
    if (closeModal) {
        closeModal.onclick = function () {
            profileModal.classList.remove('active');
        };
    }

    // Close profile modal when clicking outside
    document.addEventListener('click', function (event) {
        if (profile && profileModal && !profile.contains(event.target) && !profileModal.contains(event.target)) {
            profileModal.classList.remove('active');
        }
    });

    // Profile form handling
    const profileForm = document.getElementById('profile-form');
    if (profileForm) {
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

        if (savedProfile) {
            firstNameInput.value = savedProfile.firstName || '';
            lastNameInput.value = savedProfile.lastName || '';
            descriptionInput.value = savedProfile.description && Array.isArray(savedProfile.description) ? savedProfile.description.join('\n\n') : '';
            genderInputs.forEach(input => {
                if (input.value === savedProfile.gender) input.checked = true;
            });
            dobInput.value = savedProfile.dob || '';
            phoneInput.value = savedProfile.phone || '';
            if (savedProfile.avatar) {
                avatarPreview.src = savedProfile.avatar;
                avatarPreview.style.display = 'block';
            }
            if (savedProfile.background) {
                backgroundPreview.src = savedProfile.background;
                backgroundPreview.style.display = 'block';
            }
        }

        avatarInput?.addEventListener('change', (e) => {
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

        backgroundInput?.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    backgroundPreview.src = event.target.result;
                    backgroundPreview.style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        });

        profileForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const firstName = firstNameInput.value.trim();
            const lastName = lastNameInput.value.trim();
            let descriptionText = descriptionInput.value.trim();
            const gender = document.querySelector('input[name="gender"]:checked');
            const dob = dobInput.value;
            const phone = phoneInput.value;

            // Validate required fields
            if (!firstName || !lastName || !gender || !dob || !phone) {
                alert('Please fill out all required fields.');
                return;
            }

            // Validate phone number
            if (!/^[0-9]{10,15}$/.test(phone)) {
                alert('Please enter a valid phone number (10-15 digits).');
                return;
            }

            // Validate description length (150 words)
            const wordCount = descriptionText.split(/\s+/).filter(word => word.length > 0).length;
            if (wordCount > 150) {
                alert('Description must not exceed 150 words.');
                return;
            }

            // Split description into paragraphs
            const description = descriptionText ? descriptionText.split(/\n\n+/).map(p => p.trim()).filter(p => p) : [];

            const profileData = {
                firstName,
                lastName,
                description,
                gender: gender.value,
                dob,
                phone,
                avatar: savedProfile ? savedProfile.avatar : '',
                background: savedProfile ? savedProfile.background : ''
            };

            const saveProfile = () => {
                localStorage.setItem('userProfile', JSON.stringify(profileData));
                // Update modal immediately
                if (profileIconImg) profileIconImg.src = profileData.avatar || 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg';
                if (profileModalImg) profileModalImg.src = profileData.avatar || 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg';
                if (profileBackgroundImg && profileData.background) profileBackgroundImg.src = profileData.background;
                if (profileFullname) profileFullname.textContent = `${profileData.firstName} ${profileData.lastName}`.trim() || 'Unknown User';
                if (profileDescription) {
                    profileDescription.innerHTML = profileData.description.length > 0
                        ? profileData.description.map(p => `<p>${p}</p>`).join('')
                        : '<p>No description available</p>';
                }
                if (profileGender) profileGender.textContent = profileData.gender || 'Not specified';
                if (profileDob) profileDob.textContent = profileData.dob || 'Not specified';
                if (profilePhone) profilePhone.textContent = profileData.phone || 'Not specified';
                alert('Profile updated successfully!');
                window.location.href = 'admin_dashboard.html';
            };

            if (avatarInput.files[0]) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    profileData.avatar = event.target.result;
                    if (backgroundInput.files[0]) {
                        const bgReader = new FileReader();
                        bgReader.onload = (bgEvent) => {
                            profileData.background = bgEvent.target.result;
                            saveProfile();
                        };
                        bgReader.readAsDataURL(backgroundInput.files[0]);
                    } else {
                        saveProfile();
                    }
                };
                reader.readAsDataURL(avatarInput.files[0]);
            } else if (backgroundInput.files[0]) {
                const bgReader = new FileReader();
                bgReader.onload = (bgEvent) => {
                    profileData.background = bgEvent.target.result;
                    saveProfile();
                };
                bgReader.readAsDataURL(backgroundInput.files[0]);
            } else {
                saveProfile();
            }
        });
    }
});