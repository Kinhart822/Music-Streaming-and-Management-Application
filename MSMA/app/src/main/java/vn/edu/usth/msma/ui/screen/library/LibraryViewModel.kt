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

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository
) : ViewModel() {

    init {
        // Listen to EventBus events
        viewModelScope.launch {
            try {
                EventBus.events.collect { event ->
                    when (event) {
                        is Event.ProfileUpdatedEvent -> {
                            Log.d("LibraryViewModel", "Profile updated event received")
                            // Handle profile update if needed
                        }
                        is Event.SessionExpiredEvent -> {
                            Log.d("LibraryViewModel", "Session expired event received")
                            _favoriteSongs.value = emptyList()
                            songRepository.clearAllSongs()
                        }
                        is Event.SongFavouriteUpdateEvent -> {
                            Log.d("LibraryViewModel", "Song favorite update event received")
                            refreshFavoriteSongs()
                        }
                        is Event.InitializeDataLibrary -> {
                            Log.d("LibraryViewModel", "Initialize Song Favorite Data")
                            loadFavoriteSongs()
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun loadFavoriteSongs() {
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
                    // Update SongRepository with the fetched songs
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
                Log.d(
                    "LibraryViewModel",
                    "Finished loading favorite songs, isLoading: ${_isLoading.value}"
                )
            }
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
