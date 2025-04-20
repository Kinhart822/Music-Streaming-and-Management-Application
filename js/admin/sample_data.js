// document.addEventListener('DOMContentLoaded', () => {
//     // Sample user data
//     const users = [
//         { id: '1', role: 'user', username: 'MusicFan99' },
//         { id: '2', role: 'artist', username: 'ArtistOne' },
//         { id: '3', role: 'artist', username: 'ArtistTwo' },
//         { id: '4', role: 'user', username: 'User123' }
//     ];
//
//     // Sample song data
//     const songs = [
//         {
//             id: 's1',
//             type: 'Song',
//             purposeUpload: 'Add Song',
//             title: 'Midnight Drive',
//             duration: '3:45',
//             uploadDate: '15/04/2025',
//             uploadedBy: 'ArtistOne',
//             description: 'A smooth electronic track perfect for night drives.',
//             additionalArtists: 'DJ Night',
//             status: 'Pending',
//             image: 'https://via.placeholder.com/50'
//         },
//         {
//             id: 's2',
//             type: 'Song',
//             purposeUpload: 'Edit Song',
//             title: 'Summer Breeze',
//             duration: '4:20',
//             uploadDate: '14/04/2025',
//             uploadedBy: 'ArtistTwo',
//             description: 'An uplifting pop song capturing the essence of summer.',
//             additionalArtists: 'None',
//             status: 'Approved',
//             image: 'https://via.placeholder.com/50'
//         }
//     ];
//
//     // Sample playlist data
//     const playlists = [
//         {
//             id: 'p1',
//             type: 'Playlist',
//             purposeUpload: 'Add Playlist',
//             title: 'Summer Vibes',
//             duration: '45:00',
//             uploadDate: '13/04/2025',
//             uploadedBy: 'MusicFan99',
//             description: 'A collection of summer-themed tracks.',
//             additionalArtists: 'Various',
//             status: 'Pending',
//             image: 'https://via.placeholder.com/50'
//         }
//     ];
//
//     // Sample album data
//     const albums = [
//         {
//             id: 'a1',
//             type: 'Album',
//             purposeUpload: 'Add Album',
//             title: 'Echoes',
//             duration: '60:00',
//             uploadDate: '12/04/2025',
//             uploadedBy: 'ArtistOne',
//             description: 'A conceptual album exploring themes of memory.',
//             additionalArtists: 'None',
//             status: 'Declined',
//             image: 'https://via.placeholder.com/50'
//         }
//     ];
//
//     // Sample user profile data
//     const userProfile = {
//         firstName: 'John',
//         lastName: 'Doe',
//         description: ['Admin for the music platform.', 'Passionate about discovering new music.'],
//         gender: 'Male',
//         dob: '1990-01-01',
//         phone: '1234567890',
//         avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80',
//         background: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?ixlib=rb-4.0.3&auto=format&fit=crop&w=1350&q=80'
//     };
//
//     // Store data in localStorage if not already present
//     if (!localStorage.getItem('users')) {
//         localStorage.setItem('users', JSON.stringify(users));
//     }
//     if (!localStorage.getItem('songs')) {
//         localStorage.setItem('songs', JSON.stringify(songs));
//     }
//     if (!localStorage.getItem('playlists')) {
//         localStorage.setItem('playlists', JSON.stringify(playlists));
//     }
//     if (!localStorage.getItem('albums')) {
//         localStorage.setItem('albums', JSON.stringify(albums));
//     }
//     if (!localStorage.getItem('userProfile')) {
//         localStorage.setItem('userProfile', JSON.stringify(userProfile));
//     }
// });