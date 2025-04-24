import { fetchWithRefresh } from '/js/api/refresh.js';

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
    let rowsPerPage = 10; // Fixed, as rowsPerPageInput is not in HTML
    let currentFilterStatus = 'all';
    let currentFilterGenre = 'all';
    let currentSort = 'title-asc'; // Default to match HTML
    let searchQuery = '';
    let totalPages = 1;
    let totalElements = 0;

    // Utility Function to Format Numbers
    const formatNumber = (num) => {
        if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
        if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
        return num.toString();
    };

    // Utility Function to Debounce
    const debounce = (func, delay) => {
        let timeoutId;
        return (...args) => {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => func(...args), delay);
        };
    };

    // API Functions for Cards
    const fetchTotalSongs = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalSongs', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Failed to fetch total songs: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching total songs:', error);
            return 0;
        }
    };

    const fetchTotalPlaylists = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalPlaylists', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Failed to fetch total playlists: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching total playlists:', error);
            return 0;
        }
    };

    const fetchTotalAlbums = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalAlbums', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Failed to fetch total albums: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching total albums:', error);
            return 0;
        }
    };

    const fetchTotalListeners = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalListeners', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Failed to fetch total listeners: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching total listeners:', error);
            return 0;
        }
    };

    const fetchTotalFollowers = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalFollowers', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Failed to fetch total followers: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching total followers:', error);
            return 0;
        }
    };

    const fetchTotalLikes = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalLikes', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Failed to fetch total likes: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching total likes:', error);
            return 0;
        }
    };

    const fetchTotalDownloads = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/totalDownloads', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error(`Failed to fetch total downloads: ${response.status}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching total downloads:', error);
            return 0;
        }
    };

    // Function to Update Card Values
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

            // Update card values
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
            document.querySelectorAll('.card .card--content h1').forEach(h1 => h1.textContent = '0');
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                localStorage.clear();
                window.location.href = '../auth/login_register.html';
            }
        }
    };

    // API Functions
    const fetchGenres = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/genre/allGenres', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) throw new Error(`Failed to fetch genres: ${response.status}`);

            const data = await response.json();
            console.log('Fetched genres:', data);

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
            if (genreFilterSelect) {
                genreFilterSelect.innerHTML = '<option value="all">All Genres</option>';
            }
        }
    };

    const fetchSongs = async () => {
        try {
            const { orderBy, order } = mapSortToApi(currentSort);
            const genreId = currentFilterGenre !== 'all' ? currentFilterGenre : null;

            const requestBody = {
                page: currentPage,
                size: rowsPerPage,
                genreId: genreId ? parseInt(genreId) : null,
                orderBy,
                order,
                search: searchQuery
            };

            const response = await fetchWithRefresh('http://localhost:8080/api/v1/search/songs', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) throw new Error(`Failed to fetch songs: ${response.status}`);

            const data = await response.json();
            console.log('Fetched songs:', data);

            songs = data.songs.map(song => ({
                id: song.id,
                title: song.title || 'Unknown',
                genre: song.genreNameList?.length ? song.genreNameList[0] : 'None',
                duration: song.duration || '0:00',
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
                additionalArtistNameList: song.additionalArtistNameList || []
            }));
            currentPage = data.currentPage || 1;
            totalPages = data.totalPages || 1;
            totalElements = data.totalElements || 0;

            renderTable();
        } catch (error) {
            console.error('Error fetching songs:', error);
            if (songTableBody) {
                songTableBody.innerHTML = '<tr><td colspan="15"><span class="no-songs">Failed to load songs.</span></td></tr>';
            }
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                localStorage.clear();
                window.location.href = '../auth/login_register.html';
            }
        }
    };

    const publishSong = async (id) => {
        try {
            const response = await fetchWithRefresh(`http://localhost:8080/api/v1/artist/song/upload/${id}`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) throw new Error(`Failed to publish song: ${response.status}`);

            const data = await response.json();
            console.log('Published song:', data);
            return data;
        } catch (error) {
            throw new Error(`Failed to publish song: ${error.message}`);
        }
    };

    const deleteSong = async (id) => {
        try {
            const response = await fetchWithRefresh(`http://localhost:8080/api/v1/artist/song/delete/${id}`, {
                method: 'DELETE',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) throw new Error(`Failed to delete song: ${response.status}`);

            console.log(`Deleted song ${id}`);
        } catch (error) {
            throw new Error(`Failed to delete song: ${error.message}`);
        }
    };

    // UI Functions
    const renderTable = () => {
        if (!songTableBody || !paginationDiv) return;

        if (currentSort === 'title-asc') {
            songs.sort((a, b) => a.title.toLowerCase().localeCompare(b.title.toLowerCase()));
        } else if (currentSort === 'title-desc') {
            songs.sort((a, b) => b.title.toLowerCase().localeCompare(a.title.toLowerCase()));
        }

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
                    <td>${song.genre || 'None'}</td>
                    <td>${song.duration || '0:00'}</td>
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
                    <td class="additional-artists">${song.additionalArtistNameList.length ? song.additionalArtistNameList.join(', ') : 'None'}</td>
                    <td>${song.numberOfListeners || 0}</td>
                    <td>${song.countListen || 0}</td>
                    <td>${song.numberOfDownload || 0}</td>
                    <td>${song.numberOfUserLike || 0}</td>
                    <td class="status ${song.status.toLowerCase()}">${song.status.charAt(0).toUpperCase() + song.status.slice(1)}</td>
                    <td>
                        ${song.status === 'draft' || song.status === 'edited' ?
                `
                                <button class="publish" data-id="${song.id}" title="Publish">Publish</button>
                                <button class="edit" data-id="${song.id}" title="Edit">Edit</button>
                                <button class="delete" data-id="${song.id}" title="Delete">Delete</button>
                            `
                : song.status === 'accepted' || song.status === 'declined' ?
                    `
                                    <button class="edit" data-id="${song.id}" title="Edit">Edit</button>
                                    <button class="delete" data-id="${song.id}" title="Delete">Delete</button>
                                `
                    : 'None'
            }
                    </td>
                </tr>
            `).join('')
            : '<tr><td colspan="15"><span class="no-songs">No songs found.</span></td></tr>';
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
            renderTable();
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
            console.log('Clicked ID:', id, 'Songs:', songs.map(s => ({ id: s.id, title: s.title })));
            const song = songs.find(s => s.id === id);
            if (!song) {
                console.error('Song not found for ID:', id);
                alert('Error: Song not found.');
                return;
            }

            if (e.target.classList.contains('view-lyrics') && lyricsModal && modalLyricsContent && modalTitle) {
                modalTitle.textContent = 'Song Lyrics';
                modalLyricsContent.value = song.lyrics || '';
                modalLyricsContent.placeholder = song.lyrics ? '' : 'No lyrics available';
                lyricsModal.style.display = 'flex';
            } else if (e.target.classList.contains('view-description') && lyricsModal && modalLyricsContent && modalTitle) {
                modalTitle.textContent = 'Song Description';
                modalLyricsContent.value = song.description || '';
                modalLyricsContent.placeholder = song.description ? '' : 'No description available';
                lyricsModal.style.display = 'flex';
            } else if (e.target.classList.contains('publish')) {
                try {
                    await publishSong(id);
                    alert(`Song "${song.title}" published and status changed to Processing state.`);
                    await fetchSongs();
                    await updateCards();
                } catch (error) {
                    alert(`Failed to publish song: ${error.message}`);
                }
            } else if (e.target.classList.contains('edit')) {
                window.location.href = 'artist_manage_song.html';
            } else if (e.target.classList.contains('delete')) {
                if (confirm(`Are you sure you want to delete "${song.title}"?`)) {
                    try {
                        await deleteSong(id);
                        alert(`Song "${song.title}" deleted successfully.`);
                        await fetchSongs();
                        await updateCards();
                    } catch (error) {
                        alert(`Failed to delete song: ${error.message}`);
                    }
                }
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

    // Utility Function to Map Sort Option to API Parameters
    const mapSortToApi = (sort) => {
        switch (sort) {
            case 'title-asc':
                return { orderBy: 'title', order: 'asc' };
            case 'title-desc':
                return { orderBy: 'title', order: 'desc' };
            default:
                return { orderBy: 'title', order: 'asc' };
        }
    };

    // Initialize
    Promise.all([
        fetchGenres(),
        fetchSongs(),
        updateCards()
    ])
        .catch(error => {
            console.error('Initialization error:', error);
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                localStorage.clear();
                window.location.href = '../auth/login_register.html';
            }
        });
});