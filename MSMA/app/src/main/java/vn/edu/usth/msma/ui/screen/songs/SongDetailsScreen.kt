package vn.edu.usth.msma.ui.screen.songs

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch
import vn.edu.usth.msma.R
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.ui.components.LoadingScreen

@Composable
fun SongDetailsScreen(
    songId: Long,
    onBack: () -> Unit,
    fromMiniPlayer: Boolean = false,
    isPlaying: Boolean = false,
    isLoopEnabled: Boolean = false,
    isShuffleEnabled: Boolean = false,
    songRepository: SongRepository,
    context: Context
) {
    val miniPlayerViewModel: MiniPlayerViewModel =
        viewModel(viewModelStoreOwner = LocalViewModelStoreOwner.current!!)
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val song = songRepository.getSongById(songId)
    val scope = rememberCoroutineScope()
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    var currentDisplayedSong by remember { mutableStateOf(song) }
    val currentSongInMiniPlayerViewModel by miniPlayerViewModel.currentSong.collectAsState()
    val isPlayingInMiniPlayerViewModel by miniPlayerViewModel.isPlaying.collectAsState()

    LaunchedEffect(songId, fromMiniPlayer) {
        try {
            musicPlayerViewModel.registerPositionReceiver(context)

            if (fromMiniPlayer && song != null) {
                Log.d("SongDetailsScreen", "Initializing with isPlaying: $isPlaying")
                musicPlayerViewModel.updatePlaybackState(isPlaying)
                musicPlayerViewModel.setLoopEnabled(isLoopEnabled)
                musicPlayerViewModel.setShuffleEnabled(isShuffleEnabled)
                miniPlayerViewModel.updateCurrentSong(song)
                musicPlayerViewModel.updateCurrentSong(song)

                val intent = Intent(context, MusicService::class.java).apply {
                    action = "SYNC_STATE"
                    putExtra("SONG_ID", song.id)
                    putExtra("IS_PLAYING", isPlaying)
                    putExtra("IS_LOOP_ENABLED", isLoopEnabled)
                    putExtra("IS_SHUFFLE_ENABLED", isShuffleEnabled)
                    putExtra("CURRENT_POSITION", currentPosition)
                    putExtra("DURATION", duration)
                }
                context.startService(intent)
            } else {
                val isSameSong = currentSongInMiniPlayerViewModel?.id == song?.id

                if (!isSameSong && song != null) {
                    miniPlayerViewModel.updateCurrentSong(song)
                    musicPlayerViewModel.updateCurrentSong(song)
                    musicPlayerViewModel.playSong(context, song)
                    miniPlayerViewModel.updatePlaybackState(true)
                    musicPlayerViewModel.updatePlaybackState(true)
                } else if (!isPlayingInMiniPlayerViewModel && song != null) {
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = "RESUME"
                    }
                    context.startService(intent)
                    miniPlayerViewModel.updatePlaybackState(true)
                    musicPlayerViewModel.updatePlaybackState(true)
                }
            }
        } catch (e: Exception) {
            Log.e("SongDetailsScreen", "Error loading song: ${e.message}", e)
            Toast.makeText(context, "Error loading song: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        val songUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "MUSIC_EVENT") {
                    val action = intent.getStringExtra("ACTION")
                    Log.d("SongDetailsScreen", "Received MUSIC_EVENT: $action")

                    when (action) {
                        "EXPAND", "MINIMIZE" -> {
                            val newIsPlaying = intent.getBooleanExtra("IS_PLAYING", false)
                            Log.d(
                                "SongDetailsScreen",
                                "Updating isPlaying to $newIsPlaying for $action"
                            )
                            musicPlayerViewModel.updatePlaybackState(newIsPlaying)
                            musicPlayerViewModel.setLoopEnabled(
                                intent.getBooleanExtra(
                                    "IS_LOOP_ENABLED",
                                    false
                                )
                            )
                            musicPlayerViewModel.setShuffleEnabled(
                                intent.getBooleanExtra(
                                    "IS_SHUFFLE_ENABLED",
                                    false
                                )
                            )
                            musicPlayerViewModel.setFavorite(
                                intent.getBooleanExtra(
                                    "IS_FAVORITE",
                                    false
                                )
                            )
                            val newSongId = intent.getLongExtra("SONG_ID", 0L)
                            if (newSongId != 0L) {
                                try {
                                    val newSong = songRepository.getSongById(newSongId)
                                    newSong?.let {
                                        currentDisplayedSong = it
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "SongDetailsScreen",
                                        "Error loading song: ${e.message}"
                                    )
                                    Toast.makeText(
                                        context,
                                        "Error loading song: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            currentPosition = intent.getLongExtra("POSITION", 0L)
                            duration = intent.getLongExtra("DURATION", 0L)
                            musicPlayerViewModel.currentPosition.longValue = currentPosition
                            musicPlayerViewModel.duration.longValue = duration
                        }

                        "NEXT", "PREVIOUS" -> {
                            scope.launch {
                                val newSongId = intent.getLongExtra("SONG_ID", 0L)
                                if (newSongId != 0L) {
                                    try {
                                        val newSong = songRepository.getSongById(newSongId)
                                        newSong?.let {
                                            currentDisplayedSong = it
                                            miniPlayerViewModel.updateCurrentSong(it)
                                            musicPlayerViewModel.updateCurrentSong(it)
                                            duration =
                                                parseDuration(it.duration?.toString() ?: "0:00")
                                            currentPosition = 0L
                                            musicPlayerViewModel.updatePlaybackState(true)
                                            miniPlayerViewModel.updatePlaybackState(true)

                                            // Send broadcast to update MiniPlayer
                                            val broadcastIntent = Intent("MUSIC_EVENT").apply {
                                                putExtra("ACTION", "CURRENT_SONG")
                                                putExtra("SONG_ID", it.id)
                                                putExtra("SONG_TITLE", it.title)
                                                putExtra(
                                                    "SONG_ARTIST",
                                                    it.artistNameList?.joinToString(", ")
                                                        ?: "Unknown Artist"
                                                )
                                                putExtra("SONG_IMAGE", it.imageUrl)
                                                putExtra("IS_PLAYING", true)
                                                putExtra(
                                                    "IS_LOOP_ENABLED",
                                                    musicPlayerViewModel.isLoopEnabled.value
                                                )
                                                putExtra(
                                                    "IS_SHUFFLE_ENABLED",
                                                    musicPlayerViewModel.isShuffleEnabled.value
                                                )
                                                putExtra("POSITION", 0L)
                                                putExtra("DURATION", duration)
                                            }
                                            context?.sendBroadcast(broadcastIntent)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(
                                            "SongDetailsScreen",
                                            "Error loading song: ${e.message}"
                                        )
                                        Toast.makeText(
                                            context,
                                            "Error loading song: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }

                        "PLAY" -> {
                            Log.d("SongDetailsScreen", "Updating isPlaying to true for PLAY")
                            musicPlayerViewModel.updatePlaybackState(true)
                        }

                        "PAUSE" -> {
                            Log.d("SongDetailsScreen", "Updating isPlaying to false for PAUSE")
                            musicPlayerViewModel.updatePlaybackState(false)
                        }

                        "POSITION_UPDATE" -> {
                            currentPosition = intent.getLongExtra("POSITION", 0L)
                            duration = intent.getLongExtra("DURATION", 0L)
                        }

                        "DOWNLOAD_COMPLETE" -> {
                            Toast.makeText(
                                context,
                                "Song downloaded successfully ❤",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        "ADDED_TO_FAVORITES" -> musicPlayerViewModel.setFavorite(true)
                        "REMOVED_FROM_FAVORITES" -> musicPlayerViewModel.setFavorite(false)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction("MUSIC_EVENT")
        }
        ContextCompat.registerReceiver(
            context,
            songUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(songUpdateReceiver)
        }
    }

    currentDisplayedSong?.let { song ->
        PlaySong(
            song = song,
            onBack = onBack,
            onSeek = { newPosition ->
                currentPosition = newPosition
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "SEEK"
                    putExtra("POSITION", newPosition)
                }
                context.startService(intent)
            },
            musicPlayerViewModel = musicPlayerViewModel
        )
    }
}

@Composable
fun PlaySong(
    song: Song,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit,
    musicPlayerViewModel: MusicPlayerViewModel
) {
    val context = LocalContext.current
    val isPlaying by musicPlayerViewModel.isPlaying.collectAsState()
    val isFavorite by musicPlayerViewModel.isFavorite.collectAsState()
    var backgroundBrush by remember {
        mutableStateOf(Brush.verticalGradient(listOf(Color.Black, Color.DarkGray)))
    }
    val currentPosition by musicPlayerViewModel.currentPosition
    val duration by musicPlayerViewModel.duration

    LaunchedEffect(song.imageUrl) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(song.imageUrl)
            .allowHardware(false)
            .build()

        try {
            val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
            result?.let {
                val bitmap = it.toBitmap()
                Palette.from(bitmap).generate { palette ->
                    val dominantColor =
                        palette?.dominantSwatch?.rgb?.let { Color(it) } ?: Color.DarkGray
                    val secondaryColor = palette?.mutedSwatch?.rgb?.let { Color(it) } ?: Color.Black
                    backgroundBrush = Brush.verticalGradient(listOf(dominantColor, secondaryColor))
                }
            }
        } catch (e: Exception) {
            Log.e("SongDetailsScreen", "Lỗi: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = "MINIMIZE"
                        putExtra("IS_PLAYING", isPlaying)
                        putExtra("SONG_ID", song.id)
                        putExtra("SONG_TITLE", song.title)
                        putExtra(
                            "SONG_ARTIST",
                            song.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                        )
                        putExtra("SONG_IMAGE", song.imageUrl)
                        putExtra("IS_LOOP_ENABLED", musicPlayerViewModel.isLoopEnabled.value)
                        putExtra("IS_SHUFFLE_ENABLED", musicPlayerViewModel.isShuffleEnabled.value)
                        putExtra("IS_FAVORITE", isFavorite)
                        putExtra("POSITION", currentPosition)
                        putExtra("DURATION", duration)
                    }
                    context.startService(intent)
                    onBack()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Minimize",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = "DOWNLOAD_SONG"
                        putExtra("SONG_ID", song.id)
                    }
                    context.startService(intent)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AsyncImage(
            model = song.imageUrl,
            contentDescription = song.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = song.artistNameList?.joinToString(", ") ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = {
                    musicPlayerViewModel.toggleFavorite(context, song.id)
                    Toast.makeText(
                        context,
                        if (isFavorite) "Đã xóa khỏi yêu thích" else "Đã thêm vào yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = "Favorite/Unfavorite",
                    tint = if (isFavorite) Color(0xFF03A9F4) else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        MusicControls(
            isPlaying = isPlaying,
            onPlayPause = {
                if (isPlaying) {
                    musicPlayerViewModel.pauseSong(context)
                } else {
                    if (musicPlayerViewModel.currentPosition.longValue > 0) {
                        musicPlayerViewModel.resumeSong(context)
                    } else {
                        musicPlayerViewModel.playSong(context, song)
                    }
                }
            },
            onNext = {
                musicPlayerViewModel.playNextSong(context)
            },
            onPrevious = {
                musicPlayerViewModel.playPreviousSong(context)
            },
            progress = musicPlayerViewModel.progress,
            duration = musicPlayerViewModel.duration,
            onSeek = { newProgress ->
                val newPosition =
                    (newProgress * musicPlayerViewModel.duration.longValue).toLong()
                musicPlayerViewModel.currentPosition.longValue = newPosition
                onSeek(newPosition)
            },
            viewModel = musicPlayerViewModel,
            isLoopEnabled = musicPlayerViewModel.isLoopEnabled.collectAsState().value,
            isShuffleEnabled = musicPlayerViewModel.isShuffleEnabled.collectAsState().value,
            onToggleLoop = { musicPlayerViewModel.toggleLoop(context) },
            onToggleShuffle = { musicPlayerViewModel.toggleShuffle(context) }
        )
    }
}

@Composable
fun MusicControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    progress: Float,
    duration: MutableLongState,
    onSeek: (Float) -> Unit,
    viewModel: MusicPlayerViewModel,
    isLoopEnabled: Boolean,
    isShuffleEnabled: Boolean,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit
) {
    val context = LocalContext.current

    Log.d("MusicControls", "Rendering with isPlaying: $isPlaying")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray,
            ),
            value = progress,
            onValueChange = { newValue ->
                viewModel.isDragging.value = true
                onSeek(newValue)
            },
            onValueChangeFinished = {
                viewModel.seekTo(context, viewModel.currentPosition.longValue)
                viewModel.isDragging.value = false
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime((progress * duration.longValue).toLong()),
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = formatTime(duration.longValue),
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) Color(0xFF03A9F4) else Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Previous",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .scale(if (isPlaying) 1.1f else 1.0f)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.pause_circle_filled else R.drawable.play_circle
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Next",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onToggleLoop,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isLoopEnabled) R.drawable.repeat_one else R.drawable.repeat
                    ),
                    contentDescription = "Loop",
                    tint = if (isLoopEnabled) Color(0xFF03A9F4) else Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatTime(timeMs: Long): String {
    val minutes = (timeMs / 1000) / 60
    val seconds = (timeMs / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun parseDuration(duration: String): Long {
    return try {
        val parts = duration.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            (minutes * 60 + seconds) * 1000
        } else {
            0L
        }
    } catch (e: Exception) {
        0L
    }
}