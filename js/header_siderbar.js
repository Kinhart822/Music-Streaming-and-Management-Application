document.addEventListener('DOMContentLoaded', () => {
    let menu = document.querySelector('.menu');
    let sidebar = document.querySelector('.sidebar');
    let mainContent = document.querySelector('.main--content');
    let bell = document.querySelector('.bell');
    let notificationDropdown = document.querySelector('.notification-dropdown');
    let profile = document.querySelector('.profile');
    let profileModal = document.querySelector('.profile-modal');
    let closeModal = document.querySelector('.close-modal');
    let logoImg = document.getElementById('logo-img');
    let profileIconImg = document.getElementById('profile-icon-img');
    let profileModalImg = document.getElementById('profile-modal-img');
    let profileBackgroundImg = document.getElementById('profile-background-img');
    let profileFullname = document.getElementById('profile-fullname');
    let profileDescription = document.getElementById('profile-description');
    let profileGender = document.getElementById('profile-gender');
    let profileDob = document.getElementById('profile-dob');
    let profilePhone = document.getElementById('profile-phone');

    const savedProfile = JSON.parse(localStorage.getItem('userProfile'));
    if (savedProfile) {
        if (profileIconImg) profileIconImg.src = savedProfile.avatar || 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80';
        if (profileModalImg) profileModalImg.src = savedProfile.avatar || 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80';
        if (profileBackgroundImg && savedProfile.background) profileBackgroundImg.src = savedProfile.background;
        if (profileFullname) profileFullname.textContent = `${savedProfile.firstName || ''} ${savedProfile.lastName || ''}`.trim() || 'Unknown User';
        if (profileDescription) profileDescription.textContent = savedProfile.description || 'No description available';
        if (profileGender) profileGender.textContent = savedProfile.gender || 'Not specified';
        if (profileDob) profileDob.textContent = savedProfile.dob || 'Not specified';
        if (profilePhone) profilePhone.textContent = savedProfile.phone || 'Not specified';
    }

    if (logoImg) {
        logoImg.onclick = function () {
            location.reload();
        };
    }

    if (menu) {
        menu.onclick = function () {
            sidebar.classList.toggle('active');
            mainContent.classList.toggle('active');
        };
    }

    if (bell) {
        bell.onclick = function (event) {
            notificationDropdown.classList.toggle('active');
            if (profileModal) profileModal.classList.remove('active');
            event.stopPropagation();
        };
    }

    if (profile) {
        profile.onclick = function (event) {
            profileModal.classList.toggle('active');
            if (notificationDropdown) notificationDropdown.classList.remove('active');
            event.stopPropagation();
        };
    }

    if (closeModal) {
        closeModal.onclick = function () {
            profileModal.classList.remove('active');
        };
    }

    document.addEventListener('click', function (event) {
        if (bell && notificationDropdown && !bell.contains(event.target) && !notificationDropdown.contains(event.target)) {
            notificationDropdown.classList.remove('active');
        }
        if (profile && profileModal && !profile.contains(event.target) && !profileModal.contains(event.target)) {
            profileModal.classList.remove('active');
        }
    });

    // Phần xử lý Profile form giữ nguyên
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
            PrevdescriptionInput.value = savedProfile.description || '';
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
            const description = descriptionInput.value.trim();
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
                alert('Profile updated successfully!');
                window.location.href = 'artist_dashboard.html';
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