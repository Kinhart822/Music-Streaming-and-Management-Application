package vn.edu.usth.msma.ui.screen.library.albums

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.usth.msma.data.Song
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor() : ViewModel() {
    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAlbum(albumId: String) {
//        _isLoading.value = true
//        // Simulate API call with hardcoded data
//        val album = when (albumId) {
//            "album1" -> Album(
//                id = "album1",
//                name = "Album One",
//                artistName = "Artist One",
//                imageUrl = "",
//                songs = listOf(
//                    Song(id = "1", title = "Song One", artistNameList = listOf("Artist One"), imageUrl = ""),
//                    Song(id = "2", title = "Song Two", artistNameList = listOf("Artist One"), imageUrl = "")
//                )
//            )
//            "album2" -> Album(
//                id = "album2",
//                name = "Album Two",
//                artistName = "Artist Two",
//                imageUrl = "",
//                songs = listOf(
//                    Song(id = "3", title = "Song Three", artistNameList = listOf("Artist Two"), imageUrl = "")
//                )
//            )
//            else -> null
//        }
//        _album.value = album
//        _songs.value = album?.songs ?: emptyList()
//        _isLoading.value = false
    }

    fun searchSongs(query: String) {
        val albumSongs = _album.value?.songs ?: emptyList()
        _songs.value = if (query.isEmpty()) {
            albumSongs
        } else {
            albumSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artistNameList?.any { artist -> artist.contains(query, ignoreCase = true) } == true
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