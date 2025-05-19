package vn.edu.usth.msma.ui.screen.search.artists

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.gson.Gson
import vn.edu.usth.msma.data.Artist
import vn.edu.usth.msma.ui.components.AlbumItem
import vn.edu.usth.msma.ui.components.PlaylistItem
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.components.SongItem
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import vn.edu.usth.msma.utils.helpers.toAlbum
import vn.edu.usth.msma.utils.helpers.toPlaylist
import vn.edu.usth.msma.utils.helpers.toSong

@Composable
fun ArtistProfileScreen(
    artist: Artist,
    viewModel: ArtistProfileViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val artistState by viewModel.artist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()

    // Load artist and check follow status on initial composition
    LaunchedEffect(artist) {
        viewModel.loadArtist(artist)
        viewModel.checkFollowStatus(artist)
    }

    // Collect EventBus events to reload artist on follow/unfollow
    LaunchedEffect(Unit) {
        EventBus.events.collect { event ->
            when (event) {
                is Event.FollowArtistUpdateEvent, is Event.UnFollowArtistUpdateEvent -> {
                    viewModel.refreshArtist(artist)
                }
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Loading artist profile...",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                }
            } else if (artistState == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Artist not found",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Scrollable Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 5.dp)
                            .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        // Background Image
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                AsyncImage(
                                    model = artistState?.image,
                                    contentDescription = "${artistState?.artistName} background",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 10.dp)
                                )

                                AsyncImage(
                                    model = artistState?.avatar,
                                    contentDescription = artistState?.artistName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 48.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                        .zIndex(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(56.dp))
                        }

                        // Artist Info
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = artistState?.artistName ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${formatCount(artistState?.numberOfFollowers)} followers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (isFollowing) {
                                            viewModel.unfollowArtist(artistState!!)
                                        } else {
                                            viewModel.followArtist(artistState!!)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isFollowing) "Unfollow" else "Follow")
                                }
                            }
                        }

                        // Songs Section
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Songs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (songs.isEmpty()) {
                                Text(
                                    text = "No songs found",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Song Items
                        items(songs, key = { song -> song.id }) { song ->
                            SongItem(song = song.toSong(), onSongClick = {
                                val songJson = Gson().toJson(song.toSong())
                                navController.navigate(
                                    ScreenRoute.SongDetails.createRoute(songJson, false)
                                )
                            })
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Playlists Section
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Playlists",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (playlists.isEmpty()) {
                                Text(
                                    text = "No playlists found",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Playlist Items
                        items(playlists, key = { playlist -> playlist.id }) { playlist ->
                            PlaylistItem(playlist = playlist.toPlaylist(), onPlayerClick = {
                                val playlistJson = Gson().toJson(playlist.toPlaylist())
                                navController.navigate(
                                    ScreenRoute.PlaylistDetails.createRoute(playlistJson)
                                )
                            })
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Albums Section
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Albums",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (albums.isEmpty()) {
                                Text(
                                    text = "No albums found",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Album Items
                        items(albums, key = { album -> album.id }) { album ->
                            AlbumItem(album = album.toAlbum(), onAlbumClick = {
                                val albumJson = Gson().toJson(album.toAlbum())
                                navController.navigate(
                                    ScreenRoute.AlbumDetails.createRoute(albumJson)
                                )
                            })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatCount(number: Long?): String {
    if (number == null) return "0"
    return when {
        number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000f)
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000f)
        number >= 1_000 -> String.format("%.1fK", number / 1_000f)
        else -> number.toString()
    }.replace(".0", "")
}