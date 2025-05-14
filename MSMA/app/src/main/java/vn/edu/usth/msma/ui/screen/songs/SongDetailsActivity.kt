package vn.edu.usth.msma.ui.screen.songs

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.ui.theme.MSMATheme
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class SongDetailsActivity : ComponentActivity() {
    @Inject
    lateinit var songRepository: SongRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val musicPlayerViewModel: MusicPlayerViewModel by viewModels()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var musicEventReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SongDetailsActivity", "onCreate started")

        val songId = intent.getLongExtra("SONG_ID", 0L)
        val fromMiniPlayer = intent.getBooleanExtra("FROM_MINI_PLAYER", false)
        val isPlaying = intent.getBooleanExtra("IS_PLAYING", false)
        val isLoopEnabled = intent.getBooleanExtra("IS_LOOP_ENABLED", false)
        val isShuffleEnabled = intent.getBooleanExtra("IS_SHUFFLE_ENABLED", false)
        val currentPosition = intent.getLongExtra("CURRENT_POSITION", 0L)
        val duration = intent.getLongExtra("DURATION", 0L)

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val song = songRepository.getSongById(songId)
                if (song != null) {
                    val intent = Intent(this@SongDetailsActivity, MusicService::class.java).apply {
                        action = "MINIMIZE"
                        putExtra("IS_PLAYING", musicPlayerViewModel.isPlaying.value)
                        putExtra("SONG_ID", song.id)
                        putExtra("SONG_TITLE", song.title)
                        putExtra(
                            "SONG_ARTIST",
                            song.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                        )
                        putExtra("SONG_IMAGE", song.imageUrl)
                        putExtra("IS_LOOP_ENABLED", musicPlayerViewModel.isLoopEnabled.value)
                        putExtra("IS_SHUFFLE_ENABLED", musicPlayerViewModel.isShuffleEnabled.value)
                        putExtra("IS_FAVORITE", musicPlayerViewModel.isFavorite.value)
                        putExtra("POSITION", musicPlayerViewModel.currentPosition.longValue)
                        putExtra("DURATION", musicPlayerViewModel.duration.longValue)
                    }
                    startService(intent)

                    // Set mini player visible in preferences
                    CoroutineScope(Dispatchers.IO).launch {
                        preferencesManager.setMiniPlayerVisible(true)
                    }

                    // Broadcast to show mini player
                    val broadcastIntent = Intent("MUSIC_EVENT").apply {
                        putExtra("ACTION", "MINIMIZE")
                        putExtra("SONG_ID", song.id)
                        putExtra("SONG_TITLE", song.title)
                        putExtra(
                            "SONG_ARTIST",
                            song.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                        )
                        putExtra("SONG_IMAGE", song.imageUrl)
                        putExtra("IS_PLAYING", musicPlayerViewModel.isPlaying.value)
                        putExtra("IS_LOOP_ENABLED", musicPlayerViewModel.isLoopEnabled.value)
                        putExtra("IS_SHUFFLE_ENABLED", musicPlayerViewModel.isShuffleEnabled.value)
                        putExtra("IS_FAVORITE", musicPlayerViewModel.isFavorite.value)
                        putExtra("POSITION", musicPlayerViewModel.currentPosition.longValue)
                        putExtra("DURATION", musicPlayerViewModel.duration.longValue)
                    }
                    sendBroadcast(broadcastIntent)
                }
                finish()
                overridePendingTransition(0, 0)
            }
        })

        // Register broadcast receiver
        registerMusicEventReceiver()

        // Listen for EventBus events
        coroutineScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is Event.MediaNotificationCancelSongEvent -> {
                        Log.d(
                            "SongDetailsActivity",
                            "Received MediaNotificationCancelSongEvent, finishing activity"
                        )
                        finish()
                    }

                    else -> {}
                }
            }
        }

        setContent {
            MSMATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    SongDetailsScreen(
                        songId = songId,
                        onBack = { finish() },
                        fromMiniPlayer = fromMiniPlayer,
                        isPlaying = isPlaying,
                        isLoopEnabled = isLoopEnabled,
                        isShuffleEnabled = isShuffleEnabled,
                        songRepository = songRepository,
                        context = context,
                        initialPosition = currentPosition,
                        initialDuration = duration
                    )
                }
            }
        }
        Log.d("SongDetailsActivity", "onCreate completed")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerMusicEventReceiver() {
        musicEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "MUSIC_EVENT") {
                    when (intent.getStringExtra("ACTION")) {
                        "EXPAND" -> {
                            Log.d("SongDetailsActivity", "Received EXPAND event")
                            // Handle EXPAND event if needed
                        }

                        "MINIMIZE" -> {
                            Log.d("SongDetailsActivity", "Received MINIMIZE event")
                            // Handle MINIMIZE event if needed
                        }
                        // Ignore CLOSE event, as it's handled via EventBus
                    }
                }
            }
        }

        val filter = IntentFilter("MUSIC_EVENT")
        registerReceiver(musicEventReceiver, filter)
        Log.d("SongDetailsActivity", "Music event receiver registered")
    }

    override fun onDestroy() {
        super.onDestroy()
        musicEventReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e("SongDetailsActivity", "Error unregistering receiver", e)
            }
        }
        coroutineScope.cancel()
        Log.d("SongDetailsActivity", "onDestroy completed")
    }
}