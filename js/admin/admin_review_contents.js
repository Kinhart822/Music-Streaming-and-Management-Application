document.addEventListener('DOMContentLoaded', () => {
    // Header interactions
    const menu = document.querySelector('.menu');
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main--content');
    const bell = document.querySelector('.bell');
    const notificationDropdown = document.querySelector('.notification-dropdown');
    const logoImg = document.getElementById('logo-img');

    // Table elements
    const reviewTableBody = document.getElementById('review-table-body');
    const filterTypeSelect = document.getElementById('filter-type');
    const filterStatusSelect = document.getElementById('filter-status');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const descriptionModal = document.getElementById('description-modal');
    const modalDescriptionContent = document.getElementById('modal-description-content');
    const modalClose = document.getElementById('close-modal');

    // Load data from localStorage
    let users = JSON.parse(localStorage.getItem('users')) || [];
    let songs = JSON.parse(localStorage.getItem('songs')) || [];
    let playlists = JSON.parse(localStorage.getItem('playlists')) || [];
    let albums = JSON.parse(localStorage.getItem('albums')) || [];

    // Normalize data
    users = users.map(user => ({
        ...user,
        status: user.status ? user.status.toLowerCase() : 'draft',
        role: user.role || 'user',
        title: `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username || 'Unknown User',
        description: user.description || '',
        imageName: user.avatar || '',
        uploadDate: user.joinDate || new Date().toLocaleDateString('en-GB'),
        uploadedBy: user.username || 'System',
        purposeUpload: 'Account Creation',
        additionalArtists: [],
        duration: 'N/A'
    }));

    songs = songs.map(song => ({
        ...song,
        status: song.status ? song.status.toLowerCase() : 'draft',
        type: 'Song',
        purposeUpload: song.purposeUpload || 'Music Upload',
        uploadedBy: song.uploadedBy || 'Unknown',
        description: song.description || '',
        imageName: song.imageName || '',
        uploadDate: song.uploadDate || new Date().toLocaleDateString('en-GB'),
        additionalArtists: song.additionalArtists || []
    }));

    playlists = playlists.map(playlist => ({
        ...playlist,
        status: playlist.status ? playlist.status.toLowerCase() : 'draft',
        type: 'Playlist',
        purposeUpload: playlist.purposeUpload || 'Playlist Creation',
        uploadedBy: playlist.uploadedBy || 'Unknown',
        description: playlist.description || '',
        imageName: playlist.imageName || '',
        uploadDate: playlist.uploadDate || new Date().toLocaleDateString('en-GB'),
        additionalArtists: playlist.additionalArtists || [],
        duration: 'N/A'
    }));

    albums = albums.map(album => ({
        ...album,
        status: album.status ? album.status.toLowerCase() : 'draft',
        type: 'Album',
        purposeUpload: album.purposeUpload || 'Album Creation',
        uploadedBy: album.uploadedBy || 'Unknown',
        description: album.description || '',
        imageName: album.imageName || '',
        uploadDate: album.releaseDate || new Date().toLocaleDateString('en-GB'),
        additionalArtists: album.additionalArtists || []
    }));

    // Save normalized data to localStorage
    localStorage.setItem('users', JSON.stringify(users));
    localStorage.setItem('songs', JSON.stringify(songs));
    localStorage.setItem('playlists', JSON.stringify(playlists));
    localStorage.setItem('albums', JSON.stringify(albums));

    // Combine all items for the table
    let reviewItems = [
        ...users.map(u => ({
            ...u,
            type: u.role.charAt(0).toUpperCase() + u.role.slice(1) // User, Artist, Admin
        })),
        ...songs,
        ...playlists,
        ...albums
    ];

    let currentFilterType = 'all';
    let currentFilterStatus = 'all';
    let currentSort = 'title-asc';
    let searchQuery = '';
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput?.value) || 10;

    // Refresh page on logo click
    if (logoImg) {
        logoImg.onclick = function () {
            location.reload();
        };
    }

    // Toggle sidebar
    if (menu) {
        menu.onclick = function () {
            sidebar.classList.toggle('active');
            mainContent.classList.toggle('active');
        };
    }

    // Toggle notification dropdown
    if (bell) {
        bell.onclick = function (event) {
            notificationDropdown.classList.toggle('active');
            event.stopPropagation();
        };
    }

    // Close notification dropdown when clicking outside
    document.addEventListener('click', function (event) {
        if (bell && notificationDropdown && !bell.contains(event.target) && !notificationDropdown.contains(event.target)) {
            notificationDropdown.classList.remove('active');
        }
    });

    // Table handling
    if (reviewTableBody) {
        // Parse date (DD/MM/YYYY to a Date object)
        const parseDate = (dateStr) => {
            if (!dateStr || !/^\d{2}\/\d{2}\/\d{4}$/.test(dateStr)) return new Date(0);
            const [day, month, year] = dateStr.split('/').map(Number);
            return new Date(year, month - 1, day);
        };

        // Generate random listeners for accepted items
        const generateRandomListeners = () => {
            return Math.floor(Math.random() * 1001);
        };

        // Render pagination
        const renderPagination = (totalItems) => {
            const totalPages = Math.ceil(totalItems / rowsPerPage);
            paginationDiv.innerHTML = '';

            if (totalPages > 1) {
                const prevButton = document.createElement('button');
                prevButton.textContent = 'Previous';
                prevButton.disabled = currentPage === 1;
                prevButton.addEventListener('click', () => {
                    if (currentPage > 1) {
                        currentPage--;
                        renderTable();
                    }
                });
                paginationDiv.appendChild(prevButton);

                const maxPagesToShow = 5;
                let startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
                let endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);
                if (endPage - startPage + 1 < maxPagesToShow) {
                    startPage = Math.max(1, endPage - maxPagesToShow + 1);
                }

                if (startPage > 1) {
                    const firstPage = document.createElement('span');
                    firstPage.textContent = '1';
                    firstPage.addEventListener('click', () => {
                        currentPage = 1;
                        renderTable();
                    });
                    paginationDiv.appendChild(firstPage);
                    if (startPage > 2) {
                        paginationDiv.appendChild(document.createTextNode(' ... '));
                    }
                }

                for (let i = startPage; i <= endPage; i++) {
                    const pageSpan = document.createElement('span');
                    pageSpan.textContent = i;
                    if (i === currentPage) {
                        pageSpan.classList.add('active');
                    }
                    pageSpan.addEventListener('click', () => {
                        currentPage = i;
                        renderTable();
                    });
                    paginationDiv.appendChild(pageSpan);
                }

                if (endPage < totalPages) {
                    if (endPage < totalPages - 1) {
                        paginationDiv.appendChild(document.createTextNode(' ... '));
                    }
                    const lastPage = document.createElement('span');
                    lastPage.textContent = totalPages;
                    lastPage.addEventListener('click', () => {
                        currentPage = totalPages;
                        renderTable();
                    });
                    paginationDiv.appendChild(lastPage);
                }

                const nextButton = document.createElement('button');
                nextButton.textContent = 'Next';
                nextButton.disabled = currentPage === totalPages;
                nextButton.addEventListener('click', () => {
                    if (currentPage < totalPages) {
                        currentPage++;
                        renderTable();
                    }
                });
                paginationDiv.appendChild(nextButton);
            }
        };

        // Validate and update rows per page
        const updateRowsPerPage = () => {
            const value = parseInt(rowsPerPageInput.value);
            if (isNaN(value) || value < 1) {
                rowsPerPageInput.value = 10;
                rowsPerPage = 10;
            } else {
                rowsPerPage = value;
            }
            currentPage = 1;
            renderTable();
        };

        // Render table
        const renderTable = () => {
            let filteredItems = [...reviewItems];

            // Filter by type
            if (currentFilterType !== 'all') {
                filteredItems = filteredItems.filter(item => item.type.toLowerCase() === currentFilterType.toLowerCase());
            }

            // Filter by status
            if (currentFilterStatus !== 'all') {
                filteredItems = filteredItems.filter(item => item.status.toLowerCase() === currentFilterStatus.toLowerCase());
            }

            // Search
            if (searchQuery) {
                const query = searchQuery.toLowerCase();
                filteredItems = filteredItems.filter(item => item.title.toLowerCase().includes(query));
            }

            // Sort
            filteredItems = filteredItems.sort((a, b) => {
                if (currentSort === 'title-asc') {
                    return a.title.localeCompare(b.title);
                } else if (currentSort === 'title-desc') {
                    return b.title.localeCompare(a.title);
                } else if (currentSort === 'date-asc') {
                    return parseDate(a.uploadDate || a.releaseDate) - parseDate(b.uploadDate || b.releaseDate);
                } else if (currentSort === 'date-desc') {
                    return parseDate(b.uploadDate || b.releaseDate) - parseDate(a.uploadDate || b.releaseDate);
                }
                return 0;
            });

            // Paginate
            const totalPages = Math.ceil(filteredItems.length / rowsPerPage);
            if (currentPage > totalPages && totalPages > 0) {
                currentPage = totalPages;
            } else if (totalPages === 0) {
                currentPage = 1;
            }
            const startIndex = (currentPage - 1) * rowsPerPage;
            const paginatedItems = filteredItems.slice(startIndex, startIndex + rowsPerPage);

            // Render table
            reviewTableBody.innerHTML = paginatedItems.length > 0
                ? paginatedItems.map((item, index) => `
                    <tr>
                        <td class="image">
                            ${item.imageName ? `<span>${item.imageName}</span>` : `<span>No image</span>`}
                        </td>
                        <td>${item.type || 'Unknown'}</td>
                        <td>${item.purposeUpload || 'N/A'}</td>
                        <td>${item.title || 'Unknown'}</td>
                        <td>${item.duration || 'N/A'}</td>
                        <td>${item.uploadDate || item.releaseDate || 'Unknown'}</td>
                        <td>${item.uploadedBy || 'Unknown'}</td>
                        <td>
                            ${item.description
                    ? `<a class="view-description" href="#" data-index="${startIndex + index}" title="View Description">Show more...</a>`
                    : 'None'
                }
                        </td>
                        <td>${item.artist || (['User', 'Artist', 'Admin'].includes(item.type) ? 'N/A' : 'Unknown')}</td>
                        <td>${item.additionalArtists && item.additionalArtists.length > 0 ? item.additionalArtists.join(', ') : 'None'}</td>
                        <td class="status ${item.status.toLowerCase()}">${item.status.charAt(0).toUpperCase() + item.status.slice(1)}</td>
                        <td>
                            ${item.status === 'draft' || item.status === 'pending'
                    ? `
                                    <button class="accept" data-type="${item.type.toLowerCase()}" data-id="${item.id}" title="Accept">Accept</button>
                                    <button class="edit" data-type="${item.type.toLowerCase()}" data-id="${item.id}" title="Edit">Edit</button>
                                    <button class="decline" data-type="${item.type.toLowerCase()}" data-id="${item.id}" title="Decline">Decline</button>
                                    <button class="delete" data-type="${item.type.toLowerCase()}" data-id="${item.id}" title="Delete">Delete</button>
                                `
                    : `
                                    <button class="edit" data-type="${item.type.toLowerCase()}" data-id="${item.id}" title="Edit">Edit</button>
                                    <button class="delete" data-type="${item.type.toLowerCase()}" data-id="${item.id}" title="Delete">Delete</button>
                                `
                }
                        </td>
                    </tr>
                `).join('')
                : '<tr><td colspan="12"><span class="no-contents">No content available.</span></td></tr>';

            // Render pagination
            renderPagination(filteredItems.length);
        };

        // Filter by type
        if (filterTypeSelect) {
            filterTypeSelect.addEventListener('change', () => {
                currentFilterType = filterTypeSelect.value;
                currentPage = 1; // Reset to first page
                renderTable();
            });
        }

        // Filter by status
        if (filterStatusSelect) {
            filterStatusSelect.addEventListener('change', () => {
                currentFilterStatus = filterStatusSelect.value;
                currentPage = 1; // Reset to first page
                renderTable();
            });
        }

        // Sort by
        if (sortBySelect) {
            sortBySelect.addEventListener('change', () => {
                currentSort = sortBySelect.value;
                currentPage = 1; // Reset to first page
                renderTable();
            });
        }

        // Rows per page
        if (rowsPerPageInput) {
            // Update on input for real-time changes
            rowsPerPageInput.addEventListener('input', updateRowsPerPage);
            // Update on change to handle cases like pressing Enter
            rowsPerPageInput.addEventListener('change', updateRowsPerPage);
        }

        // Search
        if (searchInput) {
            searchInput.addEventListener('input', () => {
                searchQuery = searchInput.value.trim();
                currentPage = 1; // Reset to first page
                renderTable();
            });
        }

        // Table actions
        reviewTableBody.addEventListener('click', (e) => {
            e.preventDefault();
            const button = e.target;

            // View description
            if (button.classList.contains('view-description')) {
                const index = parseInt(button.dataset.index);
                const item = reviewItems[index];
                document.getElementById('modal-title').textContent = `${item.type} Description`;
                modalDescriptionContent.value = item.description || '';
                modalDescriptionContent.placeholder = item.description ? '' : 'No description available';
                descriptionModal.style.display = 'flex';
            }

            // Accept, decline, edit, or delete
            if (!button.matches('button')) return;

            const type = button.dataset.type;
            const id = button.dataset.id;

            let array, key, redirectUrl;
            if (type === 'song') {
                array = songs;
                key = 'songs';
                redirectUrl = '../admin/admin_manage_songs.html';
            } else if (type === 'playlist') {
                array = playlists;
                key = 'playlists';
                redirectUrl = '../admin/admin_manage_playlists.html';
            } else if (type === 'album') {
                array = albums;
                key = 'albums';
                redirectUrl = '../admin/admin_manage_albums.html';
            } else if (['user', 'artist', 'admin'].includes(type)) {
                array = users;
                key = 'users';
                redirectUrl = '../admin/admin_manage_users.html';
            } else {
                alert('Error: Invalid item type.');
                return;
            }

            const arrayIndex = array.findIndex(a => a.id === id);
            if (arrayIndex === -1) {
                alert('Error: Item not found.');
                return;
            }

            try {
                if (button.classList.contains('accept')) {
                    array[arrayIndex].status = 'accepted';
                    if (['song', 'playlist', 'album'].includes(type)) {
                        array[arrayIndex].listeners = generateRandomListeners();
                    }
                    alert(`${type.charAt(0).toUpperCase() + type.slice(1)} "${array[arrayIndex].title}" accepted.`);
                } else if (button.classList.contains('decline')) {
                    array[arrayIndex].status = 'declined';
                    if (['song', 'playlist', 'album'].includes(type)) {
                        array[arrayIndex].listeners = 0; // Reset listeners for declined items
                    }
                    alert(`${type.charAt(0).toUpperCase() + type.slice(1)} "${array[arrayIndex].title}" declined.`);
                } else if (button.classList.contains('edit')) {
                    // Store the index to edit in localStorage for the target page
                    localStorage.setItem('editIndex', arrayIndex);
                    window.location.href = redirectUrl;
                    return;
                } else if (button.classList.contains('delete')) {
                    if (confirm(`Are you sure you want to delete ${type.charAt(0).toUpperCase() + type.slice(1)} "${array[arrayIndex].title}"?`)) {
                        const deletedTitle = array[arrayIndex].title;
                        array.splice(arrayIndex, 1);
                        alert(`${type.charAt(0).toUpperCase() + type.slice(1)} "${deletedTitle}" deleted successfully.`);
                    } else {
                        return;
                    }
                }

                localStorage.setItem(key, JSON.stringify(array));

                // Update reviewItems
                reviewItems = [
                    ...users.map(u => ({
                        ...u,
                        type: u.role.charAt(0).toUpperCase() + u.role.slice(1)
                    })),
                    ...songs,
                    ...playlists,
                    ...albums
                ];

                // Refresh table
                renderTable();
            } catch (e) {
                alert(`Failed to process action: Storage quota exceeded.`);
            }
        });

        // Close modal
        if (modalClose) {
            modalClose.addEventListener('click', () => {
                descriptionModal.style.display = 'none';
                modalDescriptionContent.value = '';
                modalDescriptionContent.placeholder = 'No description available';
            });
        }

        // Close modal when clicking outside
        window.addEventListener('click', (e) => {
            if (e.target === descriptionModal) {
                descriptionModal.style.display = 'none';
                modalDescriptionContent.value = '';
                modalDescriptionContent.placeholder = 'No description available';
            }
        });

        // Initial render
        renderTable();
    }
});