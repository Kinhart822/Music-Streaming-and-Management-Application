import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";
import {showConfirmModal} from "../confirmation.js";

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const genreTableBody = document.getElementById('genre-table-body');
    const sortBySelect = document.getElementById('sort-by');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-content');
    const addGenreBtn = document.getElementById('add-genre-btn');
    const genreFormContainer = document.getElementById('add-genre-form');
    const genreForm = document.getElementById('genre-form');
    const formTitle = document.getElementById('form-title');
    const cancelAddGenreBtn = document.getElementById('cancel-add-genre');
    const nameInput = document.getElementById('genre-name');
    const briefDescriptionInput = document.getElementById('genre-brief-description');
    const fullDescriptionTextarea = document.getElementById('genre-full-description');
    const imageInput = document.getElementById('genre-image');
    const imagePreview = document.getElementById('image-preview');
    const currentImageDiv = document.getElementById('current-image');
    const nameError = document.getElementById('name-error');
    const briefDescriptionError = document.getElementById('brief-description-error');
    const fullDescriptionError = document.getElementById('full-description-error');
    const imageError = document.getElementById('image-error');
    const contentModal = document.getElementById('content-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalContentBody = document.getElementById('modal-content-body');
    const closeModal = document.getElementById('close-modal');

    // State
    let genres = [];
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput?.value) || 10;
    let currentSort = 'name-asc';
    let searchQuery = '';
    let totalPages = 1;
    let totalElements = 0;
    let editId = null;

    // Utility Functions
    const debounce = (func, delay) => {
        let timeoutId;
        return (...args) => {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => func(...args), delay);
        };
    };

    const mapSortToApi = (sort) => {
        switch (sort) {
            case 'name-asc':
                return {orderBy: 'genresName', order: 'asc'};
            case 'name-desc':
                return {orderBy: 'genresName', order: 'desc'};
            case 'date-asc':
                return {orderBy: 'createdDate', order: 'asc'};
            case 'date-desc':
                return {orderBy: 'createdDate', order: 'desc'};
            default:
                return {orderBy: 'genresName', order: 'asc'};
        }
    };

    // API Functions
    const fetchGenres = async () => {
        try {
            genreTableBody.innerHTML = '<tr><td colspan="6"><div class="spinner"></div></td></tr>';
            const {orderBy, order} = mapSortToApi(currentSort);
            const requestBody = {
                page: currentPage,
                size: rowsPerPage,
                orderBy,
                order,
                search: searchQuery
            };

            const response = await fetchWithRefresh('http://localhost:8080/api/v1/search/genres', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to fetch genres: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            if (!data || !Array.isArray(data.content)) {
                throw new Error('Invalid API response: content is not an array');
            }

            genres = data.content.map(genre => ({
                id: genre.id,
                name: genre.name || 'Unknown',
                briefDescription: genre.briefDescription || '',
                fullDescription: genre.fullDescription || '',
                imageUrl: genre.imageUrl || '',
                createdDate: genre.createdDate || ''
            }));
            currentPage = data.currentPage || 1;
            totalPages = data.totalPages || 1;
            totalElements = data.totalElements || 0;

            renderTable();
        } catch (error) {
            console.error('Error fetching genres:', error);
            showNotification('Failed to load genres. Please try again.', true);
            genreTableBody.innerHTML = `<tr><td colspan="6"><span class="no-genres">Unable to load genres. Please try again later or contact support.</span></td></tr>`;
            paginationDiv.innerHTML = '';
        }
    };

    const createGenre = async (formData) => {
        try {
            const response = await fetchWithRefresh('http://localhost:8080/api/v1/admin/genre/create', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to create genre: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to create genre: ${error.message}`);
        }
    };

    const updateGenre = async (id, formData) => {
        try {
            const response = await fetchWithRefresh(`http://localhost:8080/api/v1/admin/genre/update/${id}`, {
                method: 'PUT',
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to update genre: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(`Failed to update genre: ${error.message}`);
        }
    };

    const deleteGenre = async (id) => {
        try {
            const response = await fetchWithRefresh(`http://localhost:8080/api/v1/admin/genre/delete/${id}`, {
                method: 'DELETE',
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Failed to delete genre: ${response.status} - ${errorText}`);
            }
        } catch (error) {
            throw new Error(`Failed to delete genre: ${error.message}`);
        }
    };

    // UI Functions
    const resetForm = (mode = 'add') => {
        if (!genreForm || !formTitle || !nameInput || !briefDescriptionInput || !fullDescriptionTextarea || !imageInput || !imagePreview || !currentImageDiv) return;

        genreForm.dataset.mode = mode;
        formTitle.textContent = mode === 'add' ? 'Add Genre' : 'Edit Genre';
        nameInput.value = '';
        briefDescriptionInput.value = '';
        fullDescriptionTextarea.value = '';
        imageInput.value = '';
        imagePreview.src = '';
        imagePreview.style.display = 'none';
        currentImageDiv.textContent = '';
        nameError.style.display = 'none';
        briefDescriptionError.style.display = 'none';
        fullDescriptionError.style.display = 'none';
        imageError.style.display = 'none';
        imageInput.required = mode === 'add';
        editId = null;
    };

    const populateEditForm = (id) => {
        const genre = genres.find(g => g.id === id);
        if (!genre) {
            showNotification('Error: Genre not found.', true);
            return;
        }

        try {
            genreForm.dataset.mode = 'edit';
            formTitle.textContent = 'Edit Genre';
            nameInput.value = genre.name || '';
            briefDescriptionInput.value = genre.briefDescription || '';
            fullDescriptionTextarea.value = genre.fullDescription || '';
            currentImageDiv.textContent = genre.imageUrl ? `Current image: ${genre.imageUrl}` : '';
            imagePreview.src = genre.imageUrl || '';
            imagePreview.style.display = genre.imageUrl ? 'block' : 'none';
            imageInput.required = false;
            nameError.style.display = 'none';
            briefDescriptionError.style.display = 'none';
            fullDescriptionError.style.display = 'none';
            imageError.style.display = 'none';
            editId = id;
            genreFormContainer.classList.add('show-form');
        } catch (error) {
            console.error('Error in populateEditForm:', error);
            showNotification('Failed to load edit form.', true);
        }
    };

    const renderTable = () => {
        if (!genreTableBody || !paginationDiv) return;

        genreTableBody.innerHTML = genres.length > 0
            ? genres.map(genre => `
                <tr>
                    <td class="image">
                        ${genre.imageUrl
                ? `<img src="${genre.imageUrl}" alt="${genre.name}" style="width: 50px; height: 50px; object-fit: cover;">`
                : `<span>No image</span>`
            }
                    </td>
                    <td>${genre.name || 'Unknown'}</td>
                    <td>
                        ${genre.briefDescription
                ? `<a class="view-brief-description" href="#" data-id="${genre.id}" title="View Brief Description">Show more...</a>`
                : 'None'
            }
                    </td>
                    <td>
                        ${genre.fullDescription
                ? `<a class="view-full-description" href="#" data-id="${genre.id}" title="View Full Description">Show more...</a>`
                : 'None'
            }
                    </td>
                    <td>${genre.createdDate || 'Unknown'}</td>
                    <td>
                        <button class="edit" data-id="${genre.id}" title="Edit">Edit</button>
                        <button class="delete" data-id="${genre.id}" title="Delete">Delete</button>
                    </td>
                </tr>
            `).join('')
            : '<tr><td colspan="6"><span class="no-genres">No genres found.</span></td></tr>';

        paginationDiv.innerHTML = '';
        if (totalPages > 1) {
            const prevButton = document.createElement('button');
            prevButton.textContent = 'Previous';
            prevButton.disabled = currentPage === 1;
            prevButton.addEventListener('click', () => {
                if (currentPage > 1) {
                    currentPage--;
                    fetchGenres();
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
                    fetchGenres();
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
                    fetchGenres();
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
                    fetchGenres();
                });
                paginationDiv.appendChild(lastPage);
            }

            const nextButton = document.createElement('button');
            nextButton.textContent = 'Next';
            nextButton.disabled = currentPage === totalPages;
            nextButton.addEventListener('click', () => {
                if (currentPage < totalPages) {
                    currentPage++;
                    fetchGenres();
                }
            });
            paginationDiv.appendChild(nextButton);
        }
    };

    // Event Listeners
    if (imageInput) {
        imageInput.addEventListener('change', () => {
            const file = imageInput.files[0];
            const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp', 'image/tiff', 'image/svg+xml'];
            if (file) {
                if (!validTypes.includes(file.type)) {
                    imageError.textContent = 'Only JPG, PNG, GIF, WEBP, BMP, TIFF, or SVG files are allowed';
                    imageError.style.display = 'block';
                    imagePreview.style.display = 'none';
                    imagePreview.src = '';
                    currentImageDiv.textContent = '';
                } else if (file.size > 5 * 1024 * 1024) {
                    imageError.textContent = 'Image file size exceeds 5MB';
                    imageError.style.display = 'block';
                    imagePreview.style.display = 'none';
                    imagePreview.src = '';
                    currentImageDiv.textContent = '';
                } else {
                    imagePreview.src = URL.createObjectURL(file);
                    imagePreview.style.display = 'block';
                    currentImageDiv.textContent = `Selected: ${file.name}`;
                    imageError.style.display = 'none';
                }
            } else {
                imagePreview.style.display = 'none';
                imagePreview.src = '';
                currentImageDiv.textContent = '';
                imageError.style.display = 'none';
            }
        });
    }

    if (addGenreBtn) {
        addGenreBtn.addEventListener('click', () => {
            resetForm('add');
            genreFormContainer.classList.add('show-form');
        });
    }

    if (cancelAddGenreBtn) {
        cancelAddGenreBtn.addEventListener('click', () => {
            genreFormContainer.classList.remove('show-form');
            resetForm('add');
        });
    }

    if (sortBySelect) {
        sortBySelect.addEventListener('change', () => {
            currentSort = sortBySelect.value;
            currentPage = 1;
            fetchGenres();
        });
    }

    if (rowsPerPageInput) {
        rowsPerPageInput.addEventListener('input', debounce(() => {
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
            fetchGenres();
        }, 300));
    }

    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => {
            searchQuery = searchInput.value.trim();
            currentPage = 1;
            fetchGenres();
        }, 300));
    }

    if (genreForm) {
        genreForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            let isValid = true;

            if (nameError) nameError.style.display = 'none';
            if (briefDescriptionError) briefDescriptionError.style.display = 'none';
            if (fullDescriptionError) fullDescriptionError.style.display = 'none';
            if (imageError) imageError.style.display = 'none';

            const name = nameInput.value.trim();
            const briefDescription = briefDescriptionInput.value.trim();
            const fullDescription = fullDescriptionTextarea.value.trim();
            const image = imageInput.files[0];

            if (!name) {
                nameError.textContent = 'Please enter a valid name';
                nameError.style.display = 'block';
                isValid = false;
            }

            if (!briefDescription) {
                briefDescriptionError.textContent = 'Please enter a brief description';
                briefDescriptionError.style.display = 'block';
                isValid = false;
            }

            if (!fullDescription) {
                fullDescriptionError.textContent = 'Please enter a full description';
                fullDescriptionError.style.display = 'block';
                isValid = false;
            }

            if (genreForm.dataset.mode === 'add' && !image) {
                imageError.textContent = 'Please select an image file';
                imageError.style.display = 'block';
                isValid = false;
            } else if (image) {
                const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp', 'image/tiff', 'image/svg+xml'];
                if (!validTypes.includes(image.type)) {
                    imageError.textContent = 'Only JPG, PNG, GIF, WEBP, BMP, TIFF, or SVG files are allowed';
                    imageError.style.display = 'block';
                    isValid = false;
                } else if (image.size > 5 * 1024 * 1024) {
                    imageError.textContent = 'Image file size exceeds 5MB';
                    imageError.style.display = 'block';
                    isValid = false;
                }
            }

            if (isValid) {
                const formData = new FormData();
                formData.append('name', name);
                formData.append('briefDescription', briefDescription);
                formData.append('fullDescription', fullDescription);
                if (image) formData.append('image', image);

                const saveButton = genreForm.querySelector('button[type="submit"]');
                if (saveButton) {
                    saveButton.disabled = true;
                    saveButton.classList.add('loading');
                    saveButton.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Saving...';
                }

                try {
                    if (genreForm.dataset.mode === 'edit' && editId) {
                        await updateGenre(editId, formData);
                        showNotification(`Genre "${name}" updated successfully.`);
                    } else {
                        await createGenre(formData);
                        showNotification(`Genre "${name}" created successfully.`);
                    }
                    genreFormContainer.classList.remove('show-form');
                    resetForm('add');
                    currentPage = 1;
                    await fetchGenres();
                } catch (error) {
                    showNotification(`Failed to ${genreForm.dataset.mode === 'edit' ? 'update' : 'create'} genre: ${error.message}`, true);
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

    if (genreTableBody) {
        genreTableBody.addEventListener('click', async (e) => {
            const id = e.target.dataset.id;
            if (!id || !e.target.matches('.edit, .delete, .view-brief-description, .view-full-description')) return;

            e.preventDefault();
            const genre = genres.find(g => g.id === Number(id));
            if (!genre) {
                showNotification('Error: Genre not found.', true);
                return;
            }

            if (e.target.matches('.view-brief-description')) {
                modalTitle.textContent = `Brief Description of ${genre.name}`;
                modalContentBody.innerHTML = `
                    <textarea class="lyrics-content" readonly placeholder="No description available">${genre.briefDescription || ''}</textarea>
                `;
                contentModal.style.display = 'flex';
            } else if (e.target.matches('.view-full-description')) {
                modalTitle.textContent = `Full Description of ${genre.name}`;
                modalContentBody.innerHTML = `
                    <textarea class="lyrics-content" readonly placeholder="No description available">${genre.fullDescription || ''}</textarea>
                `;
                contentModal.style.display = 'flex';
            } else if (e.target.matches('.edit')) {
                populateEditForm(Number(id));
            } else if (e.target.matches('.delete')) {
                showConfirmModal(
                    'Confirm Delete',
                    `Are you sure you want to delete "${genre.name}"?`,
                    async () => {
                        try {
                            e.target.disabled = true;
                            e.target.innerHTML = '<i class="fa fa-refresh fa-spin"></i> Deleting';
                            await deleteGenre(Number(id));
                            showNotification(`Genre "${genre.name}" deleted successfully.`);
                            currentPage = 1;
                            await fetchGenres();
                        } catch (error) {
                            showNotification(`Failed to delete genre: ${error.message}`, true);
                        } finally {
                            e.target.disabled = false;
                            e.target.innerHTML = 'Delete';
                        }
                    }
                );
            }
        });
    }

    if (closeModal && contentModal) {
        closeModal.addEventListener('click', () => {
            contentModal.style.display = 'none';
            modalContentBody.innerHTML = '';
        });
    }

    if (contentModal) {
        window.addEventListener('click', (e) => {
            if (e.target === contentModal) {
                contentModal.style.display = 'none';
                modalContentBody.innerHTML = '';
            }
        });
    }

    // Initialize
    fetchGenres().catch(error => {
        console.error('Initialization error:', error);
        showNotification('Failed to initialize. Please try again.', true);
    });
});