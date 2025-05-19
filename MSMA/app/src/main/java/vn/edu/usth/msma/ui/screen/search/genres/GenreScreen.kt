package vn.edu.usth.msma.ui.screen.search.genres

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.gson.Gson
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.navigation.NavigationViewModel
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.components.SongItem
import vn.edu.usth.msma.ui.screen.songs.MusicPlayerViewModel
import vn.edu.usth.msma.utils.helpers.toSong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreScreen(
    genre: GenreResponse,
    onBack: () -> Unit,
    viewModel: GenreViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val isMiniPlayerVisible by navigationViewModel.preferencesManager.isMiniPlayerVisibleFlow.collectAsState(
        initial = false
    )
    var selectedSong by remember { mutableStateOf<SongResponse?>(null) }

    // Show error toast
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    DisposableEffect(context) {
        musicPlayerViewModel.registerMusicEventReceiver(context)
        onDispose {
            musicPlayerViewModel.unregisterMusicEventReceiver(context)
        }
    }

    LaunchedEffect(Unit) {
        musicPlayerViewModel.refreshCurrentSongData(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = if (isMiniPlayerVisible) 60.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Genre image
        item {
            AsyncImage(
                model = state.genre.imageUrl,
                contentDescription = state.genre.name,
                modifier = Modifier
                    .size(200.dp)
                    .aspectRatio(1f)
                    .padding(top = 16.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Genre name
        item {
            Text(
                text = state.genre.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Brief description
        item {
            Text(
                text = state.genre.briefDescription,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Full description (toggleable)
        if (state.isFullDescriptionVisible) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.genre.fullDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Show More / Show Less button
        item {
            TextButton(
                onClick = { viewModel.toggleFullDescription() }
            ) {
                Text(
                    text = if (state.isFullDescriptionVisible) "Show Less" else "Show More",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Songs header
        item {
            Text(
                text = "Songs in ${state.genre.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        // Loading indicator for songs
        if (state.isLoadingSongs) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingScreen(message = "Loading songs...")
                }
            }
        } else if (state.songs.isNotEmpty()) {
            // Songs list
            items(state.songs) { song ->
                SongItem(
                    song = song.toSong(),
                    onSongClick = {
                        val songJson = Gson().toJson(song)
                        navController.navigate(
                            ScreenRoute.SongDetails.createRoute(songJson, false)
                        ) {
                            popUpTo(ScreenRoute.SongDetails.route) { inclusive = true }
                        }
                    }
                )
            }
        } else {
            item {
                Text(
                    text = "No songs available",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}