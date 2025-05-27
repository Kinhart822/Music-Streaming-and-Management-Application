import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";

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

    // Get cached notifications
    const getCachedNotifications = () => {
        const email = getCurrentUserEmail();
        return JSON.parse(localStorage.getItem(`notifications_${email}`)) || { notifications: [], lastRead: null };
    };

    // Set cached notifications
    const setCachedNotifications = (notifications, lastRead) => {
        const email = getCurrentUserEmail();
        localStorage.setItem(`notifications_${email}`, JSON.stringify({ notifications, lastRead }));
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
        notifications.forEach((notification, index) => {
            const li = document.createElement('li');
            li.innerHTML = `
                <i class="ri-notification-3-line"></i>
                <span>${notification.title}: ${notification.content}</span>
                <small>${formatDate(notification.createdDate)}</small>
            `;
            notificationList.appendChild(li);
        });
        // Update bell icon based on unread notifications
        const cached = getCachedNotifications();
        const hasUnread = notifications.some(n => !cached.lastRead || new Date(n.createdDate) > new Date(cached.lastRead));
        bell.classList.toggle('active', hasUnread);
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
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/account/notification', {
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

            // Cache notifications
            const cached = getCachedNotifications();
            setCachedNotifications(notifications, cached.lastRead);

            // Show a toast notification for the latest notification
            if (notifications.length > 0) {
                const latestNotification = notifications[0];
                showNotification(`${latestNotification.title}: ${latestNotification.content}`);
            }
        } catch (error) {
            console.error('Error fetching notifications:', error);
            showNotification('Failed to load notifications. Using cached data.', true);
            const cached = getCachedNotifications();
            updateNotificationDropdown(cached.notifications);
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                sessionStorage.clear();
                window.location.href = '../../../auth/login_register.html';
            }
        }
    };

    // Get user profile from localStorage
    const getCachedProfile = () => {
        const email = getCurrentUserEmail();
        return JSON.parse(localStorage.getItem(`userProfile_${email}`)) || {};
    };

    // Set user profile to localStorage
    const setCachedProfile = (profileData) => {
        const email = getCurrentUserEmail();
        localStorage.setItem(`userProfile_${email}`, JSON.stringify(profileData));
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
                background: profileData.backgroundImage
            };

            // Update modal
            updateProfileModal(mappedProfileData);

            // Cache in localStorage
            setCachedProfile(mappedProfileData);
        } catch (error) {
            console.error('Error fetching profile:', error);
            showNotification('Failed to load profile data. Using cached data.', true);
            const savedProfile = getCachedProfile();
            updateProfileModal(savedProfile);
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                sessionStorage.clear();
                window.location.href = '../../../auth/login_register.html';
            }
        }
    };

    // Handle notificationsUpdate event
    window.addEventListener('notificationsUpdate', (event) => {
        console.log('Received notificationsUpdate event:', event.detail);
        const newNotification = event.detail;
        const cached = getCachedNotifications();
        // Add new notification to the top of the list
        cached.notifications.unshift(newNotification);
        // Sort notifications by createdDate (descending)
        cached.notifications = cached.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
        // Update cache
        setCachedNotifications(cached.notifications, cached.lastRead);
        // Update dropdown
        updateNotificationDropdown(cached.notifications);
        // Show toast notification
        showNotification(`${newNotification.title}: ${newNotification.content}`);
    });

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
            background: event.detail.backgroundImage
        };
        updateProfileModal(mappedProfileData);
        setCachedProfile(mappedProfileData);
    });

    // Load profile and notifications on page load
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

    if (bell) {
        bell.onclick = (event) => {
            notificationDropdown?.classList.toggle('active');
            profileModal?.classList.remove('active');
            event.stopPropagation();
            // Mark notifications as read
            const cached = getCachedNotifications();
            if (cached.notifications.length > 0) {
                const lastNotificationDate = cached.notifications[0].createdDate; // Assuming sorted by date descending
                setCachedNotifications(cached.notifications, lastNotificationDate);
                bell.classList.remove('active');
            }
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

    document.addEventListener('click', (event) => {
        if (bell && notificationDropdown && !bell.contains(event.target) && !notificationDropdown.contains(event.target)) {
            notificationDropdown.classList.remove('active');
        }
        if (profile && profileModal && !profile.contains(event.target) && !profileModal.contains(event.target)) {
            profileModal.classList.remove('active');
        }
    });

    // Initialize sidebar state
    const isSidebarCollapsed = getSidebarCollapsedState();
    if (isSidebarCollapsed) {
        sidebar?.classList.add('active');
        mainContent?.classList.add('active');
        document.documentElement.classList.add('sidebar-collapsed');
    }
});