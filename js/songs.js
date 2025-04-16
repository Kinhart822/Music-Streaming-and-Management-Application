document.addEventListener('DOMContentLoaded', () => {
    const songTableBody = document.getElementById('song-table-body');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const genreFilterSelect = document.getElementById('genre-filter');
    const sortBySelect = document.getElementById('sort-by');
    const paginationDiv = document.querySelector('.pagination');

    let songs = JSON.parse(localStorage.getItem('songs')) || [];
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput.value) || 10; // Default to 10 if invalid
    let currentGenre = 'all';
    let currentSort = 'uploadDate-asc';

    // Add listeners field if missing (for demo)
    songs = songs.map(song => ({
        ...song,
        listeners: song.listeners || Math.floor(Math.random() * 1000)
    }));
    localStorage.setItem('songs', JSON.stringify(songs));

    // Validate and update rows per page
    const updateRowsPerPage = () => {
        const value = parseInt(rowsPerPageInput.value);
        if (isNaN(value) || value < 1) {
            rowsPerPageInput.value = 10;
            rowsPerPage = 10;
        } else {
            rowsPerPage = value;
        }
        currentPage = 1; // Reset to first page
        renderTable();
    };

    // Render table
    const renderTable = () => {
        // Filter songs
        let filteredSongs = songs;
        if (currentGenre !== 'all') {
            filteredSongs = songs.filter(song => song.genre === currentGenre);
        }

        // Sort songs
        filteredSongs = [...filteredSongs].sort((a, b) => {
            if (currentSort === 'uploadDate-asc') {
                const dateA = parseDate(a.uploadDate);
                const dateB = parseDate(b.uploadDate);
                return dateA - dateB;
            } else if (currentSort === 'uploadDate-desc') {
                const dateA = parseDate(a.uploadDate);
                const dateB = parseDate(b.uploadDate);
                return dateB - dateA;
            } else if (currentSort === 'listeners-asc') {
                return (a.listeners || 0) - (b.listeners || 0);
            } else if (currentSort === 'listeners-desc') {
                return (b.listeners || 0) - (a.listeners || 0);
            }
            return 0;
        });

        // Paginate
        const totalPages = Math.ceil(filteredSongs.length / rowsPerPage);
        // Adjust currentPage if it exceeds totalPages
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedSongs = filteredSongs.slice(start, end);

        // Render table
        songTableBody.innerHTML = paginatedSongs.map((song, index) => {
            const globalIndex = start + index;
            return `
                <tr>
                    <td>${song.title}</td>
                    <td>${song.genre}</td>
                    <td>${song.duration}</td>
                    <td>${song.uploadDate}</td>
                    <td class="truncate">${song.lyrics || 'No lyrics'}</td>
                    <td class="truncate">${song.description || 'No description'}</td>
                    <td>${song.download}</td>
                    <td class="${song.status.toLowerCase()}">${song.status}</td>
                    <td>${song.listeners || 0}</td>
                    <td>
                        ${
                            song.status === 'Draft' || song.status === 'Declined'
                                ? `
                                    <span>
                                        ${
                                            song.status === 'Draft'
                                                ? `<i class="ri-upload-line publish" data-index="${globalIndex}" style="margin-right: 12px" title="Publish"></i>`
                                                : ''
                                        }
                                        <i class="ri-edit-line edit" data-index="${globalIndex}" title="Edit"></i>
                                        <i class="ri-delete-bin-line delete" data-index="${globalIndex}" title="Delete"></i>
                                    </span>
                                `
                                : ''
                        }
                    </td>
                </tr>
            `;
        }).join('');

        // Render pagination
        paginationDiv.innerHTML = '';
        if (totalPages > 1) {
            // Previous button
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

            // Page numbers
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

            // Next button
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

    // Parse date (DD/MM/YYYY to Date object)
    const parseDate = (dateStr) => {
        const [day, month, year] = dateStr.split('/').map(Number);
        return new Date(year, month - 1, day);
    };

    // Event handlers
    rowsPerPageInput.addEventListener('change', updateRowsPerPage);
    rowsPerPageInput.addEventListener('input', updateRowsPerPage); // Handle real-time input

    genreFilterSelect.addEventListener('change', () => {
        currentGenre = genreFilterSelect.value;
        currentPage = 1;
        renderTable();
    });

    sortBySelect.addEventListener('change', () => {
        currentSort = sortBySelect.value;
        currentPage = 1;
        renderTable();
    });

    // Table actions
    songTableBody.addEventListener('click', (e) => {
        const index = e.target.dataset.index;
        if (index === undefined) return;

        // Adjust index for filtered/paginated data
        let filteredSongs = songs;
        if (currentGenre !== 'all') {
            filteredSongs = songs.filter(song => song.genre === currentGenre);
        }
        filteredSongs = [...filteredSongs].sort((a, b) => {
            if (currentSort === 'uploadDate-asc') {
                return parseDate(a.uploadDate) - parseDate(b.uploadDate);
            } else if (currentSort === 'uploadDate-desc') {
                return parseDate(b.uploadDate) - parseDate(a.uploadDate);
            } else if (currentSort === 'listeners-asc') {
                return (a.listeners || 0) - (b.listeners || 0);
            } else if (currentSort === 'listeners-desc') {
                return (b.listeners || 0) - (a.listeners || 0);
            }
            return 0;
        });
        const globalIndex = parseInt(index);
        if (globalIndex < 0 || globalIndex >= filteredSongs.length) {
            alert('Error: Invalid song index.');
            return;
        }

        const songIndex = songs.findIndex(s => s.title === filteredSongs[globalIndex].title);
        if (songIndex === -1) {
            alert('Error: Song not found.');
            return;
        }

        if (e.target.classList.contains('publish')) {
            songs[songIndex].status = 'Pending';
            localStorage.setItem('songs', JSON.stringify(songs));
            renderTable();
            alert(`Song "${songs[songIndex].title}" published and status changed to Pending.`);
        } else if (e.target.classList.contains('delete')) {
            if (confirm(`Are you sure you want to delete "${songs[songIndex].title}"?`)) {
                songs.splice(songIndex, 1);
                localStorage.setItem('songs', JSON.stringify(songs));
                renderTable();
                alert('Song deleted successfully.');
            }
        } else if (e.target.classList.contains('edit')) {
            localStorage.setItem('editSongIndex', songIndex);
            window.location.href = 'artist_edit_song.html';
        }
    });

    // Initial render
    renderTable();
});