package vn.edu.usth.msma.ui.screen.library.artists

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.usth.msma.data.Song
import javax.inject.Inject

@HiltViewModel
class ArtistProfileViewModel @Inject constructor() : ViewModel() {
    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadArtist(artistId: Long) {
//        _isLoading.value = true
//        // Simulate API call with hardcoded data
//        val artist = when (artistId) {
//            "artist1" -> Artist(
//                id = "artist1",
//                name = "Artist One",
//                imageUrl = "",
//                songs = listOf(
//                    Song(id = "1", title = "Song One", artistNameList = listOf("Artist One"), imageUrl = ""),
//                    Song(id = "2", title = "Song Two", artistNameList = listOf("Artist One"), imageUrl = "")
//                )
//            )
//            "artist2" -> Artist(
//                id = "artist2",
//                name = "Artist Two",
//                imageUrl = "",
//                songs = listOf(
//                    Song(id = "3", title = "Song Three", artistNameList = listOf("Artist Two"), imageUrl = "")
//                )
//            )
//            else -> null
//        }
//        _artist.value = artist
//        _songs.value = artist?.songs ?: emptyList()
//        _isLoading.value = false
    }

    fun searchSongs(query: String) {
        val artistSongs = _artist.value?.songs ?: emptyList()
        _songs.value = if (query.isEmpty()) {
            artistSongs
        } else {
            artistSongs.filter {
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