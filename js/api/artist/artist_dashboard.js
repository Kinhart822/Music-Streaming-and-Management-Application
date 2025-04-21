import {fetchWithRefresh} from '/js/api/refresh.js';

document.addEventListener('DOMContentLoaded', () => {
    // Song table elements
    const songTableBody = document.getElementById('song-table-body');
    const filterStatusSelect = document.getElementById('filter-status');
    const genreFilterSelect = document.getElementById('genre-filter');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const lyricsModal = document.getElementById('lyrics-modal');
    const modalLyricsContent = document.getElementById('modal-lyrics-content');
    const modalClose = document.getElementById('close-modal');

    let songs = JSON.parse(localStorage.getItem('songs')) || [];
    let artists = JSON.parse(localStorage.getItem('artists')) || [];

    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput?.value) || 10;
    let currentFilterStatus = 'all';
    let currentFilterGenre = 'all';
    let currentSort = 'title-asc';
    let searchQuery = '';

    // Song table handling
    if (songTableBody) {
        // Parse date (DD/MM/YYYY to a Date object)
        const parseDate = (dateStr) => {
            if (!dateStr || !/^\d{2}\/\d{2}\/\d{4}$/.test(dateStr)) return new Date(0);
            const [day, month, year] = dateStr.split('/').map(Number);
            return new Date(year, month - 1, day);
        };

        // Validate and update rows per page
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

        // Render table
        const renderTable = () => {
            // Sort by upload date (descending) and take only the 10 most recent songs
            let recentSongs = [...songs].sort((a, b) => {
                return parseDate(b.uploadDate) - parseDate(a.uploadDate);
            }).slice(0, 10);

            let filteredSongs = [...recentSongs];

            // Filter by status
            if (currentFilterStatus !== 'all') {
                filteredSongs = filteredSongs.filter(song => song.status.toLowerCase() === currentFilterStatus);
            }

            // Filter by genre
            if (currentFilterGenre !== 'all') {
                filteredSongs = filteredSongs.filter(song => song.genre === currentFilterGenre);
            }

            // Search
            if (searchQuery) {
                const query = searchQuery.toLowerCase();
                filteredSongs = filteredSongs.filter(song => song.title.toLowerCase().includes(query));
            }

            // Sort
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

            // Paginate
            const totalPages = Math.ceil(filteredSongs.length / rowsPerPage);
            if (currentPage > totalPages && totalPages > 0) {
                currentPage = totalPages;
            } else if (totalPages === 0) {
                currentPage = 1;
            }
            const start = (currentPage - 1) * rowsPerPage;
            const end = start + rowsPerPage;
            const paginatedSongs = filteredSongs.slice(start, end);

            // Render table
            songTableBody.innerHTML = paginatedSongs.length > 0
                ? paginatedSongs.map((song, index) => {
                    const globalIndex = songs.findIndex(s => s.id === song.id);
                    const artistNames = song.additionalArtists
                        ? song.additionalArtists
                            .map(id => {
                                const artist = artists.find(a => a.id === id);
                                return artist ? artist.name : 'Unknown';
                            })
                            .join(', ')
                        : 'None';
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
                            <td>${song.listeners || 0}</td>
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
                            <td class="status ${song.status.toLowerCase()}">${song.status.charAt(0).toUpperCase() + song.status.slice(1)}</td>
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

            // Render pagination
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

        // Filter by status
        if (filterStatusSelect) {
            filterStatusSelect.addEventListener('change', () => {
                currentFilterStatus = filterStatusSelect.value;
                currentPage = 1;
                renderTable();
            });
        }

        // Filter by genre
        if (genreFilterSelect) {
            genreFilterSelect.addEventListener('change', () => {
                currentFilterGenre = genreFilterSelect.value;
                currentPage = 1;
                renderTable();
            });
        }

        // Sort by
        if (sortBySelect) {
            sortBySelect.addEventListener('change', () => {
                currentSort = sortBySelect.value;
                currentPage = 1;
                renderTable();
            });
        }

        // Rows per page
        if (rowsPerPageInput) {
            rowsPerPageInput.addEventListener('change', updateRowsPerPage);
            rowsPerPageInput.addEventListener('input', updateRowsPerPage);
        }

        // Search
        if (searchInput) {
            searchInput.addEventListener('input', () => {
                searchQuery = searchInput.value.trim();
                currentPage = 1;
                renderTable();
            });
        }

        // Table actions
        songTableBody.addEventListener('click', (e) => {
            const index = e.target.dataset.index;
            if (!index) return;

            e.preventDefault();

            const globalIndex = parseInt(index);
            if (globalIndex < 0 || globalIndex >= songs.length) {
                alert('Error: Invalid song index.');
                return;
            }

            const song = songs[globalIndex];
            if (e.target.classList.contains('view-lyrics')) {
                document.getElementById('modal-title').textContent = 'Song Lyrics';
                modalLyricsContent.value = song.lyrics || '';
                modalLyricsContent.placeholder = song.lyrics ? '' : 'No lyrics available';
                lyricsModal.style.display = 'flex';
            } else if (e.target.classList.contains('view-description')) {
                document.getElementById('modal-title').textContent = 'Song Description';
                modalLyricsContent.value = song.description || '';
                modalLyricsContent.placeholder = song.description ? '' : 'No description available';
                lyricsModal.style.display = 'flex';
            } else if (e.target.classList.contains('publish')) {
                try {
                    songs[globalIndex].status = 'Pending';
                    localStorage.setItem('songs', JSON.stringify(songs));
                    renderTable();
                    alert(`Song "${songs[globalIndex].title}" published and status changed to Pending.`);
                } catch (e) {
                    alert('Failed to publish song: Storage quota exceeded.');
                }
            } else if (e.target.classList.contains('edit')) {
                localStorage.setItem('editSongIndex', globalIndex);
                window.location.href = 'artist_manage_song.html';
            } else if (e.target.classList.contains('delete')) {
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

        // Close modal
        if (modalClose) {
            modalClose.addEventListener('click', () => {
                lyricsModal.style.display = 'none';
                modalLyricsContent.value = '';
                modalLyricsContent.placeholder = 'No content available';
            });
        }

        // Close modal when clicking outside
        window.addEventListener('click', (e) => {
            if (e.target === lyricsModal) {
                lyricsModal.style.display = 'none';
                modalLyricsContent.value = '';
                modalLyricsContent.placeholder = 'No content available';
            }
        });

        // Initial render
        renderTable();
    }

    // Update dashboard cards
    const totalSongsCard = document.querySelector('.card-1 h1');
    const uploadedSongsCard = document.querySelector('.card-2 h1');
    const totalPlaylistsCard = document.querySelector('.card-3 h1');
    const totalAlbumsCard = document.querySelector('.card-4 h1');

    if (totalSongsCard) {
        totalSongsCard.textContent = Math.min(songs.length, 10); // Reflect the limit in the total songs card
    }

    if (uploadedSongsCard) {
        const uploadedSongs = songs.filter(song => song.status === 'Accepted').length;
        uploadedSongsCard.textContent = Math.min(uploadedSongs, 10); // Limit to 10
    }

    if (totalPlaylistsCard) {
        const playlists = JSON.parse(localStorage.getItem('playlists')) || [];
        totalPlaylistsCard.textContent = playlists.length;
    }

    if (totalAlbumsCard) {
        const albums = JSON.parse(localStorage.getItem('albums')) || [];
        totalAlbumsCard.textContent = albums.length;
    }
});