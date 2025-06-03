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
class Top10TrendingViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _top10TrendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val top10TrendingSongs: StateFlow<List<Song>> = _top10TrendingSongs.asStateFlow()

    private val _isTop10Loading = MutableStateFlow(false)
    val isTop10Loading: StateFlow<Boolean> = _isTop10Loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadTop10TrendingSongs()
    }

    private fun loadTop10TrendingSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isTop10Loading.value = true
                _errorMessage.value = null
                val response = apiService.getHomeApi().getTop10TrendingSongs()
                if (response.isSuccessful) {
                    val songs = response.body()?.mapNotNull { it.toSong() } ?: emptyList()
                    _top10TrendingSongs.value = songs
                    songRepository.updateSongs(songs)
                    Log.d("Top10TrendingViewModel", "Loaded ${songs.size} trending songs")
                } else {
                    Log.e("Top10TrendingViewModel", "Failed to load trending songs: ${response.code()}")
                    _top10TrendingSongs.value = emptyList()
                    _errorMessage.value = "Failed to load trending songs: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("Top10TrendingViewModel", "Error loading trending songs", e)
                _top10TrendingSongs.value = emptyList()
                _errorMessage.value = "Error loading trending songs: ${e.message}"
            } finally {
                _isTop10Loading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}