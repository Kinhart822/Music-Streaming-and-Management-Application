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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.gson.Gson
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.ui.screen.search.genres.GenreActivity
import vn.edu.usth.msma.ui.screen.search.songs.DetailsSongActivity

@Composable
fun SearchNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "search") {
        composable("search") {
            SearchScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var isSearchFocused by remember { mutableStateOf(false) }

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
                }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Show genres
        if (!isSearchFocused && state.searchQuery.isEmpty()) {
            // Loading indicator for genres
            if (state.isLoadingGenres) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp)
                )
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
                Text(
                    text = "No genres available",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
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
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp)
                )
            } else if (state.contents.isNotEmpty()) {
                Text(
                    text = "Search results for \"${state.searchQuery}\"",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn {
                    items(state.contents) { content ->
                        ContentItemView(
                            content = content,
                            context = context
                        )
                    }
                }
            } else {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                if (content is ContentItem.SongItem) {
                    val intent = Intent(context, DetailsSongActivity::class.java).apply {
                        putExtra("SONG_ID", content.id)
                    }
                    context.startActivity(intent)
                }
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
                model = when (content) {
                    is ContentItem.SongItem -> content.imageUrl
                    is ContentItem.PlaylistItem -> content.imageUrl
                    is ContentItem.AlbumItem -> content.imageUrl
                    is ContentItem.ArtistItem -> content.avatar ?: content.image
                },
                contentDescription = when (content) {
                    is ContentItem.SongItem -> content.title
                    is ContentItem.PlaylistItem -> content.playlistName
                    is ContentItem.AlbumItem -> content.albumName
                    is ContentItem.ArtistItem -> content.artistName
                },
                modifier = Modifier
                    .size(48.dp)
                    .aspectRatio(1f)
                    .clip(if (content is ContentItem.ArtistItem) CircleShape else MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = when (content) {
                        is ContentItem.SongItem -> content.title
                        is ContentItem.PlaylistItem -> content.playlistName
                        is ContentItem.AlbumItem -> content.albumName
                        is ContentItem.ArtistItem -> content.artistName ?: "${content.firstName} ${content.lastName}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (content) {
                        is ContentItem.SongItem -> "Song"
                        is ContentItem.PlaylistItem -> "Playlist"
                        is ContentItem.AlbumItem -> "Album"
                        is ContentItem.ArtistItem -> "Artist"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}