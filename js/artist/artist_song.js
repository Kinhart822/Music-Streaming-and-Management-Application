document.addEventListener('DOMContentLoaded', () => {
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
    const durationInput = document.getElementById('song-duration');
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
    let selectedArtists = [];

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

    const filterArtists = () => {
        populateArtists(selectedArtists);
    };

    const resetForm = (mode = 'add') => {
        songForm.dataset.mode = mode;
        formTitle.textContent = mode === 'add' ? 'Add Song' : 'Edit Song';
        titleInput.value = '';
        genreSelect.value = '';
        durationInput.value = '';
        lyricsTextarea.value = '';
        descriptionTextarea.value = '';
        artistSearchInput.value = '';
        selectedArtists = [];
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
        artistsError.style.display = 'none';
        songFileError.style.display = 'none';
        imageError.style.display = 'none';
        downloadPermissionError.style.display = 'none';
        songFileInput.required = true;
        imageInput.required = true;
        downloadPermissionSelect.required = true;
        populateArtists([]);
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
            formTitle.textContent = 'Edit Song';
            titleInput.value = song.title || '';
            genreSelect.value = song.genre || '';
            durationInput.value = song.duration || '';
            lyricsTextarea.value = song.lyrics || '';
            descriptionTextarea.value = song.description || '';
            artistSearchInput.value = '';
            selectedArtists = [...(song.additionalArtists || [])];
            populateArtists(song.additionalArtists || []);
            downloadPermissionSelect.value = song.downloadPermission || '';
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
            artistsError.style.display = 'none';
            songFileError.style.display = 'none';
            imageError.style.display = 'none';
            downloadPermissionError.style.display = 'none';
            editIndex = index;

            if (addSongForm) {
                addSongForm.classList.add('active');
            } else {
                console.error('addSongForm element not found');
            }
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

    artistSearchInput.addEventListener('input', filterArtists);

    addSongBtn.addEventListener('click', () => {
        resetForm('add');
        if (addSongForm) {
            addSongForm.classList.add('active');
        } else {
            console.error('addSongForm element not found');
        }
    });

    cancelAddSongBtn.addEventListener('click', () => {
        if (addSongForm) {
            addSongForm.classList.remove('active');
            resetForm('add');
        } else {
            console.error('addSongForm element not found');
        }
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

        // Áp dụng bộ lọc trạng thái
        if (currentFilterStatus !== 'all') {
            filteredSongs = filteredSongs.filter(song => song.status.toLowerCase() === currentFilterStatus);
        }

        // Áp dụng bộ lọc thể loại
        if (currentFilterGenre !== 'all') {
            filteredSongs = filteredSongs.filter(song => song.genre === currentFilterGenre);
        }

        // Áp dụng tìm kiếm trên tiêu đề bài hát
        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            filteredSongs = filteredSongs.filter(song => song.title.toLowerCase().includes(query));
        }

        // Sắp xếp danh sách đã lọc
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

        // Tính toán phân trang
        const totalPages = Math.ceil(filteredSongs.length / rowsPerPage);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedSongs = filteredSongs.slice(start, end);

        // Hiển thị danh sách bài hát
        songTableBody.innerHTML = paginatedSongs.length > 0
            ? paginatedSongs.map((song) => {
                const globalIndex = songs.findIndex(s => s.id === song.id);
                if (globalIndex === -1) return '';
                const artistNames = song.additionalArtists
                    ? song.additionalArtists
                        .map(id => {
                            const artist = artists.find(a => a.id === id);
                            return artist ? artist.name : 'Unknown';
                        })
                        .join(', ')
                    : 'None';
                const listeners = (song.status === 'Draft' || song.status === 'Declined' || song.status === 'Pending') ? 0 : (song.listeners || generateRandomListeners());
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
                        <td class="status ${song.status.toLowerCase()}">${song.status.charAt(0).toUpperCase() + song.status.slice(1)}</td>
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
                        <td>${artistNames}</td>
                        <td>
                            ${
                    song.status === 'Draft' || song.status === 'Declined'
                        ? `
                                        <button class="publish" data-index="${globalIndex}" title="Publish">Publish</button>
                                        <button class="edit" data-index="${globalIndex}" title="Edit">Edit</button>
                                        <button class="delete" data-index="${globalIndex}" title="Delete">Delete</button>
                                    `
                        : song.status === 'Pending' || song.status === 'Accepted'
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
            : '<tr><td colspan="12"><span class="no-songs">No songs found.</span></td></tr>';

        // Cập nhật phân trang
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
        currentPage = 1; // Reset về trang đầu tiên khi tìm kiếm
        renderTable();
    });

    songForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        let isValid = true;

        titleError.style.display = 'none';
        genreError.style.display = 'none';
        durationError.style.display = 'none';
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

        if (selectedArtists.length === 0) {
            artistsError.style.display = 'block';
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
            const status = editIndex !== null ? songs[editIndex].status : 'Draft';
            const listeners = (status === 'Draft' || status === 'Declined' || status === 'Pending') ? 0 : generateRandomListeners();

            const songData = {
                id: editIndex !== null ? songs[editIndex].id : generateUUID(),
                title,
                genre,
                duration,
                lyrics: lyricsTextarea.value.trim() || '',
                description: descriptionTextarea.value.trim() || '',
                additionalArtists: selectedArtists,
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

        if (!index || !target.matches('.publish, .edit, .delete, .view-lyrics, .view-description')) {
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
        } else if (target.matches('.publish')) {
            try {
                songs[globalIndex].status = 'Pending';
                songs[globalIndex].listeners = 0;
                localStorage.setItem('songs', JSON.stringify(songs));
                renderTable();
                alert(`Song "${songs[globalIndex].title}" published and status changed to Pending.`);
            } catch (e) {
                alert('Failed to publish song: Storage quota exceeded.');
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