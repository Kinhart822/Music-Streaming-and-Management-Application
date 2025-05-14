package vn.edu.usth.msma.ui.screen.search

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.gson.Gson
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.screen.search.genres.GenreActivity
import vn.edu.usth.msma.ui.screen.songs.SongDetailsActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Show error toast
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar with Cancel button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Search for music") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon"
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.onSearchQueryChange(state.searchQuery) }
                )
            )
            if (isSearchFocused || state.searchQuery.isNotEmpty()) {
                TextButton(onClick = {
                    viewModel.onSearchQueryChange("")
                    isSearchFocused = false
                    focusManager.clearFocus()
                }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Show genres or search results
        if (!isSearchFocused && state.searchQuery.isEmpty()) {
            // Loading indicator for genres
            if (state.isLoadingGenres) {
                LoadingScreen(message = "Loading music genres...")
            } else if (state.genres.isNotEmpty()) {
                // Genres grid
                Text(
                    text = "Genres",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.genres) { genre ->
                        GenreCard(
                            genre = genre,
                            onClick = {
                                val intent = Intent(context, GenreActivity::class.java).apply {
                                    putExtra("genre", Gson().toJson(genre))
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No genres available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // Show tabs
            val tabs = listOf("All", "Songs", "Playlists", "Albums", "Artists")
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(state.selectedTab),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab) }
                    )
                }
            }

            // Loading indicator for contents
            if (state.isLoadingContents) {
                LoadingScreen(message = "Searching...")
            } else if (state.contents.isNotEmpty()) {
                Text(
                    text = "Search results for \"${state.searchQuery}\"",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.contents) { content ->
                        ContentItemView(
                            content = content,
                            context = context
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

}

@Composable
fun GenreCard(
    genre: GenreResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = genre.imageUrl,
                contentDescription = genre.name,
                modifier = Modifier
                    .size(100.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = genre.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ContentItemView(
    content: ContentItem,
    context: Context
) {
    when (content) {
        is ContentItem.SongItem -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(context, SongDetailsActivity::class.java).apply {
                            putExtra("SONG_ID", content.id)
                            putExtra("FROM_MINI_PLAYER", false)
                            putExtra("IS_PLAYING", false)
                            putExtra("IS_LOOP_ENABLED", false)
                            putExtra("IS_SHUFFLE_ENABLED", false)
                        }
                        // Send broadcast to update MiniPlayer
                        val broadcastIntent = Intent("MUSIC_EVENT").apply {
                            putExtra("ACTION", "CURRENT_SONG")
                            putExtra("SONG_ID", content.id)
                            putExtra("SONG_TITLE", content.title)
                            putExtra("SONG_ARTIST", content.artistNameList?.joinToString(", ") ?: "Unknown Artist")
                            putExtra("SONG_IMAGE", content.imageUrl)
                            putExtra("IS_PLAYING", false)
                            putExtra("IS_LOOP_ENABLED", false)
                            putExtra("IS_SHUFFLE_ENABLED", false)
                        }
                        context.sendBroadcast(broadcastIntent)
                        context.startActivity(intent)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = content.imageUrl,
                        contentDescription = content.title,
                        modifier = Modifier
                            .size(48.dp)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = content.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Song - ${content.artistNameList?.joinToString(", ") ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        is ContentItem.PlaylistItem -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(context, SongDetailsActivity::class.java).apply {
                            putExtra("playlistId", content.id)
                        }
                        context.startActivity(intent)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = content.imageUrl,
                        contentDescription = content.playlistName,
                        modifier = Modifier
                            .size(48.dp)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = content.playlistName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Playlist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        is ContentItem.AlbumItem -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(context, SongDetailsActivity::class.java).apply {
                            putExtra("albumId", content.id)
                        }
                        context.startActivity(intent)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = content.imageUrl,
                        contentDescription = content.albumName,
                        modifier = Modifier
                            .size(48.dp)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = content.albumName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Album",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        is ContentItem.ArtistItem -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        val intent = Intent(context, SongDetailsActivity::class.java).apply {
                            putExtra("artistId", content.id)
                        }
                        context.startActivity(intent)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = content.avatar ?: content.image,
                        contentDescription = content.artistName,
                        modifier = Modifier
                            .size(48.dp)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = content.artistName ?: "${content.firstName} ${content.lastName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}