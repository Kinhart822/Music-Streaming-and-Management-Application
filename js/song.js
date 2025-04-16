document.addEventListener('DOMContentLoaded', () => {
    // Song form handling
    const songForm = document.getElementById('song-form');
    if (songForm) {
        const songFileInput = document.getElementById('song-file');
        const songTitleInput = document.getElementById('song-title');
        const songLyricsInput = document.getElementById('song-lyrics');
        const songImageInput = document.getElementById('song-image');
        const imagePreview = document.getElementById('image-preview');
        const downloadInputs = document.querySelectorAll('input[name="download"]');
        const songDescriptionInput = document.getElementById('song-description');
        const songGenreInput = document.getElementById('song-genre');

        // Preview song image when selected
        songImageInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    imagePreview.src = event.target.result;
                    imagePreview.style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        });

        // Handle song form submission
        songForm.addEventListener('submit', (e) => {
            e.preventDefault();

            // Validate inputs
            const songFile = songFileInput.files[0];
            const songTitle = songTitleInput.value.trim();
            const songLyrics = songLyricsInput.value.trim();
            const songImage = songImageInput.files[0];
            const downloadPermission = document.querySelector('input[name="download"]:checked')?.value;
            const songDescription = songDescriptionInput.value.trim();
            const songGenre = songGenreInput.value;

            if (!songFile || !songTitle) {
                alert('Please provide an MP3 file and a title.');
                return;
            }

            if (!downloadPermission) {
                alert('Please select a download permission option.');
                return;
            }

            if (songFile.type !== 'audio/mpeg') {
                alert('Please upload a valid MP3 file.');
                return;
            }

            // Prepare song data (exclude file and image to avoid quota issues)
            const songData = {
                title: songTitle,
                lyrics: songLyrics,
                download: downloadPermission,
                description: songDescription,
                genre: songGenre,
                uploadDate: new Date().toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' }),
                status: 'Draft',
                duration: '3:00' // Placeholder duration
            };

            // Save song metadata to localStorage
            const saveSong = () => {
                let songs = JSON.parse(localStorage.getItem('songs')) || [];
                songs.push(songData);
                try {
                    localStorage.setItem('songs', JSON.stringify(songs));
                    alert('Song uploaded successfully!');
                    window.location.href = 'artist_manage_song.html';
                } catch (e) {
                    alert('Failed to save song: Storage quota exceeded. Please clear some data.');
                }
            };

            saveSong();
        });
    }

    // Populate song table and handle publish/delete
    const songTableBody = document.getElementById('song-table-body');
    if (songTableBody) {
        const songs = JSON.parse(localStorage.getItem('songs')) || [];

        const renderTable = () => {
            songTableBody.innerHTML = songs.map((song, index) => `
                <tr>
                    <td>${song.title}</td>
                    <td>${song.genre}</td>
                    <td>${song.duration}</td>
                    <td>${song.uploadDate}</td>
                    <td class="truncate">${song.lyrics || 'No lyrics'}</td>
                    <td class="truncate">${song.description || 'No description'}</td>
                    <td>${song.download}</td>
                    <td class="${song.status.toLowerCase()}">${song.status}</td>
                    <td>
                        ${
                            song.status === 'Draft' || song.status === 'Declined'
                                ? `
                                    <span>
                                        ${
                                            song.status === 'Draft'
                                                ? `<i class="ri-upload-line publish" data-index="${index}" title="Publish"></i>`
                                                : ''
                                        }
                                        <i class="ri-edit-line edit" data-index="${index}" title="Edit"></i>
                                        <i class="ri-delete-bin-line delete" data-index="${index}" title="Delete"></i>
                                    </span>
                                `
                                : ''
                        }
                    </td>
                </tr>
            `).join('');
        };

        renderTable();

        // Delegate event handling for publish and delete
        songTableBody.addEventListener('click', (e) => {
            const index = e.target.dataset.index;
            if (index === undefined) return;

            if (e.target.classList.contains('publish')) {
                songs[index].status = 'Pending';
                localStorage.setItem('songs', JSON.stringify(songs));
                renderTable();
                alert(`Song "${songs[index].title}" published and status changed to Pending.`);
            } else if (e.target.classList.contains('delete')) {
                if (confirm(`Are you sure you want to delete "${songs[index].title}"?`)) {
                    songs.splice(index, 1);
                    localStorage.setItem('songs', JSON.stringify(songs));
                    renderTable();
                    alert('Song deleted successfully.');
                }
            } else if (e.target.classList.contains('edit')) {
                alert('Edit functionality not implemented yet.');
            }
        });
    }
});