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

    const updateProfileModal = (profileData) => {
        if (profileIconImg) profileIconImg.src = profileData.avatar || 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80';
        if (profileModalImg) profileModalImg.src = profileData.avatar || 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80';
        if (profileBackgroundImg && profileData.background) profileBackgroundImg.src = profileData.background;
        if (profileFullname) profileFullname.textContent = `${profileData.firstName || ''} ${profileData.lastName || ''}`.trim() || 'Unknown User';
        if (profileDescription) {
            const descriptionArray = Array.isArray(profileData.description) && profileData.description.length > 0
                ? profileData.description
                : ['No description available'];
            profileDescription.innerHTML = descriptionArray
                .map(p => `<p class="description-paragraph">${p}</p>`)
                .join('');
        }
        if (profileGender) profileGender.textContent = profileData.gender || 'Not specified';
        if (profileDob) profileDob.textContent = profileData.dob || 'Not specified';
        if (profilePhone) profilePhone.textContent = profileData.phone || 'Not specified';
    };

    // Load initial profile data
    const savedProfile = JSON.parse(localStorage.getItem('userProfile')) || {};
    updateProfileModal(savedProfile);

    // Listen for profile updates from profile_form.js
    window.addEventListener('profileUpdated', (event) => {
        updateProfileModal(event.detail);
    });

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
});