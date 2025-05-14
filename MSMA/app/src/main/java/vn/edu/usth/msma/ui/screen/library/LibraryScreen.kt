package vn.edu.usth.msma.ui.screen.library

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.screen.songs.SongDetailsActivity
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.Event.InitializeDataLibrary
import vn.edu.usth.msma.utils.eventbus.EventBus

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Emit InitializeDataLibrary event when the screen is first composed
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            EventBus.publish(InitializeDataLibrary)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .padding(16.dp)
    ) {
        Text(
            text = "Bài hát yêu thích của bạn",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            LoadingScreen(message = "Loading favorite songs...")
        } else if (favoriteSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "You don't have any favorite songs yet",
                    color = Color.Black,
                    fontSize = 18.sp
                )
            }
        } else {
            LazyColumn {
                items(favoriteSongs, key = { song -> song.id }) { song ->
                    SongItem(song = song, onSongClick = {
                        Log.d("LibraryScreen", "Song clicked: ${song.title} (ID: ${song.id})")
                        try {
                            val intent = Intent(context, SongDetailsActivity::class.java).apply {
                                putExtra("SONG_ID", song.id)
                            }
                            // Send broadcast to update MiniPlayer
                            val broadcastIntent = Intent("MUSIC_EVENT").apply {
                                putExtra("ACTION", "CURRENT_SONG")
                                putExtra("SONG_ID", song.id)
                                putExtra("SONG_TITLE", song.title)
                                putExtra("SONG_ARTIST", song.artistNameList?.joinToString(", ") ?: "Unknown Artist")
                                putExtra("SONG_IMAGE", song.imageUrl)
                            }
                            context.sendBroadcast(broadcastIntent)
                            context.startActivity(intent)
                            Log.d("LibraryScreen", "Intent started for SongDetailsActivity")
                        } catch (e: Exception) {
                            Log.e("LibraryScreen", "Failed to start SongDetails", e)
                        }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onSongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSongClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                onError = { Log.e("LibraryScreen", "Failed to load image for ${song.title}") }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = song.artistNameList?.joinToString(", ") ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}