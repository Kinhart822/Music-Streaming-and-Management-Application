package vn.edu.usth.msma.ui.screen.search.genres

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.navigation.NavigationViewModel
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.screen.songs.MiniPlayerScreen
import vn.edu.usth.msma.ui.screen.songs.MiniPlayerViewModel
import vn.edu.usth.msma.ui.screen.songs.MusicPlayerViewModel
import vn.edu.usth.msma.ui.screen.songs.SongDetailsActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreScreen(
    genre: GenreResponse,
    onBack: () -> Unit,
    viewModel: GenreViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val isMiniPlayerVisible by navigationViewModel.preferencesManager.isMiniPlayerVisibleFlow.collectAsState(
        initial = false
    )

    val currentSong by miniPlayerViewModel.currentSong.collectAsState()
    val isPlaying by miniPlayerViewModel.isPlaying.collectAsState()

    // Show error toast
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    DisposableEffect(context) {
        miniPlayerViewModel.registerReceivers(context)
        onDispose {
            miniPlayerViewModel.unregisterMusicEventReceiver(context)
        }
    }

    LaunchedEffect(Unit) {
        miniPlayerViewModel.refreshCurrentSongData(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.genre.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (isMiniPlayerVisible) {
                currentSong?.let {
                    MiniPlayerScreen(
                        song = it,
                        isPlaying = isPlaying,
                        onPlayPauseClick = {
                            if (isPlaying) {
                                // Pause music
                                val intent =
                                    Intent(context, MusicService::class.java).apply {
                                        action = "PAUSE"
                                    }
                                context.startService(intent)
                                miniPlayerViewModel.updatePlaybackState(false)
                            } else {
                                // Resume music
                                val intent =
                                    Intent(context, MusicService::class.java).apply {
                                        action = "RESUME"
                                        putExtra("SEEK_POSITION", 0L)
                                    }
                                context.startService(intent)
                                miniPlayerViewModel.updatePlaybackState(true)
                            }
                        },
                        musicPlayerViewModel = musicPlayerViewModel,
                        onMiniPlayerClick = { miniPlayerViewModel.openDetails(context) },
                        onCloseClick = {
                            val intent =
                                Intent(context, MusicService::class.java).apply {
                                    action = "CLOSE"
                                }
                            context.startService(intent)
                        },
                        miniPlayerViewModel = miniPlayerViewModel
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        song = song,
                        onSongClick = {
                            val intent = Intent(context, SongDetailsActivity::class.java).apply {
                                putExtra("SONG_ID", song.id)
                            }
                            context.startActivity(intent)
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
}

@Composable
fun SongItem(
    song: SongResponse,
    onSongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onSongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.imageUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(48.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Song - ${song.artistNameList.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}