package vn.edu.usth.msma.ui.screen.search.songs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.usth.msma.service.MusicService

class MusicPlayerViewModel : ViewModel() {
    var isDragging = mutableStateOf(false)
    var currentPosition = mutableLongStateOf(0L)
    var duration = mutableLongStateOf(0L)
    var currentSongId: Long? = null
    private var lastPlayedPosition = 0L

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private var positionUpdateReceiver: BroadcastReceiver? = null

    val progress: Float
        get() = if (duration.longValue > 0) currentPosition.longValue.toFloat() / duration.longValue else 0f

    fun registerPositionReceiver(context: Context) {
        positionUpdateReceiver = object : BroadcastReceiver() {
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
                        when (intent.getStringExtra("ACTION")) {
                            "LOADING" -> {
                                _isPlaying.value = false
                            }
                            "LOADED" -> _isPlaying.value = true
                            "PAUSED" -> _isPlaying.value = false
                            "RESUMED" -> _isPlaying.value = true
                            "COMPLETED" -> {
                                _isPlaying.value = false
                                currentPosition.longValue = 0
                            }
                            "NEXT" -> {
                                currentPosition.longValue = 0
                               _isPlaying.value = true
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
    }

    fun playSong(context: Context,  song: Song) {
        Intent(context, MusicService::class.java).also { intent ->
            intent.action = "PLAY"
            intent.putExtra("SONG_PATH", song.mp3Url)
            intent.putExtra("SONG_TITLE", song.title)
            intent.putExtra("SONG_ARTIST", song.artistNameList?.joinToString(", ") ?: "Unknown Artist")
            intent.putExtra("SONG_IMAGE", song.imageUrl)
            intent.putExtra("SONG_ID", song.id)
            startService(context, intent)
        }
        currentSongId = song.id
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

    private fun sendCommandToService(
        context: Context, action: String, position: Long? = null
    ) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            position?.let { putExtra("POSITION", it) }
        }
        startService(context, intent)
    }


    private fun startService(context: Context, intent: Intent) {
        ContextCompat.startForegroundService(context, intent)
    }

    fun updateCurrentSong(songId: Long, isPlaying: Boolean) {
        currentSongId = songId
        _isPlaying.value = isPlaying
    }

    override fun onCleared() {
        positionUpdateReceiver?.let { receiver ->
            // Unregister receiver if context is still available
            // Note: Context is not available here, so rely on DisposableEffect in DetailSongScreen
        }
        super.onCleared()
    }
}