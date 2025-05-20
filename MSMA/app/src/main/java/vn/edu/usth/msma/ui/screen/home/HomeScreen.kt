package vn.edu.usth.msma.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import vn.edu.usth.msma.R
import vn.edu.usth.msma.ui.components.AlbumItem
import vn.edu.usth.msma.ui.components.ArtistItem
import androidx.compose.foundation.shape.RoundedCornerShape
import vn.edu.usth.msma.ui.components.HomeAnimation
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.components.PlaylistItem
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.screen.home.songs.Top10TrendingScreen
import vn.edu.usth.msma.ui.screen.home.songs.Top15DownloadScreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val recentPlaylists by viewModel.recentPlaylists.collectAsState()
    val recentAlbums by viewModel.recentAlbums.collectAsState()
    val recentArtists by viewModel.recentArtists.collectAsState()
    val isPlaylistsLoading by viewModel.isPlaylistsLoading.collectAsState()
    val isAlbumsLoading by viewModel.isAlbumsLoading.collectAsState()
    val isArtistsLoading by viewModel.isArtistsLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeAnimation()
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Liked Songs (Default Item)
                item {
                    Text(
                        text = "Liked Songs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .clickable { navController.navigate(ScreenRoute.FavoriteSongs.route) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.liked_song),
                                        contentDescription = "Liked Songs",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Liked Songs",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Top 10 Trending Songs
                item {
                    Text(
                        text = "Top 10 Trending Songs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .clickable { navController.navigate(ScreenRoute.Top10TrendingSongs.route) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.top_10),
                                        contentDescription = "Top 10 Trending Songs",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Top 10 Trending",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Top 15 Download Songs
                item {
                    Text(
                        text = "Top 15 Download Songs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .clickable { navController.navigate(ScreenRoute.Top15DownloadedSongs.route) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.top_15),
                                        contentDescription = "Top 15 Download Songs",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Top 15 Downloads",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recent Playlists
                item {
                    Text(
                        text = "Recent Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (isPlaylistsLoading) {
                        LoadingScreen(message = "Loading recent playlists...")
                    } else if (recentPlaylists.isEmpty()) {
                        Text(
                            text = "No recent playlists found",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(recentPlaylists) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    onPlayerClick = {
                                        val playlistJson = Gson().toJson(playlist) ?: return@PlaylistItem
                                        navController.navigate(
                                            ScreenRoute.PlaylistDetails.createRoute(playlistJson)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recent Albums
                item {
                    Text(
                        text = "Recent Albums",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (isAlbumsLoading) {
                        LoadingScreen(message = "Loading recent albums...")
                    } else if (recentAlbums.isEmpty()) {
                        Text(
                            text = "No recent albums found",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(recentAlbums) { album ->
                                AlbumItem(
                                    album = album,
                                    onAlbumClick = {
                                        val albumJson = Gson().toJson(album) ?: return@AlbumItem
                                        navController.navigate(
                                            ScreenRoute.AlbumDetails.createRoute(albumJson)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recent Artists
                item {
                    Text(
                        text = "Recent Artists",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (isArtistsLoading) {
                        LoadingScreen(message = "Loading recent artists...")
                    } else if (recentArtists.isEmpty()) {
                        Text(
                            text = "No recent artists found",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(recentArtists) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    onArtistClick = {
                                        val artistJson = Gson().toJson(artist) ?: return@ArtistItem
                                        navController.navigate(
                                            ScreenRoute.ArtistDetails.createRoute(artistJson)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}