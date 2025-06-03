package vn.edu.usth.msma.ui.screen.search.artists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.Artist
import vn.edu.usth.msma.data.dto.response.management.AlbumResponse
import vn.edu.usth.msma.data.dto.response.management.PlaylistResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.AlbumRepository
import vn.edu.usth.msma.repository.ArtistRepository
import vn.edu.usth.msma.repository.PlaylistRepository
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.utils.eventbus.Event.*
import vn.edu.usth.msma.utils.eventbus.EventBus
import vn.edu.usth.msma.utils.helpers.toArtist
import javax.inject.Inject

@HiltViewModel
class ArtistProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {
    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    private val _songs = MutableStateFlow<List<SongResponse>>(emptyList())
    val songs: StateFlow<List<SongResponse>> = _songs.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistResponse>>(emptyList())
    val playlists: StateFlow<List<PlaylistResponse>> = _playlists.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumResponse>>(emptyList())
    val albums: StateFlow<List<AlbumResponse>> = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    fun loadArtist(artist: Artist) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().viewArtistProfile(artist.id)
                if (response.isSuccessful && response.body() != null) {
                    val artistData = response.body()!!
                    _artist.value = artistData.toArtist()
                    artistRepository.updateArtist(artistData)
                    fetchArtistSongs(artist.id)
                    fetchArtistPlaylists(artist.id)
                    fetchArtistAlbums(artist.id)
                } else {
                    _artist.value = artist
                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error loading artist: ${e.message}", e)
                _artist.value = artist
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchArtistSongs(artistId: Long) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().getArtistSongs(artistId)
                if (response.isSuccessful && response.body() != null) {
                    val songData = response.body()!!
                    songRepository.updateSongResponseList(songData)
                    _songs.value = songData
                } else {
                    _songs.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error loading songs: ${e.message}", e)
                _songs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchArtistPlaylists(artistId: Long) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().getArtistPlaylists(artistId)
                if (response.isSuccessful && response.body() != null) {
                    val playlistData = response.body()!!
                    playlistRepository.updatePlaylistResponseList(playlistData)
                    _playlists.value = playlistData
                } else {
                    _playlists.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error loading songs: ${e.message}", e)
                _playlists.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchArtistAlbums(artistId: Long) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().getArtistAlbums(artistId)
                if (response.isSuccessful && response.body() != null) {
                    val albumData = response.body()!!
                    albumRepository.updateAlbumResponseList(albumData)
                    _albums.value = albumData
                } else {
                    _albums.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error loading songs: ${e.message}", e)
                _albums.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkFollowStatus(artist: Artist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().checkFollowedArtist(artist.id)
                if (response.isSuccessful && response.body() != null) {
                    _isFollowing.value = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error checking follow status: ${e.message}", e)
            }
        }
    }

    fun followArtist(artist: Artist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().followArtist(artist.id)
                if (response.isSuccessful) {
                    _isFollowing.value = true
                    _artist.update { currentArtist ->
                        currentArtist?.copy(
                            numberOfFollowers = (currentArtist.numberOfFollowers ?: 0) + 1
                        )
                    }
                    EventBus.publish(FollowArtistUpdateEvent)
                    // Refresh artist data in the background
                    refreshArtist(artist)                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error following artist: ${e.message}", e)
            }
        }
    }

    fun unfollowArtist(artist: Artist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().unfollowArtist(artist.id)
                if (response.isSuccessful) {
                    _isFollowing.value = false
                    EventBus.publish(UnFollowArtistUpdateEvent)
                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error unfollowing artist: ${e.message}", e)
            }
        }
    }

    fun refreshArtist(artist: Artist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getArtistApi().viewArtistProfile(artist.id)
                if (response.isSuccessful && response.body() != null) {
                    val artistData = response.body()!!
                    _artist.value = artistData.toArtist()
                    artistRepository.updateArtist(artistData)
                    fetchArtistSongs(artist.id)
                }
            } catch (e: Exception) {
                Log.e("ArtistProfileViewModel", "Error refreshing artist: ${e.message}", e)
            }
        }
    }
}