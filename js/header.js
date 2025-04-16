document.addEventListener('DOMContentLoaded', () => {
    // Header interactions
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

    // Dropdown toggles
    let songsToggle = document.querySelector('.dropdown-toggle-songs');
    let songsMenu = document.querySelector('.dropdown-menu-songs');
    let playlistsToggle = document.querySelector('.dropdown-toggle-playlists');
    let playlistsMenu = document.querySelector('.dropdown-menu-playlists');
    let albumsToggle = document.querySelector('.dropdown-toggle-albums');
    let albumsMenu = document.querySelector('.dropdown-menu-albums');

    // Load saved profile data for header and modal
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

    // Load dropdown states from localStorage
    const isSongsOpen = localStorage.getItem('songsDropdownOpen') === 'true';
    const isPlaylistsOpen = localStorage.getItem('playlistsDropdownOpen') === 'true';
    const isAlbumsOpen = localStorage.getItem('albumsDropdownOpen') === 'true';

    if (isSongsOpen && songsMenu) songsMenu.classList.add('active');
    if (isPlaylistsOpen && playlistsMenu) playlistsMenu.classList.add('active');
    if (isAlbumsOpen && albumsMenu) albumsMenu.classList.add('active');

    // Refresh page on logo click without resetting dropdown states
    if (logoImg) {
        logoImg.onclick = function() {
            location.reload();
        };
    }

    // Toggle sidebar
    if (menu) {
        menu.onclick = function() {
            sidebar.classList.toggle('active');
            mainContent.classList.toggle('active');
        };
    }

    // Toggle notification dropdown
    if (bell) {
        bell.onclick = function(event) {
            notificationDropdown.classList.toggle('active');
            if (profileModal) profileModal.classList.remove('active');
            event.stopPropagation();
        };
    }

    // Toggle profile modal
    if (profile) {
        profile.onclick = function(event) {
            profileModal.classList.toggle('active');
            if (notificationDropdown) notificationDropdown.classList.remove('active');
            event.stopPropagation();
        };
    }

    // Close profile modal
    if (closeModal) {
        closeModal.onclick = function() {
            profileModal.classList.remove('active');
        };
    }

    // Toggle songs dropdown
    if (songsToggle && songsMenu) {
        songsToggle.onclick = function(event) {
            songsMenu.classList.toggle('active');
            const isOpen = songsMenu.classList.contains('active');
            localStorage.setItem('songsDropdownOpen', isOpen);
            event.stopPropagation();
        };
    }

    // Toggle playlists dropdown
    if (playlistsToggle && playlistsMenu) {
        playlistsToggle.onclick = function(event) {
            playlistsMenu.classList.toggle('active');
            const isOpen = playlistsMenu.classList.contains('active');
            localStorage.setItem('playlistsDropdownOpen', isOpen);
            event.stopPropagation();
        };
    }

    // Toggle albums dropdown
    if (albumsToggle && albumsMenu) {
        albumsToggle.onclick = function(event) {
            albumsMenu.classList.toggle('active');
            const isOpen = albumsMenu.classList.contains('active');
            localStorage.setItem('albumsDropdownOpen', isOpen);
            event.stopPropagation();
        };
    }

    // Prevent dropdown from closing when clicking on menu items
    const dropdownMenus = [songsMenu, playlistsMenu, albumsMenu];
    dropdownMenus.forEach(menu => {
        if (menu) {
            menu.addEventListener('click', (event) => {
                // Prevent clicks on menu items from bubbling up to document
                event.stopPropagation();
            });
        }
    });

    // Close dropdowns when clicking outside
    document.addEventListener('click', function(event) {
        if (bell && notificationDropdown && !bell.contains(event.target) && !notificationDropdown.contains(event.target)) {
            notificationDropdown.classList.remove('active');
        }
        if (profile && profileModal && !profile.contains(event.target) && !profileModal.contains(event.target)) {
            profileModal.classList.remove('active');
        }
        if (songsToggle && songsMenu && !songsToggle.contains(event.target) && !songsMenu.contains(event.target)) {
            songsMenu.classList.remove('active');
            localStorage.setItem('songsDropdownOpen', 'false');
        }
        if (playlistsToggle && playlistsMenu && !playlistsToggle.contains(event.target) && !playlistsMenu.contains(event.target)) {
            playlistsMenu.classList.remove('active');
            localStorage.setItem('playlistsDropdownOpen', 'false');
        }
        if (albumsToggle && albumsMenu && !albumsToggle.contains(event.target) && !albumsMenu.contains(event.target)) {
            albumsMenu.classList.remove('active');
            localStorage.setItem('albumsDropdownOpen', 'false');
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

        // Load saved profile data into form
        if (savedProfile) {
            firstNameInput.value = savedProfile.firstName || '';
            lastNameInput.value = savedProfile.lastName || '';
            descriptionInput.value = savedProfile.description || '';
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

        // Preview avatar when selected
        avatarInput.addEventListener('change', (e) => {
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

        // Preview background image when selected
        backgroundInput.addEventListener('change', (e) => {
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

        // Handle profile form submission
        profileForm.addEventListener('submit', (e) => {
            e.preventDefault();

            // Validate inputs
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

            // Prepare profile data
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

            // Save avatar if uploaded
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

    // Playlist form handling
    const playlistForm = document.getElementById('playlist-form');
    if (playlistForm) {
        const playlistTitleInput = document.getElementById('playlist-title');
        const playlistDescriptionInput = document.getElementById('playlist-description');
        const playlistImageInput = document.getElementById('playlist-image');
        const imagePreview = document.getElementById('image-preview');

        // Preview playlist image when selected
        playlistImageInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    imagePreview.src = event.target.result;
                    imagePreview.style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        });

        // Handle playlist form submission
        playlistForm.addEventListener('submit', (e) => {
            e.preventDefault();

            // Validate inputs
            const playlistTitle = playlistTitleInput.value.trim();
            const playlistDescription = playlistDescriptionInput.value.trim();
            const playlistImage = playlistImageInput.files[0];

            if (!playlistTitle) {
                alert('Please provide a title.');
                return;
            }

            // Prepare playlist data
            const playlistData = {
                title: playlistTitle,
                description: playlistDescription,
                creationDate: new Date().toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' }),
                status: 'Draft'
            };

            // Save playlist metadata to localStorage
            const savePlaylist = () => {
                let playlists = JSON.parse(localStorage.getItem('playlists')) || [];
                playlists.push(playlistData);
                try {
                    localStorage.setItem('playlists', JSON.stringify(playlists));
                    alert('Playlist created successfully!');
                    window.location.href = 'artist_manage_playlist.html';
                } catch (e) {
                    alert('Failed to save playlist: Storage quota exceeded. Please clear some data.');
                }
            };

            savePlaylist();
        });
    }

    // Album form handling
    const albumForm = document.getElementById('album-form');
    if (albumForm) {
        const albumTitleInput = document.getElementById('album-title');
        const albumDescriptionInput = document.getElementById('album-description');
        const albumImageInput = document.getElementById('album-image');
        const imagePreview = document.getElementById('image-preview');

        // Preview album image when selected
        albumImageInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    imagePreview.src = event.target.result;
                    imagePreview.style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        });

        // Handle album form submission
        albumForm.addEventListener('submit', (e) => {
            e.preventDefault();

            // Validate inputs
            const albumTitle = albumTitleInput.value.trim();
            const albumDescription = albumDescriptionInput.value.trim();
            const albumImage = albumImageInput.files[0];

            if (!albumTitle) {
                alert('Please provide a title.');
                return;
            }

            // Prepare album data
            const albumData = {
                title: albumTitle,
                description: albumDescription,
                releaseDate: new Date().toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' }),
                status: 'Draft'
            };

            // Save album metadata to localStorage
            const saveAlbum = () => {
                let albums = JSON.parse(localStorage.getItem('albums')) || [];
                albums.push(albumData);
                try {
                    localStorage.setItem('albums', JSON.stringify(albums));
                    alert('Album created successfully!');
                    window.location.href = 'artist_manage_album.html';
                } catch (e) {
                    alert('Failed to save album: Storage quota exceeded. Please clear some data.');
                }
            };

            saveAlbum();
        });
    }

    // Update dashboard cards
    const totalSongsCard = document.querySelector('.card-1 h1');
    const uploadedSongsCard = document.querySelector('.card-2 h1');
    const totalPlaylistsCard = document.querySelector('.card-3 h1');
    const totalAlbumsCard = document.querySelector('.card-4 h1');

    if (totalSongsCard) {
        const songs = JSON.parse(localStorage.getItem('songs')) || [];
        totalSongsCard.textContent = songs.length;
    }

    if (uploadedSongsCard) {
        const songs = JSON.parse(localStorage.getItem('songs')) || [];
        const uploadedSongs = songs.filter(song => song.status === 'Accepted').length;
        uploadedSongsCard.textContent = uploadedSongs;
    }

    if (totalPlaylistsCard) {
        const playlists = JSON.parse(localStorage.getItem('playlists')) || [];
        totalPlaylistsCard.textContent = playlists.length;
    }

    if (totalAlbumsCard) {
        const albums = JSON.parse(localStorage.getItem('albums')) || [];
        totalAlbumsCard.textContent = albums.length;
    }
});