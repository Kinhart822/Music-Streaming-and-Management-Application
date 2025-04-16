document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('edit-playlist-form');
    const titleInput = document.getElementById('playlist-title');
    const descriptionTextarea = document.getElementById('playlist-description');
    const imageInput = document.getElementById('playlist-image');
    const imagePreview = document.getElementById('image-preview');
    const songListDiv = document.getElementById('song-list');
    const deleteButton = document.getElementById('delete-playlist');
    const titleError = document.getElementById('title-error');

    // Load playlist data
    const playlistIndex = localStorage.getItem('editPlaylistIndex');
    let playlists = JSON.parse(localStorage.getItem('playlists')) || [];
    if (playlistIndex === null || isNaN(playlistIndex) || playlistIndex < 0 || playlistIndex >= playlists.length) {
        alert('Invalid playlist selected.');
        window.location.href = 'artist_manage_playlist.html';
        return;
    }

    const playlist = playlists[playlistIndex];
    titleInput.value = playlist.title || '';
    descriptionTextarea.value = playlist.description || '';
    if (playlist.image) {
        imagePreview.src = playlist.image;
        imagePreview.style.display = 'block';
    }

    // Load songs
    const songs = JSON.parse(localStorage.getItem('songs')) || [];
    songListDiv.innerHTML = songs.length > 0
        ? songs.map(song => `
            <label>
                <input type="checkbox" name="songs" value="${song.title}"
                    ${playlist.songs && playlist.songs.includes(song.title) ? 'checked' : ''}>
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
            imagePreview.src = playlist.image || '';
            imagePreview.style.display = playlist.image ? 'block' : 'none';
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

            // Calculate time length (assuming songs have duration)
            let totalSeconds = 0;
            selectedSongs.forEach(songTitle => {
                const song = songs.find(s => s.title === songTitle);
                if (song && song.duration) {
                    const [minutes, seconds] = song.duration.split(':').map(Number);
                    totalSeconds += (minutes * 60) + seconds;
                }
            });
            const timeLength = `${Math.floor(totalSeconds / 60)}:${(totalSeconds % 60).toString().padStart(2, '0')}`;

            // Update playlist
            playlists[playlistIndex] = {
                ...playlists[playlistIndex],
                title: titleInput.value.trim(),
                description: descriptionTextarea.value.trim() || '',
                image: imageInput.files[0] ? URL.createObjectURL(imageInput.files[0]) : playlist.image || '',
                songs: selectedSongs,
                timeLength: timeLength,
                releaseDate: playlist.releaseDate || new Date().toLocaleDateString('en-GB'),
                status: playlist.status || 'Draft'
            };

            localStorage.setItem('playlists', JSON.stringify(playlists));
            localStorage.removeItem('editPlaylistIndex');
            alert('Playlist updated successfully.');
            window.location.href = 'artist_manage_playlist.html';
        }
    });

    // Delete playlist
    deleteButton.addEventListener('click', () => {
        if (confirm(`Are you sure you want to delete "${playlists[playlistIndex].title}"? This action cannot be undone.`)) {
            playlists.splice(playlistIndex, 1);
            localStorage.setItem('playlists', JSON.stringify(playlists));
            localStorage.removeItem('editPlaylistIndex');
            alert('Playlist deleted successfully.');
            window.location.href = 'artist_manage_playlist.html';
        }
    });
});