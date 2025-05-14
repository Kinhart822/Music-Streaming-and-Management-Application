package vn.edu.usth.msma.ui.screen.songs

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
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.utils.eventbus.Event.*
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val apiService: ApiService
) : ViewModel() {
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

    var currentPosition = mutableLongStateOf(0L)
    var duration = mutableLongStateOf(0L)

    private var musicEventReceiver: BroadcastReceiver? = null
    private var positionUpdateReceiver: BroadcastReceiver? = null

    fun updateCurrentSong(song: Song) {
        _currentSong.value = song
        checkFavoriteStatus(song.id)
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun registerReceivers(context: Context) {
        registerMusicEventReceiver(context)
        registerPositionUpdateReceiver(context)

        // Subscribe to EventBus events
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is SongPlayingUpdateEvent -> { 
                        _isPlaying.value = true 
                        Log.d("MiniPlayerViewModel", "Updated isPlaying to true from EventBus")
                    }
                    is SongPauseUpdateEvent -> { 
                        _isPlaying.value = false 
                        Log.d("MiniPlayerViewModel", "Updated isPlaying to false from EventBus")
                    }
                    is SongLoopUpdateEvent -> {
                        _isLoopEnabled.value = true
                        Log.d("MiniPlayerViewModel", "Updated isLoopEnabled to true from EventBus")
                    }
                    is SongUnLoopUpdateEvent -> {
                        _isLoopEnabled.value = false
                        Log.d("MiniPlayerViewModel", "Updated isLoopEnabled to false from EventBus")
                    }
                    is SongShuffleUpdateEvent -> {
                        _isShuffleEnabled.value = true
                        Log.d("MiniPlayerViewModel", "Updated isShuffleEnabled to true from EventBus")
                    }
                    is SongUnShuffleUpdateEvent -> {
                        _isShuffleEnabled.value = false
                        Log.d("MiniPlayerViewModel", "Updated isShuffleEnabled to false from EventBus")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun registerMusicEventReceiver(context: Context) {
        if (musicEventReceiver != null) {
            Log.d("MiniPlayerViewModel", "MusicEventReceiver already registered")
            return
        }

        Log.d("MiniPlayerViewModel", "Registering new MusicEventReceiver")
        musicEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "MUSIC_EVENT") {
                    val action = intent.getStringExtra("ACTION")
                    Log.d("MiniPlayerViewModel", "Received MUSIC_EVENT: $action")

                    when (action) {
                        "PAUSED" -> {
                            Log.d("MiniPlayerViewModel", "Updating isPlaying to false for PAUSED")
                            _isPlaying.value = false
                        }

                        "RESUMED" -> {
                            Log.d("MiniPlayerViewModel", "Updating isPlaying to true for RESUMED")
                            _isPlaying.value = true
                        }

                        "LOADED" -> {
                            Log.d("MiniPlayerViewModel", "Updating isPlaying to true for LOADED")
                            _isPlaying.value = true
                        }

                        "COMPLETED" -> {
                            Log.d("MiniPlayerViewModel", "Updating isPlaying to false for COMPLETED")
                            _isPlaying.value = false
                        }

                        "ADDED_TO_FAVORITES" -> {
                            Log.d("MiniPlayerViewModel", "Updating isFavorite to true")
                            _isFavorite.value = true
                        }

                        "REMOVED_FROM_FAVORITES" -> {
                            Log.d("MiniPlayerViewModel", "Updating isFavorite to false")
                            _isFavorite.value = false
                        }

                        "MINIMIZE", "CURRENT_SONG", "EXPAND", "NEXT", "PREVIOUS" -> {
                            val songId = intent.getLongExtra("SONG_ID", 0L)
                            Log.d("MiniPlayerViewModel", "Processing $action for songId: $songId")
                            if (songId != 0L) {
                                val song = songRepository.getSongById(songId)
                                if (song == null) {
                                    Log.e("MiniPlayerViewModel", "Failed to find song with ID $songId")
                                } else {
                                    Log.d("MiniPlayerViewModel", "Updated current song: ${song.title}")
                                    _currentSong.value = song
                                    _isPlaying.value = intent.getBooleanExtra("IS_PLAYING", false)
                                    _isLoopEnabled.value = intent.getBooleanExtra("IS_LOOP_ENABLED", false)
                                    _isShuffleEnabled.value = intent.getBooleanExtra("IS_SHUFFLE_ENABLED", false)
                                    _isFavorite.value = intent.getBooleanExtra("IS_FAVORITE", false)
                                    currentPosition.longValue = intent.getLongExtra("POSITION", 0L)
                                    duration.longValue = intent.getLongExtra("DURATION", 0L)
                                    checkFavoriteStatus(songId)
                                }
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter("MUSIC_EVENT")
        ContextCompat.registerReceiver(
            context,
            musicEventReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.d("MiniPlayerViewModel", "Successfully registered MusicEventReceiver")
    }

    private fun registerPositionUpdateReceiver(context: Context) {
        if (positionUpdateReceiver != null) return

        positionUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "POSITION_UPDATE") {
                    currentPosition.longValue = intent.getLongExtra("POSITION", 0L)
                    duration.longValue = intent.getLongExtra("DURATION", 0L)
                }
            }
        }

        val filter = IntentFilter("POSITION_UPDATE")
        ContextCompat.registerReceiver(
            context,
            positionUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregisterMusicEventReceiver(context: Context) {
        musicEventReceiver?.let {
            context.unregisterReceiver(it)
            musicEventReceiver = null
        }
        positionUpdateReceiver?.let {
            context.unregisterReceiver(it)
            positionUpdateReceiver = null
        }
    }

    fun refreshCurrentSongData(context: Context) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = "GET_CURRENT_SONG"
        }
        context.startService(intent)
    }

    fun openDetails(context: Context) {
        Log.d("MiniPlayerViewModel", "openDetails called")
        _currentSong.value?.let { song ->
            Log.d("MiniPlayerViewModel", "Opening details for song: ${song.id} - ${song.title}")

            // Send broadcast to MusicService
            val intent = Intent(context, MusicService::class.java).apply {
                action = "EXPAND"
                putExtra("SONG_ID", song.id)
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artistNameList?.joinToString(", ") ?: "Unknown Artist")
                putExtra("SONG_IMAGE", song.imageUrl)
                putExtra("IS_PLAYING", _isPlaying.value)
                putExtra("IS_LOOP_ENABLED", _isLoopEnabled.value)
                putExtra("IS_SHUFFLE_ENABLED", _isShuffleEnabled.value)
                putExtra("IS_FAVORITE", _isFavorite.value)
                putExtra("FROM_MINI_PLAYER", true)
                putExtra("CURRENT_POSITION", currentPosition.longValue)
                putExtra("DURATION", duration.longValue)
            }
            Log.d("MiniPlayerViewModel", "Sending EXPAND broadcast to MusicService")
            context.startService(intent)

            val activityIntent = Intent(context, SongDetailsActivity::class.java).apply {
                putExtra("SONG_ID", song.id)
                putExtra("FROM_MINI_PLAYER", true)
                putExtra("IS_PLAYING", _isPlaying.value)
                putExtra("IS_LOOP_ENABLED", _isLoopEnabled.value)
                putExtra("IS_SHUFFLE_ENABLED", _isShuffleEnabled.value)
                putExtra("IS_FAVORITE", _isFavorite.value)
                putExtra("CURRENT_POSITION", currentPosition.longValue)
                putExtra("DURATION", duration.longValue)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            Log.d("MiniPlayerViewModel", "Starting SongDetailsActivity")
            try {
                context.startActivity(activityIntent)
                Log.d("MiniPlayerViewModel", "Successfully started SongDetailsActivity")
            } catch (e: Exception) {
                Log.e("MiniPlayerViewModel", "Error starting SongDetailsActivity", e)
            }
        } ?: run {
            Log.e("MiniPlayerViewModel", "Cannot open details: currentSong is null")
        }
    }

    fun seekTo(context: Context, position: Long) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = "SEEK"
            putExtra("POSITION", position)
        }
        context.startService(intent)
        currentPosition.longValue = position
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
                    Log.d("MiniPlayerViewModel", "Successfully ${if (_isFavorite.value) "liked" else "unliked"} song $songId")
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = if (_isFavorite.value) "ADDED_TO_FAVORITES" else "REMOVED_FROM_FAVORITES"
                        putExtra("SONG_ID", songId)
                    }
                    EventBus.publish(SongFavouriteUpdateEvent)
                    context.startService(intent)
                } else {
                    Log.e("MiniPlayerViewModel", "Failed to toggle favorite: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MiniPlayerViewModel", "Error toggling favorite", e)
            }
        }
    }

    private fun checkFavoriteStatus(songId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getSongApi().checkLikedSongs(songId)
                if (response.isSuccessful) {
                    _isFavorite.value = response.body() ?: false
                    Log.d("MiniPlayerViewModel", "Favorite status for song $songId: ${_isFavorite.value}")
                } else {
                    Log.e("MiniPlayerViewModel", "Failed to check favorite status: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MiniPlayerViewModel", "Error checking favorite status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}