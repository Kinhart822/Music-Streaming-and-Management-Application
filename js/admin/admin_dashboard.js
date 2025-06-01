import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";
import {showConfirmModal} from "../confirmation.js";

document.addEventListener('DOMContentLoaded', () => {
    // DOM elements
    const pendingTableBody = document.getElementById('pending-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const sortBySelect = document.getElementById('sort-by');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const descriptionModal = document.getElementById('description-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalDescriptionContent = document.getElementById('modal-description-content');
    const modalClose = document.getElementById('close-modal');
    const totalListenersCard = document.getElementById('total-listeners');
    const totalArtistsCard = document.getElementById('total-artists');
    const totalSongsCard = document.getElementById('total-songs');
    const totalPlaylistsCard = document.getElementById('total-playlists');
    const totalAlbumsCard = document.getElementById('total-albums');
    const pendingSongsCard = document.getElementById('pending-songs');
    const pendingPlaylistsCard = document.getElementById('pending-playlists');
    const pendingAlbumsCard = document.getElementById('pending-albums');

    let currentPage = 1;
    let rowsPerPage = 10;
    let currentFilterStatus = 'all';
    let currentSort = 'releaseDate-desc';
    let searchQuery = '';

    const formatNumber = (num) => {
        if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
        if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
        return num.toString();
    };

    // Fetch dashboard stats using provided APIs
    const fetchDashboardStats = async () => {
        try {
            const apiEndpoints = [
                {
                    url: 'http://spring-music-container:8080/api/v1/admin/manage/countUser',
                    card: totalListenersCard,
                    key: 'totalUsers'
                },
                {
                    url: 'http://spring-music-container:8080/api/v1/admin/manage/countArtist',
                    card: totalArtistsCard,
                    key: 'totalArtists'
                },
                {url: 'http://spring-music-container:8080/api/v1/admin/manage/countSong', card: totalSongsCard, key: 'totalSongs'},
                {
                    url: 'http://spring-music-container:8080/api/v1/admin/manage/countPlaylist',
                    card: totalPlaylistsCard,
                    key: 'totalPlaylists'
                },
                {
                    url: 'http://spring-music-container:8080/api/v1/admin/manage/countAlbum',
                    card: totalAlbumsCard,
                    key: 'totalAlbums'
                },
                {
                    url: 'http://spring-music-container:8080/api/v1/admin/manage/countPendingSong',
                    card: pendingSongsCard,
                    key: 'pendingSongs'
                },
                {
                    url: 'http://spring-music-container:8080/api/v1/admin/manage/countPendingPlaylist',
                    card: pendingPlaylistsCard,
                    key: 'pendingPlaylists'
                },
                {
                    url: 'http://spring-music-container:8080/api/v1/admin/manage/countPendingAlbum',
                    card: pendingAlbumsCard,
                    key: 'pendingAlbums'
                }
            ];

            const stats = {
                totalUsers: 0,
                totalArtists: 0,
                totalSongs: 0,
                totalPlaylists: 0,
                totalAlbums: 0,
                pendingSongs: 0,
                pendingPlaylists: 0,
                pendingAlbums: 0
            };

            const fetchPromises = apiEndpoints.map(async ({url, card, key}) => {
                try {
                    const response = await fetchWithRefresh(url, {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    });

                    if (!response.ok) {
                        throw new Error(`Failed to fetch ${key} (Status: ${response.status})`);
                    }

                    const data = await response.json();
                    stats[key] = Number(data) || 0;
                    card.textContent = formatNumber(stats[key]);
                } catch (error) {
                    console.error(`Error fetching ${key}:`, error);
                    card.textContent = stats[key].toLocaleString();
                }
            });

            await Promise.all(fetchPromises);

            // Nếu tất cả API đều thất bại, hiển thị thông báo lỗi
            if (Object.values(stats).every(value => value === 0)) {
                showNotification('Failed to load dashboard stats. Please check the server.', true);
            }
        } catch (error) {
            console.error('Error fetching dashboard stats:', error);
            showNotification('Failed to load dashboard stats. Please check the server.', true);

            // Cập nhật card với giá trị mặc định
            totalListenersCard.textContent = '0';
            totalArtistsCard.textContent = '0';
            totalSongsCard.textContent = '0';
            totalPlaylistsCard.textContent = '0';
            totalAlbumsCard.textContent = '0';
            pendingSongsCard.textContent = '0';
            pendingPlaylistsCard.textContent = '0';
            pendingAlbumsCard.textContent = '0';
        }
    };

    // Fetch recent contents from API
    const fetchRecentContents = async () => {
        try {
            const [orderBy, order] = currentSort.split('-');
            const request = {
                page: currentPage,
                size: rowsPerPage,
                search: searchQuery || '',
                status: currentFilterStatus === 'all' ? null : currentFilterStatus.toUpperCase(),
                orderBy: orderBy,
                order: order.toUpperCase()
            };

            const response = await fetchWithRefresh('http://spring-music-container:8080/api/v1/search/recentContents', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(request)
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch recent contents (Status: ${response.status})`);
            }

            const data = await response.json();
            return data || {content: [], currentPage: 1, totalPages: 1, totalElements: 0};
        } catch (error) {
            console.error('Error fetching recent contents:', error);
            showNotification('Failed to load recent contents. Please try again.', true);
            return {content: [], currentPage: 1, totalPages: 1, totalElements: 0};
        }
    };

    // Render table
    const renderTable = async () => {
        pendingTableBody.innerHTML = '<tr><td colspan="9"><div class="spinner"></div></td></tr>';
        const data = await fetchRecentContents();
        const contents = Array.isArray(data.content) ? data.content : [];
        const totalPages = Number(data.totalPages) || 1;
        currentPage = Number(data.currentPage) || 1;

        pendingTableBody.innerHTML = contents.length > 0
            ? contents.map((item, index) => {
                const type = item.songStatus ? 'Song' : item.playlistName ? 'Playlist' : 'Album';
                const title = item.title || item.playlistName || item.albumName || 'Unknown';
                const status = (item.songStatus || item.status || 'DRAFT').toLowerCase();
                const duration = item.duration || item.playTimeLength || item.albumTimeLength || 'N/A';
                const artists = Array.isArray(item.artistNameList) && item.artistNameList.length > 0
                    ? item.artistNameList.join(', ')
                    : 'Unknown';
                const imageUrl = item.imageUrl || '';
                const description = item.description || '';
                const releaseDate = item.releaseDate || 'Unknown';
                const id = item.id || '';

                let actionButtons;
                if (status === 'pending') {
                    actionButtons = `
                        <button class="publish" data-type="${type.toLowerCase()}" data-id="${id}" title="Accept">Accept</button>
                        <button class="decline" data-type="${type.toLowerCase()}" data-id="${id}" title="Decline">Decline</button>
                        <button class="delete" data-type="${type.toLowerCase()}" data-id="${id}" title="Delete">Delete</button>
                    `;
                } else if (status === 'accepted' || status === 'declined') {
                    actionButtons = `
                        <button class="delete" data-type="${type.toLowerCase()}" data-id="${id}" title="Delete">Delete</button>
                    `;
                } else {
                    actionButtons = 'None';
                }

                return `
                    <tr>
                        <td class="image">
                            ${imageUrl ? `<img src="${imageUrl}" alt="${title}" style="max-width: 50px; border-radius: 4px;">` : `<span>No image</span>`}
                        </td>
                        <td>${type}</td>
                        <td class="truncate">${title}</td>
                        <td>${releaseDate}</td>
                        <td>
                            ${description
                    ? `<a class="view-description" href="#" data-index="${index}" title="View Description">Show more...</a>`
                    : 'None'
                }
                        </td>
                        <td>${duration}</td>
                        <td>${artists}</td>
                        <td class="status ${status}">${status.charAt(0).toUpperCase() + status.slice(1)}</td>
                        <td>${actionButtons}</td>
                    </tr>
                `;
            }).join('')
            : '<tr><td colspan="9"><span class="no-contents">No content available.</span></td></tr>';

        // Render pagination
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

    // Event listeners
    filterStatusSelect.addEventListener('change', () => {
        currentFilterStatus = filterStatusSelect.value;
        currentPage = 1;
        renderTable();
    });

    sortBySelect.addEventListener('change', () => {
        currentSort = sortBySelect.value;
        currentPage = 1;
        renderTable();
    });

    searchInput.addEventListener('input', () => {
        searchQuery = searchInput.value.trim();
        currentPage = 1;
        renderTable();
    });

    // Close description modal
    modalClose.addEventListener('click', () => {
        descriptionModal.style.display = 'none';
        modalDescriptionContent.value = '';
        modalDescriptionContent.placeholder = 'No description available';
    });

    window.addEventListener('click', (e) => {
        if (e.target === descriptionModal) {
            descriptionModal.style.display = 'none';
            modalDescriptionContent.value = '';
            modalDescriptionContent.placeholder = 'No description available';
        }
    });

    // Table actions
    pendingTableBody.addEventListener('click', async (e) => {
        e.preventDefault();
        const button = e.target;

        // View description
        if (button.classList.contains('view-description')) {
            const index = parseInt(button.dataset.index, 10);
            const data = await fetchRecentContents();
            const item = data.content[index];
            if (!item) {
                showNotification('Failed to load description.', true);
                return;
            }
            const type = item.songStatus ? 'Song' : item.playlistName ? 'Playlist' : 'Album';
            const title = item.title || item.playlistName || item.albumName || 'Unknown';
            modalTitle.textContent = `${type} Description`;
            modalDescriptionContent.value = item.description || '';
            modalDescriptionContent.placeholder = item.description ? '' : 'No description available';
            descriptionModal.style.display = 'flex';
            return;
        }

        if (!button.matches('button')) return;

        const type = button.dataset.type;
        const id = button.dataset.id;
        if (!type || !id) {
            showNotification('Invalid action.', true);
            return;
        }

        let apiUrl, method, actionLabel;

        // Configure API endpoints
        switch (type) {
            case 'song':
                if (button.classList.contains('publish')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/song/publish/${id}`;
                    method = 'POST';
                    actionLabel = 'publish';
                } else if (button.classList.contains('decline')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/song/decline/${id}`;
                    method = 'POST';
                    actionLabel = 'decline';
                } else if (button.classList.contains('delete')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/song/delete/${id}`;
                    method = 'DELETE';
                    actionLabel = 'delete';
                }
                break;
            case 'playlist':
                if (button.classList.contains('publish')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/playlist/publish/${id}`;
                    method = 'POST';
                    actionLabel = 'publish';
                } else if (button.classList.contains('decline')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/playlist/decline/${id}`;
                    method = 'POST';
                    actionLabel = 'decline';
                } else if (button.classList.contains('delete')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/playlist/delete/${id}`;
                    method = 'DELETE';
                    actionLabel = 'delete';
                }
                break;
            case 'album':
                if (button.classList.contains('publish')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/album/publish/${id}`;
                    method = 'POST';
                    actionLabel = 'publish';
                } else if (button.classList.contains('decline')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/album/decline/${id}`;
                    method = 'POST';
                    actionLabel = 'decline';
                } else if (button.classList.contains('delete')) {
                    apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/album/delete/${id}`;
                    method = 'DELETE';
                    actionLabel = 'delete';
                }
                break;
            default:
                showNotification('Invalid content type.', true);
                return;
        }

        // Show confirmation modal
        showConfirmModal(
            `Confirm ${actionLabel.charAt(0).toUpperCase() + actionLabel.slice(1)}`,
            `Are you sure you want to ${actionLabel} this ${type}?`,
            async () => {
                try {
                    button.disabled = true;
                    button.textContent = actionLabel === 'publish' ? 'Accepting...' :
                        actionLabel === 'decline' ? 'Declining...' : 'Deleting...';

                    const response = await fetchWithRefresh(apiUrl, {
                        method: method,
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    });

                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({}));
                        throw new Error(errorData.message || `Failed to ${actionLabel} ${type}`);
                    }
                    await response.json().catch(() => ({}));
                    showNotification(`${actionLabel}ed successfully!`);
                    await fetchDashboardStats();
                    await renderTable();
                } catch (error) {
                    console.error(`Error ${actionLabel}ing ${type}:`, error);
                    showNotification(`Failed to ${actionLabel} ${type}: ${error.message}`, true);
                } finally {
                    button.disabled = false;
                    button.textContent = actionLabel === 'publish' ? 'Accept' :
                        actionLabel === 'decline' ? 'Decline' : 'Delete';
                }
            }
        );
    });

    // Initial render
    fetchDashboardStats();
    renderTable();
});