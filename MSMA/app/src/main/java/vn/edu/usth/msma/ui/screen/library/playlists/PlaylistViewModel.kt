package vn.edu.usth.msma.ui.screen.library.playlists

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.usth.msma.data.Song
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor() : ViewModel() {
    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPlaylist(playlistId: String) {
//        _isLoading.value = true
//        // Simulate API call with hardcoded data
//        val playlist = when (playlistId) {
//            "playlist1" -> Playlist(
//                id = "playlist1",
//                name = "Chill Hits",
//                imageUrl = "",
//                songs = listOf(
//                    Song(id = "1", title = "Song One", artistNameList = listOf("Artist One"), imageUrl = ""),
//                    Song(id = "2", title = "Song Two", artistNameList = listOf("Artist One"), imageUrl = "")
//                )
//            )
//            "playlist2" -> Playlist(
//                id = "playlist2",
//                name = "Pop Favorites",
//                imageUrl = "",
//                songs = listOf(
//                    Song(id = "3", title = "Song Three", artistNameList = listOf("Artist Two"), imageUrl = "")
//                )
//            )
//            else -> null
//        }
//        _playlist.value = playlist
//        _songs.value = playlist?.songs ?: emptyList()
//        _isLoading.value = false
    }

    fun searchSongs(query: String) {
        val playlistSongs = _playlist.value?.songs ?: emptyList()
        _songs.value = if (query.isEmpty()) {
            playlistSongs
        } else {
            playlistSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artistNameList?.any { artist ->
                            artist.contains(
                                query,
                                ignoreCase = true
                            )
                        } == true
            }
        }
    }

    fun applyFilter(filter: String) {
        val currentSongs = _songs.value
        _songs.value = when (filter) {
            "Sort by Title" -> currentSongs.sortedBy { it.title }
            "Sort by Artist" -> currentSongs.sortedBy { it.artistNameList?.joinToString() }
            else -> currentSongs
        }
    }
}