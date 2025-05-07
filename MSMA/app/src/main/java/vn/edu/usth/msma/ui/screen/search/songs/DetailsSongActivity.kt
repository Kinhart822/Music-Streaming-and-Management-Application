package vn.edu.usth.msma.ui.screen.search.songs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.ui.theme.MSMATheme
import javax.inject.Inject

@AndroidEntryPoint
class DetailsSongActivity : ComponentActivity() {
    @Inject lateinit var songRepository: SongRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val songId = intent.getLongExtra("SONG_ID", 0L)
        setContent {
            MSMATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    DetailSongScreen(songId = songId, onBack = { finish() }, songRepository, context)
                }
            }
        }
    }
}