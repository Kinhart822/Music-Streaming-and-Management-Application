package vn.edu.usth.msma.ui.screen.home.songs

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
import vn.edu.usth.msma.utils.helpers.toSong
import javax.inject.Inject

@HiltViewModel
class Top15DownloadedViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository
) : ViewModel() {
    private val _top15DownloadSongs = MutableStateFlow<List<Song>>(emptyList())
    val top15DownloadSongs: StateFlow<List<Song>> = _top15DownloadSongs.asStateFlow()

    private val _isTop15Loading = MutableStateFlow(false)
    val isTop15Loading: StateFlow<Boolean> = _isTop15Loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadTop15DownloadSongs()
    }

    private fun loadTop15DownloadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isTop15Loading.value = true
                _errorMessage.value = null
                val response = apiService.getHomeApi().getTop15DownloadedSongs()
                if (response.isSuccessful) {
                    val songs = response.body()?.mapNotNull { it.toSong() } ?: emptyList()
                    _top15DownloadSongs.value = songs
                    songRepository.updateSongs(songs)
                    Log.d("Top15DownloadedViewModel", "Loaded ${songs.size} download songs")
                } else {
                    Log.e("Top15DownloadedViewModel", "Failed to load download songs: ${response.code()}")
                    _top15DownloadSongs.value = emptyList()
                    _errorMessage.value = "Failed to load download songs: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("Top15DownloadedViewModel", "Error loading download songs", e)
                _top15DownloadSongs.value = emptyList()
                _errorMessage.value = "Error loading download songs: ${e.message}"
            } finally {
                _isTop15Loading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}