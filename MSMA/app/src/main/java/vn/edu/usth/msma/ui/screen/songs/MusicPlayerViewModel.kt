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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.utils.eventbus.Event.SongFavouriteUpdateEvent
import vn.edu.usth.msma.utils.eventbus.Event.SongLoopUpdateEvent
import vn.edu.usth.msma.utils.eventbus.Event.SongPauseUpdateEvent
import vn.edu.usth.msma.utils.eventbus.Event.SongPlayingUpdateEvent
import vn.edu.usth.msma.utils.eventbus.Event.SongShuffleUpdateEvent
import vn.edu.usth.msma.utils.eventbus.Event.SongUnLoopUpdateEvent
import vn.edu.usth.msma.utils.eventbus.Event.SongUnShuffleUpdateEvent
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val apiService: ApiService
) : ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null

    var isDragging = mutableStateOf(false)
    var currentPosition = mutableLongStateOf(0L)
    var duration = mutableLongStateOf(0L)
    private var currentSongId: Long? = null
    private var lastPlayedPosition = 0L

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isLoopEnabled = MutableStateFlow(false)
    val isLoopEnabled: StateFlow<Boolean> = _isLoopEnabled.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private var positionUpdateReceiver: BroadcastReceiver? = null

    val progress: Float
        get() = if (duration.longValue > 0) currentPosition.longValue.toFloat() / duration.longValue else 0f

    fun updateCurrentSong(song: Song) {
        _currentSong.value = song
        checkFavoriteStatus(song.id)
        // Sync shuffle state with MusicService
        if (_isShuffleEnabled.value) {
            sendCommandToService(context!!, "SHUFFLE_ON")
        }
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    @OptIn(FlowPreview::class)
    fun registerPositionReceiver(context: Context) {
        this.context = context
        positionUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "POSITION_UPDATE" -> {
                        if (!isDragging.value) {
                            val position = intent.getLongExtra("CURRENT_POSITION", 0L)
                            val audioDuration = intent.getLongExtra("DURATION", 0L)
                            currentPosition.longValue = position
                            duration.longValue = audioDuration
                        }
                    }

                    "MUSIC_EVENT" -> {
                        when (intent.getStringExtra("ACTION")) {
                            "LOADING" -> {
                                _isPlaying.value = false
                            }

                            "LOADED" -> {
                                _isPlaying.value = true
                                updateCurrentSongFromIntent(intent)
                            }

                            "PAUSED" -> _isPlaying.value = false
                            "RESUMED" -> _isPlaying.value = true
                            "COMPLETED" -> {
                                _isPlaying.value = false
                                currentPosition.longValue = 0
                                if (_isShuffleEnabled.value) {
                                    playNextSong(context)
                                }
                            }

                            "NEXT", "PREVIOUS" -> {
                                currentPosition.longValue = 0
                                _isPlaying.value = true
                                updateCurrentSongFromIntent(intent)
                                _isLoopEnabled.value = intent.getBooleanExtra("IS_LOOP_ENABLED", false)
                                _isShuffleEnabled.value = intent.getBooleanExtra("IS_SHUFFLE_ENABLED", false)
                                refreshCurrentSongData(context)
                            }

                            "ADDED_TO_FAVORITES" -> {
                                Log.d("MusicPlayerViewModel", "Updating isFavorite to true")
                                _isFavorite.value = true
                            }

                            "REMOVED_FROM_FAVORITES" -> {
                                Log.d("MusicPlayerViewModel", "Updating isFavorite to false")
                                _isFavorite.value = false
                            }

                            "MINIMIZE", "EXPAND" -> {
                                val songId = intent.getLongExtra("SONG_ID", 0L)
                                val action = intent.getStringExtra("ACTION")
                                Log.d("MusicPlayerViewModel", "Processing $action for songId: $songId")
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
                                        currentPosition.longValue = intent.getLongExtra("CURRENT_POSITION", 0L)
                                        duration.longValue = intent.getLongExtra("DURATION", 0L)
                                    }
                                }
                                refreshCurrentSongData(context)
                            }

                            "CURRENT_SONG" -> {
                                val songId = intent.getLongExtra("SONG_ID", 0L)
                                val action = intent.getStringExtra("ACTION")
                                Log.d("MusicPlayerViewModel", "Processing $action for songId: $songId")
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
                                        currentPosition.longValue = intent.getLongExtra("CURRENT_POSITION", 0L)
                                        duration.longValue = intent.getLongExtra("DURATION", 0L)
                                    }
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
            positionUpdateReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Subscribe to EventBus events
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is SongPlayingUpdateEvent -> { _isPlaying.value = true }
                    is SongPauseUpdateEvent -> { _isPlaying.value = false }
                    is SongLoopUpdateEvent -> { _isLoopEnabled.value = true }
                    is SongUnLoopUpdateEvent -> { _isLoopEnabled.value = false }
                    is SongShuffleUpdateEvent -> { _isShuffleEnabled.value = true }
                    is SongUnShuffleUpdateEvent -> { _isShuffleEnabled.value = false }
                    else -> {}
                }
            }
        }
    }

    fun refreshCurrentSongData(context: Context) {
        Log.d("MusicPlayerViewModel", "Refreshing current song data")
        val intent = Intent(context, MusicService::class.java).apply {
            action = "GET_CURRENT_SONG"
        }
        context.startService(intent)
    }

    private fun updateCurrentSongFromIntent(intent: Intent) {
        val songId = intent.getLongExtra("SONG_ID", 0L)
        if (songId != 0L) {
            currentSongId = songId
            checkFavoriteStatus(songId)

            val path = intent.getStringExtra("SONG_PATH") ?: ""
            val title = intent.getStringExtra("SONG_TITLE") ?: "Unknown Title"
            val artist = intent.getStringExtra("SONG_ARTIST") ?: "Unknown Artist"
            val imageUrl = intent.getStringExtra("SONG_IMAGE") ?: ""

            val song = songRepository.getSongById(songId)

            val playerDuration = intent.getLongExtra("DURATION", 0L)
            val songDuration = song?.duration?.let { parseDuration(it) } ?: playerDuration

            _currentSong.value = Song(
                id = songId,
                title = title,
                artistNameList = listOf(artist),
                imageUrl = imageUrl,
                mp3Url = path,
                duration = song?.duration,
                releaseDate = song?.releaseDate,
                lyrics = song?.lyrics,
                downloadPermission = song?.downloadPermission,
                description = song?.description,
                songStatus = song?.songStatus,
                genreNameList = song?.genreNameList,
                numberOfListeners = song?.numberOfListeners,
                countListen = song?.countListen,
                numberOfUserLike = song?.numberOfUserLike,
                numberOfDownload = song?.numberOfDownload
            )

            duration.longValue = songDuration
        }
    }

    private fun parseDuration(duration: String): Long {
        return try {
            val parts = duration.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val seconds = parts[1].toLong()
                (minutes * 60 + seconds) * 1000
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun playSong(context: Context, song: Song) {
        Intent(context, MusicService::class.java).also { intent ->
            intent.action = "PLAY"
            intent.putExtra("SONG_PATH", song.mp3Url)
            intent.putExtra("SONG_TITLE", song.title)
            intent.putExtra(
                "SONG_ARTIST",
                song.artistNameList?.joinToString(", ") ?: "Unknown Artist"
            )
            intent.putExtra("SONG_IMAGE", song.imageUrl)
            intent.putExtra("SONG_ID", song.id)
            intent.putExtra("IS_LOOP_ENABLED", _isLoopEnabled.value)
            intent.putExtra("IS_SHUFFLE_ENABLED", _isShuffleEnabled.value)
            intent.putExtra("IS_FAVORITE", _isFavorite.value)

            context.startService(intent)
        }
        currentSongId = song.id
        _currentSong.value = song
        checkFavoriteStatus(song.id)
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
    }

    fun toggleLoop(context: Context) {
        val newLoopState = !_isLoopEnabled.value
        sendCommandToService(
            context,
            if (newLoopState) "LOOP_ON" else "LOOP_OFF"
        )
    }

    fun toggleShuffle(context: Context) {
        val newShuffleState = !_isShuffleEnabled.value
        sendCommandToService(
            context,
            if (newShuffleState) "SHUFFLE_ON" else "SHUFFLE_OFF"
        )
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
                    Log.d(
                        "MusicPlayerViewModel",
                        "Favorite status for song $songId: ${_isFavorite.value}"
                    )
                } else {
                    Log.e(
                        "MusicPlayerViewModel",
                        "Failed to check favorite status: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerViewModel", "Error checking favorite status", e)
            }
        }
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

                    // Publish event to update library
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

    private fun sendCommandToService(
        context: Context, action: String, position: Long? = null
    ) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            position?.let { putExtra("CURRENT_POSITION", it) }
        }

        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context?.unregisterReceiver(positionUpdateReceiver)
        } catch (e: Exception) {
            Log.e("MusicPlayerViewModel", "Error unregistering receiver", e)
        }
    }
}