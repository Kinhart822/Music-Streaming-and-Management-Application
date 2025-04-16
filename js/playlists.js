document.addEventListener('DOMContentLoaded', () => {
    const playlistTableBody = document.getElementById('playlist-table-body');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const sortBySelect = document.getElementById('sort-by');
    const paginationDiv = document.querySelector('.pagination');

    let playlists = JSON.parse(localStorage.getItem('playlists')) || [];
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput.value) || 10;
    let currentSort = 'creationDate-asc';

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

    // Parse date (DD/MM/YYYY to Date object)
    const parseDate = (dateStr) => {
        if (!dateStr) return new Date(0);
        const [day, month, year] = dateStr.split('/').map(Number);
        return new Date(year, month - 1, day);
    };

    // Render table
    const renderTable = () => {
        // Sort playlists
        let sortedPlaylists = [...playlists].sort((a, b) => {
            if (currentSort === 'creationDate-asc') {
                return parseDate(a.releaseDate) - parseDate(b.releaseDate);
            } else if (currentSort === 'creationDate-desc') {
                return parseDate(b.releaseDate) - parseDate(a.releaseDate);
            }
            return 0;
        });

        // Paginate
        const totalPages = Math.ceil(sortedPlaylists.length / rowsPerPage);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedPlaylists = sortedPlaylists.slice(start, end);

        // Render table
        playlistTableBody.innerHTML = paginatedPlaylists.length > 0
            ? paginatedPlaylists.map((playlist, index) => {
                const globalIndex = start + index;
                return `
                    <tr>
                        <td>${playlist.title}</td>
                        <td class="truncate">${playlist.description || 'No description'}</td>
                        <td>${playlist.timeLength || '0:00'}</td>
                        <td>${playlist.releaseDate}</td>
                        <td class="${playlist.status.toLowerCase()}">${playlist.status}</td>
                        <td>${playlist.songs ? playlist.songs.length : 0}</td>
                        <td>
                            ${
                                playlist.status === 'Draft' || playlist.status === 'Declined'
                                    ? `
                                        <span>
                                            ${
                                                playlist.status === 'Draft'
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
            }).join('')
            : '<tr><td colspan="7">No playlists available. <a href="artist_add_playlist.html">Add a new playlist</a>.</td></tr>';

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

    // Event handlers
    rowsPerPageInput.addEventListener('change', updateRowsPerPage);
    rowsPerPageInput.addEventListener('input', updateRowsPerPage);

    sortBySelect.addEventListener('change', () => {
        currentSort = sortBySelect.value;
        currentPage = 1;
        renderTable();
    });

    // Table actions
    playlistTableBody.addEventListener('click', (e) => {
        const index = e.target.dataset.index;
        if (index === undefined) return;

        let sortedPlaylists = [...playlists].sort((a, b) => {
            if (currentSort === 'creationDate-asc') {
                return parseDate(a.releaseDate) - parseDate(b.releaseDate);
            } else if (currentSort === 'creationDate-desc') {
                return parseDate(b.releaseDate) - parseDate(a.releaseDate);
            }
            return 0;
        });

        const globalIndex = parseInt(index);
        if (globalIndex < 0 || globalIndex >= sortedPlaylists.length) {
            alert('Error: Invalid playlist index.');
            return;
        }

        const playlistIndex = playlists.findIndex(p => p.title === sortedPlaylists[globalIndex].title);
        if (playlistIndex === -1) {
            alert('Error: Playlist not found.');
            return;
        }

        if (e.target.classList.contains('publish')) {
            playlists[playlistIndex].status = 'Pending';
            localStorage.setItem('playlists', JSON.stringify(playlists));
            renderTable();
            alert(`Playlist "${playlists[playlistIndex].title}" published and status changed to Pending.`);
        } else if (e.target.classList.contains('delete')) {
            if (confirm(`Are you sure you want to delete "${playlists[playlistIndex].title}"?`)) {
                playlists.splice(playlistIndex, 1);
                localStorage.setItem('playlists', JSON.stringify(playlists));
                renderTable();
                alert('Playlist deleted successfully.');
            }
        } else if (e.target.classList.contains('edit')) {
            localStorage.setItem('editPlaylistIndex', playlistIndex);
            window.location.href = 'artist_edit_playlist.html';
        }
    });

    // Initial render
    renderTable();
});