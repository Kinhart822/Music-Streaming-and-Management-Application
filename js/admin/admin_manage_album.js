document.addEventListener('DOMContentLoaded', () => {
    const albumTableBody = document.getElementById('album-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.querySelector('.search input');
    const addAlbumBtn = document.getElementById('add-album-btn');
    const addAlbumForm = document.getElementById('add-album-form');
    const albumForm = document.getElementById('album-form');
    const formTitle = document.getElementById('form-title');
    const cancelAddAlbumBtn = document.getElementById('cancel-add-album');
    const titleInput = document.getElementById('album-title');
    const descriptionTextarea = document.getElementById('album-description');
    const songsContainer = document.getElementById('album-songs');
    const selectedSongsList = document.getElementById('selected-songs-list');
    const artistSearchInput = document.getElementById('artists-search');
    const additionalArtistsContainer = document.getElementById('song-artists');
    const selectedArtistsList = document.getElementById('selected-artists-list');
    const imageInput = document.getElementById('album-image');
    const currentImageDiv = document.getElementById('current-image');
    const titleError = document.getElementById('title-error');
    const songsError = document.getElementById('songs-error');
    const artistsError = document.getElementById('artists-error');
    const imageError = document.getElementById('image-error');
    const songSearchInput = document.getElementById('song-search');
    const contentModal = document.getElementById('content-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalContentBody = document.getElementById('modal-content-body');
    const closeModal = document.getElementById('close-modal');

    let albums = JSON.parse(localStorage.getItem('albums')) || [];
    let songs = JSON.parse(localStorage.getItem('songs')) || [
        {
            id: 'song1',
            title: 'Blank Space',
            artist: 'Taylor Swift',
            genre: 'Pop',
            duration: '3:55',
            status: 'accepted',
            songFileName: 'blank_space.mp3',
            imageName: 'blank_space.jpg',
            downloadPermission: 'Yes',
            uploadDate: '01/01/2025',
            listeners: 500,
            lyrics: '',
            description: '',
            additionalArtists: []
        },
        {
            id: 'song2',
            title: 'Shape of You',
            artist: 'Ed Sheeran',
            genre: 'Pop',
            duration: '4:20',
            status: 'accepted',
            songFileName: 'shape_of_you.mp3',
            imageName: 'shape_of_you.jpg',
            downloadPermission: 'No',
            uploadDate: '02/01/2025',
            listeners: 600,
            lyrics: '',
            description: '',
            additionalArtists: []
        }
    ];
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
    let currentSort = 'title-asc';
    let searchQuery = '';
    let songSearchQuery = '';
    let editIndex = null;
    let selectedSongs = [];
    let selectedArtists = [];

    const generateUUID = () => {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    };

    const parseDate = (dateStr) => {
        if (!dateStr || !/^\d{2}\/\d{2}\/\d{4}$/.test(dateStr)) return new Date(0);
        const [day, month, year] = dateStr.split('/').map(Number);
        return new Date(year, month - 1, day);
    };

    const updateSelectedSongsDisplay = () => {
        const selectedTitles = selectedSongs.map(id => {
            const song = songs.find(s => s.id === id);
            return song ? song.title : 'Unknown';
        });

        selectedSongsList.innerHTML = selectedTitles.length > 0
            ? selectedTitles.map((title, index) => `
                <li data-song-id="${selectedSongs[index]}">
                    ${title}
                    <span class="remove-song" data-song-id="${selectedSongs[index]}">×</span>
                </li>
            `).join('')
            : '<li>No songs selected</li>';

        selectedSongsList.querySelectorAll('.remove-song').forEach(button => {
            button.addEventListener('click', () => {
                const songId = button.dataset.songId;
                selectedSongs = selectedSongs.filter(id => id !== songId);
                updateSelectedSongsDisplay();
                populateSongs(selectedSongs);
            });
        });
    };

    const updateSelectedArtistsDisplay = () => {
        const selectedNames = selectedArtists.map(id => {
            const artist = artists.find(a => a.id === id);
            return artist ? artist.name : 'Unknown';
        });

        selectedArtistsList.innerHTML = selectedNames.length > 0
            ? selectedNames.map((name, index) => `
                <li data-artist-id="${selectedArtists[index]}">
                    ${name}
                    <span class="remove-artist" data-artist-id="${selectedArtists[index]}">×</span>
                </li>
            `).join('')
            : '<li>No artists selected</li>';

        selectedArtistsList.querySelectorAll('.remove-artist').forEach(button => {
            button.addEventListener('click', () => {
                const artistId = button.dataset.artistId;
                selectedArtists = selectedArtists.filter(id => id !== artistId);
                updateSelectedArtistsDisplay();
                populateArtists(selectedArtists);
            });
        });
    };

    const populateSongs = (preSelectedSongs = []) => {
        if (preSelectedSongs.length > 0 && selectedSongs.length === 0) {
            selectedSongs = [...preSelectedSongs].filter(id => {
                const song = songs.find(s => s.id === id);
                return song && song.status.toLowerCase() === 'accepted';
            });
        }
        let filteredSongs = songs.filter(song => song.status.toLowerCase() === 'accepted');
        if (songSearchQuery) {
            const query = songSearchQuery.toLowerCase();
            filteredSongs = filteredSongs.filter(song => song.title.toLowerCase().includes(query));
        }

        if (filteredSongs.length === 0) {
            songsContainer.innerHTML = '<div class="no-songs-text">No songs available</div>';
            updateSelectedSongsDisplay();
            return;
        }

        let html = '';
        for (let i = 0; i < filteredSongs.length; i += 2) {
            const song1 = filteredSongs[i];
            const song2 = filteredSongs[i + 1];

            html += `<div class="song-row">`;
            [song1, song2].forEach(song => {
                if (song) {
                    const isSelected = selectedSongs.includes(song.id) ? 'selected' : '';
                    html += `
                        <span class="song-item ${isSelected}" data-song-id="${song.id}">${song.title}</span>
                    `;
                } else {
                    html += `<span class="song-item" style="visibility: hidden; flex: 1;"></span>`;
                }
            });
            html += `</div>`;
        }

        songsContainer.innerHTML = html;

        songsContainer.querySelectorAll('.song-item[data-song-id]').forEach(item => {
            item.addEventListener('dblclick', () => {
                const songId = item.dataset.songId;
                if (selectedSongs.includes(songId)) {
                    selectedSongs = selectedSongs.filter(id => id !== songId);
                } else {
                    selectedSongs.push(songId);
                }
                updateSelectedSongsDisplay();
                populateSongs(selectedSongs);
                populateArtists(selectedArtists); // Refresh artists to exclude song artists
            });
        });

        updateSelectedSongsDisplay();
    };

    const populateArtists = (preSelectedArtists = []) => {
        if (preSelectedArtists.length > 0 && selectedArtists.length === 0) {
            selectedArtists = [...preSelectedArtists];
        }
        const query = artistSearchInput.value.trim().toLowerCase();
        let filteredArtists = query
            ? artists.filter(artist => artist.name.toLowerCase().includes(query))
            : artists;

        // Exclude artists who are primary artists of selected songs
        const songArtists = selectedSongs
            .map(songId => songs.find(s => s.id === songId)?.artist)
            .filter(artistId => artistId);
        filteredArtists = filteredArtists.filter(artist => !songArtists.includes(artist.id));

        let html = '';
        for (let i = 0; i < filteredArtists.length; i += 2) {
            const artist1 = filteredArtists[i];
            const artist2 = filteredArtists[i + 1];

            html += `<div class="artist-row">`;
            [artist1, artist2].forEach(artist => {
                if (artist) {
                    const isSelected = selectedArtists.includes(artist.id) ? 'selected' : '';
                    html += `
                        <span class="artist-item ${isSelected}" data-artist-id="${artist.id}">${artist.name}</span>
                    `;
                } else {
                    html += `<span class="artist-item" style="visibility: hidden; flex: 1;"></span>`;
                }
            });
            html += `</div>`;
        }

        additionalArtistsContainer.innerHTML = html;

        additionalArtistsContainer.querySelectorAll('.artist-item[data-artist-id]').forEach(item => {
            item.addEventListener('dblclick', () => {
                const artistId = item.dataset.artistId;
                if (selectedArtists.includes(artistId)) {
                    selectedArtists = selectedArtists.filter(id => id !== artistId);
                } else {
                    selectedArtists.push(artistId);
                }
                updateSelectedArtistsDisplay();
                populateArtists(selectedArtists);
            });
        });

        updateSelectedArtistsDisplay();
    };

    const filterSongs = () => {
        songSearchQuery = songSearchInput.value.trim();
        populateSongs(selectedSongs);
    };

    const filterArtists = () => {
        populateArtists(selectedArtists);
    };

    const resetForm = (mode = 'add') => {
        albumForm.dataset.mode = mode;
        formTitle.textContent = mode === 'add' ? 'Add Album' : 'Edit Album';
        titleInput.value = '';
        descriptionTextarea.value = '';
        songSearchInput.value = '';
        artistSearchInput.value = '';
        songSearchQuery = '';
        selectedSongs = [];
        selectedArtists = [];
        songsContainer.innerHTML = '';
        additionalArtistsContainer.innerHTML = '';
        imageInput.value = '';
        currentImageDiv.textContent = '';
        titleError.style.display = 'none';
        songsError.style.display = 'none';
        artistsError.style.display = 'none';
        imageError.style.display = 'none';
        imageInput.required = mode === 'add';
        populateSongs([]);
        populateArtists([]);
        editIndex = null;
    };

    const populateEditForm = (index) => {
        const album = albums[index];
        if (!album) {
            console.error('Album not found at index:', index);
            alert('Error: Album not found.');
            return;
        }

        try {
            albumForm.dataset.mode = 'edit';
            formTitle.textContent = 'Edit Album';
            titleInput.value = album.title || '';
            descriptionTextarea.value = album.description || '';
            songSearchInput.value = '';
            artistSearchInput.value = '';
            songSearchQuery = '';
            selectedSongs = [...(album.songs || [])].filter(id => {
                const song = songs.find(s => s.id === id);
                return song && song.status.toLowerCase() === 'accepted';
            });
            selectedArtists = [...(album.additionalArtists || [])];
            populateSongs(album.songs || []);
            populateArtists(album.additionalArtists || []);
            currentImageDiv.textContent = album.imageName ? `Current image: ${album.imageName}` : '';
            imageInput.required = false;
            titleError.style.display = 'none';
            songsError.style.display = 'none';
            artistsError.style.display = 'none';
            imageError.style.display = 'none';
            editIndex = index;
            addAlbumForm.classList.add('active');
        } catch (error) {
            console.error('Error in populateEditForm:', error);
            alert('Error: Failed to load edit form.');
        }
    };

    imageInput.addEventListener('change', () => {
        const file = imageInput.files[0];
        if (file) {
            if (['image/jpeg', 'image/png'].includes(file.type)) {
                currentImageDiv.textContent = `Selected: ${file.name}`;
                imageError.style.display = 'none';
            } else {
                currentImageDiv.textContent = '';
                imageError.textContent = 'Only JPG or PNG files are allowed';
                imageError.style.display = 'block';
            }
        }
    });

    songSearchInput.addEventListener('input', filterSongs);
    artistSearchInput.addEventListener('input', filterArtists);

    addAlbumBtn.addEventListener('click', () => {
        resetForm('add');
        addAlbumForm.classList.add('active');
    });

    cancelAddAlbumBtn.addEventListener('click', () => {
        addAlbumForm.classList.remove('active');
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
        let filteredAlbums = [...albums];

        if (currentFilterStatus !== 'all') {
            filteredAlbums = filteredAlbums.filter(album => album.status.toLowerCase() === currentFilterStatus);
        }

        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            filteredAlbums = filteredAlbums.filter(album => album.title.toLowerCase().includes(query));
        }

        filteredAlbums = filteredAlbums.sort((a, b) => {
            if (currentSort === 'title-asc') {
                return a.title.localeCompare(b.title);
            } else if (currentSort === 'title-desc') {
                return b.title.localeCompare(a.title);
            } else if (currentSort === 'dateUpload-asc') {
                return parseDate(a.releaseDate) - parseDate(b.releaseDate);
            } else if (currentSort === 'dateUpload-desc') {
                return parseDate(b.releaseDate) - parseDate(a.releaseDate);
            }
            return 0;
        });

        const totalPages = Math.ceil(filteredAlbums.length / rowsPerPage);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedAlbums = filteredAlbums.slice(start, end);

        albumTableBody.innerHTML = paginatedAlbums.length > 0
            ? paginatedAlbums.map((album) => {
                const globalIndex = albums.findIndex(p => p.id === album.id);
                const artistNames = album.additionalArtists
                    ? album.additionalArtists
                        .map(id => artists.find(a => a.id === id)?.name || 'Unknown')
                        .join(', ')
                    : 'None';
                return `
                    <tr>
                        <td class="image">
                            ${album.imageName
                    ? `<span>${album.imageName}</span>`
                    : `<span>No image</span>`
                }
                        </td>
                        <td>${album.title || 'Unknown'}</td>
                        <td>
                            ${album.description
                    ? `<a class="view-description" href="#" data-index="${globalIndex}" title="View Description">Show more...</a>`
                    : 'None'
                }
                        </td>
                        <td>${album.releaseDate || 'Unknown'}</td>
                        <td>${artistNames}</td>
                        <td>
                            <a class="view-songs" href="#" data-index="${globalIndex}" title="View Songs">${album.songs.length} song${album.songs.length !== 1 ? 's' : ''}</a>
                        </td>
                        <td class="status ${album.status.toLowerCase()}">${album.status.charAt(0).toUpperCase() + album.status.slice(1)}</td>
                        <td>
                            ${
                    album.status === 'draft' || album.status === 'pending'
                        ? `
                            <button class="accept" data-index="${globalIndex}" title="Accept">Accept</button>
                            <button class="edit" data-index="${globalIndex}" title="Edit">Edit</button>
                            <button class="decline" data-index="${globalIndex}" title="Decline">Decline</button>
                            <button class="delete" data-index="${globalIndex}" title="Delete">Delete</button>
                        `
                        : `
                            <button class="edit" data-index="${globalIndex}" title="Edit">Edit</button>
                            <button class="delete" data-index="${globalIndex}" title="Delete">Delete</button>
                        `
                }
                        </td>
                    </tr>
                `;
            }).join('')
            : '<tr><td colspan="8"><span class="no-albums">No albums found.</span></td></tr>';

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

    albumForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        let isValid = true;

        titleError.style.display = 'none';
        songsError.style.display = 'none';
        artistsError.style.display = 'none';
        imageError.style.display = 'none';

        const title = titleInput.value.trim();
        if (!title) {
            titleError.style.display = 'block';
            isValid = false;
        }

        if (selectedSongs.length === 0) {
            songsError.textContent = 'Please select at least one accepted song';
            songsError.style.display = 'block';
            isValid = false;
        } else {
            const invalidSongs = selectedSongs.filter(id => {
                const song = songs.find(s => s.id === id);
                return !song || song.status.toLowerCase() !== 'accepted';
            });
            if (invalidSongs.length > 0) {
                songsError.textContent = 'All selected songs must have Accepted status';
                songsError.style.display = 'block';
                isValid = false;
            }
        }

        let imageName = editIndex !== null ? albums[editIndex].imageName : '';
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

        if (isValid) {
            const albumData = {
                id: editIndex !== null ? albums[editIndex].id : generateUUID(),
                title,
                description: descriptionTextarea.value.trim() || '',
                songs: selectedSongs,
                additionalArtists: selectedArtists,
                imageName,
                status: editIndex !== null ? albums[editIndex].status : 'draft',
                releaseDate: editIndex !== null ? albums[editIndex].releaseDate : new Date().toLocaleDateString('en-GB')
            };

            try {
                if (editIndex !== null) {
                    albums[editIndex] = albumData;
                    alert(`Album "${title}" updated successfully. Note: Image files are not stored locally; a backend is required for file storage.`);
                } else {
                    albums.push(albumData);
                    albums.sort((a, b) => {
                        if (currentSort === 'title-asc') {
                            return a.title.localeCompare(b.title);
                        } else if (currentSort === 'title-desc') {
                            return b.title.localeCompare(a.title);
                        } else if (currentSort === 'dateUpload-asc') {
                            return parseDate(a.releaseDate) - parseDate(b.releaseDate);
                        } else if (currentSort === 'dateUpload-desc') {
                            return parseDate(b.releaseDate) - parseDate(a.releaseDate);
                        }
                        return 0;
                    });
                    const newAlbumIndex = albums.findIndex(p => p.id === albumData.id);
                    let filteredAlbums = [...albums];
                    if (currentFilterStatus !== 'all') {
                        filteredAlbums = filteredAlbums.filter(p => p.status.toLowerCase() === currentFilterStatus);
                    }
                    if (searchQuery) {
                        const query = searchQuery.toLowerCase();
                        filteredAlbums = filteredAlbums.filter(p => p.title.toLowerCase().includes(query));
                    }
                    const newAlbumFilteredIndex = filteredAlbums.findIndex(p => p.id === albumData.id);
                    if (newAlbumFilteredIndex !== -1) {
                        currentPage = Math.floor(newAlbumFilteredIndex / rowsPerPage) + 1;
                    } else {
                        currentFilterStatus = 'all';
                        searchQuery = '';
                        filterStatusSelect.value = 'all';
                        searchInput.value = '';
                        currentPage = Math.floor(newAlbumIndex / rowsPerPage) + 1;
                    }
                    alert(`Album "${title}" added successfully. Note: Image files are not stored locally; a backend is required for file storage.`);
                }
                localStorage.setItem('albums', JSON.stringify(albums));
                addAlbumForm.classList.remove('active');
                resetForm('add');
                renderTable();
            } catch (e) {
                alert(`Failed to ${editIndex !== null ? 'update' : 'save'} album: Storage quota exceeded.`);
            }
        }
    });

    albumTableBody.addEventListener('click', (e) => {
        const target = e.target;
        const index = target.dataset.index;
        if (!index || !target.matches('.accept, .edit, .decline, .delete, .view-songs, .view-description')) {
            return;
        }

        e.preventDefault();

        const globalIndex = parseInt(index, 10);
        if (isNaN(globalIndex) || globalIndex < 0 || globalIndex >= albums.length) {
            console.error('Invalid album index:', index);
            alert('Error: Invalid album index.');
            return;
        }

        const album = albums[globalIndex];
        if (target.matches('.view-songs')) {
            const albumSongs = songs.filter(song => album.songs.includes(song.id));
            modalTitle.textContent = `Songs in ${album.title}`;
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
                        ${
                albumSongs.length > 0
                    ? albumSongs.map(song => `
                                    <tr>
                                        <td>${song.title || 'Unknown'}</td>
                                        <td>${song.genre || 'Unknown'}</td>
                                        <td>${song.duration || '0:00'}</td>
                                        <td class="status ${song.status.toLowerCase()}">${song.status.charAt(0).toUpperCase() + song.status.slice(1)}</td>
                                    </tr>
                                `).join('')
                    : '<tr><td colspan="4">No songs found.</td></tr>'
            }
                    </tbody>
                </table>
            `;
            contentModal.style.display = 'flex';
        } else if (target.matches('.view-description')) {
            modalTitle.textContent = `Description of ${album.title}`;
            modalContentBody.innerHTML = `
                <textarea class="lyrics-content" readonly placeholder="No description available">${album.description || ''}</textarea>
            `;
            contentModal.style.display = 'flex';
        } else if (target.matches('.accept')) {
            try {
                albums[globalIndex].status = 'accepted';
                localStorage.setItem('albums', JSON.stringify(albums));
                renderTable();
                alert(`Album "${albums[globalIndex].title}" accepted.`);
            } catch (e) {
                alert('Failed to accept album: Storage quota exceeded.');
            }
        } else if (target.matches('.edit')) {
            populateEditForm(globalIndex);
        } else if (target.matches('.decline')) {
            try {
                albums[globalIndex].status = 'declined';
                localStorage.setItem('albums', JSON.stringify(albums));
                renderTable();
                alert(`Album "${albums[globalIndex].title}" declined.`);
            } catch (e) {
                alert('Failed to decline album: Storage quota exceeded.');
            }
        } else if (target.matches('.delete')) {
            if (confirm(`Are you sure you want to delete "${albums[globalIndex].title}"?`)) {
                try {
                    const deletedTitle = albums[globalIndex].title;
                    albums.splice(globalIndex, 1);
                    localStorage.setItem('albums', JSON.stringify(albums));
                    renderTable();
                    alert(`Album "${deletedTitle}" deleted successfully.`);
                } catch (e) {
                    alert('Failed to delete album: Storage quota exceeded.');
                }
            }
        }
    });

    closeModal.addEventListener('click', () => {
        contentModal.style.display = 'none';
        modalContentBody.innerHTML = '';
    });

    window.addEventListener('click', (e) => {
        if (e.target === contentModal) {
            contentModal.style.display = 'none';
            modalContentBody.innerHTML = '';
        }
    });

    resetForm('add');
    renderTable();
});