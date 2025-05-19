package vn.edu.usth.msma.ui.screen.search.playlists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.Playlist
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.PlaylistRepository
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import vn.edu.usth.msma.utils.helpers.toPlaylist
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _songs = MutableStateFlow<List<SongResponse>>(emptyList())
    val songs: StateFlow<List<SongResponse>> = _songs.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPlaylist(playlist: Playlist) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlaylistApi().getPlaylist(playlist.id)
                if (response.isSuccessful && response.body() != null) {
                    val playlistData = response.body()!!
                    _playlist.value = playlistData.toPlaylist()
                    playlistRepository.updatePlaylist(playlistData.toPlaylist())
                    fetchPlaylistSongs(playlist.id)
                } else {
                    _playlist.value = playlist
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error loading playlist: ${e.message}", e)
                _playlist.value = playlist
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchPlaylistSongs(playlistId: Long) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlaylistApi().getPlaylistSongs(playlistId)
                if (response.isSuccessful && response.body() != null) {
                    val songData = response.body()!!
                    songRepository.updateSongResponseList(songData)
                    _songs.value = songData
                } else {
                    _songs.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error loading songs: ${e.message}", e)
                _songs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkSavedStatus(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlaylistApi().checkSavedPlaylist(playlist.id)
                if (response.isSuccessful && response.body() != null) {
                    _isSaved.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error checking follow status: ${e.message}", e)
            }
        }
    }

    fun savingPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlaylistApi().saveArtistPlaylist(playlist.id)
                if (response.isSuccessful) {
                    _isSaved.value = true
                    EventBus.publish(Event.SavingPlaylistEvent)
                    // Refresh artist data in the background
                    refreshPlaylist(playlist)
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error following artist: ${e.message}", e)
            }
        }
    }

    fun unSavingPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlaylistApi().unSaveArtistPlaylist(playlist.id)
                if (response.isSuccessful) {
                    _isSaved.value = false
                    EventBus.publish(Event.UnSavingPlaylistEvent)
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error unfollowing artist: ${e.message}", e)
            }
        }
    }

    fun refreshPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlaylistApi().getPlaylist(playlist.id)
                if (response.isSuccessful && response.body() != null) {
                    val playlistData = response.body()!!
                    _playlist.value = playlistData.toPlaylist()
                    playlistRepository.updatePlaylist(playlistData.toPlaylist())
                    fetchPlaylistSongs(playlist.id)
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error refreshing artist: ${e.message}", e)
            }
        }
    }
}