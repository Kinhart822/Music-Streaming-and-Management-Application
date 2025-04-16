document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('edit-song-form');
    const fileInput = document.getElementById('song-file');
    const titleInput = document.getElementById('song-title');
    const lyricsTextarea = document.getElementById('song-lyrics');
    const imageInput = document.getElementById('song-image');
    const imagePreview = document.getElementById('image-preview');
    const downloadYesInput = document.querySelector('input[name="download"][value="Yes"]');
    const downloadNoInput = document.querySelector('input[name="download"][value="No"]');
    const descriptionTextarea = document.getElementById('song-description');
    const genreSelect = document.getElementById('song-genre');
    const deleteButton = document.getElementById('delete-song');

    const fileError = document.getElementById('file-error');
    const titleError = document.getElementById('title-error');
    const downloadError = document.getElementById('download-error');

    // Load song data
    const songIndex = localStorage.getItem('editSongIndex');
    let songs = JSON.parse(localStorage.getItem('songs')) || [];
    if (songIndex === null || isNaN(songIndex) || songIndex < 0 || songIndex >= songs.length) {
        alert('Invalid song selected.');
        window.location.href = 'artist_manage_song.html';
        return;
    }

    const song = songs[songIndex];
    titleInput.value = song.title || '';
    lyricsTextarea.value = song.lyrics || '';
    descriptionTextarea.value = song.description || '';
    genreSelect.value = song.genre || '';
    if (song.download === 'Yes') {
        downloadYesInput.checked = true;
    } else {
        downloadNoInput.checked = true;
    }
    if (song.image) {
        imagePreview.src = song.image;
        imagePreview.style.display = 'block';
    }

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
            imagePreview.style.display = 'none';
        }
    });

    // Form submission
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        let isValid = true;

        // Reset errors
        fileError.style.display = 'none';
        titleError.style.display = 'none';
        downloadError.style.display = 'none';

        // Validate inputs
        if (!titleInput.value.trim()) {
            titleError.style.display = 'block';
            isValid = false;
        }
        if (!downloadYesInput.checked && !downloadNoInput.checked) {
            downloadError.style.display = 'block';
            isValid = false;
        }

        if (isValid) {
            // Update song
            const updatedSong = {
                title: titleInput.value.trim(),
                lyrics: lyricsTextarea.value.trim() || '',
                description: descriptionTextarea.value.trim() || '',
                genre: genreSelect.value || '',
                download: downloadYesInput.checked ? 'Yes' : 'No',
                status: song.status || 'Draft',
                listeners: song.listeners || 0,
                uploadDate: song.uploadDate || new Date().toLocaleDateString('en-GB'),
                file: fileInput.files[0] ? fileInput.files[0].name : song.file || '',
                image: imageInput.files[0] ? URL.createObjectURL(imageInput.files[0]) : song.image || ''
            };

            songs[songIndex] = updatedSong;
            localStorage.setItem('songs', JSON.stringify(songs));
            localStorage.removeItem('editSongIndex');
            alert('Song updated successfully.');
            window.location.href = 'artist_manage_song.html';
        }
    });

    // Delete song
    deleteButton.addEventListener('click', () => {
        if (confirm(`Are you sure you want to delete "${songs[songIndex].title}"? This action cannot be undone.`)) {
            songs.splice(songIndex, 1);
            localStorage.setItem('songs', JSON.stringify(songs));
            localStorage.removeItem('editSongIndex');
            alert('Song deleted successfully.');
            window.location.href = 'artist_manage_song.html';
        }
    });
});