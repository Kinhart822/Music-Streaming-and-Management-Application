document.addEventListener('DOMContentLoaded', () => {
    const albumTableBody = document.getElementById('album-table-body');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const sortBySelect = document.getElementById('sort-by');
    const paginationDiv = document.querySelector('.pagination');

    let albums = JSON.parse(localStorage.getItem('albums')) || [];
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput.value) || 10;
    let currentSort = 'releaseDate-asc';

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
        // Sort albums
        let sortedAlbums = [...albums].sort((a, b) => {
            if (currentSort === 'releaseDate-asc') {
                return parseDate(a.releaseDate) - parseDate(b.releaseDate);
            } else if (currentSort === 'releaseDate-desc') {
                return parseDate(b.releaseDate) - parseDate(a.releaseDate);
            }
            return 0;
        });

        // Paginate
        const totalPages = Math.ceil(sortedAlbums.length / rowsPerPage);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedAlbums = sortedAlbums.slice(start, end);

        // Render table
        albumTableBody.innerHTML = paginatedAlbums.length > 0
            ? paginatedAlbums.map((album, index) => {
                const globalIndex = start + index;
                const timeLength = album.timeLength || '0:00';
                const songsDisplay = Array.isArray(album.songs) ? album.songs.length : (album.songs || 0);
                return `
                    <tr>
                        <td>${album.title || 'Untitled'}</td>
                        <td class="truncate">${album.description || 'No description'}</td>
                        <td>${timeLength}</td>
                        <td>${album.releaseDate || 'Unknown'}</td>
                        <td class="${album.status ? album.status.toLowerCase() : ''}">${album.status || 'Unknown'}</td>
                        <td>${songsDisplay}</td>
                        <td>
                            ${
                                album.status === 'Draft' || album.status === 'Declined'
                                    ? `
                                        <span>
                                            ${
                                                album.status === 'Draft'
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
            : '<tr><td colspan="7">No albums available. <a href="artist_add_album.html">Add a new album</a>.</td></tr>';

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
    albumTableBody.addEventListener('click', (e) => {
        const index = e.target.dataset.index;
        if (index === undefined) return;

        let sortedAlbums = [...albums].sort((a, b) => {
            if (currentSort === 'releaseDate-asc') {
                return parseDate(a.releaseDate) - parseDate(b.releaseDate);
            } else if (currentSort === 'releaseDate-desc') {
                return parseDate(b.releaseDate) - parseDate(a.releaseDate);
            }
            return 0;
        });

        const globalIndex = parseInt(index);
        if (globalIndex < 0 || globalIndex >= sortedAlbums.length) {
            alert('Error: Invalid album index.');
            return;
        }

        const albumIndex = albums.findIndex(a => a.title === sortedAlbums[globalIndex].title);
        if (albumIndex === -1) {
            alert('Error: Album not found.');
            return;
        }

        if (e.target.classList.contains('publish')) {
            albums[albumIndex].status = 'Pending';
            localStorage.setItem('albums', JSON.stringify(albums));
            renderTable();
            alert(`Album "${albums[albumIndex].title}" published and status changed to Pending.`);
        } else if (e.target.classList.contains('delete')) {
            if (confirm(`Are you sure you want to delete "${albums[albumIndex].title}"?`)) {
                albums.splice(albumIndex, 1);
                localStorage.setItem('albums', JSON.stringify(albums));
                renderTable();
                alert('Album deleted successfully.');
            }
        } else if (e.target.classList.contains('edit')) {
            localStorage.setItem('editAlbumIndex', albumIndex);
            window.location.href = 'artist_edit_album.html';
        }
    });

    // Initial render
    renderTable();
});