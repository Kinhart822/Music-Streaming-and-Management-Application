package vn.edu.usth.msma.ui.screen.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.Album
import vn.edu.usth.msma.data.Artist
import vn.edu.usth.msma.data.Playlist
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.AlbumRepository
import vn.edu.usth.msma.repository.ArtistRepository
import vn.edu.usth.msma.repository.PlaylistRepository
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import vn.edu.usth.msma.utils.helpers.toAlbum
import vn.edu.usth.msma.utils.helpers.toArtist
import vn.edu.usth.msma.utils.helpers.toPlaylist
import vn.edu.usth.msma.utils.helpers.toSong
import javax.inject.Inject

// Sealed class for library items
sealed class LibraryItem {
    data class SongData(val song: Song) : LibraryItem()
    data class ArtistData(val artist: Artist) : LibraryItem()
    data class PlaylistData(val playlist: Playlist) : LibraryItem()
    data class AlbumData(val album: Album) : LibraryItem()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    private val _followedArtists = MutableStateFlow<List<Artist>>(emptyList())
    val followedArtists: StateFlow<List<Artist>> = _followedArtists.asStateFlow()

    private val _savedPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val savedPlaylists: StateFlow<List<Playlist>> = _savedPlaylists.asStateFlow()

    private val _savedAlbums = MutableStateFlow<List<Album>>(emptyList())
    val savedAlbums: StateFlow<List<Album>> = _savedAlbums.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LibraryItem>>(emptyList())
    val searchResults: StateFlow<List<LibraryItem>> = _searchResults.asStateFlow()

    private val _isSongsLoading = MutableStateFlow(false)
    val isSongsLoading: StateFlow<Boolean> = _isSongsLoading.asStateFlow()

    private val _isArtistsLoading = MutableStateFlow(false)
    val isArtistsLoading: StateFlow<Boolean> = _isArtistsLoading.asStateFlow()

    private val _isPlaylistsLoading = MutableStateFlow(false)
    val isPlaylistsLoading: StateFlow<Boolean> = _isPlaylistsLoading.asStateFlow()

    private val _isAlbumsLoading = MutableStateFlow(false)
    val isAlbumsLoading: StateFlow<Boolean> = _isAlbumsLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Initialize data on ViewModel creation
        loadFavoriteSongs()
        loadFollowedArtists()
        loadSavedPlaylists()
        loadSavedAlbums()

        // Collect EventBus events
        viewModelScope.launch {
            try {
                EventBus.events.collect { event ->
                    when (event) {
                        is Event.ProfileUpdatedEvent -> {
                            Log.d("LibraryViewModel", "Profile updated")
                        }
                        is Event.SongFavouriteUpdateEvent -> {
                            Log.d("LibraryViewModel", "Song favorite updated")
                            refreshFavoriteSongs()
                        }
                        is Event.InitializeDataLibrary -> {
                            Log.d("LibraryViewModel", "Initializing library data")
                            loadFavoriteSongs()
                            loadFollowedArtists()
                            loadSavedPlaylists()
                            loadSavedAlbums()
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error collecting EventBus events", e)
            }
        }
    }

    fun loadFavoriteSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSongsLoading.value = true
                _errorMessage.value = null

                val response = apiService.getSongApi().getLikedSongs()
                if (response.isSuccessful) {
                    val songs = response.body()?.mapNotNull { it.toSong() } ?: emptyList()
                    _favoriteSongs.value = songs
                    songRepository.updateSongs(songs)
                    Log.d("LibraryViewModel", "Loaded ${songs.size} favorite songs")
                } else {
                    Log.e("LibraryViewModel", "Failed to load favorite songs: ${response.code()}")
                    _favoriteSongs.value = emptyList()
                    _errorMessage.value = "Failed to load favorite songs: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading favorite songs", e)
                _favoriteSongs.value = emptyList()
                _errorMessage.value = "Error loading favorite songs: ${e.message}"
            } finally {
                _isSongsLoading.value = false
            }
        }
    }

    fun loadFollowedArtists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isArtistsLoading.value = true
                _errorMessage.value = null

                val response = apiService.getArtistApi().getFollowedArtists()
                if (response.isSuccessful) {
                    val artists = response.body()?.mapNotNull { it.toArtist() } ?: emptyList()
                    _followedArtists.value = artists
                    artistRepository.updateArtists(artists)
                    Log.d("LibraryViewModel", "Loaded ${artists.size} followed artists")
                } else {
                    Log.e("LibraryViewModel", "Failed to load followed artists: ${response.code()}")
                    _followedArtists.value = emptyList()
                    _errorMessage.value = "Failed to load followed artists: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading followed artists", e)
                _followedArtists.value = emptyList()
                _errorMessage.value = "Error loading followed artists: ${e.message}"
            } finally {
                _isArtistsLoading.value = false
            }
        }
    }

    fun loadSavedPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isPlaylistsLoading.value = true
                _errorMessage.value = null

                val response = apiService.getPlaylistApi().getAllUserSavedPlaylists()
                if (response.isSuccessful) {
                    val playlists = response.body()?.mapNotNull { it.toPlaylist() } ?: emptyList()
                    _savedPlaylists.value = playlists
                    playlistRepository.updatePlaylists(playlists)
                    Log.d("LibraryViewModel", "Loaded ${playlists.size} saved playlists")
                } else {
                    Log.e("LibraryViewModel", "Failed to load saved playlists: ${response.code()}")
                    _savedPlaylists.value = emptyList()
                    _errorMessage.value = "Failed to load saved playlists: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading saved playlists", e)
                _savedPlaylists.value = emptyList()
                _errorMessage.value = "Error loading saved playlists: ${e.message}"
            } finally {
                _isPlaylistsLoading.value = false
            }
        }
    }

    fun loadSavedAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isAlbumsLoading.value = true
                _errorMessage.value = null

                val response = apiService.getAlbumApi().getAllUserSavedAlbums()
                if (response.isSuccessful) {
                    val albums = response.body()?.mapNotNull { it.toAlbum() } ?: emptyList()
                    _savedAlbums.value = albums
                    albumRepository.updateAlbums(albums)
                    Log.d("LibraryViewModel", "Loaded ${albums.size} saved albums")
                } else {
                    Log.e("LibraryViewModel", "Failed to load saved albums: ${response.code()}")
                    _savedAlbums.value = emptyList()
                    _errorMessage.value = "Failed to load saved albums: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading saved albums", e)
                _savedAlbums.value = emptyList()
                _errorMessage.value = "Error loading saved albums: ${e.message}"
            } finally {
                _isAlbumsLoading.value = false
            }
        }
    }

    fun searchContent(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<LibraryItem>()
            // Search songs
            results.addAll(
                _favoriteSongs.value
                    .filter {
                        it.title.contains(query, ignoreCase = true) ||
                                it.artistNameList?.any { artist -> artist.contains(query, ignoreCase = true) } == true
                    }
                    .map { LibraryItem.SongData(it) }
            )
            // Search artists
            results.addAll(
                _followedArtists.value
                    .filter { it.artistName?.contains(query, ignoreCase = true) ?: false }
                    .map { LibraryItem.ArtistData(it) }
            )
            // Search playlists
            results.addAll(
                _savedPlaylists.value
                    .filter {
                        it.playlistName.contains(query, ignoreCase = true) ||
                                it.artistNameList?.any { artist -> artist.contains(query, ignoreCase = true) } == true
                    }
                    .map { LibraryItem.PlaylistData(it) }
            )
            // Search albums
            results.addAll(
                _savedAlbums.value
                    .filter {
                        it.albumName.contains(query, ignoreCase = true) ||
                                it.artistNameList?.any { artist -> artist.contains(query, ignoreCase = true) } == true
                    }
                    .map { LibraryItem.AlbumData(it) }
            )

            _searchResults.value = results
        }
    }

    fun applyFilter(filter: String) {
        viewModelScope.launch {
            val currentResults = _searchResults.value
            _searchResults.value = when (filter) {
                "Sort by Title" -> currentResults.sortedWith { item1, item2 ->
                    val name1 = when (item1) {
                        is LibraryItem.SongData -> item1.song.title
                        is LibraryItem.ArtistData -> item1.artist.artistName
                        is LibraryItem.PlaylistData -> item1.playlist.playlistName
                        is LibraryItem.AlbumData -> item1.album.albumName
                    } ?: ""
                    val name2 = when (item2) {
                        is LibraryItem.SongData -> item2.song.title
                        is LibraryItem.ArtistData -> item2.artist.artistName
                        is LibraryItem.PlaylistData -> item2.playlist.playlistName
                        is LibraryItem.AlbumData -> item2.album.albumName
                    } ?: ""
                    name1.compareTo(name2, ignoreCase = true)
                }
                "Sort by Artist" -> currentResults.sortedWith { item1, item2 ->
                    val artist1 = when (item1) {
                        is LibraryItem.SongData -> item1.song.artistNameList?.joinToString() ?: ""
                        is LibraryItem.ArtistData -> item1.artist.artistName
                        is LibraryItem.PlaylistData -> item1.playlist.artistNameList?.joinToString() ?: ""
                        is LibraryItem.AlbumData -> item1.album.artistNameList?.joinToString() ?: ""
                    } ?: ""
                    val artist2 = when (item2) {
                        is LibraryItem.SongData -> item2.song.artistNameList?.joinToString() ?: ""
                        is LibraryItem.ArtistData -> item2.artist.artistName
                        is LibraryItem.PlaylistData -> item2.playlist.artistNameList?.joinToString() ?: ""
                        is LibraryItem.AlbumData -> item2.album.artistNameList?.joinToString() ?: ""
                    } ?: ""
                    artist1.compareTo(artist2, ignoreCase = true)
                }
                else -> currentResults
            }
        }
    }

    fun refreshFavoriteSongs() {
        Log.d("LibraryViewModel", "Refreshing favorite songs")
        loadFavoriteSongs()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        Log.d("LibraryViewModel", "ViewModel cleared")
        super.onCleared()
    }
}