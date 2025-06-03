import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";
import {showConfirmModal} from "../confirmation.js";

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const songTableBody = document.getElementById('song-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const genreFilterSelect = document.getElementById('genre-filter');
    const sortBySelect = document.getElementById('sort-by');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const lyricsModal = document.getElementById('lyrics-modal');
    const modalLyricsContent = document.getElementById('modal-lyrics-content');
    const closeModal = document.getElementById('close-modal');
    const modalTitle = document.getElementById('modal-title');

    // State
    let songs = [];
    let genres = [];
    let currentPage = 1;
    let rowsPerPage = 10;
    let currentFilterStatus = 'all';
    let currentFilterGenre = 'all';
    let currentSort = 'title-asc';
    let searchQuery = '';
    let totalPages = 1;
    let totalElements = 0;

    // Utility Functions
    const formatNumber = (num) => {
        if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
        if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
        return num.toString();
    };

    const formatDuration = (duration) => {
        if (!duration || isNaN(duration)) return '0:00';
        const minutes = Math.floor(duration / 60);
        const seconds = Math.floor(duration % 60);
        return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
    };

    const debounce = (func, delay) => {
        let timeoutId;
        return (...args) => {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => func(...args), delay);
        };
    };

    const mapSortToApi = (sort) => {
        switch (sort) {
            case 'title-asc':
                return {orderBy: 'title', order: 'asc'};
            case 'title-desc':
                return {orderBy: 'title', order: 'desc'};
            default:
                return {orderBy: 'title', order: 'asc'};
        }
    };

    // API Functions for Cards
    const fetchTotalSongs = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalSongs', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch total songs: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching total songs:', error);
            showNotification('Failed to fetch total songs.', true);
            return 0;
        }
    };

    const fetchTotalPlaylists = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalPlaylists', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch total playlists: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching total playlists:', error);
            showNotification('Failed to fetch total playlists.', true);
            return 0;
        }
    };

    const fetchTotalAlbums = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalAlbums', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch total albums: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching total albums:', error);
            showNotification('Failed to fetch total albums.', true);
            return 0;
        }
    };

    const fetchTotalListeners = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalListeners', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch total listeners: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching total listeners:', error);
            showNotification('Failed to fetch total listeners.', true);
            return 0;
        }
    };

    const fetchTotalFollowers = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalFollowers', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch total followers: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching total followers:', error);
            showNotification('Failed to fetch total followers.', true);
            return 0;
        }
    };

    const fetchTotalLikes = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalLikes', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch total likes: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching total likes:', error);
            showNotification('Failed to fetch total likes.', true);
            return 0;
        }
    };

    const fetchTotalDownloads = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalDownloads', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch total downloads: ${response.status} - ${errorText}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching total downloads:', error);
            showNotification('Failed to fetch total downloads.', true);
            return 0;
        }
    };

    // Update Card Values
    const updateCards = async () => {
        try {
            const [
                totalSongs,
                totalPlaylists,
                totalAlbums,
                totalListeners,
                totalFollowers,
                totalLikes,
                totalDownloads
            ] = await Promise.all([
                fetchTotalSongs(),
                fetchTotalPlaylists(),
                fetchTotalAlbums(),
                fetchTotalListeners(),
                fetchTotalFollowers(),
                fetchTotalLikes(),
                fetchTotalDownloads()
            ]);

            const songCard = document.querySelector('[data-card="total-songs"] .card--content h1');
            const playlistCard = document.querySelector('[data-card="total-playlists"] .card--content h1');
            const albumCard = document.querySelector('[data-card="total-albums"] .card--content h1');
            const listenerCard = document.querySelector('[data-card="total-listeners"] .card--content h1');
            const followerCard = document.querySelector('[data-card="total-followers"] .card--content h1');
            const likesCard = document.querySelector('[data-card="song-likes"] .card--content h1');
            const downloadsCard = document.querySelector('[data-card="song-downloads"] .card--content h1');

            if (songCard) songCard.textContent = formatNumber(totalSongs);
            if (playlistCard) playlistCard.textContent = formatNumber(totalPlaylists);
            if (albumCard) albumCard.textContent = formatNumber(totalAlbums);
            if (listenerCard) listenerCard.textContent = formatNumber(totalListeners);
            if (followerCard) followerCard.textContent = formatNumber(totalFollowers);
            if (likesCard) likesCard.textContent = formatNumber(totalLikes);
            if (downloadsCard) downloadsCard.textContent = formatNumber(totalDownloads);
        } catch (error) {
            console.error('Error updating cards:', error);
            showNotification('Failed to update dashboard cards.', true);
            document.querySelectorAll('.card .card--content h1').forEach(h1 => h1.textContent = '0');
        }
    };

    // API Functions
    const fetchGenres = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/genre/allGenres', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch genres: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            genres = data.map(genre => ({
                id: genre.id,
                name: genre.name || 'Unknown'
            }));

            if (genreFilterSelect) {
                genreFilterSelect.innerHTML = '<option value="all">All Genres</option>' +
                    genres.map(genre => `<option value="${genre.id}">${genre.name}</option>`).join('');
            }
        } catch (error) {
            console.error('Error fetching genres:', error);
            showNotification('Failed to load genres.', true);
            if (genreFilterSelect) {
                genreFilterSelect.innerHTML = '<option value="all">All Genres</option>';
            }
        }
    };

    const fetchSongs = async () => {
        try {
            songTableBody.innerHTML = '<tr><td colspan="15"><div class="spinner"></div></td></tr>';
            const {orderBy, order} = mapSortToApi(currentSort);
            const genreId = currentFilterGenre !== 'all' ? currentFilterGenre : null;

            const requestBody = {
                page: currentPage,
                size: rowsPerPage,
                genreId: genreId ? parseInt(genreId) : null,
                orderBy,
                order,
                search: searchQuery
            };

            const response = await fetchWithRefresh('http://localhost:8080/api/v1/search/recentSongs', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch songs: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            if (!data || !Array.isArray(data.songs)) {
                throw new Error('Invalid API response: songs is not an array');
            }

            songs = data.songs.map(song => ({
                id: song.id,
                title: song.title || 'Unknown',
                genreNameList: song.genreNameList || [],
                duration: song.duration || 0,
                status: song.songStatus ? song.songStatus.toLowerCase() : 'draft',
                songFileName: song.mp3Url || '',
                imageName: song.imageUrl || '',
                downloadPermission: song.downloadPermission ? 'Yes' : song.downloadPermission === false ? 'No' : 'None',
                uploadDate: song.releaseDate || '',
                numberOfListeners: song.numberOfListeners || 0,
                countListen: song.countListen || 0,
                numberOfDownload: song.numberOfDownload || 0,
                numberOfUserLike: song.numberOfUserLike || 0,
                lyrics: song.lyrics || '',
                description: song.description || '',
                artistNameList: song.artistNameList || []
            }));
            currentPage = data.currentPage || 1;
            totalPages = data.totalPages || 1;
            totalElements = data.totalElements || 0;

            renderTable();
        } catch (error) {
            console.error('Error fetching songs:', error);
            showNotification('Unable to load songs. Please try again later or contact support.', true);
            songTableBody.innerHTML = '<tr><td colspan="15"><span class="no-songs">Unable to load songs.</span></td></tr>';
            paginationDiv.innerHTML = '';
        }
    };

    const publishSong = async (id) => {
        try {
            const response = await fetchWithRefresh(`http://localhost:8080/api/v1/artist/song/upload/${id}`, {
                method: 'POST',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to publish song: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to publish song: ${error.message}`);
        }
    };

    const deleteSong = async (id) => {
        try {
            const response = await fetchWithRefresh(`http://localhost:8080/api/v1/artist/song/delete/${id}`, {
                method: 'DELETE',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to delete song: ${response.status} - ${errorText}`);
            }
        } catch (error) {
            throw new Error(`Failed to delete song: ${error.message}`);
        }
    };

    // UI Functions
    const renderTable = () => {
        if (!songTableBody || !paginationDiv) return;

        let filteredSongs = currentFilterStatus !== 'all'
            ? songs.filter(s => s.status.toLowerCase() === currentFilterStatus)
            : songs;

        songTableBody.innerHTML = filteredSongs.length > 0
            ? filteredSongs.map(song => `
                <tr>
                    <td class="image">
                        ${song.imageName
                ? `<img src="${song.imageName}" alt="${song.title}" class="song-image" style="width: 50px; height: 50px; object-fit: cover;">`
                : '<span>No image</span>'
            }
                    </td>
                    <td>${song.title || 'Unknown'}</td>
                    <td>${song.genreNameList.join(', ') || 'None'}</td>
                    <td>${formatDuration(song.duration)}</td>
                    <td>${song.uploadDate || 'Unknown'}</td>
                    <td>
                        ${song.lyrics
                ? `<a class="view-lyrics" href="#" data-id="${song.id}" title="View Lyrics">Show more...</a>`
                : 'None'
            }
                    </td>
                    <td>
                        ${song.description
                ? `<a class="view-description" href="#" data-id="${song.id}" title="View Description">Show more...</a>`
                : 'None'
            }
                    </td>
                    <td>${song.downloadPermission || 'None'}</td>
                    <td class="artists">${song.artistNameList.length ? song.artistNameList.join(', ') : 'None'}</td>
                    <td>${formatNumber(song.numberOfListeners)}</td>
                    <td>${formatNumber(song.countListen)}</td>
                    <td>${formatNumber(song.numberOfDownload)}</td>
                    <td>${formatNumber(song.numberOfUserLike)}</td>
                    <td class="status ${song.status.toLowerCase()}">${song.status.charAt(0).toUpperCase() + song.status.slice(1)}</td>
                    <td>
                        ${song.status === 'draft' || song.status === 'edited'
                ? `
                                <button class="publish" data-id="${song.id}" title="Publish">Publish</button>
                                <button class="edit" data-id="${song.id}" title="Edit">Edit</button>
                                <button class="delete" data-id="${song.id}" title="Delete">Delete</button>
                            `
                : song.status === 'accepted' || song.status === 'declined'
                    ? `
                                <button class="edit" data-id="${song.id}" title="Edit">Edit</button>
                                <button class="delete" data-id="${song.id}" title="Delete">Delete</button>
                            `
                    : 'None'
            }
                    </td>
                </tr>
            `).join('')
            : '<tr><td colspan="15"><span class="no-songs">No songs found.</span></td></tr>';

        if (filteredSongs.length === 0 || totalPages <= 1) {
            paginationDiv.innerHTML = '';
            return;
        }

        paginationDiv.innerHTML = '';
        const prevButton = document.createElement('button');
        prevButton.textContent = 'Previous';
        prevButton.disabled = currentPage === 1;
        prevButton.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                fetchSongs();
            }
        });
        paginationDiv.appendChild(prevButton);

        const maxPagesToShow = 5;
        const startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
        const endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);

        if (startPage > 1) {
            const firstPage = document.createElement('span');
            firstPage.textContent = '1';
            firstPage.addEventListener('click', () => {
                currentPage = 1;
                fetchSongs();
            });
            paginationDiv.appendChild(firstPage);
            if (startPage > 2) paginationDiv.appendChild(document.createTextNode(' ... '));
        }

        for (let i = startPage; i <= endPage; i++) {
            const pageSpan = document.createElement('span');
            pageSpan.textContent = i;
            if (i === currentPage) pageSpan.classList.add('active');
            pageSpan.addEventListener('click', () => {
                currentPage = i;
                fetchSongs();
            });
            paginationDiv.appendChild(pageSpan);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) paginationDiv.appendChild(document.createTextNode(' ... '));
            const lastPage = document.createElement('span');
            lastPage.textContent = totalPages;
            lastPage.addEventListener('click', () => {
                currentPage = totalPages;
                fetchSongs();
            });
            paginationDiv.appendChild(lastPage);
        }

        const nextButton = document.createElement('button');
        nextButton.textContent = 'Next';
        nextButton.disabled = currentPage === totalPages;
        nextButton.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                fetchSongs();
            }
        });
        paginationDiv.appendChild(nextButton);
    };

    // Event Listeners
    if (filterStatusSelect) {
        filterStatusSelect.addEventListener('change', () => {
            currentFilterStatus = filterStatusSelect.value;
            currentPage = 1;
            renderTable();
        });
    }

    if (genreFilterSelect) {
        genreFilterSelect.addEventListener('change', () => {
            currentFilterGenre = genreFilterSelect.value;
            currentPage = 1;
            fetchSongs();
        });
    }

    if (sortBySelect) {
        sortBySelect.addEventListener('change', () => {
            currentSort = sortBySelect.value;
            currentPage = 1;
            fetchSongs();
        });
    }

    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => {
            searchQuery = searchInput.value.trim();
            currentPage = 1;
            fetchSongs();
        }, 300));
    }

    if (songTableBody) {
        songTableBody.addEventListener('click', async (e) => {
            const id = Number(e.target.dataset.id);
            if (!id) return;

            e.preventDefault();
            const song = songs.find(s => s.id === id);
            if (!song) {
                showNotification('Error: Song not found.', true);
                return;
            }

            if (e.target.classList.contains('view-lyrics') && lyricsModal && modalLyricsContent && modalTitle) {
                modalTitle.textContent = `Lyrics of ${song.title}`;
                modalLyricsContent.value = song.lyrics || '';
                modalLyricsContent.placeholder = song.lyrics ? '' : 'No lyrics available';
                lyricsModal.style.display = 'flex';
            } else if (e.target.classList.contains('view-description') && lyricsModal && modalLyricsContent && modalTitle) {
                modalTitle.textContent = `Description of ${song.title}`;
                modalLyricsContent.value = song.description || '';
                modalLyricsContent.placeholder = song.description ? '' : 'No description available';
                lyricsModal.style.display = 'flex';
            } else if (e.target.classList.contains('publish')) {
                showConfirmModal(
                    'Confirm Publish',
                    `Are you sure you want to publish "${song.title}"?`,
                    async () => {
                        try {
                            e.target.disabled = true;
                            e.target.textContent = 'Publishing...';
                            await publishSong(id);
                            showNotification(`Song "${song.title}" published and status changed to Pending.`);
                            await fetchSongs();
                            await updateCards();
                        } catch (error) {
                            showNotification(`Failed to publish song: ${error.message}`, true);
                        } finally {
                            e.target.disabled = false;
                            e.target.textContent = 'Publish';
                        }
                    }
                );
            } else if (e.target.classList.contains('edit')) {
                window.location.href = '../artist/artist_manage_song.html';
            } else if (e.target.classList.contains('delete')) {
                showConfirmModal(
                    'Confirm Delete',
                    `Are you sure you want to delete "${song.title}"?`,
                    async () => {
                        try {
                            e.target.disabled = true;
                            e.target.textContent = 'Deleting...';
                            await deleteSong(id);
                            showNotification(`Song "${song.title}" deleted successfully.`);
                            await fetchSongs();
                            await updateCards();
                        } catch (error) {
                            showNotification(`Failed to delete song: ${error.message}`, true);
                        } finally {
                            e.target.disabled = false;
                            e.target.textContent = 'Delete';
                        }
                    }
                );
            }
        });
    }

    if (closeModal && lyricsModal && modalLyricsContent) {
        closeModal.addEventListener('click', () => {
            lyricsModal.style.display = 'none';
            modalLyricsContent.value = '';
            modalLyricsContent.placeholder = 'No content available';
        });
    }

    if (lyricsModal && modalLyricsContent) {
        window.addEventListener('click', (e) => {
            if (e.target === lyricsModal) {
                lyricsModal.style.display = 'none';
                modalLyricsContent.value = '';
                modalLyricsContent.placeholder = 'No content available';
            }
        });
    }

    // Initialize
    Promise.all([
        fetchGenres(),
        fetchSongs(),
        updateCards()
    ])
        .catch(error => {
            console.error('Initialization error:', error);
            showNotification('Failed to initialize. Please try again.', true);
        });
});