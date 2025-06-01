import {fetchWithRefresh} from "../refresh.js";
import {showNotification} from "../notification.js";
import {showConfirmModal} from "../confirmation.js";

document.addEventListener('DOMContentLoaded', () => {
    // CommonStatus mapping
    const CommonStatus = {
        LOCKED: {value: -4, label: 'Locked', cssClass: 'locked'},
        DELETED: {value: -3, label: 'Deleted', cssClass: 'deleted'},
        INACTIVE: {value: -1, label: 'Inactive', cssClass: 'inactive'},
        ACTIVE: {value: 1, label: 'Active', cssClass: 'active'}
    };

    // DOM elements
    const userTableBody = document.getElementById('user-table-body');
    const filterRoleSelect = document.getElementById('filter-role');
    const filterStatusSelect = document.getElementById('filter-status');
    const addUserBtn = document.getElementById('add-user-btn');
    const addUserForm = document.getElementById('add-user-form');
    const userForm = document.getElementById('user-form');
    const cancelAddUserBtn = document.getElementById('cancel-add-user');
    const emailInput = document.getElementById('user-email');
    const passwordInput = document.getElementById('user-password');
    const roleSelect = document.getElementById('user-role');
    const emailError = document.getElementById('email-error');
    const passwordError = document.getElementById('password-error');
    const rowsPerPageInput = document.getElementById('rows-per-page');
    const paginationDiv = document.querySelector('.pagination');
    const searchInput = document.getElementById('search-user');
    const userDetailsModal = document.getElementById('user-details-modal');
    const closeModalBtn = userDetailsModal.querySelector('.close');
    const userDetailsTable = document.getElementById('user-details-table');

    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput.value) || 10;
    let currentFilterRole = 'all';
    let currentFilterStatus = 'all';
    let currentSort = 'createdDate-desc';
    let searchQuery = '';

    // Validate email format
    const validateEmail = (email) => {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    };

    // Validate password length
    const validatePassword = (password) => {
        return password.length >= 6;
    };

    // Get status by value
    const getStatusByValue = (status) => {
        for (const key in CommonStatus) {
            if (CommonStatus[key].value === status) {
                return CommonStatus[key];
            }
        }
        return {label: 'Unknown', cssClass: ''}; // Fallback
    };

    // Fetch users from API
    const fetchUsers = async () => {
        try {
            const sortParts = currentSort.split('-');
            const orderBy = sortParts[0];
            const order = sortParts[1];

            const request = {
                page: currentPage,
                size: rowsPerPage,
                search: searchQuery || '',
                userType: currentFilterRole === 'all' ? null : currentFilterRole,
                status: currentFilterStatus === 'all' ? null : parseInt(currentFilterStatus),
                orderBy: orderBy,
                order: order
            };

            const response = await fetchWithRefresh('http://spring-music-container:8080/api/v1/search/accounts', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(request)
            });

            if (!response.ok) {
                throw new Error('Failed to fetch users');
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching users:', error);
            showNotification('Failed to load users. Please try again.', true);
            return {content: [], currentPage: 1, totalPages: 1, totalElements: 0};
        }
    };

    // Render table
    const renderTable = async () => {
        userTableBody.innerHTML = '<tr><td colspan="4"><div class="spinner"></div></td></tr>';
        const data = await fetchUsers();
        const users = data.content || [];
        const totalPages = data.totalPages || 1;

        currentPage = data.currentPage || 1;

        userTableBody.innerHTML = users.length > 0
            ? users.map(user => {
                const userType = user.userType || 'USER';
                const status = getStatusByValue(user.status);
                const isValidForAction = (userType === 'ADMIN' && status.value === CommonStatus.LOCKED.value) ||
                    (userType === 'ARTIST' && status.value === CommonStatus.INACTIVE.value);
                return `
                    <tr>
                        <td class="truncate">${user.email}</td>
                        <td>${userType.charAt(0).toUpperCase() + userType.slice(1).toLowerCase()}</td>
                        <td class="status ${status.cssClass}">${status.label}</td>
                        <td>
                            <button class="view" data-user='${JSON.stringify(user)}'>View</button>
                            ${isValidForAction ? `
                                <button class="accept" data-id="${user.id}">Accept</button>
                                <button class="revoke" data-id="${user.id}">Revoke</button>
                            ` : ''}
                            <button class="delete" data-id="${user.id}">Delete</button>
                        </td>
                    </tr>
                `;
            }).join('')
            : '<tr><td colspan="4"><span class="no-users">No users found.</span></td></tr>';

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

    // Show user details in modal
    const showUserDetails = (user) => {
        const userType = user.userType || 'USER';
        const status = getStatusByValue(user.status);

        userDetailsTable.innerHTML = `
            <tr><td>Full Name</td><td>${`${user.firstName || ''} ${user.lastName || ''}`.trim() || 'N/A'}</td></tr>
            <tr><td>Email</td><td>${user.email || 'N/A'}</td></tr>
            <tr><td>User Type</td><td>${userType.charAt(0).toUpperCase() + userType.slice(1).toLowerCase()}</td></tr>
            ${userType === 'ARTIST' ? `<tr><td>Artist Name</td><td>${user.artistName || 'N/A'}</td></tr>` : ''}
            ${userType === 'ARTIST' ? `<tr><td>Description</td><td>${user.description || 'N/A'}</td></tr>` : ''}
            <tr><td>Gender</td><td>${user.gender || 'N/A'}</td></tr>
            <tr><td>Birth Day</td><td>${user.birthDay || 'N/A'}</td></tr>
            <tr><td>Phone</td><td>${user.phone || 'N/A'}</td></tr>
            <tr><td>Status</td><td class="status ${status.cssClass}">${status.label}</td></tr>
            <tr><td>Created Date</td><td>${user.createdDate || 'N/A'}</td></tr>
            <tr><td>Last Modified Date</td><td>${user.lastModifiedDate || 'N/A'}</td></tr>
        `;

        const avatarImg = document.getElementById('user-avatar');
        if (user.avatar) {
            avatarImg.src = user.avatar;
            avatarImg.style.display = 'block';
        } else {
            avatarImg.style.display = 'none';
        }

        userDetailsModal.style.display = 'flex';
    };

    // Close modal
    closeModalBtn.addEventListener('click', () => {
        userDetailsModal.style.display = 'none';
    });

    // Close modal when clicking outside
    window.addEventListener('click', (e) => {
        if (e.target === userDetailsModal) {
            userDetailsModal.style.display = 'none';
        }
    });

    // Search input handler
    searchInput.addEventListener('input', () => {
        searchQuery = searchInput.value.trim();
        currentPage = 1;
        renderTable();
    });

    // Filter by role
    filterRoleSelect.addEventListener('change', () => {
        currentFilterRole = filterRoleSelect.value;
        currentPage = 1;
        renderTable();
    });

    // Filter by status
    filterStatusSelect.addEventListener('change', () => {
        currentFilterStatus = filterStatusSelect.value;
        currentPage = 1;
        renderTable();
    });

    // Toggle add user form
    addUserBtn.addEventListener('click', () => {
        addUserForm.classList.toggle('show-form');
        emailInput.value = '';
        passwordInput.value = '';
        roleSelect.value = 'USER';
        emailError.style.display = 'none';
        passwordError.style.display = 'none';
    });

    cancelAddUserBtn.addEventListener('click', () => {
        addUserForm.classList.remove('show-form');
    });

    // Add user via API
    userForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const email = emailInput.value.trim();
        const password = passwordInput.value.trim();
        const role = roleSelect.value;

        emailError.style.display = 'none';
        passwordError.style.display = 'none';

        if (!validateEmail(email)) {
            emailError.textContent = 'Please enter a valid email';
            emailError.style.display = 'block';
            return;
        }

        if (!validatePassword(password)) {
            passwordError.textContent = 'Password must be at least 6 characters';
            passwordError.style.display = 'block';
            return;
        }

        const requestBody = {email, password};

        let apiEndpoint;
        switch (role) {
            case 'USER':
                apiEndpoint = 'http://spring-music-container:8080/api/v1/admin/manage/createUser';
                break;
            case 'ADMIN':
                apiEndpoint = 'http://spring-music-container:8080/api/v1/admin/manage/createAdmin';
                break;
            case 'ARTIST':
                apiEndpoint = 'http://spring-music-container:8080/api/v1/admin/manage/createArtist';
                break;
            default:
                showNotification('Invalid role selected', true);
                return;
        }

        try {
            userForm.querySelector('button[type="submit"]').disabled = true;
            userForm.querySelector('button[type="submit"]').textContent = 'Adding...';

            const response = await fetchWithRefresh(apiEndpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorData = await response.json();
                if (errorData.code === 'USERNAME_EXISTED') {
                    emailError.textContent = 'This email is already in use';
                    emailError.style.display = 'block';
                } else {
                    throw new Error(errorData.message || 'Failed to create user');
                }
                return;
            }

            addUserForm.classList.remove('show-form');
            showNotification('User created successfully!');
            await renderTable();
        } catch (error) {
            console.error('Error creating user:', error);
            showNotification(`Failed to create user: ${error.message}`, true);
        } finally {
            userForm.querySelector('button[type="submit"]').disabled = false;
            userForm.querySelector('button[type="submit"]').textContent = 'Add User';
        }
    });

    // Handle view, accept, revoke, and delete actions
    userTableBody.addEventListener('click', async (e) => {
        if (e.target.classList.contains('view')) {
            const user = JSON.parse(e.target.dataset.user);
            showUserDetails(user);
        } else if (e.target.classList.contains('accept') || e.target.classList.contains('revoke') || e.target.classList.contains('delete')) {
            const userId = e.target.dataset.id;
            const user = JSON.parse(e.target.closest('tr').querySelector('.view').dataset.user);
            const email = user.email;
            let action, actionLabel, apiUrl, method;

            if (e.target.classList.contains('accept')) {
                action = 'ACCEPTED';
                actionLabel = 'accept';
                apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/processingDeleteRequest/${userId}?manageProcess=${action}`;
                method = 'POST';
            } else if (e.target.classList.contains('revoke')) {
                action = 'REVOKED';
                actionLabel = 'revoke';
                apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/processingDeleteRequest/${userId}?manageProcess=${action}`;
                method = 'POST';
            } else {
                actionLabel = 'delete';
                apiUrl = `http://spring-music-container:8080/api/v1/admin/manage/adminDeleteUser/${userId}`;
                method = 'DELETE';
            }

            showConfirmModal(
                `Confirm ${actionLabel.charAt(0).toUpperCase() + actionLabel.slice(1)}`,
                `Are you sure you want to ${actionLabel} the account for ${email}?`,
                async () => {
                    try {
                        e.target.disabled = true;
                        e.target.textContent = e.target.classList.contains('accept') ? 'Accepting...' :
                            e.target.classList.contains('revoke') ? 'Revoking...' : 'Deleting...';

                        const response = await fetchWithRefresh(apiUrl, {
                            method: method,
                            headers: {
                                'Content-Type': 'application/json'
                            }
                        });

                        if (!response.ok) {
                            const errorData = await response.json();
                            if (errorData.code === 'ENTITY_NOT_FOUND') {
                                throw new Error('User not found');
                            } else if (errorData.code === 'INVALID_TYPE') {
                                throw new Error('Invalid user type or process');
                            } else if (errorData.code === 'INVALID_STATUS') {
                                throw new Error('Invalid user status for this action');
                            } else {
                                throw new Error(errorData.message || `Failed to ${actionLabel} user`);
                            }
                        }
                        await response.json();
                        showNotification((actionLabel === 'delete' ? 'Delete successfully' : `User ${actionLabel}d successfully!`));
                        await renderTable();
                    } catch (error) {
                        console.error(`Error ${actionLabel}ing user:`, error);
                        showNotification(`Failed to ${actionLabel} user: ${error.message}`, true);
                    } finally {
                        e.target.disabled = false;
                        e.target.textContent = e.target.classList.contains('accept') ? 'Accept' :
                            e.target.classList.contains('revoke') ? 'Revoke' : 'Delete';
                    }
                }
            );
        }
    });

    // Update rows per page
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

    rowsPerPageInput.addEventListener('change', updateRowsPerPage);
    rowsPerPageInput.addEventListener('input', updateRowsPerPage);

    // Initial render
    renderTable();
});