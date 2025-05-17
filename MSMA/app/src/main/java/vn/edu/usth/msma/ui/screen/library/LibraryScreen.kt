package vn.edu.usth.msma.ui.screen.library

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.screen.songs.MusicPlayerViewModel
import vn.edu.usth.msma.utils.eventbus.Event.InitializeDataLibrary
import vn.edu.usth.msma.utils.eventbus.EventBus

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val context = LocalContext.current
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(context) {
        musicPlayerViewModel.registerMusicEventReceiver(context)
        onDispose {
            musicPlayerViewModel.unregisterMusicEventReceiver(context)
        }
    }

    LaunchedEffect(Unit) {
        musicPlayerViewModel.refreshCurrentSongData(context)
    }

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
                        val songJson = Gson().toJson(song)
                        navController.navigate(
                            ScreenRoute.SongDetails.createRoute(songJson, false)
                        ) {
                            popUpTo(ScreenRoute.SongDetails.route) { inclusive = true }
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