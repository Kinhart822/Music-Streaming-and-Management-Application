package vn.edu.usth.msma.ui.screen.songs

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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

@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    var isDragging = mutableStateOf(false)
    var currentPosition = mutableLongStateOf(0L)
    var duration = mutableLongStateOf(0L)
    private var lastPlayedPosition = 0L

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isLoopEnabled = MutableStateFlow(false)
    val isLoopEnabled: StateFlow<Boolean> = _isLoopEnabled.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private var musicEventReceiver: BroadcastReceiver? = null
    @SuppressLint("StaticFieldLeak")
    private var receiverContext: Context? = null

    val progress: Float
        get() = if (duration.longValue > 0) currentPosition.longValue.toFloat() / duration.longValue else 0f

    fun registerMusicEventReceiver(context: Context) {
        if (musicEventReceiver != null) {
            Log.d("MusicPlayerViewModel", "BroadcastReceiver already registered")
            return
        }

        Log.d("MusicPlayerViewModel", "Registering new BroadcastReceiver")
        receiverContext = context
        musicEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "POSITION_UPDATE" -> {
                        if (!isDragging.value) {
                            val position = intent.getLongExtra("POSITION", 0L)
                            val audioDuration = intent.getLongExtra("DURATION", 0L)

                            currentPosition.longValue = position
                            duration.longValue = audioDuration
                        }
                    }

                    "MUSIC_EVENT" -> {
                        val action = intent.getStringExtra("ACTION")
                        Log.d("MusicPlayerViewModel", "Received MUSIC_EVENT: $action")

                        when (action) {
                            "LOADING" -> _isPlaying.value = false
                            "LOADED" -> _isPlaying.value = true
                            "PAUSED" -> _isPlaying.value = false
                            "RESUMED" -> _isPlaying.value = true
                            "COMPLETED" -> {
                                _isPlaying.value = false
                                currentPosition.longValue = 0
                                if (_isShuffleEnabled.value) {
                                    playNextSong(context)
                                }
                            }

                            "CURRENT_SONG", "NEXT", "PREVIOUS" -> {
                                val songId = intent.getLongExtra("SONG_ID", 0L)
                                if (songId != 0L) {
                                    val song = songRepository.getSongById(songId)
                                    if (song == null) {
                                        Log.e("MusicPlayerViewModel", "Failed to find song with ID $songId")
                                    } else {
                                        Log.d("MusicPlayerViewModel", "Updated current song: ${song.title}")
                                        _currentSong.value = song
                                        checkFavoriteStatus(songId)
                                        _isPlaying.value = intent.getBooleanExtra("IS_PLAYING", false)
                                        _isLoopEnabled.value = intent.getBooleanExtra("IS_LOOP_ENABLED", false)
                                        _isShuffleEnabled.value = intent.getBooleanExtra("IS_SHUFFLE_ENABLED", false)
                                        currentPosition.longValue = intent.getLongExtra("POSITION", 0L)
                                        duration.longValue = intent.getLongExtra("DURATION", 0L)
                                    }
                                } else {
                                    Log.e("MusicPlayerViewModel", "Invalid song ID: $songId")
                                }
                            }

                            "ADDED_TO_FAVORITES" -> _isFavorite.value = true
                            "REMOVED_FROM_FAVORITES" -> _isFavorite.value = false
                            "LOOP_ON" -> _isLoopEnabled.value = true
                            "LOOP_OFF" -> _isLoopEnabled.value = false
                            "SHUFFLE_ON" -> _isShuffleEnabled.value = true
                            "SHUFFLE_OFF" -> _isShuffleEnabled.value = false

                            "SHOW_MINI_PLAYER" -> {
                                Log.d("MusicPlayerViewModel", "Received SHOW_MINI_PLAYER event")
                                viewModelScope.launch {
                                    preferencesManager.setMiniPlayerVisible(true)
                                }
                            }
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction("POSITION_UPDATE")
            addAction("MUSIC_EVENT")
        }
        ContextCompat.registerReceiver(
            context,
            musicEventReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Subscribe to EventBus events
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is SongPlayingUpdateEvent -> _isPlaying.value = true
                    is SongPauseUpdateEvent -> _isPlaying.value = false
                    is SongLoopUpdateEvent -> _isLoopEnabled.value = true
                    is SongUnLoopUpdateEvent -> _isLoopEnabled.value = false
                    is SongShuffleUpdateEvent -> _isShuffleEnabled.value = true
                    is SongUnShuffleUpdateEvent -> _isShuffleEnabled.value = false
                    else -> {}
                }
            }
        }
    }

    fun unregisterMusicEventReceiver() {
        try {
            musicEventReceiver?.let { receiver ->
                receiverContext?.unregisterReceiver(receiver)
                Log.d("MusicPlayerViewModel", "BroadcastReceiver unregistered")
            }
            musicEventReceiver = null
            receiverContext = null
        } catch (e: Exception) {
            Log.e("MusicPlayerViewModel", "Error unregistering receiver", e)
        }
    }

    fun refreshCurrentSongData(context: Context) {
        Log.d("MusicPlayerViewModel", "Refreshing current song data")
        val intent = Intent(context, MusicService::class.java).apply {
            action = "GET_CURRENT_SONG"
        }
        context.startService(intent)
    }

    fun playSong(context: Context, song: Song) {
        Intent(context, MusicService::class.java).also { intent ->
            intent.action = "PLAY"
            intent.putExtra("SONG_PATH", song.mp3Url)
            intent.putExtra("SONG_TITLE", song.title)
            intent.putExtra("SONG_ARTIST", song.artistNameList?.joinToString(", ") ?: "Unknown Artist")
            intent.putExtra("SONG_IMAGE", song.imageUrl)
            intent.putExtra("SONG_ID", song.id)
            intent.putExtra("IS_LOOP_ENABLED", _isLoopEnabled.value)
            intent.putExtra("IS_SHUFFLE_ENABLED", _isShuffleEnabled.value)
            intent.putExtra("IS_FAVORITE", _isFavorite.value)
            context.startService(intent)
        }
        updateCurrentSong(song)
        updatePlaybackState(true)
    }

    fun playNextSong(context: Context) {
        sendCommandToService(context, "NEXT")
        _isPlaying.value = false
    }

    fun playPreviousSong(context: Context) {
        sendCommandToService(context, "PREVIOUS")
        _isPlaying.value = false
    }

    fun resumeSong(context: Context) {
        sendCommandToService(context, "RESUME")
        _isPlaying.value = true
    }

    fun pauseSong(context: Context) {
        lastPlayedPosition = currentPosition.longValue
        sendCommandToService(context, "PAUSE")
        _isPlaying.value = false
    }

    fun seekTo(context: Context, position: Long) {
        sendCommandToService(context, "SEEK_POSITION", position = position)
        currentPosition.longValue = position
        lastPlayedPosition = position

        // Broadcast seek update to all screens
        val broadcastIntent = Intent("MUSIC_EVENT").apply {
            putExtra("ACTION", "POSITION_UPDATE")
            putExtra("POSITION", position)
            putExtra("DURATION", duration.longValue)
        }
        context.sendBroadcast(broadcastIntent)
    }

    fun toggleLoop(context: Context) {
        val newLoopState = !_isLoopEnabled.value
        sendCommandToService(context, if (newLoopState) "LOOP_ON" else "LOOP_OFF")
    }

    fun toggleShuffle(context: Context) {
        val newShuffleState = !_isShuffleEnabled.value
        sendCommandToService(context, if (newShuffleState) "SHUFFLE_ON" else "SHUFFLE_OFF")
    }

    fun toggleFavorite(context: Context, songId: Long) {
        viewModelScope.launch {
            try {
                val response = if (_isFavorite.value) {
                    apiService.getSongApi().userUnlikeSong(songId)
                } else {
                    apiService.getSongApi().userLikeSong(songId)
                }

                if (response.isSuccessful) {
                    _isFavorite.value = !_isFavorite.value
                    Log.d(
                        "MusicPlayerViewModel",
                        "Successfully ${if (_isFavorite.value) "liked" else "unliked"} song $songId"
                    )
                    EventBus.publish(SongFavouriteUpdateEvent)
                    sendCommandToService(
                        context,
                        if (_isFavorite.value) "ADDED_TO_FAVORITES" else "REMOVED_FROM_FAVORITES"
                    )
                } else {
                    Log.e("MusicPlayerViewModel", "Failed to toggle favorite: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error toggling favorite", e)
            }
        }
    }

    fun updateCurrentSong(song: Song) {
        _currentSong.value = song
        checkFavoriteStatus(song.id)
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun setLoopEnabled(enabled: Boolean) {
        _isLoopEnabled.value = enabled
    }

    fun setShuffleEnabled(enabled: Boolean) {
        _isShuffleEnabled.value = enabled
    }

    fun setFavorite(isFavorite: Boolean) {
        _isFavorite.value = isFavorite
    }

    private fun checkFavoriteStatus(songId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getSongApi().checkLikedSongs(songId)
                if (response.isSuccessful) {
                    _isFavorite.value = response.body() ?: false
                    Log.d("MusicPlayerViewModel", "Favorite status for song $songId: ${_isFavorite.value}")
                } else {
                    Log.e("MusicPlayerViewModel", "Failed to check favorite status: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error checking favorite status", e)
            }
        }
    }

    private fun sendCommandToService(context: Context, action: String, position: Long? = null) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            position?.let { putExtra("POSITION", it) }
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the receiver when the ViewModel is cleared
        unregisterMusicEventReceiver()
        Log.d("MusicPlayerViewModel", "ViewModel cleared, resources cleaned up")
    }
}