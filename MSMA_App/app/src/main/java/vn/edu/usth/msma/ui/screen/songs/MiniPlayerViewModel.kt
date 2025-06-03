package vn.edu.usth.msma.ui.screen.songs

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.runtime.mutableLongStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.utils.eventbus.Event.*
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

//@HiltViewModel
//class MiniPlayerViewModel @Inject constructor(
//    private val songRepository: SongRepository,
//    private val preferencesManager: PreferencesManager,
//    private val apiService: ApiService
//) : ViewModel() {
//    private val _currentSong = MutableStateFlow<Song?>(null)
//    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
//
//    private val _isPlaying = MutableStateFlow(false)
//    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
//
//    private val _isFavorite = MutableStateFlow(false)
//    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()
//
//    private val _isLoopEnabled = MutableStateFlow(false)
//    val isLoopEnabled: StateFlow<Boolean> = _isLoopEnabled.asStateFlow()
//
//    private val _isShuffleEnabled = MutableStateFlow(false)
//    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()
//
//    var currentPosition = mutableLongStateOf(0L)
//    var duration = mutableLongStateOf(0L)
//
//    private var musicEventReceiver: BroadcastReceiver? = null
//
//    fun updateCurrentSong(song: Song) {
//        _currentSong.value = song
//        checkFavoriteStatus(song.id)
//    }
//
//    fun updatePlaybackState(isPlaying: Boolean) {
//        _isPlaying.value = isPlaying
//    }
//
//    fun unregisterMusicEventReceiver(context: Context) {
//        try {
//            musicEventReceiver?.let {
//                context.unregisterReceiver(it)
//                musicEventReceiver = null
//            }
//        } catch (e: Exception) {
//            Log.e("MiniPlayerViewModel", "Error unregistering receiver", e)
//        }
//    }
//
//    fun registerMusicEventReceiver(context: Context) {
//        if (musicEventReceiver != null) {
//            Log.d("MiniPlayerViewModel", "BroadcastReceiver already registered")
//            return
//        }
//
//        Log.d("MiniPlayerViewModel", "Registering new BroadcastReceiver")
//        musicEventReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                when (intent.action) {
//                    "POSITION_UPDATE" -> {
//                        val newPosition = intent.getLongExtra("POSITION", 0L)
//                        val newDuration = intent.getLongExtra("DURATION", 0L)
//
//                        currentPosition.longValue = newPosition
//                        duration.longValue = newDuration
//                    }
//
//                    "MUSIC_EVENT" -> {
//                        val action = intent.getStringExtra("ACTION")
//                        Log.d("MiniPlayerViewModel", "Received MUSIC_EVENT: $action")
//
//                        when (action) {
//                            "PAUSED" -> _isPlaying.value = false
//                            "RESUMED" -> _isPlaying.value = true
//                            "LOADED" -> _isPlaying.value = true
//                            "COMPLETED" -> _isPlaying.value = false
//                            "ADDED_TO_FAVORITES" -> _isFavorite.value = true
//                            "REMOVED_FROM_FAVORITES" -> _isFavorite.value = false
//                            "LOOP_ON" -> _isLoopEnabled.value = true
//                            "LOOP_OFF" -> _isLoopEnabled.value = false
//                            "SHUFFLE_ON" -> _isShuffleEnabled.value = true
//                            "SHUFFLE_OFF" -> _isShuffleEnabled.value = false
//
//                            "CURRENT_SONG", "NEXT", "PREVIOUS" -> {
//                                val songId = intent.getLongExtra("SONG_ID", 0L)
//                                if (songId != 0L) {
//                                    val song = songRepository.getSongById(songId)
//                                    if (song == null) {
//                                        Log.e(
//                                            "MiniPlayerViewModel",
//                                            "Failed to find song with ID $songId"
//                                        )
//                                    } else {
//                                        Log.d(
//                                            "MiniPlayerViewModel",
//                                            "Updated current song: ${song.title}"
//                                        )
//                                        _currentSong.value = song
//                                        checkFavoriteStatus(songId)
//                                        _isPlaying.value =
//                                            intent.getBooleanExtra("IS_PLAYING", false)
//                                        _isLoopEnabled.value =
//                                            intent.getBooleanExtra("IS_LOOP_ENABLED", false)
//                                        _isShuffleEnabled.value =
//                                            intent.getBooleanExtra("IS_SHUFFLE_ENABLED", false)
//                                        currentPosition.longValue =
//                                            intent.getLongExtra("POSITION", 0L)
//                                        duration.longValue = intent.getLongExtra("DURATION", 0L)
//                                    }
//                                } else {
//                                    Log.e("MiniPlayerViewModel", "Invalid song ID: $songId")
//                                }
//                            }
//
//                            "SHOW_MINI_PLAYER" -> {
//                                Log.d("MiniPlayerViewModel", "Received SHOW_MINI_PLAYER event")
//                                viewModelScope.launch {
//                                    preferencesManager.setMiniPlayerVisible(true)
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        val filter = IntentFilter().apply {
//            addAction("MUSIC_EVENT")
//            addAction("POSITION_UPDATE")
//        }
//        ContextCompat.registerReceiver(
//            context,
//            musicEventReceiver,
//            filter,
//            ContextCompat.RECEIVER_EXPORTED
//        )
//
//        // Subscribe to EventBus events
//        viewModelScope.launch {
//            EventBus.events.collect { event ->
//                when (event) {
//                    is SongPlayingUpdateEvent -> {
//                        _isPlaying.value = true
//                    }
//
//                    is SongPauseUpdateEvent -> {
//                        _isPlaying.value = false
//                    }
//
//                    is SongLoopUpdateEvent -> {
//                        _isLoopEnabled.value = true
//                    }
//
//                    is SongUnLoopUpdateEvent -> {
//                        _isLoopEnabled.value = false
//                    }
//
//                    is SongShuffleUpdateEvent -> {
//                        _isShuffleEnabled.value = true
//                    }
//
//                    is SongUnShuffleUpdateEvent -> {
//                        _isShuffleEnabled.value = false
//                    }
//
//                    else -> {}
//                }
//            }
//        }
//    }
//
//    fun refreshCurrentSongData(context: Context) {
//        Log.d("MiniPlayerViewModel", "Refreshing current song data")
//        val intent = Intent(context, MusicService::class.java).apply {
//            action = "GET_CURRENT_SONG"
//        }
//        context.startService(intent)
//    }
//
//    fun seekTo(context: Context, position: Long) {
//        val intent = Intent(context, MusicService::class.java).apply {
//            action = "SEEK_POSITION"
//            putExtra("POSITION", position)
//        }
//        context.startService(intent)
//        currentPosition.longValue = position
//
//        // Broadcast seek update to all screens
//        val broadcastIntent = Intent("MUSIC_EVENT").apply {
//            putExtra("ACTION", "POSITION_UPDATE")
//            putExtra("POSITION", position)
//            putExtra("DURATION", duration.longValue)
//        }
//        context.sendBroadcast(broadcastIntent)
//    }
//
//    fun toggleFavorite(context: Context, songId: Long) {
//        viewModelScope.launch {
//            try {
//                val response = if (_isFavorite.value) {
//                    apiService.getSongApi().userUnlikeSong(songId)
//                } else {
//                    apiService.getSongApi().userLikeSong(songId)
//                }
//
//                if (response.isSuccessful) {
//                    _isFavorite.value = !_isFavorite.value
//                    Log.d(
//                        "MiniPlayerViewModel",
//                        "Successfully ${if (_isFavorite.value) "liked" else "unliked"} song $songId"
//                    )
//                    val intent = Intent(context, MusicService::class.java).apply {
//                        action =
//                            if (_isFavorite.value) "ADDED_TO_FAVORITES" else "REMOVED_FROM_FAVORITES"
//                        putExtra("SONG_ID", songId)
//                    }
//                    EventBus.publish(SongFavouriteUpdateEvent)
//                    context.startService(intent)
//                } else {
//                    Log.e("MiniPlayerViewModel", "Failed to toggle favorite: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("MiniPlayerViewModel", "Error toggling favorite", e)
//            }
//        }
//    }
//
//    private fun checkFavoriteStatus(songId: Long) {
//        viewModelScope.launch {
//            try {
//                val response = apiService.getSongApi().checkLikedSongs(songId)
//                if (response.isSuccessful) {
//                    _isFavorite.value = response.body() ?: false
//                    Log.d(
//                        "MiniPlayerViewModel",
//                        "Favorite status for song $songId: ${_isFavorite.value}"
//                    )
//                } else {
//                    Log.e(
//                        "MiniPlayerViewModel",
//                        "Failed to check favorite status: ${response.code()}"
//                    )
//                }
//            } catch (e: Exception) {
//                Log.e("MiniPlayerViewModel", "Error checking favorite status", e)
//            }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        // Make sure we clean up resources
//    }
//}