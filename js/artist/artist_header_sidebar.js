import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";
import {messaging, onMessage} from '../../firebase_config.js';

document.addEventListener('DOMContentLoaded', () => {
    const menu = document.querySelector('.menu');
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main--content');
    const bell = document.querySelector('.bell');
    const notificationDropdown = document.querySelector('.notification-dropdown');
    const profile = document.querySelector('.profile');
    const profileModal = document.querySelector('.profile-modal');
    const closeModal = document.querySelector('.close-modal');
    const logoImg = document.getElementById('logo-img');
    const profileIconImg = document.getElementById('profile-icon-img');
    const profileModalImg = document.getElementById('profile-modal-img');
    const profileBackgroundImg = document.getElementById('profile-background-img');
    const profileFullname = document.getElementById('profile-fullname');
    const profileDescription = document.getElementById('profile-description');
    const profileGender = document.getElementById('profile-gender');
    const profileDob = document.getElementById('profile-dob');
    const profilePhone = document.getElementById('profile-phone');
    const notificationList = document.querySelector('.notification-list');

    // Get current user email from sessionStorage
    const getCurrentUserEmail = () => {
        return sessionStorage.getItem('currentUserEmail') || 'default';
    };

    // Get sidebar collapsed state
    const getSidebarCollapsedState = () => {
        const email = getCurrentUserEmail();
        return localStorage.getItem(`sidebarCollapsed_${email}`) === 'true';
    };

    // Set sidebar collapsed state
    const setSidebarCollapsedState = (isCollapsed) => {
        const email = getCurrentUserEmail();
        localStorage.setItem(`sidebarCollapsed_${email}`, isCollapsed);
    };

    // Update notification dropdown
    const updateNotificationDropdown = (notifications) => {
        if (!notificationList) return;
        notificationList.innerHTML = '';
        if (notifications.length === 0) {
            const li = document.createElement('li');
            li.textContent = 'No notifications available';
            notificationList.appendChild(li);
            bell.classList.remove('active');
            return;
        }
        notifications.forEach((notification) => {
            const li = document.createElement('li');
            li.innerHTML = `
                <i class="ri-notification-3-line"></i>
                <span>${notification.title}: ${notification.content}</span>
                <small>${formatDate(notification.createdDate)}</small>
            `;
            notificationList.appendChild(li);
        });
    };

    // Format date for display
    const formatDate = (dateString) => {
        try {
            // Split dateString into date and time parts
            const [datePart, timePart] = dateString.split(' ');
            const [day, month, year] = datePart.split('/');
            const [hours, minutes] = timePart.split(':');

            // Create a Date object (months are 0-based in JavaScript, so subtract 1)
            const date = new Date(year, month - 1, day, hours, minutes);

            // Validate date
            if (isNaN(date.getTime())) {
                throw new Error('Invalid date');
            }

            const now = new Date();
            const diffInSeconds = (now - date) / 1000;

            if (diffInSeconds < 60) return `${Math.floor(diffInSeconds)}s ago`;
            if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}m ago`;
            if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}h ago`;
            return `${Math.floor(diffInSeconds / 86400)}d ago`;
        } catch (error) {
            console.error('Error parsing date:', error);
            return 'Invalid date';
        }
    };
    // Fetch notifications from API
    const loadNotifications = async () => {
        try {
            const response = await fetchWithRefresh('https://music-streaming-and-management.onrender.com/api/v1/account/notification', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch notifications: ${response.status} - ${errorText}`);
            }

            let notifications = await response.json();
            console.log('Fetched notifications:', notifications);

            // Sort notifications by createdDate (descending)
            notifications = notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));

            // Update dropdown
            updateNotificationDropdown(notifications);
        } catch (error) {
            console.error('Error fetching notifications:', error);
            showNotification('Failed to load notifications. Using cached data.', true);
        }
    };

    // Fetch profile data from API
    const loadProfile = async () => {
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
                background: profileData.image
            };

            // Update modal
            updateProfileModal(mappedProfileData);
        } catch (error) {
            console.error('Error fetching profile:', error);
            showNotification('Failed to load profile data. Using cached data.', true);
        }
    };

    // Update profile modal
    const updateProfileModal = (profileData) => {
        console.log('Updating profile modal with:', profileData);

        const defaultAvatar = 'https://static.vecteezy.com/system/resources/previews/008/442/086/non_2x/illustration-of-human-icon-user-symbol-icon-modern-design-on-blank-background-free-vector.jpg';
        const defaultBackground = 'https://codetheweb.blog/assets/img/posts/css-advanced-background-images/cover.jpg';

        if (profileIconImg) {
            profileIconImg.src = profileData.avatar || defaultAvatar;
        }
        if (profileModalImg) {
            profileModalImg.src = profileData.avatar || defaultAvatar;
        }
        if (profileBackgroundImg) {
            profileBackgroundImg.src = profileData.background || defaultBackground;
        }
        if (profileFullname) {
            profileFullname.textContent = `${profileData.firstName || ''} ${profileData.lastName || ''}`.trim() || 'Unknown User';
        }
        if (profileDescription) {
            profileDescription.innerHTML = '';
            const description = profileData.description?.trim();
            if (description) {
                const paragraphs = description.split(/\n\n+|\n+/).map(p => p.trim()).filter(p => p);
                paragraphs.forEach(paragraph => {
                    const p = document.createElement('p');
                    p.textContent = paragraph;
                    profileDescription.appendChild(p);
                });
            } else {
                const p = document.createElement('p');
                p.textContent = 'No description available';
                profileDescription.appendChild(p);
            }
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

    // Listen for profile updates
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
            background: event.detail.image
        };
        updateProfileModal(mappedProfileData);
    });

    // Load profile and notifications on a page load
    loadProfile();
    loadNotifications();

    // Event listeners
    if (logoImg) {
        logoImg.onclick = () => {
            location.reload();
        };
    }

    if (menu) {
        menu.onclick = () => {
            sidebar?.classList.toggle('active');
            mainContent?.classList.toggle('active');
            const isCollapsed = sidebar.classList.contains('active');
            document.documentElement.classList.toggle('sidebar-collapsed', isCollapsed);
            setSidebarCollapsedState(isCollapsed);
        };
    }

    if (profile) {
        profile.onclick = (event) => {
            console.log('Profile icon clicked, toggling modal');
            profileModal?.classList.toggle('active');
            notificationDropdown?.classList.remove('active');
            event.stopPropagation();
            loadProfile();
        };
    }

    if (closeModal) {
        closeModal.onclick = () => {
            profileModal?.classList.remove('active');
        };
    }

    // Khi nhận thông báo foreground từ Firebase
    onMessage(messaging, (payload) => {
        console.log('Received foreground message:', payload);
        const bell = document.querySelector('.bell');
        if (bell) {
            bell.classList.add('has-unread');
            bell.innerHTML = `<i class="bx bxs-bell-ring bx-tada" />`;
            showNotification('Heads up! You have a new notification waiting.', false);
            // Đánh dấu có thông báo chưa đọc
            localStorage.setItem('hasUnreadNotification', 'true');
        }
    });

    if (bell && notificationDropdown) {
        loadNotifications();
        bell.onclick = (event) => {
            notificationDropdown.classList.toggle('active');
            profileModal?.classList.remove('active');
            // Nếu đang có thông báo chưa đọc thì đổi icon và trạng thái
            if (bell.classList.contains('has-unread')) {
                bell.classList.remove('has-unread');
                const icon = bell.querySelector('i');
                if (icon) {
                    icon.className = 'bx bx-bell';
                    icon.classList.remove('bxs-bell-ring', 'bx-tada');
                }
                localStorage.setItem('hasUnreadNotification', 'false');
            }
            event.stopPropagation();
        };
    }

    // Initialize sidebar state
    const isSidebarCollapsed = getSidebarCollapsedState();
    if (isSidebarCollapsed) {
        sidebar?.classList.add('active');
        mainContent?.classList.add('active');
        document.documentElement.classList.add('sidebar-collapsed');
    }

    // Khôi phục trạng thái bell
    if (bell && localStorage.getItem('hasUnreadNotification') === 'true') {
        bell.classList.add('has-unread');
        bell.innerHTML = `<i class="bx bxs-bell-ring bx-tada" />`;
    }
});