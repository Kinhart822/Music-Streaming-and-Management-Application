import {fetchWithRefresh} from '/js/api/refresh.js';

document.addEventListener('DOMContentLoaded', () => {
    // Profile elements
    const profile = document.querySelector('.profile');
    const profileModal = document.querySelector('.profile-modal');
    const closeModal = document.querySelector('.close-modal');
    let bell = document.querySelector('.bell');
    let notificationDropdown = document.querySelector('.notification-dropdown');
    let menu = document.querySelector('.menu');
    let sidebar = document.querySelector('.sidebar');
    let mainContent = document.querySelector('.main--content');
    let logoImg = document.getElementById('logo-img');
    const profileIconImg = document.getElementById('profile-icon-img');
    const profileModalImg = document.getElementById('profile-modal-img');
    const profileBackgroundImg = document.getElementById('profile-background-img');
    const profileFullname = document.getElementById('profile-fullname');
    const profileDescription = document.getElementById('profile-description');

    // Get current user email from sessionStorage
    const getCurrentUserEmail = () => {
        return sessionStorage.getItem('currentUserEmail');
    };

    // Get sidebar collapsed state
    const getSidebarCollapsedState = () => {
        const email = getCurrentUserEmail();
        if (email) {
            return localStorage.getItem(`sidebarCollapsed_${email}`) === 'true';
        } else {
            return localStorage.getItem('sidebarCollapsed_default') === 'true';
        }
    };

    // Set sidebar collapsed state
    const setSidebarCollapsedState = (isCollapsed) => {
        const email = getCurrentUserEmail();
        if (email) {
            localStorage.setItem(`sidebarCollapsed_${email}`, isCollapsed);
        } else {
            localStorage.setItem('sidebarCollapsed_default', isCollapsed);
        }
    };

    // Synchronize sidebar state with HTML class
    const isSidebarCollapsed = getSidebarCollapsedState();
    if (isSidebarCollapsed) {
        sidebar?.classList.add('active');
        mainContent?.classList.add('active');
        document.documentElement.classList.add('sidebar-collapsed');
    }

    // Default admin profile data
    const adminProfile = {
        avatar: 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg',
        background: 'https://codetheweb.blog/assets/img/posts/css-advanced-background-images/cover.jpg',
        firstName: 'Admin',
        lastName: 'User',
        description: ['System administrator responsible for managing users, songs, playlists, and albums.']
    };

    // Populate profile data
    if (profileIconImg) {
        profileIconImg.src = adminProfile.avatar;
    }
    if (profileModalImg) {
        profileModalImg.src = adminProfile.avatar;
    }
    if (profileBackgroundImg) {
        profileBackgroundImg.src = adminProfile.background || ''; // Empty string hides the image
        if (!adminProfile.background) {
            profileBackgroundImg.style.display = 'none'; // Hide if no background
        }
    }
    if (profileFullname) {
        profileFullname.textContent = `${adminProfile.firstName} ${adminProfile.lastName}`.trim();
    }
    if (profileDescription) {
        const descriptionArray = Array.isArray(adminProfile.description) ? adminProfile.description : [adminProfile.description || 'No description available'];
        profileDescription.innerHTML = descriptionArray.map(p => `<p>${p}</p>`).join('');
    }

    // Refresh page on logo click
    if (logoImg) {
        logoImg.onclick = function () {
            location.reload();
        };
    }

    // Toggle sidebar
    if (menu) {
        menu.onclick = function () {
            sidebar?.classList.toggle('active');
            mainContent?.classList.toggle('active');
            // Update HTML class and localStorage
            const isCollapsed = sidebar.classList.contains('active');
            document.documentElement.classList.toggle('sidebar-collapsed', isCollapsed);
            setSidebarCollapsedState(isCollapsed);
        };
    }

    // Toggle notification dropdown
    if (bell) {
        bell.onclick = function (event) {
            notificationDropdown?.classList.toggle('active');
            profileModal?.classList.remove('active');
            event.stopPropagation();
        };
    }

    // Close notification dropdown when clicking outside
    document.addEventListener('click', function (event) {
        if (bell && notificationDropdown && !bell.contains(event.target) && !notificationDropdown.contains(event.target)) {
            notificationDropdown.classList.remove('active');
        }
    });

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
});