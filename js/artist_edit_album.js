document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('edit-album-form');
    const titleInput = document.getElementById('album-title');
    const descriptionTextarea = document.getElementById('album-description');
    const imageInput = document.getElementById('album-image');
    const imagePreview = document.getElementById('image-preview');
    const songListDiv = document.getElementById('song-list');
    const deleteButton = document.getElementById('delete-album');
    const titleError = document.getElementById('title-error');

    // Load album data
    const albumIndex = localStorage.getItem('editAlbumIndex');
    let albums = JSON.parse(localStorage.getItem('albums')) || [];
    if (albumIndex === null || isNaN(albumIndex) || albumIndex < 0 || albumIndex >= albums.length) {
        alert('Invalid album selected.');
        window.location.href = 'artist_manage_album.html';
        return;
    }

    const album = albums[albumIndex];
    titleInput.value = album.title || '';
    descriptionTextarea.value = album.description || '';
    if (album.image) {
        imagePreview.src = album.image;
        imagePreview.style.display = 'block';
    }

    // Load songs
    const songs = JSON.parse(localStorage.getItem('songs')) || [];
    songListDiv.innerHTML = songs.length > 0
        ? songs.map(song => `
            <label>
                <input type="checkbox" name="songs" value="${song.title}"
                    ${album.songs && album.songs.includes(song.title) ? 'checked' : ''}>
                ${song.title}
            </label>
        `).join('')
        : '<p>No songs available. Please add songs first.</p>';

    // Image preview
    imageInput.addEventListener('change', () => {
        const file = imageInput.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                imagePreview.src = e.target.result;
                imagePreview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        } else {
            imagePreview.src = album.image || '';
            imagePreview.style.display = album.image ? 'block' : 'none';
        }
    });

    // Form submission
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        let isValid = true;

        // Reset errors
        titleError.style.display = 'none';

        // Validate inputs
        if (!titleInput.value.trim()) {
            titleError.style.display = 'block';
            isValid = false;
        }

        if (isValid) {
            // Get selected songs
            const selectedSongs = Array.from(document.querySelectorAll('input[name="songs"]:checked'))
                .map(input => input.value);

            // Calculate time length
            let totalSeconds = 0;
            selectedSongs.forEach(songTitle => {
                const song = songs.find(s => s.title === songTitle);
                if (song && song.duration) {
                    const [minutes, seconds] = song.duration.split(':').map(Number);
                    totalSeconds += (minutes * 60) + seconds;
                }
            });
            const timeLength = `${Math.floor(totalSeconds / 60)}:${(totalSeconds % 60).toString().padStart(2, '0')}`;

            // Update album
            albums[albumIndex] = {
                ...albums[albumIndex],
                title: titleInput.value.trim(),
                description: descriptionTextarea.value.trim() || '',
                image: imageInput.files[0] ? URL.createObjectURL(imageInput.files[0]) : album.image || '',
                songs: selectedSongs,
                timeLength: timeLength,
                releaseDate: album.releaseDate || new Date().toLocaleDateString('en-GB'),
                status: album.status || 'Draft'
            };

            localStorage.setItem('albums', JSON.stringify(albums));
            localStorage.removeItem('editAlbumIndex');
            alert('Album updated successfully.');
            window.location.href = 'artist_manage_album.html';
        }
    });

    // Delete album
    deleteButton.addEventListener('click', () => {
        if (confirm(`Are you sure you want to delete "${albums[albumIndex].title}"? This action cannot be undone.`)) {
            albums.splice(albumIndex, 1);
            localStorage.setItem('albums', JSON.stringify(albums));
            localStorage.removeItem('editAlbumIndex');
            alert('Album deleted successfully.');
            window.location.href = 'artist_manage_album.html';
        }
    });
});