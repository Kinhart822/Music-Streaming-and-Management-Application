package vn.edu.usth.msma.ui.screen.library.songs

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
class FavoriteSongsViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                EventBus.events.collect { event ->
                    when (event) {
                        is Event.SongFavouriteUpdateEvent -> {
                            Log.d("FavoriteSongsViewModel", "Song favorite update event received")
                            refreshFavoriteSongs()
                        }
                        is Event.SessionExpiredEvent -> {
                            Log.d("FavoriteSongsViewModel", "Session expired event received")
                            _favoriteSongs.value = emptyList()
                            songRepository.clearAllSongs()
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("FavoriteSongsViewModel", "Error in EventBus collection", e)
            }
        }
        loadFavoriteSongs()
    }

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var originalSongs = listOf<Song>()

    fun loadFavoriteSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("FavoriteSongsViewModel", "Starting to load favorite songs")
                _isLoading.value = true

                val response = apiService.getSongApi().getLikedSongs()
                if (response.isSuccessful) {
                    val songs = response.body()?.mapNotNull { songResponse ->
                        try {
                            songResponse.toSong()
                        } catch (e: Exception) {
                            Log.e("FavoriteSongsViewModel", "Error mapping song: $songResponse", e)
                            null
                        }
                    } ?: emptyList()
                    _favoriteSongs.value = songs
                    originalSongs = songs
                    songRepository.updateSongs(songs)
                    Log.d("FavoriteSongsViewModel", "Loaded ${songs.size} favorite songs")
                } else {
                    Log.e(
                        "FavoriteSongsViewModel",
                        "Failed to load favorite songs: ${response.code()} - ${response.message()}"
                    )
                    _favoriteSongs.value = emptyList()
                    songRepository.clearAllSongs()
                }
            } catch (e: Exception) {
                Log.e("FavoriteSongsViewModel", "Exception while loading favorite songs", e)
                _favoriteSongs.value = emptyList()
                songRepository.clearAllSongs()
            } finally {
                _isLoading.value = false
                Log.d("FavoriteSongsViewModel", "Finished loading favorite songs")
            }
        }
    }

    fun searchSongs(query: String) {
        if (query.isEmpty()) {
            _favoriteSongs.value = originalSongs
        } else {
            _favoriteSongs.value = originalSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artistNameList?.any { artist -> artist.contains(query, ignoreCase = true) } == true
            }
        }
    }

    fun applyFilter(filter: String) {
        _favoriteSongs.value = when (filter) {
            "Sort by Title" -> originalSongs.sortedBy { it.title }
            "Sort by Artist" -> originalSongs.sortedBy { it.artistNameList?.joinToString() }
            else -> originalSongs.sortedByDescending { it.id } // Recently Added
        }
    }

    fun refreshFavoriteSongs() {
        Log.d("FavoriteSongsViewModel", "Refreshing favorite songs")
        loadFavoriteSongs()
    }

    override fun onCleared() {
        Log.d("FavoriteSongsViewModel", "ViewModel cleared")
        super.onCleared()
    }
}