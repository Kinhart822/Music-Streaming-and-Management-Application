package vn.edu.usth.msma.ui.screen.songs

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val song = songRepository.getSongById(songId)
                if (song != null) {
                    val intent = Intent(this@SongDetailsActivity, MusicService::class.java).apply {
                        action = "MINIMIZE"
                        putExtra("IS_PLAYING", isPlaying)
                        putExtra("SONG_ID", song.id)
                        putExtra("SONG_TITLE", song.title)
                        putExtra(
                            "SONG_ARTIST",
                            song.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                        )
                        putExtra("SONG_IMAGE", song.imageUrl)
                        putExtra("IS_LOOP_ENABLED", isLoopEnabled)
                        putExtra("IS_SHUFFLE_ENABLED", isShuffleEnabled)
                        putExtra("IS_FAVORITE", intent.getBooleanExtra("IS_FAVORITE", false))
                        putExtra("POSITION", intent.getLongExtra("CURRENT_POSITION", 0L))
                        putExtra("DURATION", intent.getLongExtra("DURATION", 0L))
                    }
                    startService(intent)
                }
                finish()
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
                        context = context
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