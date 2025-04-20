document.addEventListener('DOMContentLoaded', () => {
    const songTableBody = document.getElementById('song-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const genreFilterSelect = document.getElementById('genre-filter');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.querySelector('.search input');
    const addSongBtn = document.getElementById('add-song-btn');
    const addSongForm = document.getElementById('add-song-form');
    const songForm = document.getElementById('song-form');
    const cancelAddSongBtn = document.getElementById('cancel-add-song');
    const titleInput = document.getElementById('song-title');
    const genreSelect = document.getElementById('song-genre');
    const durationInput = document.getElementById('song-duration');
    const lyricsTextarea = document.getElementById('song-lyrics');
    const descriptionTextarea = document.getElementById('song-description');
    const artistSearchInput1 = document.getElementById('artist-search-1');
    const artistSearchInput2 = document.getElementById('artist-search-2');
    const artistContainer = document.getElementById('song-artist');
    const additionalArtistsContainer = document.getElementById('song-additional_artists');
    const selectedArtistList = document.getElementById('selected-artist');
    const selectedArtistsList = document.getElementById('selected-artists-list');
    const artistError = document.getElementById('artist-error');
    const artistsError = document.getElementById('artists-error');
    const songFileInput = document.getElementById('song-file');
    const imageInput = document.getElementById('song-image');
    const imagePreview = document.getElementById('image-preview');
    const currentSongFileDiv = document.getElementById('current-song-file');
    const currentImageDiv = document.getElementById('current-image');
    const downloadPermissionSelect = document.getElementById('download-permission');
    const titleError = document.getElementById('title-error');
    const genreError = document.getElementById('genre-error');
    const durationError = document.getElementById('duration-error');
    const songFileError = document.getElementById('song-file-error');
    const imageError = document.getElementById('image-error');
    const downloadPermissionError = document.getElementById('download-permission-error');
    const lyricsModal = document.getElementById('lyrics-modal');
    const modalLyricsContent = document.getElementById('modal-lyrics-content');
    const closeModal = document.getElementById('close-modal');

    let songs = JSON.parse(localStorage.getItem('songs')) || [];
    let artists = JSON.parse(localStorage.getItem('artists')) || [
        {id: 'Taylor Swift', name: 'Taylor Swift'},
        {id: 'Ed Sheeran', name: 'Ed Sheeran'},
        {id: 'Tom Hiddenson', name: 'Tom Hiddenson'},
        {id: 'Jelly Roll', name: 'Jelly Roll'},
        {id: 'Alan Walker', name: 'Alan Walker'},
        {id: 'Adele', name: 'Adele'},
        {id: 'Bruno Mars', name: 'Bruno Mars'},
        {id: 'Billie Eilish', name: 'Billie Eilish'},
        {id: 'The Weeknd', name: 'The Weeknd'},
        {id: 'Shawn Mendes', name: 'Shawn Mendes'},
        {id: 'Olivia Rodrigo', name: 'Olivia Rodrigo'},
        {id: 'Ariana Grande', name: 'Ariana Grande'},
        {id: 'Drake', name: 'Drake'},
        {id: 'Post Malone', name: 'Post Malone'},
        {id: 'Beyoncé', name: 'Beyoncé'},
        {id: 'Harry Styles', name: 'Harry Styles'},
        {id: 'Doja Cat', name: 'Doja Cat'},
        {id: 'Justin Bieber', name: 'Justin Bieber'},
        {id: 'Dua Lipa', name: 'Dua Lipa'},
        {id: 'Sia', name: 'Sia'},
    ];

    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput.value) || 10;
    let currentFilterStatus = 'all';
    let currentFilterGenre = 'all';
    let currentSort = 'title-asc';
    let searchQuery = '';
    let editIndex = null;
    let selectedArtist = null;
    let selectedAdditionalArtists = [];

    const generateUUID = () => {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    };

    const generateRandomListeners = () => {
        return Math.floor(Math.random() * 1001);
    };

    const parseDate = (dateStr) => {
        if (!dateStr || !/^\d{2}\/\d{2}\/\d{4}$/.test(dateStr)) return new Date(0);
        const [day, month, year] = dateStr.split('/').map(Number);
        return new Date(year, month - 1, day);
    };

    const validateDuration = (duration) => {
        return /^([0-5]?\d):([0-5]\d)$/.test(duration);
    };

    const updateSelectedArtistDisplay = (container, selectedId, listElement) => {
        const artist = artists.find(a => a.id === selectedId);
        const name = artist ? artist.name : 'None';
        listElement.innerHTML = selectedId
            ? `<li data-artist-id="${selectedId}">${name}<span class="remove-artist" data-artist-id="${selectedId}">×</span></li>`
            : '<li>No artist selected</li>';

        listElement.querySelectorAll('.remove-artist').forEach(button => {
            button.addEventListener('click', () => {
                if (container === artistContainer) {
                    selectedArtist = null;
                } else {
                    selectedAdditionalArtists = selectedAdditionalArtists.filter(id => id !== button.dataset.artistId);
                }
                updateSelectedArtistDisplay(container, selectedArtist, selectedArtistList);
                populateArtists(container, container === artistContainer ? selectedArtist : selectedAdditionalArtists);
            });
        });
    };

    const updateSelectedAdditionalArtistsDisplay = () => {
        const selectedNames = selectedAdditionalArtists.map(id => {
            const artist = artists.find(a => a.id === id);
            return artist ? artist.name : 'Unknown';
        });
        selectedArtistsList.innerHTML = selectedNames.length > 0
            ? selectedNames.map((name, index) => `
                <li data-artist-id="${selectedAdditionalArtists[index]}">
                    ${name}
                    <span class="remove-artist" data-artist-id="${selectedAdditionalArtists[index]}">×</span>
                </li>
            `).join('')
            : '<li>No additional artists selected</li>';

        selectedArtistsList.querySelectorAll('.remove-artist').forEach(button => {
            button.addEventListener('click', () => {
                const artistId = button.dataset.artistId;
                selectedAdditionalArtists = selectedAdditionalArtists.filter(id => id !== artistId);
                updateSelectedAdditionalArtistsDisplay();
                populateArtists(additionalArtistsContainer, selectedAdditionalArtists);
            });
        });
    };

    const populateArtists = (container, preSelectedArtists = []) => {
        const isPrimaryArtist = container === artistContainer;
        const searchInput = isPrimaryArtist ? artistSearchInput1 : artistSearchInput2;
        const selectedArtists = isPrimaryArtist ? (preSelectedArtists ? [preSelectedArtists] : [selectedArtist]) : preSelectedArtists;

        const query = searchInput.value.trim().toLowerCase();
        let filteredArtists = query
            ? artists.filter(artist => artist.name.toLowerCase().includes(query))
            : artists;

        // Exclude the selected primary artist from an additional artists list
        if (!isPrimaryArtist && selectedArtist) {
            filteredArtists = filteredArtists.filter(artist => artist.id !== selectedArtist);
        }

        let html = '';
        for (let i = 0; i < filteredArtists.length; i += 2) {
            const artist1 = filteredArtists[i];
            const artist2 = filteredArtists[i + 1];

            html += `<div class="artist-row">`;
            [artist1, artist2].forEach(artist => {
                if (artist) {
                    const isSelected = isPrimaryArtist
                        ? selectedArtist === artist.id
                        : selectedArtists.includes(artist.id);
                    html += `
                        <span class="artist-item ${isSelected ? 'selected' : ''}" data-artist-id="${artist.id}">${artist.name}</span>
                    `;
                } else {
                    html += `<span class="artist-item" style="visibility: hidden; flex: 1;"></span>`;
                }
            });
            html += `</div>`;
        }

        container.innerHTML = html;

        container.querySelectorAll('.artist-item[data-artist-id]').forEach(item => {
            item.addEventListener('dblclick', () => {
                const artistId = item.dataset.artistId;
                if (isPrimaryArtist) {
                    selectedArtist = selectedArtist === artistId ? null : artistId;
                    updateSelectedArtistDisplay(container, selectedArtist, selectedArtistList);
                    // Refresh additional artists to exclude the newly selected primary artist
                    populateArtists(additionalArtistsContainer, selectedAdditionalArtists);
                } else {
                    if (selectedAdditionalArtists.includes(artistId)) {
                        selectedAdditionalArtists = selectedAdditionalArtists.filter(id => id !== artistId);
                    } else {
                        selectedAdditionalArtists.push(artistId);
                    }
                    updateSelectedAdditionalArtistsDisplay();
                }
                populateArtists(container, isPrimaryArtist ? selectedArtist : selectedAdditionalArtists);
            });
        });

        if (isPrimaryArtist) {
            updateSelectedArtistDisplay(container, selectedArtist, selectedArtistList);
        } else {
            updateSelectedAdditionalArtistsDisplay();
        }
    };

    const filterArtists = (container, searchInput) => {
        const isPrimaryArtist = container === artistContainer;
        populateArtists(container, isPrimaryArtist ? selectedArtist : selectedAdditionalArtists);
    };

    const resetForm = (mode = 'add') => {
        songForm.dataset.mode = mode;
        titleInput.value = '';
        genreSelect.value = 'Pop';
        durationInput.value = '';
        lyricsTextarea.value = '';
        descriptionTextarea.value = '';
        artistSearchInput1.value = '';
        artistSearchInput2.value = '';
        selectedArtist = null;
        selectedAdditionalArtists = [];
        artistContainer.innerHTML = '';
        additionalArtistsContainer.innerHTML = '';
        songFileInput.value = '';
        imageInput.value = '';
        downloadPermissionSelect.value = '';
        imagePreview.style.display = 'none';
        imagePreview.src = '';
        currentSongFileDiv.textContent = '';
        currentImageDiv.textContent = '';
        titleError.style.display = 'none';
        genreError.style.display = 'none';
        durationError.style.display = 'none';
        artistError.style.display = 'none';
        artistsError.style.display = 'none';
        songFileError.style.display = 'none';
        imageError.style.display = 'none';
        downloadPermissionError.style.display = 'none';
        songFileInput.required = true;
        imageInput.required = true;
        downloadPermissionSelect.required = true;
        populateArtists(artistContainer, null);
        populateArtists(additionalArtistsContainer, []);
        editIndex = null;
    };

    const populateEditForm = (index) => {
        const song = songs[index];
        if (!song) {
            console.error('Song not found at index:', index);
            alert('Error: Song not found.');
            return;
        }

        try {
            songForm.dataset.mode = 'edit';
            titleInput.value = song.title || '';
            genreSelect.value = song.genre || 'Pop';
            durationInput.value = song.duration || '';
            lyricsTextarea.value = song.lyrics || '';
            descriptionTextarea.value = song.description || '';
            artistSearchInput1.value = '';
            artistSearchInput2.value = '';
            selectedArtist = song.artist || null;
            selectedAdditionalArtists = song.additionalArtists || [];
            populateArtists(artistContainer, selectedArtist);
            populateArtists(additionalArtistsContainer, selectedAdditionalArtists);
            downloadPermissionSelect.value = song.downloadPermission || 'No';
            songFileInput.value = '';
            imageInput.value = '';
            imagePreview.style.display = 'none';
            imagePreview.src = '';
            currentSongFileDiv.textContent = song.songFileName ? `Current file: ${song.songFileName}` : '';
            currentImageDiv.textContent = song.imageName ? `Current image: ${song.imageName}` : '';
            songFileInput.required = false;
            imageInput.required = false;
            downloadPermissionSelect.required = true;
            titleError.style.display = 'none';
            genreError.style.display = 'none';
            durationError.style.display = 'none';
            artistError.style.display = 'none';
            artistsError.style.display = 'none';
            songFileError.style.display = 'none';
            imageError.style.display = 'none';
            downloadPermissionError.style.display = 'none';
            editIndex = index;

            addSongForm.classList.add('active');
        } catch (error) {
            console.error('Error in populateEditForm:', error);
            alert('Error: Failed to load edit form.');
        }
    };

    imageInput.addEventListener('change', () => {
        const file = imageInput.files[0];
        if (file && ['image/jpeg', 'image/png'].includes(file.type)) {
            imagePreview.src = URL.createObjectURL(file);
            imagePreview.style.display = 'block';
            imageError.style.display = 'none';
        } else {
            imagePreview.style.display = 'none';
            imagePreview.src = '';
            if (file) {
                imageError.textContent = 'Only JPG or PNG files are allowed';
                imageError.style.display = 'block';
            }
        }
    });

    artistSearchInput1.addEventListener('input', () => filterArtists(artistContainer, artistSearchInput1));
    artistSearchInput2.addEventListener('input', () => filterArtists(additionalArtistsContainer, artistSearchInput2));

    addSongBtn.addEventListener('click', () => {
        resetForm('add');
        addSongForm.classList.add('active');
    });

    cancelAddSongBtn.addEventListener('click', () => {
        addSongForm.classList.remove('active');
        resetForm('add');
    });

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

    const renderTable = () => {
        let filteredSongs = [...songs];

        // Apply status filter
        if (currentFilterStatus !== 'all') {
            filteredSongs = filteredSongs.filter(song => song.status.toLowerCase() === currentFilterStatus.toLowerCase());
        }

        // Apply genre filter
        if (currentFilterGenre !== 'all') {
            filteredSongs = filteredSongs.filter(song => song.genre === currentFilterGenre);
        }

        // Apply search on song title
        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            filteredSongs = filteredSongs.filter(song => song.title.toLowerCase().includes(query));
        }

        // Sort filtered list
        filteredSongs = filteredSongs.sort((a, b) => {
            if (currentSort === 'title-asc') {
                return a.title.localeCompare(b.title);
            } else if (currentSort === 'title-desc') {
                return b.title.localeCompare(a.title);
            } else if (currentSort === 'date-asc') {
                return parseDate(a.uploadDate) - parseDate(b.uploadDate);
            } else if (currentSort === 'date-desc') {
                return parseDate(b.uploadDate) - parseDate(a.uploadDate);
            } else if (currentSort === 'listeners-asc') {
                return (a.listeners || 0) - (b.listeners || 0);
            } else if (currentSort === 'listeners-desc') {
                return (b.listeners || 0) - (a.listeners || 0);
            }
            return 0;
        });

        // Calculate pagination
        const totalPages = Math.ceil(filteredSongs.length / rowsPerPage);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedSongs = filteredSongs.slice(start, end);

        // Render a song list
        songTableBody.innerHTML = paginatedSongs.length > 0
            ? paginatedSongs.map((song) => {
                const globalIndex = songs.findIndex(s => s.id === song.id);
                if (globalIndex === -1) return '';
                const artistName = song.artist
                    ? artists.find(a => a.id === song.artist)?.name || 'Unknown'
                    : 'None';
                const additionalArtistNames = song.additionalArtists
                    ? song.additionalArtists
                        .map(id => artists.find(a => a.id === id)?.name || 'Unknown')
                        .join(', ')
                    : 'None';
                const listeners = (song.status === 'draft' || song.status === 'declined' || song.status === 'pending') ? 0 : (song.listeners || generateRandomListeners());
                return `
                    <tr>
                        <td class="image">
                            ${song.imageName
                    ? `<span>${song.imageName}</span>`
                    : `<span>No image</span>`
                }
                        </td>
                        <td>${song.title || 'Unknown'}</td>
                        <td>${song.genre || 'Unknown'}</td>
                        <td>${song.duration || '0:00'}</td>
                        <td>${song.uploadDate || 'Unknown'}</td>
                        <td>${listeners}</td>
                        <td>
                            ${song.lyrics
                    ? `<a class="view-lyrics" href="#" data-index="${globalIndex}" title="View Lyrics">Show more...</a>`
                    : 'None'
                }
                        </td>
                        <td>
                            ${song.description
                    ? `<a class="view-description" href="#" data-index="${globalIndex}" title="View Description">Show more...</a>`
                    : 'None'
                }
                        </td>
                        <td>${song.downloadPermission || 'No'}</td>
                        <td>${artistName}</td>
                        <td>${additionalArtistNames}</td>
                        <td class="status ${song.status.toLowerCase()}">${song.status.charAt(0).toUpperCase() + song.status.slice(1)}</td>
                        <td>
                            ${song.status.toLowerCase() === 'accepted' || song.status.toLowerCase() === 'declined'
                    ? `
                                    <button class="edit" data-index="${globalIndex}" title="Edit">Edit</button>
                                    <button class="delete" data-index="${globalIndex}" title="Delete">Delete</button>
                                `
                    : `
                                    <button class="accept" data-index="${globalIndex}" title="Accept">Accept</button>
                                    <button class="edit" data-index="${globalIndex}" title="Edit">Edit</button>
                                    <button class="decline" data-index="${globalIndex}" title="Decline">Decline</button>
                                    <button class="delete" data-index="${globalIndex}" title="Delete">Delete</button>
                                `
                }
                        </td>
                    </tr>
                `;
            }).join('')
            : '<tr><td colspan="13"><span class="no-songs" style="">No songs found.</span></td></tr>';

        // Update pagination
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

    filterStatusSelect.addEventListener('change', () => {
        currentFilterStatus = filterStatusSelect.value;
        currentPage = 1;
        renderTable();
    });

    genreFilterSelect.addEventListener('change', () => {
        currentFilterGenre = genreFilterSelect.value;
        currentPage = 1;
        renderTable();
    });

    sortBySelect.addEventListener('change', () => {
        currentSort = sortBySelect.value;
        currentPage = 1;
        renderTable();
    });

    rowsPerPageInput.addEventListener('change', updateRowsPerPage);
    rowsPerPageInput.addEventListener('input', updateRowsPerPage);

    searchInput.addEventListener('input', () => {
        searchQuery = searchInput.value.trim();
        currentPage = 1;
        renderTable();
    });

    songForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        let isValid = true;

        titleError.style.display = 'none';
        genreError.style.display = 'none';
        durationError.style.display = 'none';
        artistError.style.display = 'none';
        artistsError.style.display = 'none';
        songFileError.style.display = 'none';
        imageError.style.display = 'none';
        downloadPermissionError.style.display = 'none';

        const title = titleInput.value.trim();
        if (!title) {
            titleError.style.display = 'block';
            isValid = false;
        }

        const genre = genreSelect.value;
        if (!genre) {
            genreError.style.display = 'block';
            isValid = false;
        }

        const duration = durationInput.value.trim();
        if (!validateDuration(duration)) {
            durationError.style.display = 'block';
            isValid = false;
        }

        if (!selectedArtist) {
            artistError.style.display = 'block';
            isValid = false;
        }

        let songFileName = editIndex !== null ? songs[editIndex].songFileName : '';
        const songFile = songFileInput.files[0];
        if (songFile) {
            if (songFile.type !== 'audio/mpeg') {
                songFileError.textContent = 'Only MP3 files are allowed';
                songFileError.style.display = 'block';
                isValid = false;
            } else if (songFile.size > 10 * 1024 * 1024) {
                songFileError.textContent = 'Song file size exceeds 10MB';
                songFileError.style.display = 'block';
                isValid = false;
            } else {
                songFileName = songFile.name;
            }
        } else if (editIndex === null) {
            songFileError.textContent = 'Please select an MP3 file';
            songFileError.style.display = 'block';
            isValid = false;
        }

        let imageName = editIndex !== null ? songs[editIndex].imageName : '';
        const image = imageInput.files[0];
        if (image) {
            if (!['image/jpeg', 'image/png'].includes(image.type)) {
                imageError.textContent = 'Only JPG or PNG files are allowed';
                imageError.style.display = 'block';
                isValid = false;
            } else if (image.size > 5 * 1024 * 1024) {
                imageError.textContent = 'Image file size exceeds 5MB';
                imageError.style.display = 'block';
                isValid = false;
            } else {
                imageName = image.name;
            }
        } else if (editIndex === null) {
            imageError.textContent = 'Please select an image file';
            imageError.style.display = 'block';
            isValid = false;
        }

        const downloadPermission = downloadPermissionSelect.value;
        if (!downloadPermission) {
            downloadPermissionError.style.display = 'block';
            isValid = false;
        }

        if (isValid) {
            const status = editIndex !== null ? songs[editIndex].status : 'draft';
            const listeners = (status === 'draft' || status === 'declined' || status === 'pending') ? 0 : generateRandomListeners();

            const songData = {
                id: editIndex !== null ? songs[editIndex].id : generateUUID(),
                title,
                genre,
                duration,
                lyrics: lyricsTextarea.value.trim() || '',
                description: descriptionTextarea.value.trim() || '',
                artist: selectedArtist,
                additionalArtists: selectedAdditionalArtists,
                songFileName,
                imageName,
                downloadPermission,
                status,
                uploadDate: editIndex !== null ? songs[editIndex].uploadDate : new Date().toLocaleDateString('en-GB'),
                listeners
            };

            try {
                if (editIndex !== null) {
                    songs[editIndex] = songData;
                    alert(`Song "${title}" updated successfully. Note: Song and image files are not stored locally; a backend is required for file storage.`);
                } else {
                    songs.push(songData);
                    // Sort songs to ensure new song appears in the correct position
                    songs.sort((a, b) => {
                        if (currentSort === 'title-asc') {
                            return a.title.localeCompare(b.title);
                        } else if (currentSort === 'title-desc') {
                            return b.title.localeCompare(a.title);
                        } else if (currentSort === 'date-asc') {
                            return parseDate(a.uploadDate) - parseDate(b.uploadDate);
                        } else if (currentSort === 'date-desc') {
                            return parseDate(b.uploadDate) - parseDate(a.uploadDate);
                        } else if (currentSort === 'listeners-asc') {
                            return (a.listeners || 0) - (b.listeners || 0);
                        } else if (currentSort === 'listeners-desc') {
                            return (b.listeners || 0) - (a.listeners || 0);
                        }
                        return 0;
                    });
                    // Find the index of the new song after sorting
                    const newSongIndex = songs.findIndex(s => s.id === songData.id);
                    // Calculate the page where the new song should appear
                    let filteredSongs = [...songs];
                    if (currentFilterStatus !== 'all') {
                        filteredSongs = filteredSongs.filter(song => song.status.toLowerCase() === currentFilterStatus.toLowerCase());
                    }
                    if (currentFilterGenre !== 'all') {
                        filteredSongs = filteredSongs.filter(song => song.genre === currentFilterGenre);
                    }
                    if (searchQuery) {
                        const query = searchQuery.toLowerCase();
                        filteredSongs = filteredSongs.filter(song => song.title.toLowerCase().includes(query));
                    }
                    const newSongFilteredIndex = filteredSongs.findIndex(s => s.id === songData.id);
                    if (newSongFilteredIndex !== -1) {
                        // If the new song matches filters, show it on the correct page
                        currentPage = Math.floor(newSongFilteredIndex / rowsPerPage) + 1;
                    } else {
                        // If the new song doesn't match current filters, reset to page 1 and clear filters
                        currentFilterStatus = 'all';
                        currentFilterGenre = 'all';
                        searchQuery = '';
                        filterStatusSelect.value = 'all';
                        genreFilterSelect.value = 'all';
                        searchInput.value = '';
                        currentPage = Math.floor(newSongIndex / rowsPerPage) + 1;
                    }
                    alert(`Song "${title}" added successfully. Note: Song and image files are not stored locally; a backend is required for file storage.`);
                }
                localStorage.setItem('songs', JSON.stringify(songs));
                addSongForm.classList.remove('active');
                resetForm('add');
                renderTable();
            } catch (e) {
                alert(`Failed to ${editIndex !== null ? 'update' : 'save'} song: Storage quota exceeded.`);
            }
        }
    });

    songTableBody.addEventListener('click', (e) => {
        const target = e.target;
        const index = target.dataset.index;

        if (!index || !target.matches('.accept, .edit, .decline, .delete, .view-lyrics, .view-description')) {
            return;
        }

        e.preventDefault();

        const globalIndex = parseInt(index, 10);
        if (isNaN(globalIndex) || globalIndex < 0 || globalIndex >= songs.length) {
            console.error('Invalid song index:', index);
            alert('Error: Invalid song index.');
            return;
        }

        const song = songs[globalIndex];
        if (target.matches('.view-lyrics')) {
            document.getElementById('modal-title').textContent = 'Song Lyrics';
            modalLyricsContent.value = song.lyrics || '';
            modalLyricsContent.placeholder = song.lyrics ? '' : 'No lyrics available';
            lyricsModal.style.display = 'flex';
        } else if (target.matches('.view-description')) {
            document.getElementById('modal-title').textContent = 'Song Description';
            modalLyricsContent.value = song.description || '';
            modalLyricsContent.placeholder = song.description ? '' : 'No description available';
            lyricsModal.style.display = 'flex';
        } else if (target.matches('.accept')) {
            try {
                songs[globalIndex].status = 'accepted';
                songs[globalIndex].listeners = generateRandomListeners();
                localStorage.setItem('songs', JSON.stringify(songs));
                renderTable();
                alert(`Song "${songs[globalIndex].title}" accepted.`);
            } catch (e) {
                alert('Failed to accept song: Storage quota exceeded.');
            }
        } else if (target.matches('.decline')) {
            try {
                songs[globalIndex].status = 'declined';
                songs[globalIndex].listeners = 0; // Reset listeners for declined songs
                localStorage.setItem('songs', JSON.stringify(songs));
                renderTable();
                alert(`Song "${songs[globalIndex].title}" declined.`);
            } catch (e) {
                alert('Failed to decline song: Storage quota exceeded.');
            }
        } else if (target.matches('.edit')) {
            populateEditForm(globalIndex);
        } else if (target.matches('.delete')) {
            if (confirm(`Are you sure you want to delete "${songs[globalIndex].title}"?`)) {
                try {
                    const deletedTitle = songs[globalIndex].title;
                    songs.splice(globalIndex, 1);
                    localStorage.setItem('songs', JSON.stringify(songs));
                    renderTable();
                    alert(`Song "${deletedTitle}" deleted successfully.`);
                } catch (e) {
                    alert('Failed to delete song: Storage quota exceeded.');
                }
            }
        }
    });

    closeModal.addEventListener('click', () => {
        lyricsModal.style.display = 'none';
        modalLyricsContent.value = '';
        modalLyricsContent.placeholder = 'No content available';
    });

    window.addEventListener('click', (e) => {
        if (e.target === lyricsModal) {
            lyricsModal.style.display = 'none';
            modalLyricsContent.value = '';
            modalLyricsContent.placeholder = 'No content available';
        }
    });

    resetForm('add');
    renderTable();
});