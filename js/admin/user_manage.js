document.addEventListener('DOMContentLoaded', () => {
    const userTableBody = document.getElementById('user-table-body');
    const filterRoleSelect = document.getElementById('filter-role');
    const sortBySelect = document.getElementById('sort-by');
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

    let users = JSON.parse(localStorage.getItem('users')) || [];
    let currentPage = 1;
    let rowsPerPage = parseInt(rowsPerPageInput.value) || 10;
    let currentFilter = 'all';
    let currentSort = 'joinDate-asc';

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
        // Filter users
        let filteredUsers = users.filter(user => {
            if (currentFilter === 'all') return true;
            return user.role === currentFilter;
        });

        // Sort users
        filteredUsers = [...filteredUsers].sort((a, b) => {
            if (currentSort === 'joinDate-asc') {
                return parseDate(a.joinDate) - parseDate(b.joinDate);
            } else if (currentSort === 'joinDate-desc') {
                return parseDate(b.joinDate) - parseDate(a.joinDate);
            }
            return 0;
        });

        // Paginate
        const totalPages = Math.ceil(filteredUsers.length / rowsPerPage);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (totalPages === 0) {
            currentPage = 1;
        }
        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;
        const paginatedUsers = filteredUsers.slice(start, end);

        // Render table
        userTableBody.innerHTML = paginatedUsers.length > 0
            ? paginatedUsers.map((user, index) => {
                const globalIndex = users.findIndex(u => u.email === user.email);
                return `
                            <tr>
                                <td class="truncate">${user.username || 'Unknown'}</td>
                                <td class="truncate">${user.email}</td>
                                <td>${user.role.charAt(0).toUpperCase() + user.role.slice(1)}</td>
                                <td>${user.joinDate || 'Unknown'}</td>
                                <td>
                                    <button class="delete" data-index="${globalIndex}">Delete</button>
                                </td>
                            </tr>
                        `;
            }).join('')
            : '<tr><td colspan="5">No users found.</td></tr>';

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

    // Filter by role
    filterRoleSelect.addEventListener('change', () => {
        currentFilter = filterRoleSelect.value;
        currentPage = 1;
        renderTable();
    });

    // Sort by
    sortBySelect.addEventListener('change', () => {
        currentSort = sortBySelect.value;
        currentPage = 1;
        renderTable();
    });

    // Toggle add user form
    addUserBtn.addEventListener('click', () => {
        addUserForm.classList.toggle('active');
        emailInput.value = '';
        passwordInput.value = '';
        roleSelect.value = 'artist';
        emailError.style.display = 'none';
        passwordError.style.display = 'none';
    });

    cancelAddUserBtn.addEventListener('click', () => {
        addUserForm.classList.remove('active');
    });

    // Add user
    userForm.addEventListener('submit', (e) => {
        e.preventDefault();
        let isValid = true;

        // Reset errors
        emailError.style.display = 'none';
        passwordError.style.display = 'none';

        // Validate email
        const email = emailInput.value.trim();
        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            emailError.style.display = 'block';
            isValid = false;
        } else if (users.some(user => user.email === email)) {
            emailError.textContent = 'Email already exists';
            emailError.style.display = 'block';
            isValid = false;
        }

        // Validate password
        const password = passwordInput.value.trim();
        if (password.length < 6) {
            passwordError.style.display = 'block';
            isValid = false;
        }

        if (isValid) {
            const username = email.split('@')[0];
            const newUser = {
                username,
                email,
                password,
                role: roleSelect.value,
                joinDate: new Date().toLocaleDateString('en-GB')
            };
            users.push(newUser);
            localStorage.setItem('users', JSON.stringify(users));
            addUserForm.classList.remove('active');
            renderTable();
            alert(`User "${username}" added successfully as ${roleSelect.value}.`);
        }
    });

    // Delete user
    userTableBody.addEventListener('click', (e) => {
        if (e.target.classList.contains('delete')) {
            const index = parseInt(e.target.dataset.index);
            const user = users[index];
            if (user.role === 'admin') {
                alert('Cannot delete admin account.');
                return;
            }
            if (confirm(`Are you sure you want to delete user "${user.username}"?`)) {
                users.splice(index, 1);
                localStorage.setItem('users', JSON.stringify(users));
                renderTable();
                alert(`User "${user.username}" deleted successfully.`);
            }
        }
    });

    // Rows per page event handlers
    rowsPerPageInput.addEventListener('change', updateRowsPerPage);
    rowsPerPageInput.addEventListener('input', updateRowsPerPage);

    // Initial render
    renderTable();
});