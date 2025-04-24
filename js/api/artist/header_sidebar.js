import { fetchWithRefresh } from '/js/api/refresh.js';

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

    // Synchronize sidebar state with html class
    const isSidebarCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    if (isSidebarCollapsed) {
        sidebar?.classList.add('active');
        mainContent?.classList.add('active');
    }

    const updateProfileModal = (profileData) => {
        console.log('Updating profile modal with:', profileData);
        if (profileIconImg) {
            profileIconImg.src = profileData.avatar || 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg';
        }
        if (profileModalImg) {
            profileModalImg.src = profileData.avatar || 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg';
        }
        if (profileBackgroundImg && profileData.background) {
            profileBackgroundImg.src = profileData.background;
        }
        if (profileFullname) {
            profileFullname.textContent = `${profileData.firstName || ''} ${profileData.lastName || ''}`.trim() || 'Unknown User';
        }
        if (profileDescription) {
            let description;
            if (Array.isArray(profileData.description)) {
                description = profileData.description;
            } else {
                // Split by double or single newlines, prioritizing double newlines
                const rawDescription = profileData.description || 'No description available';
                description = rawDescription.split(/\n\n+|\n+/).map(p => p.trim()).filter(p => p);
                // If no valid paragraphs after splitting, use fallback
                if (description.length === 0) {
                    description = ['No description available'];
                }
            }

            // Clear existing content
            profileDescription.innerHTML = '';

            // Create and append each paragraph
            description.forEach(paragraph => {
                const p = document.createElement('p');
                p.textContent = paragraph;
                profileDescription.appendChild(p);
            });
        }
        if (profileGender) {
            profileGender.textContent = profileData.gender || 'Not specified';
        }
        if (profileDob) {
            profileDob.textContent = profileData.dob || 'Not specified';
        }
        if (profilePhone) {
            profilePhone.textContent = profileData.phone || 'Not specified';
        }
    };

    // Fetch profile data from API
    const loadProfile = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/account/profile/artist', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch profile data: ${response.status}`);
            }

            const profileData = await response.json();
            console.log('Fetched profile data:', profileData);

            // Map backend fields to modal expectations
            const mappedProfileData = {
                firstName: profileData.firstName,
                lastName: profileData.lastName,
                description: profileData.description,
                gender: profileData.gender,
                dob: profileData.birthDay,
                phone: profileData.phone,
                avatar: profileData.avatar,
                background: profileData.backgroundImage
            };

            // Update modal
            updateProfileModal(mappedProfileData);

            // Cache in localStorage for offline fallback
            localStorage.setItem('userProfile', JSON.stringify(mappedProfileData));
        } catch (error) {
            console.error('Error fetching profile:', error);
            // Fallback to cached profile
            const savedProfile = JSON.parse(localStorage.getItem('userProfile')) || {};
            updateProfileModal(savedProfile);
            // Redirect to log in if no tokens or persistent failure
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token')) {
                window.location.href = '../auth/login_register.html';
            }
        }
    };

    // Load profile on a page load
    loadProfile();

    // Listen for profile updates from artist_manage_profile.js
    window.addEventListener('profileUpdated', (event) => {
        console.log('Received profileUpdated event:', event.detail);
        const mappedProfileData = {
            firstName: event.detail.firstName,
            lastName: event.detail.lastName,
            description: event.detail.description,
            gender: event.detail.gender,
            dob: event.detail.dateOfBirth,
            phone: event.detail.phone,
            avatar: event.detail.avatar,
            background: event.detail.backgroundImage
        };
        updateProfileModal(mappedProfileData);
        // Update localStorage
        localStorage.setItem('userProfile', JSON.stringify(mappedProfileData));
    });

    if (logoImg) {
        logoImg.onclick = function () {
            location.reload();
        };
    }

    if (menu) {
        menu.onclick = function () {
            sidebar?.classList.toggle('active');
            mainContent?.classList.toggle('active');
            // Update html class and localStorage
            const isCollapsed = sidebar.classList.contains('active');
            document.documentElement.classList.toggle('sidebar-collapsed', isCollapsed);
            localStorage.setItem('sidebarCollapsed', isCollapsed);
        };
    }

    if (bell) {
        bell.onclick = function (event) {
            notificationDropdown?.classList.toggle('active');
            profileModal?.classList.remove('active');
            event.stopPropagation();
        };
    }

    if (profile) {
        profile.onclick = function (event) {
            console.log('Profile icon clicked, toggling modal');
            profileModal?.classList.toggle('active');
            notificationDropdown?.classList.remove('active');
            event.stopPropagation();
            // Refresh profile data on click to ensure latest data
            loadProfile();
        };
    }

    if (closeModal) {
        closeModal.onclick = function () {
            profileModal?.classList.remove('active');
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