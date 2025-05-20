package vn.edu.usth.msma.ui.screen.home

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
import vn.edu.usth.msma.data.dto.response.management.HistoryListenResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.AlbumRepository
import vn.edu.usth.msma.repository.ArtistRepository
import vn.edu.usth.msma.repository.PlaylistRepository
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.utils.helpers.toAlbum
import vn.edu.usth.msma.utils.helpers.toArtist
import vn.edu.usth.msma.utils.helpers.toPlaylist
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository
) : ViewModel() {
    private val _recentPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val recentPlaylists: StateFlow<List<Playlist>> = _recentPlaylists.asStateFlow()

    private val _recentAlbums = MutableStateFlow<List<Album>>(emptyList())
    val recentAlbums: StateFlow<List<Album>> = _recentAlbums.asStateFlow()

    private val _recentArtists = MutableStateFlow<List<Artist>>(emptyList())
    val recentArtists: StateFlow<List<Artist>> = _recentArtists.asStateFlow()

    private val _recentSongs = MutableStateFlow<List<HistoryListenResponse>>(emptyList())
    val recentSongs: StateFlow<List<HistoryListenResponse>> = _recentSongs.asStateFlow()

    private val _isPlaylistsLoading = MutableStateFlow(false)
    val isPlaylistsLoading: StateFlow<Boolean> = _isPlaylistsLoading.asStateFlow()

    private val _isAlbumsLoading = MutableStateFlow(false)
    val isAlbumsLoading: StateFlow<Boolean> = _isAlbumsLoading.asStateFlow()

    private val _isArtistsLoading = MutableStateFlow(false)
    val isArtistsLoading: StateFlow<Boolean> = _isArtistsLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSongsLoading = MutableStateFlow(false)
    val isSongsLoading: StateFlow<Boolean> = _isSongsLoading.asStateFlow()

    init {
        loadRecentPlaylists()
        loadRecentAlbums()
        loadRecentFollowedArtists()
        loadRecentSongs()
    }

    fun refreshData(){
        loadRecentPlaylists()
        loadRecentAlbums()
        loadRecentFollowedArtists()
        loadRecentSongs()
    }

    private fun loadRecentPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isPlaylistsLoading.value = true
                _errorMessage.value = null
                val response = apiService.getHomeApi().getRecentPlaylists()
                if (response.isSuccessful) {
                    val playlists = response.body()?.mapNotNull { it.toPlaylist() } ?: emptyList()
                    _recentPlaylists.value = playlists
                    playlistRepository.updatePlaylists(playlists)
                    Log.d("HomeViewModel", "Loaded ${playlists.size} recent playlists")
                } else {
                    Log.e("HomeViewModel", "Failed to load recent playlists: ${response.code()}")
                    _recentPlaylists.value = emptyList()
                    _errorMessage.value = "Failed to load recent playlists: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading recent playlists", e)
                _recentPlaylists.value = emptyList()
                _errorMessage.value = "Error loading recent playlists: ${e.message}"
            } finally {
                _isPlaylistsLoading.value = false
            }
        }
    }

    private fun loadRecentAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isAlbumsLoading.value = true
                _errorMessage.value = null
                val response = apiService.getHomeApi().getRecentAlbums()
                if (response.isSuccessful) {
                    val albums = response.body()?.mapNotNull { it.toAlbum() } ?: emptyList()
                    _recentAlbums.value = albums
                    albumRepository.updateAlbums(albums)
                    Log.d("HomeViewModel", "Loaded ${albums.size} recent albums")
                } else {
                    Log.e("HomeViewModel", "Failed to load recent albums: ${response.code()}")
                    _recentAlbums.value = emptyList()
                    _errorMessage.value = "Failed to load recent albums: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading recent albums", e)
                _recentAlbums.value = emptyList()
                _errorMessage.value = "Error loading recent albums: ${e.message}"
            } finally {
                _isAlbumsLoading.value = false
            }
        }
    }

    private fun loadRecentFollowedArtists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isArtistsLoading.value = true
                _errorMessage.value = null
                val response = apiService.getHomeApi().getRecentFollowedArtistsOfUser()
                if (response.isSuccessful) {
                    val artists = response.body()?.mapNotNull { it.toArtist() } ?: emptyList()
                    _recentArtists.value = artists
                    artistRepository.updateArtists(artists)
                    Log.d("HomeViewModel", "Loaded ${artists.size} recent artists")
                } else {
                    Log.e("HomeViewModel", "Failed to load recent artists: ${response.code()}")
                    _recentArtists.value = emptyList()
                    _errorMessage.value = "Failed to load recent artists: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading recent artists", e)
                _recentArtists.value = emptyList()
                _errorMessage.value = "Error loading recent artists: ${e.message}"
            } finally {
                _isArtistsLoading.value = false
            }
        }
    }

    private fun loadRecentSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSongsLoading.value = true
                _errorMessage.value = null
                val response = apiService.getHomeApi().recentListening()
                if (response.isSuccessful) {
                    val songs = response.body() ?: emptyList()
                    songRepository.updateHistoryListenResponseList(songs)
                    _recentSongs.value = songs
                    Log.d("HomeViewModel", "Loaded ${songs.size} recent songs")
                } else {
                    Log.e("HomeViewModel", "Failed to load recent songs: ${response.code()}")
                    _recentSongs.value = emptyList()
                    _errorMessage.value = "Failed to load recent songs: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading recent songs", e)
                _recentSongs.value = emptyList()
                _errorMessage.value = "Error loading recent songs: ${e.message}"
            } finally {
                _isSongsLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}