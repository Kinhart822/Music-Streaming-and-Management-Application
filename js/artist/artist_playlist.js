import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";
import {showConfirmModal} from "../confirmation.js";

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const playlistTableBody = document.getElementById('playlist-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const addPlaylistBtn = document.getElementById('add-playlist-btn');
    const addPlaylistForm = document.getElementById('add-playlist-form');
    const playlistForm = document.getElementById('playlist-form');
    const formTitle = document.getElementById('form-title');
    const cancelAddPlaylistBtn = document.getElementById('cancel-add-playlist');
    const titleInput = document.getElementById('playlist-title');
    const descriptionTextarea = document.getElementById('playlist-description');
    const songSearchInput = document.getElementById('song-search');
    const songsContainer = document.getElementById('playlist-songs');
    const selectedSongsList = document.getElementById('selected-songs-list');
    const artistSearchInput = document.getElementById('artist-search');
    const additionalArtistsContainer = document.getElementById('playlist-additional_artists');
    const selectedArtistsList = document.getElementById('selected-artists-list');
    const artistsError = document.getElementById('artists-error');
    const imageInput = document.getElementById('playlist-image');
    const imagePreview = document.getElementById('image-preview');
    const currentImageDiv = document.getElementById('current-image');
    const titleError = document.getElementById('name-error');
    const songsError = document.getElementById('songs-error');
    const imageError = document.getElementById('image-error');
    const contentModal = document.getElementById('content-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalContentBody = document.getElementById('modal-content-body');
    const closeModal = document.getElementById('close-modal');

    // State
    let songs = [];
    let artists = [];
    let playlists = [];
    let selectedSongs = [];
    let selectedArtists = [];
    let editPlaylistId = null;
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput?.value) || 10;
    let currentFilterStatus = 'all';
    let currentSort = 'name-asc';
    let searchQuery = '';
    let totalPages = 1;
    let totalElements = 0;

    // Utility Functions
    const formatDuration = (seconds) => {
        if (!seconds || isNaN(seconds)) return '0:00';
        const minutes = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${minutes}:${secs < 10 ? '0' : ''}${secs}`;
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
            case 'name-asc':
                return {orderBy: 'title', order: 'asc'};
            case 'name-desc':
                return {orderBy: 'title', order: 'desc'};
            case 'date-asc':
                return {orderBy: 'releaseDate', order: 'asc'};
            case 'date-desc':
                return {orderBy: 'releaseDate', order: 'desc'};
            default:
                return {orderBy: 'title', order: 'asc'};
        }
    };

    // API Functions
    const fetchSongs = async () => {
        try {
            const response = await fetchWithRefresh('http://spring-music-container:8080/api/v1/artist/song/allAcceptedSong', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch songs: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            songs = data.map(song => ({
                id: song.id,
                title: song.title || 'Unknown',
                genreNameList: song.genreNameList || [],
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
                artistNameList: song.artistNameList || []
            }));
            if (songsContainer && selectedSongsList) {
                populateSongs(selectedSongs);
            }
        } catch (error) {
            console.error('Error fetching songs:', error);
            showNotification('Failed to load songs.', true);
            if (songsContainer) {
                songsContainer.innerHTML = '<div class="no-songs-text">Failed to load songs.</div>';
            }
        }
    };

    const fetchArtists = async () => {
        try {
            const response = await fetchWithRefresh('http://spring-music-container:8080/api/v1/artist/otherArtists', {
                method: 'GET',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch artists: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            artists = data.map(artist => ({
                id: artist.id,
                name: artist.artistName?.trim() || 'Unknown'
            }));
            if (additionalArtistsContainer && selectedArtistsList) {
                populateArtists(selectedArtists);
            }
        } catch (error) {
            console.error('Error fetching artists:', error);
            showNotification('Failed to load artists.', true);
            if (additionalArtistsContainer) {
                additionalArtistsContainer.innerHTML = '<div class="no-artists-text">Failed to load artists.</div>';
            }
        }
    };

    const fetchPlaylists = async () => {
        try {
            playlistTableBody.innerHTML = '<tr><td colspan="9"><div class="spinner"></div></td></tr>';
            const {orderBy, order} = mapSortToApi(currentSort);
            const requestBody = {
                page: currentPage,
                size: rowsPerPage,
                orderBy,
                order,
                search: searchQuery
            };

            const response = await fetchWithRefresh('http://spring-music-container:8080/api/v1/search/artistPlaylists', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch playlists: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            console.log('API Response:', data); // Debug log

            if (!data || !Array.isArray(data.playlists)) {
                throw new Error('Invalid API response: playlists is not an array');
            }

            playlists = data.playlists.map(playlist => ({
                id: playlist.id,
                playlistName: playlist.playlistName || 'Unknown',
                description: playlist.description || '',
                playTimeLength: playlist.playTimeLength || 0,
                releaseDate: playlist.releaseDate || '',
                songs: playlist.songNameList || [],
                artistNameList: playlist.artistNameList || [],
                imageUrl: playlist.imageUrl || '',
                status: playlist.status ? playlist.status.toLowerCase() : 'draft'
            }));
            currentPage = data.currentPage || 1;
            totalPages = data.totalPages || 1;
            totalElements = data.totalElements || 0;

            renderTable();
        } catch (error) {
            console.error('Error fetching playlists:', error);
            showNotification('Unable to load playlists. Please try again later or contact support.', true);
            playlistTableBody.innerHTML = `<tr><td colspan="9"><span class="no-playlists">Unable to load playlists. Please try again later or contact support.</span></td></tr>`;
            paginationDiv.innerHTML = '';
        }
    };

    const createPlaylist = async (formData) => {
        try {
            const response = await fetchWithRefresh('http://spring-music-container:8080/api/v1/artist/playlist/create', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to create playlist: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to create playlist: ${error.message}`);
        }
    };

    const updatePlaylist = async (id, formData) => {
        try {
            const response = await fetchWithRefresh(`http://spring-music-container:8080/api/v1/artist/playlist/update/${id}`, {
                method: 'PUT',
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to update playlist: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to update playlist: ${error.message}`);
        }
    };

    const publishPlaylist = async (id) => {
        try {
            const response = await fetchWithRefresh(`http://spring-music-container:8080/api/v1/artist/playlist/upload/${id}`, {
                method: 'POST',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to publish playlist: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to publish playlist: ${error.message}`);
        }
    };

    const deletePlaylist = async (id) => {
        try {
            const response = await fetchWithRefresh(`http://spring-music-container:8080/api/v1/artist/playlist/delete/${id}`, {
                method: 'DELETE',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to delete playlist: ${response.status} - ${errorText}`);
            }
        } catch (error) {
            throw new Error(`Failed to delete playlist: ${error.message}`);
        }
    };

    // UI Functions
    const updateSelectedSongsDisplay = () => {
        if (!selectedSongsList) return;
        selectedSongsList.innerHTML = selectedSongs.length > 0
            ? selectedSongs.map(songId => {
                const song = songs.find(s => s.id === songId);
                const title = song ? song.title : 'Unknown';
                return `
                    <li data-song-id="${songId}">
                        ${title}
                        <span class="remove-song" data-song-id="${songId}">×</span>
                    </li>
                `;
            }).join('')
            : '<li>No songs selected</li>';

        selectedSongsList.querySelectorAll('.remove-song').forEach(button => {
            button.addEventListener('click', () => {
                const songId = Number(button.dataset.songId);
                selectedSongs = selectedSongs.filter(id => id !== songId);
                updateSelectedSongsDisplay();
                populateSongs(selectedSongs);
            });
        });
    };

    const updateSelectedArtistsDisplay = () => {
        if (!selectedArtistsList) return;
        selectedArtistsList.innerHTML = selectedArtists.length > 0
            ? selectedArtists.map(artistId => {
                const artist = artists.find(a => a.id === artistId);
                const name = artist ? artist.name : 'Unknown';
                return `
                    <li data-artist-id="${artistId}">
                        ${name}
                        <span class="remove-artist" data-artist-id="${artistId}">×</span>
                    </li>
                `;
            }).join('')
            : '<li>No artists selected</li>';

        selectedArtistsList.querySelectorAll('.remove-artist').forEach(button => {
            button.addEventListener('click', () => {
                const artistId = Number(button.dataset.artistId);
                selectedArtists = selectedArtists.filter(id => id !== artistId);
                updateSelectedArtistsDisplay();
                populateArtists(selectedArtists);
            });
        });
    };

    const populateSongs = (preSelectedSongIds = []) => {
        if (!songsContainer || !songSearchInput) return;
        if (preSelectedSongIds.length && !selectedSongs.length) {
            selectedSongs = preSelectedSongIds.filter(songId => {
                const song = songs.find(s => s.id === songId);
                return song && song.status.toLowerCase() === 'accepted';
            });
        }

        const query = songSearchInput.value.trim().toLowerCase();
        const filteredSongs = query
            ? songs.filter(song => song.title.toLowerCase().includes(query) && song.status.toLowerCase() === 'accepted')
            : songs.filter(song => song.status.toLowerCase() === 'accepted');

        songsContainer.innerHTML = filteredSongs.length
            ? filteredSongs.reduce((html, song, i) => {
                if (i % 2 === 0) html += '<div class="song-row">';
                html += `
                    <span class="song-item ${selectedSongs.includes(song.id) ? 'selected' : ''}" data-song-id="${song.id}">
                        ${song.title}
                    </span>
                `;
                if (i % 2 === 1 || i === filteredSongs.length - 1) {
                    if (i % 2 === 0) html += '<span class="song-item" style="visibility: hidden; flex: 1;"></span>';
                    html += '</div>';
                }
                return html;
            }, '')
            : '<div class="no-songs-text">No songs available</div>';

        songsContainer.querySelectorAll('.song-item[data-song-id]').forEach(item => {
            item.addEventListener('dblclick', () => {
                const songId = Number(item.dataset.songId);
                selectedSongs = selectedSongs.includes(songId)
                    ? selectedSongs.filter(id => id !== songId)
                    : [...selectedSongs, songId];
                updateSelectedSongsDisplay();
                populateSongs(selectedSongs);
            });
        });

        updateSelectedSongsDisplay();
    };

    const populateArtists = (preSelectedArtistIds = []) => {
        if (!additionalArtistsContainer || !artistSearchInput) return;
        if (preSelectedArtistIds.length && !selectedArtists.length) {
            selectedArtists = [...preSelectedArtistIds];
        }

        const query = artistSearchInput.value.trim().toLowerCase();
        const filteredArtists = query
            ? artists.filter(artist => artist.name.toLowerCase().includes(query))
            : artists;

        additionalArtistsContainer.innerHTML = filteredArtists.length
            ? filteredArtists.reduce((html, artist, i) => {
                if (i % 2 === 0) html += '<div class="artist-row">';
                html += `
                    <span class="artist-item ${selectedArtists.includes(artist.id) ? 'selected' : ''}" data-artist-id="${artist.id}">
                        ${artist.name}
                    </span>
                `;
                if (i % 2 === 1 || i === filteredArtists.length - 1) {
                    if (i % 2 === 0) html += '<span class="artist-item" style="visibility: hidden; flex: 1;"></span>';
                    html += '</div>';
                }
                return html;
            }, '')
            : '<div class="no-artists-text">No artists available</div>';

        additionalArtistsContainer.querySelectorAll('.artist-item[data-artist-id]').forEach(item => {
            item.addEventListener('dblclick', () => {
                const artistId = Number(item.dataset.artistId);
                selectedArtists = selectedArtists.includes(artistId)
                    ? selectedArtists.filter(id => id !== artistId)
                    : [...selectedArtists, artistId];
                updateSelectedArtistsDisplay();
                populateArtists(selectedArtists);
            });
        });

        updateSelectedArtistsDisplay();
    };

    const resetForm = (mode = 'add') => {
        if (!playlistForm || !formTitle || !titleInput || !descriptionTextarea || !songSearchInput ||
            !artistSearchInput || !imageInput || !imagePreview || !currentImageDiv) return;

        playlistForm.dataset.mode = mode;
        formTitle.textContent = mode === 'add' ? 'Add Playlist' : 'Edit Playlist';
        titleInput.value = '';
        descriptionTextarea.value = '';
        songSearchInput.value = '';
        artistSearchInput.value = '';
        selectedSongs = [];
        selectedArtists = [];
        imageInput.value = '';
        imagePreview.style.display = 'none';
        imagePreview.src = '';
        currentImageDiv.textContent = '';
        if (titleError) titleError.style.display = 'none';
        if (songsError) songsError.style.display = 'none';
        if (artistsError) artistsError.style.display = 'none';
        if (imageError) imageError.style.display = 'none';
        editPlaylistId = null;
        fetchSongs();
        fetchArtists();
    };

    const populateEditForm = (playlist) => {
        if (!playlistForm || !addPlaylistForm) {
            showNotification('Error: Form not found.', true);
            return;
        }
        playlistForm.dataset.mode = 'edit';
        formTitle.textContent = 'Edit Playlist';
        titleInput.value = playlist.playlistName || '';
        descriptionTextarea.value = playlist.description || '';
        songSearchInput.value = '';
        artistSearchInput.value = '';

        selectedSongs = (playlist.songs || []).map(title => {
            const song = songs.find(s => s.title === title);
            return song ? song.id : null;
        }).filter(id => id !== null);
        selectedArtists = (playlist.artistNameList || []).map(name => {
            const artist = artists.find(a => a.name === name);
            return artist ? artist.id : null;
        }).filter(id => id !== null);
        imageInput.value = '';
        imagePreview.style.display = playlist.imageUrl ? 'block' : 'none';
        imagePreview.src = playlist.imageUrl || '';
        currentImageDiv.textContent = playlist.imageUrl ? `Current image: ${playlist.imageUrl}` : '';
        if (titleError) titleError.style.display = 'none';
        if (songsError) songsError.style.display = 'none';
        if (artistsError) artistsError.style.display = 'none';
        if (imageError) imageError.style.display = 'none';
        editPlaylistId = playlist.id;
        addPlaylistForm.classList.add('active');
        fetchSongs();
        fetchArtists();
    };

    const renderTable = () => {
        if (!playlistTableBody || !paginationDiv) return;

        let filteredPlaylists = currentFilterStatus !== 'all'
            ? playlists.filter(p => p.status.toLowerCase() === currentFilterStatus)
            : playlists;

        playlistTableBody.innerHTML = filteredPlaylists.length > 0
            ? filteredPlaylists.map(playlist => `
                <tr>
                    <td class="image">
                        ${playlist.imageUrl
                ? `<img src="${playlist.imageUrl}" alt="${playlist.playlistName}" class="playlist-image" style="width: 50px; height: 50px; object-fit: cover;">`
                : '<span>No image</span>'
            }
                    </td>
                    <td>${playlist.playlistName || 'Unknown'}</td>
                    <td>
                        ${playlist.description
                ? `<a class="view-description" href="#" data-id="${playlist.id}" title="View Description">Show more...</a>`
                : 'None'
            }
                    </td>
                    <td>${formatDuration(playlist.playTimeLength)}</td>
                    <td>${playlist.releaseDate || 'Unknown'}</td>
                    <td class="artists">${playlist.artistNameList.length ? playlist.artistNameList.join(', ') : 'None'}</td>
                    <td>
                        <a class="view-songs" href="#" data-id="${playlist.id}" title="View Songs">${playlist.songs.length} song${playlist.songs.length !== 1 ? 's' : ''}</a>
                    </td>
                    <td class="status ${playlist.status.toLowerCase()}">${playlist.status.charAt(0).toUpperCase() + playlist.status.slice(1)}</td>
                    <td>
                        ${playlist.status === 'draft' || playlist.status === 'edited'
                ? `
                                <button class="publish" data-id="${playlist.id}" title="Publish">Publish</button>
                                <button class="edit" data-id="${playlist.id}" title="Edit">Edit</button>
                                <button class="delete" data-id="${playlist.id}" title="Delete">Delete</button>
                            `
                : playlist.status === 'accepted' || playlist.status === 'declined'
                    ? `
                                <button class="edit" data-id="${playlist.id}" title="Edit">Edit</button>
                                <button class="delete" data-id="${playlist.id}" title="Delete">Delete</button>
                            `
                    : 'None'
            }
                    </td>
                </tr>
            `).join('')
            : '<tr><td colspan="9"><span class="no-playlists">No playlists found.</span></td></tr>';

        if (filteredPlaylists.length === 0 || totalPages <= 1) {
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
                fetchPlaylists();
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
                fetchPlaylists();
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
                fetchPlaylists();
            });
            paginationDiv.appendChild(pageSpan);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) paginationDiv.appendChild(document.createTextNode(' ... '));
            const lastPage = document.createElement('span');
            lastPage.textContent = totalPages;
            lastPage.addEventListener('click', () => {
                currentPage = totalPages;
                fetchPlaylists();
            });
            paginationDiv.appendChild(lastPage);
        }

        const nextButton = document.createElement('button');
        nextButton.textContent = 'Next';
        nextButton.disabled = currentPage === totalPages;
        nextButton.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                fetchPlaylists();
            }
        });
        paginationDiv.appendChild(nextButton);
    };

    // Event Listeners
    if (imageInput) {
        imageInput.addEventListener('change', () => {
            const file = imageInput.files[0];
            const allowedImageTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp', 'image/tiff', 'image/svg+xml'];
            if (file) {
                if (!allowedImageTypes.includes(file.type)) {
                    if (imageError) {
                        imageError.textContent = 'Only image files (JPG, PNG, GIF, WEBP, BMP, TIFF, SVG) are allowed';
                        imageError.style.display = 'block';
                    }
                    imagePreview.style.display = 'none';
                    imagePreview.src = '';
                } else if (file.size > 5 * 1024 * 1024) {
                    if (imageError) {
                        imageError.textContent = 'Image file size exceeds 5MB';
                        imageError.style.display = 'block';
                    }
                    imagePreview.style.display = 'none';
                    imagePreview.src = '';
                } else {
                    imagePreview.src = URL.createObjectURL(file);
                    imagePreview.style.display = 'block';
                    if (imageError) imageError.style.display = 'none';
                }
            } else {
                imagePreview.style.display = 'none';
                imagePreview.src = '';
                if (imageError) imageError.style.display = 'none';
            }
        });
    }

    if (songSearchInput) {
        songSearchInput.addEventListener('input', debounce(() => {
            populateSongs(selectedSongs);
        }, 300));
    }

    if (artistSearchInput) {
        artistSearchInput.addEventListener('input', debounce(() => {
            populateArtists(selectedArtists);
        }, 300));
    }

    if (addPlaylistBtn) {
        addPlaylistBtn.addEventListener('click', () => {
            resetForm('add');
            if (addPlaylistForm) addPlaylistForm.classList.add('active');
        });
    }

    if (cancelAddPlaylistBtn) {
        cancelAddPlaylistBtn.addEventListener('click', () => {
            if (addPlaylistForm) addPlaylistForm.classList.remove('active');
            resetForm('add');
        });
    }

    if (filterStatusSelect) {
        filterStatusSelect.addEventListener('change', () => {
            currentFilterStatus = filterStatusSelect.value;
            currentPage = 1;
            renderTable();
        });
    }

    if (sortBySelect) {
        sortBySelect.addEventListener('change', () => {
            currentSort = sortBySelect.value;
            currentPage = 1;
            fetchPlaylists();
        });
    }

    if (rowsPerPageInput) {
        const updateRowsPerPage = debounce(() => {
            const value = parseInt(rowsPerPageInput.value);
            if (isNaN(value) || value < 1) {
                rowsPerPageInput.value = rowsPerPage || 10;
                return;
            }
            if (value > 100) {
                rowsPerPageInput.value = 100;
                rowsPerPage = 100;
            } else {
                rowsPerPage = value;
            }
            currentPage = 1;
            fetchPlaylists();
        }, 300);
        rowsPerPageInput.addEventListener('input', updateRowsPerPage);
    }

    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => {
            searchQuery = searchInput.value.trim();
            currentPage = 1;
            fetchPlaylists();
        }, 300));
    }

    if (playlistForm) {
        playlistForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            let isValid = true;

            if (titleError) titleError.style.display = 'none';
            if (songsError) songsError.style.display = 'none';
            if (artistsError) artistsError.style.display = 'none';
            if (imageError) imageError.style.display = 'none';

            const title = titleInput?.value.trim() || '';
            if (!title) {
                if (titleError) {
                    titleError.textContent = 'Please enter a valid title';
                    titleError.style.display = 'block';
                }
                isValid = false;
            }

            if (selectedSongs.length > 0) {
                const invalidSongs = selectedSongs.filter(songId => {
                    const song = songs.find(s => s.id === songId);
                    return !song || song.status.toLowerCase() !== 'accepted';
                });
                if (invalidSongs.length) {
                    if (songsError) {
                        songsError.textContent = 'All selected songs must have Accepted status';
                        songsError.style.display = 'block';
                    }
                    isValid = false;
                }
            }

            const image = imageInput?.files[0];
            let imageName = editPlaylistId ? (playlists.find(p => p.id === editPlaylistId)?.imageUrl || '') : '';
            if (image) {
                if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp', 'image/tiff', 'image/svg+xml'].includes(image.type)) {
                    if (imageError) {
                        imageError.textContent = 'Only image files (JPG, PNG, GIF, WEBP, BMP, TIFF, SVG) are allowed';
                        imageError.style.display = 'block';
                    }
                    isValid = false;
                } else if (image.size > 5 * 1024 * 1024) {
                    if (imageError) {
                        imageError.textContent = 'Image file size exceeds 5MB';
                        imageError.style.display = 'block';
                    }
                    isValid = false;
                } else {
                    imageName = image.name;
                }
            }

            if (isValid) {
                const formData = new FormData();
                formData.append('playlistName', title);
                formData.append('description', descriptionTextarea?.value.trim() || '');
                selectedSongs.forEach(songId => formData.append('songIds', songId));
                selectedArtists.forEach(artistId => formData.append('artistIds', artistId));
                if (image) formData.append('image', image);

                const saveButton = playlistForm.querySelector('button[type="submit"]');
                if (saveButton) {
                    saveButton.disabled = true;
                    saveButton.classList.add('loading');
                    saveButton.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Saving...';
                }

                try {
                    if (editPlaylistId) {
                        await updatePlaylist(editPlaylistId, formData);
                        showNotification(`Playlist "${title}" updated successfully.`);
                    } else {
                        await createPlaylist(formData);
                        showNotification(`Playlist "${title}" created successfully.`);
                    }
                    if (addPlaylistForm) addPlaylistForm.classList.remove('active');
                    resetForm('add');
                    currentPage = 1;
                    await fetchPlaylists();
                } catch (error) {
                    showNotification(`Failed to ${editPlaylistId ? 'update' : 'save'} playlist: ${error.message}`, true);
                } finally {
                    if (saveButton) {
                        saveButton.disabled = false;
                        saveButton.classList.remove('loading');
                        saveButton.innerHTML = 'Save';
                    }
                }
            }
        });
    }

    if (playlistTableBody) {
        playlistTableBody.addEventListener('click', async (e) => {
            const id = Number(e.target.dataset.id);
            if (!id) return;

            e.preventDefault();
            const playlist = playlists.find(p => p.id === id);
            if (!playlist) {
                showNotification('Error: Playlist not found.', true);
                return;
            }

            if (e.target.classList.contains('view-songs') && contentModal && modalContentBody && modalTitle) {
                modalTitle.textContent = `Songs in ${playlist.playlistName}`;
                modalContentBody.innerHTML = `
                    <table>
                        <thead>
                            <tr>
                                <th>Title</th>
                                <th>Genre</th>
                                <th>Duration</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${playlist.songs.length
                    ? playlist.songs.map(title => {
                        const song = songs.find(s => s.title === title) || {};
                        return `
                                        <tr>
                                            <td>${title}</td>
                                            <td>${song.genreNameList?.join(', ') || 'Unknown'}</td>
                                            <td>${song.duration || '0:00'}</td>
                                            <td class="status ${song.status?.toLowerCase() || 'draft'}">${(song.status || 'draft').charAt(0).toUpperCase() + (song.status || 'draft').slice(1)}</td>
                                        </tr>
                                    `;
                    }).join('')
                    : '<tr><td colspan="4">No songs found.</td></tr>'
                }
                        </tbody>
                    </table>
                `;
                contentModal.style.display = 'flex';
            } else if (e.target.classList.contains('view-description') && contentModal && modalContentBody && modalTitle) {
                modalTitle.textContent = `Description of ${playlist.playlistName}`;
                modalContentBody.innerHTML = `
                    <textarea class="lyrics-content" readonly placeholder="No description available">${playlist.description || ''}</textarea>
                `;
                contentModal.style.display = 'flex';
            } else if (e.target.classList.contains('publish')) {
                showConfirmModal(
                    'Confirm Publish',
                    `Are you sure you want to publish "${playlist.playlistName}"?`,
                    async () => {
                        try {
                            e.target.disabled = true;
                            e.target.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Publishing';
                            await publishPlaylist(id);
                            showNotification(`Playlist "${playlist.playlistName}" published and status changed to Pending.`);
                            await fetchPlaylists();
                        } catch (error) {
                            showNotification(`Failed to publish playlist: ${error.message}`, true);
                        } finally {
                            e.target.disabled = false;
                            e.target.innerHTML = 'Publish';
                        }
                    }
                );
            } else if (e.target.classList.contains('edit')) {
                populateEditForm(playlist);
            } else if (e.target.classList.contains('delete')) {
                showConfirmModal(
                    'Confirm Delete',
                    `Are you sure you want to delete "${playlist.playlistName}"?`,
                    async () => {
                        try {
                            e.target.disabled = true;
                            e.target.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Deleting';
                            await deletePlaylist(id);
                            showNotification(`Playlist "${playlist.playlistName}" deleted successfully.`);
                            await fetchPlaylists();
                        } catch (error) {
                            showNotification(`Failed to delete playlist: ${error.message}`, true);
                        } finally {
                            e.target.disabled = false;
                            e.target.innerHTML = 'Delete';
                        }
                    }
                );
            }
        });
    }

    if (closeModal && contentModal && modalContentBody) {
        closeModal.addEventListener('click', () => {
            contentModal.style.display = 'none';
            modalContentBody.innerHTML = '';
        });
    }

    if (contentModal && modalContentBody) {
        window.addEventListener('click', (e) => {
            if (e.target === contentModal) {
                contentModal.style.display = 'none';
                modalContentBody.innerHTML = '';
            }
        });
    }

    // Initialize
    Promise.all([fetchSongs(), fetchArtists(), fetchPlaylists()])
        .then(() => {
            if (playlistForm) resetForm('add');
        })
        .catch(error => {
            console.error('Initialization error:', error);
            showNotification('Failed to initialize. Please try again.', true);
        });
});