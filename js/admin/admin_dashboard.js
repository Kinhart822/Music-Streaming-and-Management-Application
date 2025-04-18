document.addEventListener('DOMContentLoaded', () => {
    // Load data from localStorage
    const users = JSON.parse(localStorage.getItem('users')) || [];
    const songs = JSON.parse(localStorage.getItem('songs')) || [];
    const playlists = JSON.parse(localStorage.getItem('playlists')) || [];
    const albums = JSON.parse(localStorage.getItem('albums')) || [];

    // Update overview cards
    document.getElementById('total-listeners').textContent = users.filter(u => u.role === 'listener').length;
    document.getElementById('total-artists').textContent = users.filter(u => u.role === 'artist').length;
    document.getElementById('total-songs').textContent = songs.length;
    document.getElementById('total-playlists').textContent = playlists.length;
    document.getElementById('total-albums').textContent = albums.length;
    document.getElementById('pending-songs').textContent = songs.filter(s => s.status === 'Pending').length;
    document.getElementById('pending-playlists').textContent = playlists.filter(p => p.status === 'Pending').length;
    document.getElementById('pending-albums').textContent = albums.filter(a => a.status === 'Pending').length;

    // Populate pending content table
    const pendingTableBody = document.getElementById('pending-table-body');
    const pendingItems = [
        ...songs.map(s => ({ ...s, type: 'Song' })),
        ...playlists.map(p => ({ ...p, type: 'Playlist' })),
        ...albums.map(a => ({ ...a, type: 'Album' }))
    ].filter(item => item.status === 'Pending');

    pendingTableBody.innerHTML = pendingItems.length > 0
        ? pendingItems.slice(0, 5).map((item, index) => `
                    <tr>
                        <td class="truncate">${item.title || 'Untitled'}</td>
                        <td>${item.type}</td>
                        <td>${item.uploadedBy || 'Unknown'}</td>
                        <td>${item.uploadDate || item.releaseDate || 'Unknown'}</td>
                        <td class="pending">${item.status}</td>
                        <td>
                            <button class="approve" data-type="${item.type.toLowerCase()}" data-index="${index}">Approve</button>
                            <button class="reject" data-type="${item.type.toLowerCase()}" data-index="${index}">Reject</button>
                        </td>
                    </tr>
                `).join('')
        : '<tr><td colspan="6">No pending content available.</td></tr>';

    // Handle approve/reject actions
    pendingTableBody.addEventListener('click', (e) => {
        const button = e.target;
        if (!button.matches('button')) return;

        const type = button.dataset.type;
        const index = parseInt(button.dataset.index);
        const item = pendingItems[index];

        let array, key;
        if (type === 'song') {
            array = songs;
            key = 'songs';
        } else if (type === 'playlist') {
            array = playlists;
            key = 'playlists';
        } else if (type === 'album') {
            array = albums;
            key = 'albums';
        }

        const arrayIndex = array.findIndex(a => a.title === item.title && a.uploadedBy === item.uploadedBy);
        if (arrayIndex === -1) {
            alert('Error: Item not found.');
            return;
        }

        if (button.classList.contains('approve')) {
            array[arrayIndex].status = 'Approved';
            alert(`${item.type} "${item.title}" has been approved.`);
        } else if (button.classList.contains('reject')) {
            array[arrayIndex].status = 'Declined';
            alert(`${item.type} "${item.title}" has been rejected.`);
        }

        localStorage.setItem(key, JSON.stringify(array));

        // Reload data
        document.getElementById('pending-songs').textContent = songs.filter(s => s.status === 'Pending').length;
        document.getElementById('pending-playlists').textContent = playlists.filter(p => p.status === 'Pending').length;
        document.getElementById('pending-albums').textContent = albums.filter(a => a.status === 'Pending').length;

        // Refresh table
        const newPendingItems = [
            ...songs.map(s => ({ ...s, type: 'Song' })),
            ...playlists.map(p => ({ ...p, type: 'Playlist' })),
            ...albums.map(a => ({ ...a, type: 'Album' }))
        ].filter(item => item.status === 'Pending');

        pendingTableBody.innerHTML = newPendingItems.length > 0
            ? newPendingItems.slice(0, 5).map((item, index) => `
                        <tr>
                            <td class="truncate">${item.title || 'Untitled'}</td>
                            <td>${item.type}</td>
                            <td>${item.uploadedBy || 'Unknown'}</td>
                            <td>${item.uploadDate || item.releaseDate || 'Unknown'}</td>
                            <td class="pending">${item.status}</td>
                            <td>
                                <button class="approve" data-type="${item.type.toLowerCase()}" data-index="${index}">Approve</button>
                                <button class="reject" data-type="${item.type.toLowerCase()}" data-index="${index}">Reject</button>
                            </td>
                        </tr>
                    `).join('')
            : '<tr><td colspan="6">No pending content available.</td></tr>';
    });
});