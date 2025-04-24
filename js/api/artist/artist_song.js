import { fetchWithRefresh } from '/js/api/refresh.js';

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const songTableBody = document.getElementById('song-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const genreFilterSelect = document.getElementById('genre-filter');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const addSongBtn = document.getElementById('add-song-btn');
    const addSongForm = document.getElementById('add-song-form');
    const songForm = document.getElementById('song-form');
    const formTitle = document.getElementById('form-title');
    const cancelAddSongBtn = document.getElementById('cancel-add-song');
    const titleInput = document.getElementById('song-title');
    const genreSelect = document.getElementById('song-genre');
    const lyricsTextarea = document.getElementById('song-lyrics');
    const descriptionTextarea = document.getElementById('song-description');
    const artistSearchInput = document.getElementById('artist-search');
    const additionalArtistsContainer = document.getElementById('song-additional_artists');
    const selectedArtistsList = document.getElementById('selected-artists-list');
    const artistsError = document.getElementById('artists-error');
    const songFileInput = document.getElementById('song-file');
    const imageInput = document.getElementById('song-image');
    const imagePreview = document.getElementById('image-preview');
    const currentSongFileDiv = document.getElementById('current-song-file');
    const currentImageDiv = document.getElementById('current-image');
    const downloadPermissionSelect = document.getElementById('download-permission');
    const titleError = document.getElementById('title-error');
    const songFileError = document.getElementById('song-file-error');
    const imageError = document.getElementById('image-error');
    const downloadPermissionError = document.getElementById('download-permission-error');
    const lyricsError = document.getElementById('lyrics-error');
    const lyricsModal = document.getElementById('lyrics-modal');
    const modalLyricsContent = document.getElementById('modal-lyrics-content');
    const closeModal = document.getElementById('close-modal');
    const modalTitle = document.getElementById('modal-title');
    const songFileNote = document.getElementById('song-file-note');

    // State
    let songs = [];
    let artists = [];
    let genres = [];
    let selectedArtists = [];
    let editSongId = null;
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput?.value) || 10;
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

    // Utility Function to Count Words
    const countWords = (text) => {
        return text.trim().split(/\s+/).filter(word => word.length > 0).length;
    };

    // Utility Function to Map Sort Option to API Parameters
    const mapSortToApi = (sort) => {
        switch (sort) {
            case 'title-asc':
                return { orderBy: 'title', order: 'asc' };
            case 'title-desc':
                return { orderBy: 'title', order: 'desc' };
            case 'date-asc':
                return { orderBy: 'releaseDate', order: 'asc' };
            case 'date-desc':
                return { orderBy: 'releaseDate', order: 'desc' };
            default:
                return { orderBy: 'title', order: 'asc' };
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
            if (genreSelect) {
                genreSelect.innerHTML = '<option value="">No Genre</option>' +
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
            if (paginationDiv) {
                paginationDiv.innerHTML = '';
            }
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                localStorage.clear();
                window.location.href = '../auth/login_register.html';
            }
        }
    };

    const fetchArtists = async () => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/otherArtists', {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) throw new Error(`Failed to fetch artists: ${response.status}`);

            const data = await response.json();
            console.log('Fetched artists:', data);

            artists = data.map(artist => ({
                id: artist.id,
                name: `${artist.artistName}`.trim() || 'Unknown'
            }));
            if (additionalArtistsContainer && selectedArtistsList) {
                populateArtists(selectedArtists);
            }
        } catch (error) {
            console.error('Error fetching artists:', error);
            if (additionalArtistsContainer) {
                additionalArtistsContainer.innerHTML = '<div class="no-artists-text">Failed to load artists.</div>';
            }
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                localStorage.clear();
                window.location.href = '../auth/login_register.html';
            }
        }
    };

    const createSong = async (formData) => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/artist/song/createDraft', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) throw new Error(`Failed to create song: ${response.status}`);

            const data = await response.json();
            console.log('Created song:', data);
            return data;
        } catch (error) {
            throw new Error(`Failed to create song: ${error.message}`);
        }
    };

    const updateSong = async (id, formData) => {
        try {
            const response = await fetchWithRefresh(`http://localhost:8080/api/v1/artist/song/update/${id}`, {
                method: 'PUT',
                body: formData
            });

            if (!response.ok) throw new Error(`Failed to update song: ${response.status}`);

            const data = await response.json();
            console.log('Updated song:', data);
            return data;
        } catch (error) {
            throw new Error(`Failed to update song: ${error.message}`);
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
        if (!songForm || !formTitle || !titleInput || !genreSelect || !lyricsTextarea || !descriptionTextarea ||
            !artistSearchInput || !songFileInput || !imageInput || !downloadPermissionSelect ||
            !imagePreview || !currentSongFileDiv || !currentImageDiv || !songFileNote) return;

        songForm.dataset.mode = mode;
        formTitle.textContent = mode === 'add' ? 'Add Song' : 'Edit Song';
        titleInput.value = '';
        genreSelect.value = '';
        lyricsTextarea.value = '';
        descriptionTextarea.value = '';
        artistSearchInput.value = '';
        selectedArtists = [];
        songFileInput.value = '';
        imageInput.value = '';
        downloadPermissionSelect.value = '';
        imagePreview.style.display = 'none';
        imagePreview.src = '';
        currentSongFileDiv.textContent = '';
        currentImageDiv.textContent = '';
        if (titleError) titleError.style.display = 'none';
        if (songFileError) songFileError.style.display = 'none';
        if (imageError) imageError.style.display = 'none';
        if (artistsError) artistsError.style.display = 'none';
        if (downloadPermissionError) downloadPermissionError.style.display = 'none';
        if (lyricsError) lyricsError.style.display = 'none';
        songFileInput.required = mode === 'add';
        songFileInput.disabled = mode !== 'add';
        songFileNote.style.display = mode === 'add' ? 'none' : 'block';
        imageInput.required = false;
        downloadPermissionSelect.required = false;
        fetchArtists();
    };

    const populateEditForm = (song) => {
        if (!songForm || !addSongForm) return;
        songForm.dataset.mode = 'edit';
        formTitle.textContent = 'Edit Song';
        titleInput.value = song.title || '';
        genreSelect.value = genres.find(g => g.name === song.genre)?.id || '';
        lyricsTextarea.value = song.lyrics || '';
        descriptionTextarea.value = song.description || '';
        artistSearchInput.value = '';
        selectedArtists = (song.additionalArtistNameList || []).map(name => {
            const artist = artists.find(a => a.name === name);
            return artist ? artist.id : null;
        }).filter(id => id !== null);

        downloadPermissionSelect.value = song.downloadPermission === 'Yes' ? 'Yes' : song.downloadPermission === 'No' ? 'No' : '';
        songFileInput.value = '';
        songFileInput.disabled = true;
        songFileInput.required = false;
        songFileNote.style.display = 'block';
        imageInput.value = '';
        imagePreview.style.display = 'none';
        imagePreview.src = '';
        currentSongFileDiv.textContent = song.songFileName ? `Current file: ${song.songFileName}` : '';
        currentImageDiv.textContent = song.imageName ? `Current image: ${song.imageName}` : '';
        if (titleError) titleError.style.display = 'none';
        if (songFileError) songFileError.style.display = 'none';
        if (imageError) imageError.style.display = 'none';
        if (artistsError) artistsError.style.display = 'none';
        if (downloadPermissionError) downloadPermissionError.style.display = 'none';
        if (lyricsError) lyricsError.style.display = 'none';
        editSongId = song.id;
        addSongForm.classList.add('active');
        fetchArtists();
    };

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

        // Hide pagination if no songs are found or only one page
        if (filteredSongs.length === 0 || totalPages <= 1) {
            paginationDiv.innerHTML = '';
            return;
        }

        // Render pagination
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

    if (artistSearchInput) {
        artistSearchInput.addEventListener('input', () => {
            populateArtists(selectedArtists);
        });
    }

    if (addSongBtn) {
        addSongBtn.addEventListener('click', () => {
            resetForm('add');
            if (addSongForm) addSongForm.classList.add('active');
        });
    }

    if (cancelAddSongBtn) {
        cancelAddSongBtn.addEventListener('click', () => {
            if (addSongForm) addSongForm.classList.remove('active');
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
            fetchSongs();
        }, 300);
        rowsPerPageInput.addEventListener('input', updateRowsPerPage);
    }

    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => {
            searchQuery = searchInput.value.trim();
            currentPage = 1;
            fetchSongs();
        }, 300));
    }

    if (songForm) {
        songForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            let isValid = true;

            if (titleError) titleError.style.display = 'none';
            if (songFileError) songFileError.style.display = 'none';
            if (imageError) imageError.style.display = 'none';
            if (artistsError) artistsError.style.display = 'none';
            if (downloadPermissionError) downloadPermissionError.style.display = 'none';
            if (lyricsError) lyricsError.style.display = 'none';

            const title = titleInput?.value.trim() || '';
            if (!title) {
                if (titleError) {
                    titleError.textContent = 'Please enter a valid title';
                    titleError.style.display = 'block';
                }
                isValid = false;
            }

            const genreId = genreSelect?.value || '';
            const lyrics = lyricsTextarea?.value.trim() || '';
            if (lyrics && countWords(lyrics) <= 100) {
                if (lyricsError) {
                    lyricsError.textContent = 'Lyrics must be more than 100 words if provided';
                    lyricsError.style.display = 'block';
                }
                isValid = false;
            }

            const description = descriptionTextarea?.value.trim() || '';

            const songFile = songFileInput?.files[0];
            let songFileName = editSongId ? (songs.find(s => s.id === editSongId)?.songFileName || '') : '';
            if (songFile) {
                if (songFile.type !== 'audio/mpeg') {
                    if (songFileError) {
                        songFileError.textContent = 'Only MP3 files are allowed';
                        songFileError.style.display = 'block';
                    }
                    isValid = false;
                } else if (songFile.size > 10 * 1024 * 1024) {
                    if (songFileError) {
                        songFileError.textContent = 'Song file size exceeds 10MB';
                        songFileError.style.display = 'block';
                    }
                    isValid = false;
                } else {
                    songFileName = songFile.name;
                }
            } else if (!editSongId) {
                if (songFileError) {
                    songFileError.textContent = 'Please select an MP3 file';
                    songFileError.style.display = 'block';
                }
                isValid = false;
            }

            const image = imageInput?.files[0];
            let imageName = editSongId ? (songs.find(s => s.id === editSongId)?.imageName || '') : '';
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

            const downloadPermission = downloadPermissionSelect?.value || '';

            if (isValid) {
                const formData = new FormData();
                formData.append('title', title);
                if (genreId) formData.append('genreId', genreId);
                formData.append('lyrics', lyrics);
                formData.append('description', description);
                selectedArtists.forEach(artistId => formData.append('additionalArtistIds', artistId));
                if (songFile) formData.append('file', songFile);
                if (image) formData.append('image', image);
                if (downloadPermission) formData.append('downloadPermission', downloadPermission === 'Yes');

                try {
                    if (editSongId) {
                        await updateSong(editSongId, formData);
                        alert(`Song "${title}" updated successfully.`);
                    } else {
                        await createSong(formData);
                        alert(`Song "${title}" created successfully.`);
                    }
                    if (addSongForm) addSongForm.classList.remove('active');
                    resetForm('add');
                    await fetchSongs();
                } catch (error) {
                    alert(`Failed to ${editSongId ? 'update' : 'save'} song: ${error.message}`);
                }
            }
        });
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
                } catch (error) {
                    alert(`Failed to publish song: ${error.message}`);
                }
            } else if (e.target.classList.contains('edit')) {
                populateEditForm(song);
            } else if (e.target.classList.contains('delete')) {
                if (confirm(`Are you sure you want to delete "${song.title}"?`)) {
                    try {
                        await deleteSong(id);
                        alert(`Song "${song.title}" deleted successfully.`);
                        await fetchSongs();
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

    // Initialize
    Promise.all([fetchGenres(), fetchSongs(), fetchArtists()])
        .then(() => {
            if (songForm) resetForm('add');
        })
        .catch(error => {
            console.error('Initialization error:', error);
            if (error.message.includes('No tokens') || error.message.includes('Invalid refresh token') || error.message.includes('Invalid access token')) {
                localStorage.clear();
                window.location.href = '../auth/login_register.html';
            }
        });
});