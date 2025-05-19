package vn.edu.usth.msma.ui.screen.search.albums

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
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.AlbumRepository
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import vn.edu.usth.msma.utils.helpers.toAlbum
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository
) : ViewModel() {
    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private val _songs = MutableStateFlow<List<SongResponse>>(emptyList())
    val songs: StateFlow<List<SongResponse>> = _songs.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAlbum(album: Album) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAlbumApi().getAlbum(album.id)
                if (response.isSuccessful && response.body() != null) {
                    val albumData = response.body()!!
                    _album.value = albumData.toAlbum()
                    albumRepository.updateAlbum(albumData.toAlbum())
                    fetchAlbumSongs(album.id)
                } else {
                    _album.value = album
                }
            } catch (e: Exception) {
                Log.e("AlbumViewModel", "Error loading playlist: ${e.message}", e)
                _album.value = album
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAlbumSongs(albumId: Long) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAlbumApi().getAlbumSongs(albumId)
                if (response.isSuccessful && response.body() != null) {
                    val songData = response.body()!!
                    songRepository.updateSongResponseList(songData)
                    _songs.value = songData
                } else {
                    _songs.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AlbumViewModel", "Error loading songs: ${e.message}", e)
                _songs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkSavedStatus(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAlbumApi().checkSavedAlbum(album.id)
                if (response.isSuccessful && response.body() != null) {
                    _isSaved.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("AlbumViewModel", "Error checking follow status: ${e.message}", e)
            }
        }
    }

    fun savingAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAlbumApi().saveArtistAlbum(album.id)
                if (response.isSuccessful) {
                    _isSaved.value = true
                    EventBus.publish(Event.SavingAlbumEvent)
                    refreshAlbum(album)
                }
            } catch (e: Exception) {
                Log.e("AlbumViewModel", "Error following artist: ${e.message}", e)
            }
        }
    }

    fun unSavingAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAlbumApi().unSaveArtistAlbum(album.id)
                if (response.isSuccessful) {
                    _isSaved.value = false
                    EventBus.publish(Event.UnSavingAlbumEvent)
                }
            } catch (e: Exception) {
                Log.e("AlbumViewModel", "Error unfollowing artist: ${e.message}", e)
            }
        }
    }

    fun refreshAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAlbumApi().getAlbum(album.id)
                if (response.isSuccessful && response.body() != null) {
                    val albumData = response.body()!!
                    _album.value = albumData.toAlbum()
                    albumRepository.updateAlbum(albumData.toAlbum())
                    fetchAlbumSongs(album.id)
                }
            } catch (e: Exception) {
                Log.e("AlbumViewModel", "Error refreshing artist: ${e.message}", e)
            }
        }
    }
}