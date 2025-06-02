import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";
import {showConfirmModal} from "../confirmation.js";

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const albumTableBody = document.getElementById('album-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const addAlbumBtn = document.getElementById('add-album-btn');
    const addAlbumForm = document.getElementById('add-album-form');
    const albumForm = document.getElementById('album-form');
    const formTitle = document.getElementById('form-title');
    const cancelAddAlbumBtn = document.getElementById('cancel-add-album');
    const titleInput = document.getElementById('album-title');
    const descriptionTextarea = document.getElementById('album-description');
    const songSearchInput = document.getElementById('song-search');
    const songsContainer = document.getElementById('album-songs');
    const selectedSongsList = document.getElementById('selected-songs-list');
    const artistSearchInput = document.getElementById('artist-search');
    const artistsContainer = document.getElementById('album-artists');
    const selectedArtistsList = document.getElementById('selected-artists-list');
    const artistsError = document.getElementById('artists-error');
    const imageInput = document.getElementById('album-image');
    const imagePreview = document.getElementById('image-preview');
    const currentImageDiv = document.getElementById('current-image');
    const titleError = document.getElementById('title-error');
    const songsError = document.getElementById('songs-error');
    const imageError = document.getElementById('image-error');
    const contentModal = document.getElementById('content-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalContentBody = document.getElementById('modal-content-body');
    const closeModal = document.getElementById('close-modal');

    // State
    let songs = [];
    let artists = [];
    let albums = [];
    let selectedSongs = [];
    let selectedArtists = [];
    let editAlbumId = null;
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput?.value) || 10;
    let currentFilterStatus = 'all';
    let currentSort = 'title-asc';
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
            case 'title-asc':
                return {orderBy: 'title', order: 'asc'};
            case 'title-desc':
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
            const response = await fetchWithRefresh('https://music-streaming-and-management.onrender.com/api/v1/artist/song/allAcceptedSong', {
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
            const response = await fetchWithRefresh('https://music-streaming-and-management.onrender.com/api/v1/artist/otherArtists', {
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
            if (artistsContainer && selectedArtistsList) {
                populateArtists(selectedArtists);
            }
        } catch (error) {
            console.error('Error fetching artists:', error);
            showNotification('Failed to load artists.', true);
            if (artistsContainer) {
                artistsContainer.innerHTML = '<div class="no-artists-text">Failed to load artists.</div>';
            }
        }
    };

    const fetchAlbums = async () => {
        try {
            albumTableBody.innerHTML = '<tr><td colspan="9"><div class="spinner"></div></td></tr>';
            const {orderBy, order} = mapSortToApi(currentSort);
            const requestBody = {
                page: currentPage,
                size: rowsPerPage,
                orderBy,
                order,
                search: searchQuery
            };

            const response = await fetchWithRefresh('https://music-streaming-and-management.onrender.com/api/v1/search/artistAlbums', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch albums: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            if (!data || !Array.isArray(data.albums)) {
                throw new Error('Invalid API response: albums is not an array');
            }

            albums = data.albums.map(album => ({
                id: album.id,
                albumName: album.albumName || 'Unknown',
                description: album.description || '',
                albumTimeLength: album.albumTimeLength || 0,
                releaseDate: album.releaseDate || '',
                songs: album.songNameList || [],
                artistNameList: album.artistNameList || [],
                imageUrl: album.imageUrl || '',
                status: album.status ? album.status.toLowerCase() : 'draft'
            }));
            currentPage = data.currentPage || 1;
            totalPages = data.totalPages || 1;
            totalElements = data.totalElements || 0;

            renderTable();
        } catch (error) {
            console.error('Error fetching albums:', error);
            showNotification('Unable to load albums. Please try again later or contact support.', true);
            albumTableBody.innerHTML = `<tr><td colspan="9"><span class="no-albums">Unable to load albums. Please try again later or contact support.</span></td></tr>`;
            paginationDiv.innerHTML = '';
        }
    };

    const createAlbum = async (formData) => {
        try {
            const response = await fetchWithRefresh('https://music-streaming-and-management.onrender.com/api/v1/artist/album/create', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to create album: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to create album: ${error.message}`);
        }
    };

    const updateAlbum = async (id, formData) => {
        try {
            const response = await fetchWithRefresh(`https://music-streaming-and-management.onrender.com/api/v1/artist/album/update/${id}`, {
                method: 'PUT',
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to update album: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to update album: ${error.message}`);
        }
    };

    const publishAlbum = async (id) => {
        try {
            const response = await fetchWithRefresh(`https://music-streaming-and-management.onrender.com/api/v1/artist/album/upload/${id}`, {
                method: 'POST',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to publish album: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to publish album: ${error.message}`);
        }
    };

    const deleteAlbum = async (id) => {
        try {
            const response = await fetchWithRefresh(`https://music-streaming-and-management.onrender.com/api/v1/artist/album/delete/${id}`, {
                method: 'DELETE',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to delete album: ${response.status} - ${errorText}`);
            }
        } catch (error) {
            throw new Error(`Failed to delete album: ${error.message}`);
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
        if (!artistsContainer || !artistSearchInput) return;
        if (preSelectedArtistIds.length && !selectedArtists.length) {
            selectedArtists = [...preSelectedArtistIds];
        }

        const query = artistSearchInput.value.trim().toLowerCase();
        const filteredArtists = query
            ? artists.filter(artist => artist.name.toLowerCase().includes(query))
            : artists;

        artistsContainer.innerHTML = filteredArtists.length
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

        artistsContainer.querySelectorAll('.artist-item[data-artist-id]').forEach(item => {
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
        if (!albumForm || !formTitle || !titleInput || !descriptionTextarea || !songSearchInput ||
            !artistSearchInput || !imageInput || !imagePreview || !currentImageDiv) return;

        albumForm.dataset.mode = mode;
        formTitle.textContent = mode === 'add' ? 'Add Album' : 'Edit Album';
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
        editAlbumId = null;
        fetchSongs();
        fetchArtists();
    };

    const populateEditForm = (album) => {
        if (!albumForm || !addAlbumForm) {
            showNotification('Error: Form not found.', true);
            return;
        }
        albumForm.dataset.mode = 'edit';
        formTitle.textContent = 'Edit Album';
        titleInput.value = album.albumName || '';
        descriptionTextarea.value = album.description || '';
        songSearchInput.value = '';
        artistSearchInput.value = '';

        selectedSongs = (album.songs || []).map(title => {
            const song = songs.find(s => s.title === title);
            return song ? song.id : null;
        }).filter(id => id !== null);
        selectedArtists = (album.artistNameList || []).map(name => {
            const artist = artists.find(a => a.name === name);
            return artist ? artist.id : null;
        }).filter(id => id !== null);
        imageInput.value = '';
        imagePreview.style.display = album.imageUrl ? 'block' : 'none';
        imagePreview.src = album.imageUrl || '';
        currentImageDiv.textContent = album.imageUrl ? `Current image: ${album.imageUrl}` : '';
        if (titleError) titleError.style.display = 'none';
        if (songsError) songsError.style.display = 'none';
        if (artistsError) artistsError.style.display = 'none';
        if (imageError) imageError.style.display = 'none';
        editAlbumId = album.id;
        addAlbumForm.classList.add('active');
        fetchSongs();
        fetchArtists();
    };

    const renderTable = () => {
        if (!albumTableBody || !paginationDiv) return;

        let filteredAlbums = currentFilterStatus !== 'all'
            ? albums.filter(a => a.status.toLowerCase() === currentFilterStatus)
            : albums;

        albumTableBody.innerHTML = filteredAlbums.length > 0
            ? filteredAlbums.map(album => `
                <tr>
                    <td class="image">
                        ${album.imageUrl
                ? `<img src="${album.imageUrl}" alt="${album.albumName}" class="album-image" style="width: 50px; height: 50px; object-fit: cover;">`
                : '<span>No image</span>'
            }
                    </td>
                    <td>${album.albumName || 'Unknown'}</td>
                    <td>
                        ${album.description
                ? `<a class="view-description" href="#" data-id="${album.id}" title="View Description">Show more...</a>`
                : 'None'
            }
                    </td>
                    <td>${formatDuration(album.albumTimeLength)}</td>
                    <td>${album.releaseDate || 'Unknown'}</td>
                    <td class="artists">${album.artistNameList.length ? album.artistNameList.join(', ') : 'None'}</td>
                    <td>
                        <a class="view-songs" href="#" data-id="${album.id}" title="View Songs">${album.songs.length} song${album.songs.length !== 1 ? 's' : ''}</a>
                    </td>
                    <td class="status ${album.status.toLowerCase()}">${album.status.charAt(0).toUpperCase() + album.status.slice(1)}</td>
                    <td>
                        ${album.status === 'draft' || album.status === 'edited'
                ? `
                                <button class="publish" data-id="${album.id}" title="Publish">Publish</button>
                                <button class="edit" data-id="${album.id}" title="Edit">Edit</button>
                                <button class="delete" data-id="${album.id}" title="Delete">Delete</button>
                            `
                : album.status === 'accepted' || album.status === 'declined'
                    ? `
                                <button class="edit" data-id="${album.id}" title="Edit">Edit</button>
                                <button class="delete" data-id="${album.id}" title="Delete">Delete</button>
                            `
                    : 'None'
            }
                    </td>
                </tr>
            `).join('')
            : '<tr><td colspan="9"><span class="no-albums">No albums found.</span></td></tr>';

        if (filteredAlbums.length === 0 || totalPages <= 1) {
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
                fetchAlbums();
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
                fetchAlbums();
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
                fetchAlbums();
            });
            paginationDiv.appendChild(pageSpan);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) paginationDiv.appendChild(document.createTextNode(' ... '));
            const lastPage = document.createElement('span');
            lastPage.textContent = totalPages;
            lastPage.addEventListener('click', () => {
                currentPage = totalPages;
                fetchAlbums();
            });
            paginationDiv.appendChild(lastPage);
        }

        const nextButton = document.createElement('button');
        nextButton.textContent = 'Next';
        nextButton.disabled = currentPage === totalPages;
        nextButton.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                fetchAlbums();
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

    if (addAlbumBtn) {
        addAlbumBtn.addEventListener('click', () => {
            resetForm('add');
            if (addAlbumForm) addAlbumForm.classList.add('active');
        });
    }

    if (cancelAddAlbumBtn) {
        cancelAddAlbumBtn.addEventListener('click', () => {
            if (addAlbumForm) addAlbumForm.classList.remove('active');
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
            fetchAlbums();
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
            fetchAlbums();
        }, 300);
        rowsPerPageInput.addEventListener('input', updateRowsPerPage);
    }

    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => {
            searchQuery = searchInput.value.trim();
            currentPage = 1;
            fetchAlbums();
        }, 300));
    }

    if (albumForm) {
        albumForm.addEventListener('submit', async (e) => {
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
            let imageName = editAlbumId ? (albums.find(a => a.id === editAlbumId)?.imageUrl || '') : '';
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
                formData.append('albumName', title);
                formData.append('description', descriptionTextarea?.value.trim() || '');
                selectedSongs.forEach(songId => formData.append('songIds', songId));
                selectedArtists.forEach(artistId => formData.append('artistIds', artistId));
                if (image) formData.append('image', image);

                const saveButton = albumForm.querySelector('button[type="submit"]');
                if (saveButton) {
                    saveButton.disabled = true;
                    saveButton.classList.add('loading');
                    saveButton.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Saving...';
                }

                try {
                    if (editAlbumId) {
                        await updateAlbum(editAlbumId, formData);
                        showNotification(`Album "${title}" updated successfully.`);
                    } else {
                        await createAlbum(formData);
                        showNotification(`Album "${title}" created successfully.`);
                    }
                    if (addAlbumForm) addAlbumForm.classList.remove('active');
                    resetForm('add');
                    currentPage = 1;
                    await fetchAlbums();
                } catch (error) {
                    showNotification(`Failed to ${editAlbumId ? 'update' : 'save'} album: ${error.message}`, true);
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

    if (albumTableBody) {
        albumTableBody.addEventListener('click', async (e) => {
            const id = Number(e.target.dataset.id);
            if (!id) return;

            e.preventDefault();
            const album = albums.find(a => a.id === id);
            if (!album) {
                showNotification('Error: Album not found.', true);
                return;
            }

            if (e.target.classList.contains('view-songs') && contentModal && modalContentBody && modalTitle) {
                modalTitle.textContent = `Songs in ${album.albumName}`;
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
                            ${album.songs.length
                    ? album.songs.map(title => {
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
                modalTitle.textContent = `Description of ${album.albumName}`;
                modalContentBody.innerHTML = `
                    <textarea class="lyrics-content" readonly placeholder="No description available">${album.description || ''}</textarea>
                `;
                contentModal.style.display = 'flex';
            } else if (e.target.classList.contains('publish')) {
                showConfirmModal(
                    'Confirm Publish',
                    `Are you sure you want to publish "${album.albumName}"?`,
                    async () => {
                        try {
                            e.target.disabled = true;
                            e.target.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Publishing';
                            await publishAlbum(id);
                            showNotification(`Album "${album.albumName}" published and status changed to Pending.`);
                            await fetchAlbums();
                        } catch (error) {
                            showNotification(`Failed to publish album: ${error.message}`, true);
                        } finally {
                            e.target.disabled = false;
                            e.target.innerHTML = 'Publish';
                        }
                    }
                );
            } else if (e.target.classList.contains('edit')) {
                populateEditForm(album);
            } else if (e.target.classList.contains('delete')) {
                showConfirmModal(
                    'Confirm Delete',
                    `Are you sure you want to delete "${album.albumName}"?`,
                    async () => {
                        try {
                            e.target.disabled = true;
                            e.target.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Deleting';
                            await deleteAlbum(id);
                            showNotification(`Album "${album.albumName}" deleted successfully.`);
                            await fetchAlbums();
                        } catch (error) {
                            showNotification(`Failed to delete album: ${error.message}`, true);
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
    Promise.all([fetchSongs(), fetchArtists(), fetchAlbums()])
        .then(() => {
            if (albumForm) resetForm('add');
        })
        .catch(error => {
            console.error('Initialization error:', error);
            showNotification('Failed to initialize. Please try again.', true);
        });
});