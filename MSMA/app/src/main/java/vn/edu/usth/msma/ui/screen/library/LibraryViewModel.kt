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
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import vn.edu.usth.msma.utils.helpers.toSong
import javax.inject.Inject

// Sealed class for library items
sealed class LibraryItem {
    data class SongItem(val song: Song) : LibraryItem()
    data class ArtistItem(val id: String, val name: String, val imageUrl: String) : LibraryItem()
    data class PlaylistItem(val id: String, val name: String, val imageUrl: String) : LibraryItem()
    data class AlbumItem(val id: String, val name: String, val imageUrl: String, val artistName: String) : LibraryItem()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                EventBus.events.collect { event ->
                    when (event) {
                        is Event.ProfileUpdatedEvent -> {
                            Log.d("LibraryViewModel", "Profile updated event received")
                        }
                        is Event.SessionExpiredEvent -> {
                            Log.d("LibraryViewModel", "Session expired event received")
                            _favoriteSongs.value = emptyList()
                            _artists.value = emptyList()
                            _playlists.value = emptyList()
                            _albums.value = emptyList()
                            _searchResults.value = emptyList()
                            songRepository.clearAllSongs()
                        }
                        is Event.SongFavouriteUpdateEvent -> {
                            Log.d("LibraryViewModel", "Song favorite update event received")
                            refreshFavoriteSongs()
                        }
                        is Event.InitializeDataLibrary -> {
                            Log.d("LibraryViewModel", "Initialize Library Data")
                            loadFavoriteSongs()
                            loadArtists()
                            loadPlaylists()
                            loadAlbums()
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error in EventBus collection", e)
            }
        }
    }

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    private val _artists = MutableStateFlow<List<LibraryItem.ArtistItem>>(emptyList())
    val artists: StateFlow<List<LibraryItem.ArtistItem>> = _artists.asStateFlow()

    private val _playlists = MutableStateFlow<List<LibraryItem.PlaylistItem>>(emptyList())
    val playlists: StateFlow<List<LibraryItem.PlaylistItem>> = _playlists.asStateFlow()

    private val _albums = MutableStateFlow<List<LibraryItem.AlbumItem>>(emptyList())
    val albums: StateFlow<List<LibraryItem.AlbumItem>> = _albums.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LibraryItem>>(emptyList())
    val searchResults: StateFlow<List<LibraryItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var originalSongs = listOf<Song>()
    private var originalArtists = listOf<LibraryItem.ArtistItem>()
    private var originalPlaylists = listOf<LibraryItem.PlaylistItem>()
    private var originalAlbums = listOf<LibraryItem.AlbumItem>()

    fun loadFavoriteSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("LibraryViewModel", "Starting to load favorite songs")
                _isLoading.value = true

                val response = apiService.getSongApi().getLikedSongs()
                if (response.isSuccessful) {
                    val songs = response.body()?.mapNotNull { songResponse ->
                        try {
                            songResponse.toSong()
                        } catch (e: Exception) {
                            Log.e("LibraryViewModel", "Error mapping song: $songResponse", e)
                            null
                        }
                    } ?: emptyList()
                    _favoriteSongs.value = songs
                    originalSongs = songs
                    songRepository.updateSongs(songs)
                    Log.d("LibraryViewModel", "Loaded ${songs.size} favorite songs")
                } else {
                    Log.e(
                        "LibraryViewModel",
                        "Failed to load favorite songs: ${response.code()} - ${response.message()}"
                    )
                    _favoriteSongs.value = emptyList()
                    songRepository.clearAllSongs()
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Exception while loading favorite songs", e)
                _favoriteSongs.value = emptyList()
                songRepository.clearAllSongs()
            } finally {
                _isLoading.value = false
                Log.d("LibraryViewModel", "Finished loading favorite songs")
            }
        }
    }

    fun loadArtists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                // Simulate API call with hardcoded data
                val tempArtists = listOf(
                    LibraryItem.ArtistItem("artist1", "Taylor Swift", ""),
                    LibraryItem.ArtistItem("artist2", "Ed Sheeran", ""),
                    LibraryItem.ArtistItem("artist3", "Billie Eilish", ""),
                    LibraryItem.ArtistItem("artist4", "Drake", ""),
                    LibraryItem.ArtistItem("artist5", "Beyoncé", "")
                )
                _artists.value = tempArtists
                originalArtists = tempArtists
                Log.d("LibraryViewModel", "Loaded ${tempArtists.size} artists")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading artists", e)
                _artists.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                // Simulate API call with hardcoded data
                val tempPlaylists = listOf(
                    LibraryItem.PlaylistItem("playlist1", "Chill Hits", ""),
                    LibraryItem.PlaylistItem("playlist2", "Workout Mix", ""),
                    LibraryItem.PlaylistItem("playlist3", "Road Trip Songs", ""),
                    LibraryItem.PlaylistItem("playlist4", "Study Playlist", ""),
                    LibraryItem.PlaylistItem("playlist5", "Party Anthems", "")
                )
                _playlists.value = tempPlaylists
                originalPlaylists = tempPlaylists
                Log.d("LibraryViewModel", "Loaded ${tempPlaylists.size} playlists")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading playlists", e)
                _playlists.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                // Simulate API call with hardcoded data
                val tempAlbums = listOf(
                    LibraryItem.AlbumItem("album1", "Folklore", "", "Taylor Swift"),
                    LibraryItem.AlbumItem("album2", "Divide", "", "Ed Sheeran"),
                    LibraryItem.AlbumItem("album3", "When We All Fall Asleep", "", "Billie Eilish"),
                    LibraryItem.AlbumItem("album4", "Views", "", "Drake"),
                    LibraryItem.AlbumItem("album5", "Lemonade", "", "Beyoncé")
                )
                _albums.value = tempAlbums
                originalAlbums = tempAlbums
                Log.d("LibraryViewModel", "Loaded ${tempAlbums.size} albums")
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error loading albums", e)
                _albums.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchContent(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        val results = mutableListOf<LibraryItem>()
        // Search songs
        results.addAll(
            originalSongs
                .filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.artistNameList?.any { artist -> artist.contains(query, ignoreCase = true) } == true
                }
                .map { LibraryItem.SongItem(it) }
        )
        // Search artists
        results.addAll(
            originalArtists
                .filter { it.name.contains(query, ignoreCase = true) }
        )
        // Search playlists
        results.addAll(
            originalPlaylists
                .filter { it.name.contains(query, ignoreCase = true) }
        )
        // Search albums
        results.addAll(
            originalAlbums
                .filter { it.name.contains(query, ignoreCase = true) || it.artistName.contains(query, ignoreCase = true) }
        )

        _searchResults.value = results
    }

    fun applyFilter(filter: String) {
        val currentResults = _searchResults.value
        _searchResults.value = when (filter) {
            "Sort by Title" -> currentResults.sortedWith { item1, item2 ->
                val name1 = when (item1) {
                    is LibraryItem.SongItem -> item1.song.title
                    is LibraryItem.ArtistItem -> item1.name
                    is LibraryItem.PlaylistItem -> item1.name
                    is LibraryItem.AlbumItem -> item1.name
                }
                val name2 = when (item2) {
                    is LibraryItem.SongItem -> item2.song.title
                    is LibraryItem.ArtistItem -> item2.name
                    is LibraryItem.PlaylistItem -> item2.name
                    is LibraryItem.AlbumItem -> item2.name
                }
                name1.compareTo(name2, ignoreCase = true)
            }
            "Sort by Artist" -> currentResults.sortedWith { item1, item2 ->
                val artist1 = when (item1) {
                    is LibraryItem.SongItem -> item1.song.artistNameList?.joinToString() ?: ""
                    is LibraryItem.ArtistItem -> item1.name
                    is LibraryItem.PlaylistItem -> item1.name // Treat playlist name as artist for sorting
                    is LibraryItem.AlbumItem -> item1.artistName
                }
                val artist2 = when (item2) {
                    is LibraryItem.SongItem -> item2.song.artistNameList?.joinToString() ?: ""
                    is LibraryItem.ArtistItem -> item2.name
                    is LibraryItem.PlaylistItem -> item2.name
                    is LibraryItem.AlbumItem -> item2.artistName
                }
                artist1.compareTo(artist2, ignoreCase = true)
            }
            else -> currentResults // Recently Added (default order)
        }
    }

    fun refreshFavoriteSongs() {
        Log.d("LibraryViewModel", "Refreshing favorite songs")
        loadFavoriteSongs()
    }

    override fun onCleared() {
        Log.d("LibraryViewModel", "ViewModel cleared")
        super.onCleared()
    }
}