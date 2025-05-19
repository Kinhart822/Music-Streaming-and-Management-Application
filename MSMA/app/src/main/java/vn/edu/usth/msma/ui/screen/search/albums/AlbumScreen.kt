package vn.edu.usth.msma.ui.screen.search.albums

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.gson.Gson
import vn.edu.usth.msma.data.Album
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.components.SongItem
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import vn.edu.usth.msma.utils.helpers.toSong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    album: Album,
    viewModel: AlbumViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val albumState by viewModel.album.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    LaunchedEffect(album) {
        viewModel.loadAlbum(album)
        viewModel.checkSavedStatus(album)
    }

    LaunchedEffect(Unit) {
        EventBus.events.collect { event ->
            when (event) {
                is Event.SavingAlbumEvent, is Event.UnSavingAlbumEvent -> {
                    viewModel.refreshAlbum(album)
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
                        text = "Loading album...",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                }
            } else if (albumState == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Album not found",
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
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
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
                                    model = albumState?.imageUrl,
                                    contentDescription = "${albumState?.albumName} background",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 10.dp),
                                    onError = {
                                        Log.e(
                                            "PlaylistScreen",
                                            "Failed to load background image for ${albumState?.albumName}"
                                        )
                                    }
                                )
                            }
                        }

                        // Playlist Info
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = albumState?.albumName ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (isSaved) {
                                            viewModel.unSavingAlbum(albumState!!)
                                        } else {
                                            viewModel.savingAlbum(albumState!!)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isSaved) "Unsave" else "Save")
                                }
                            }
                        }

                        // Songs Section
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
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
                    }
                }
            }
        }

    }
}