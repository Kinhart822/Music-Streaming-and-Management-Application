document.addEventListener('DOMContentLoaded', () => {
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
    const songsContainer = document.getElementById('playlist-songs');
    const selectedSongsList = document.getElementById('selected-songs-list');
    const artistSearchInput = document.getElementById('artist-search');
    const additionalArtistsContainer = document.getElementById('playlist-additional_artists');
    const selectedArtistsList = document.getElementById('selected-artists-list');
    const imageInput = document.getElementById('playlist-image');
    const imagePreview = document.getElementById('image-preview');
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

    let playlists = JSON.parse(localStorage.getItem('playlists')) || [];
    let songs = JSON.parse(localStorage.getItem('songs')) || [
        {
            id: 'song1',
            title: 'Blank Space',
            artist: 'Taylor Swift',
            genre: 'Pop',
            duration: '3:55',
            status: 'Draft',
            songFileName: 'blank_space.mp3',
            imageName: 'blank_space.jpg',
            downloadPermission: 'Yes',
            uploadDate: '01/01/2025',
            listeners: 500,
            lyrics: '',
            description: '',
            additionalArtists: ['Taylor Swift']
        },
        {
            id: 'song2',
            title: 'Shape of You',
            artist: 'Ed Sheeran',
            genre: 'Pop',
            duration: '4:20',
            status: 'Pending',
            songFileName: 'shape_of_you.mp3',
            imageName: 'shape_of_you.jpg',
            downloadPermission: 'No',
            uploadDate: '02/01/2025',
            listeners: 600,
            lyrics: '',
            description: '',
            additionalArtists: ['Ed Sheeran']
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
            });
        });

        updateSelectedSongsDisplay();
    };

    const populateArtists = (preSelectedArtists = []) => {
        if (preSelectedArtists.length > 0 && selectedArtists.length === 0) {
            selectedArtists = [...preSelectedArtists];
        }
        const query = artistSearchInput.value.trim().toLowerCase();
        const filteredArtists = query
            ? artists.filter(artist => artist.name.toLowerCase().includes(query))
            : artists;

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
        playlistForm.dataset.mode = mode;
        formTitle.textContent = mode === 'add' ? 'Add Playlist' : 'Edit Playlist';
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
        imagePreview.style.display = 'none';
        imagePreview.src = '';
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
        const playlist = playlists[index];
        playlistForm.dataset.mode = 'edit';
        formTitle.textContent = 'Edit Playlist';
        titleInput.value = playlist.title;
        descriptionTextarea.value = playlist.description || '';
        songSearchInput.value = '';
        artistSearchInput.value = '';
        songSearchQuery = '';
        selectedSongs = [...(playlist.songs || [])].filter(id => {
            const song = songs.find(s => s.id === id);
            return song && song.status.toLowerCase() === 'accepted';
        });
        selectedArtists = [...(playlist.additionalArtists || [])];
        populateSongs(playlist.songs || []);
        populateArtists(playlist.additionalArtists || []);
        currentImageDiv.textContent = playlist.imageName ? `Current image: ${playlist.imageName}` : '';
        imagePreview.style.display = 'none';
        imagePreview.src = '';
        imageInput.required = false;
        titleError.style.display = 'none';
        songsError.style.display = 'none';
        artistsError.style.display = 'none';
        imageError.style.display = 'none';
        editIndex = index;
        addPlaylistForm.classList.add('active');
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

    songSearchInput.addEventListener('input', filterSongs);
    artistSearchInput.addEventListener('input', filterArtists);

    addPlaylistBtn.addEventListener('click', () => {
        resetForm('add');
        addPlaylistForm.classList.add('active');
    });

    cancelAddPlaylistBtn.addEventListener('click', () => {
        addPlaylistForm.classList.remove('active');
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
        let filteredPlaylists = [...playlists];

        if (currentFilterStatus !== 'all') {
            filteredPlaylists = filteredPlaylists.filter(playlist => playlist.status.toLowerCase() === currentFilterStatus);
        }

        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            filteredPlaylists = filteredPlaylists.filter(playlist => playlist.title.toLowerCase().includes(query));
        }

        filteredPlaylists = filteredPlaylists.sort((a, b) => {
            if (currentSort === 'title-asc') {
                return a.title.localeCompare(b.title);
            } else if (currentSort === 'title-desc') {
                return b.title.localeCompare(a.title);
            } else if (currentSort === 'date-asc') {
                return parseDate(a.releaseDate) - parseDate(b.releaseDate);
            } else if (currentSort === 'date-desc') {
                return parseDate(b.releaseDate) - parseDate(a.releaseDate);
            }
            return 0;
        });

        const totalPages = Math.ceil(filteredPlaylists.length / rowsPerPage);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedPlaylists = filteredPlaylists.slice(start, end);

        playlistTableBody.innerHTML = paginatedPlaylists.length > 0
            ? paginatedPlaylists.map((playlist, index) => {
                const globalIndex = playlists.findIndex(p => p.id === playlist.id);
                const artistNames = playlist.additionalArtists
                    ? playlist.additionalArtists
                        .map(id => {
                            const artist = artists.find(a => a.id === id);
                            return artist ? artist.name : 'Unknown';
                        })
                        .join(', ')
                    : 'None';
                return `
                    <tr>
                        <td class="image">
                            ${playlist.imageName
                    ? `<span>${playlist.imageName}</span>`
                    : `<span>No image</span>`
                }
                        </td>
                        <td>${playlist.title || 'Unknown'}</td>
                        <td>
                            ${playlist.description
                    ? `<a class="view-description" href="#" data-index="${globalIndex}" title="View Description">Show more...</a>`
                    : 'None'
                }
                        </td>
                        <td>${playlist.releaseDate || 'Unknown'}</td>
                        <td>${artistNames}</td>
                        <td>
                            <a class="view-songs" href="#" data-index="${globalIndex}" title="View Songs">${playlist.songs.length} song${playlist.songs.length !== 1 ? 's' : ''}</a>
                        </td>
                        <td class="status ${playlist.status.toLowerCase()}">${playlist.status.charAt(0).toUpperCase() + playlist.status.slice(1)}</td>
                        <td>
                            ${
                    playlist.status === 'Draft' || playlist.status === 'Declined'
                        ? `
                                        <button class="publish" data-index="${globalIndex}" title="Publish">Publish</button>
                                        <button class="edit" data-index="${globalIndex}" title="Edit">Edit</button>
                                        <button class="delete" data-index="${globalIndex}" title="Delete">Delete</button>
                                    `
                        : playlist.status === 'Pending' || playlist.status === 'Accepted'
                            ? `
                                            <button class="edit" data-index="${globalIndex}" title="Edit">Edit</button>
                                            <button class="delete" data-index="${globalIndex}" title="Delete">Delete</button>
                                        `
                            : ''
                }
                        </td>
                    </tr>
                `;
            }).join('')
            : '<tr><td colspan="8"><span class="no-playlists">No playlists found.</span></td></tr>';

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

    playlistForm.addEventListener('submit', async (e) => {
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

        let imageName = editIndex !== null ? playlists[editIndex].imageName : '';
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
            const playlistData = {
                id: editIndex !== null ? playlists[editIndex].id : generateUUID(),
                title,
                description: descriptionTextarea.value.trim() || '',
                songs: selectedSongs,
                additionalArtists: selectedArtists,
                imageName,
                status: editIndex !== null ? playlists[editIndex].status : 'Draft',
                releaseDate: editIndex !== null ? playlists[editIndex].releaseDate : new Date().toLocaleDateString('en-GB')
            };

            try {
                if (editIndex !== null) {
                    playlists[editIndex] = playlistData;
                    alert(`Playlist "${title}" updated successfully. Note: Image files are not stored locally; a backend is required for file storage.`);
                } else {
                    playlists.push(playlistData);
                    alert(`Playlist "${title}" added successfully. Note: Image files are not stored locally; a backend is required for file storage.`);
                }
                localStorage.setItem('playlists', JSON.stringify(playlists));
                addPlaylistForm.classList.remove('active');
                resetForm('add');
                renderTable();
            } catch (e) {
                alert(`Failed to ${editIndex !== null ? 'update' : 'save'} playlist: Storage quota exceeded.`);
            }
        }
    });

    playlistTableBody.addEventListener('click', (e) => {
        const index = e.target.dataset.index;
        if (!index) return;

        e.preventDefault();

        const globalIndex = parseInt(index);
        if (globalIndex < 0 || globalIndex >= playlists.length) {
            alert('Error: Invalid playlist index.');
            return;
        }

        const playlist = playlists[globalIndex];
        if (e.target.classList.contains('view-songs')) {
            const playlistSongs = songs.filter(song => playlist.songs.includes(song.id));
            modalTitle.textContent = `Songs in ${playlist.title}`;
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
                playlistSongs.length > 0
                    ? playlistSongs.map(song => `
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
        } else if (e.target.classList.contains('view-description')) {
            modalTitle.textContent = `Description of ${playlist.title}`;
            modalContentBody.innerHTML = `
                <textarea class="lyrics-content" readonly placeholder="No description available">${playlist.description || ''}</textarea>
            `;
            contentModal.style.display = 'flex';
        } else if (e.target.classList.contains('publish')) {
            try {
                playlists[globalIndex].status = 'Pending';
                localStorage.setItem('playlists', JSON.stringify(playlists));
                renderTable();
                alert(`Playlist "${playlists[globalIndex].title}" published and status changed to Pending.`);
            } catch (e) {
                alert('Failed to publish playlist: Storage quota exceeded.');
            }
        } else if (e.target.classList.contains('edit')) {
            populateEditForm(globalIndex);
        } else if (e.target.classList.contains('delete')) {
            if (confirm(`Are you sure you want to delete "${playlists[globalIndex].title}"?`)) {
                try {
                    const deletedTitle = playlists[globalIndex].title;
                    playlists.splice(globalIndex, 1);
                    localStorage.setItem('playlists', JSON.stringify(playlists));
                    renderTable();
                    alert(`Playlist "${deletedTitle}" deleted successfully.`);
                } catch (e) {
                    alert('Failed to delete playlist: Storage quota exceeded.');
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