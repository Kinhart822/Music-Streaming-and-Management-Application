package vn.edu.usth.msma.ui.screen.songs

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.edu.usth.msma.R
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.navigation.NavigationViewModel
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.ui.components.LoadingScreen
import kotlin.math.roundToInt

@Composable
fun SongDetailsScreen(
    song: Song,
    onBack: () -> Unit,
    fromMiniPlayer: Boolean = false,
    songRepository: SongRepository,
    context: Context,
    navController: NavController
) {
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val currentSong by musicPlayerViewModel.currentSong.collectAsState()
    val isPlaying by musicPlayerViewModel.isPlaying.collectAsState()
    val isLoopEnabled by musicPlayerViewModel.isLoopEnabled.collectAsState()
    val isShuffleEnabled by musicPlayerViewModel.isShuffleEnabled.collectAsState()
    val currentPosition by musicPlayerViewModel.currentPosition
    val duration by musicPlayerViewModel.duration

    // Sử dụng state để theo dõi bài hát hiện tại
    var currentDisplayedSong by remember { mutableStateOf(song) }

    // Animation for drag offset
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(durationMillis = 300),
        label = "dragOffset"
    )

    LaunchedEffect(Unit) {
        try {
            musicPlayerViewModel.registerMusicEventReceiver(context)

            // Check if the current song is already loaded
            val isSameSong = currentSong?.id == song.id

            if (!fromMiniPlayer) {
                if (!isSameSong) {
                    musicPlayerViewModel.playSong(context, song)
                } else if (isPlaying) {
                    musicPlayerViewModel.resumeSong(context)
                }
            }

            // Cập nhật trạng thái từ MiniPlayer nếu đang mở từ MiniPlayer
            if (fromMiniPlayer) {
                musicPlayerViewModel.refreshCurrentSongData(context)
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
                        "CLOSE" -> onBack()
                        "CURRENT_SONG", "NEXT", "PREVIOUS" -> {
                            scope.launch {
                                val newSongId = intent.getLongExtra("SONG_ID", 0L)
                                if (newSongId != 0L) {
                                    val newSong = songRepository.getSongById(newSongId)
                                    newSong?.let {
                                        // Cập nhật UI với bài hát mới
                                        currentDisplayedSong = newSong

                                        musicPlayerViewModel.updateCurrentSong(it)
                                        musicPlayerViewModel.updatePlaybackState(true)
                                        musicPlayerViewModel.setLoopEnabled(
                                            intent.getBooleanExtra("IS_LOOP_ENABLED", false)
                                        )
                                        musicPlayerViewModel.setShuffleEnabled(
                                            intent.getBooleanExtra("IS_SHUFFLE_ENABLED", false)
                                        )
                                    }
                                }
                            }
                        }

                        "PLAY", "RESUMED", "LOADED" -> {
                            Log.d("SongDetailsScreen", "Updating isPlaying to true for PLAY")
                            musicPlayerViewModel.updatePlaybackState(true)
                        }

                        "PAUSE" -> {
                            Log.d("SongDetailsScreen", "Updating isPlaying to false for PAUSE")
                            musicPlayerViewModel.updatePlaybackState(false)
                        }

                        "POSITION_UPDATE" -> {
                            musicPlayerViewModel.currentPosition.longValue = currentPosition
                            musicPlayerViewModel.duration.longValue = duration
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

    currentSong?.let { song ->
        PlaySong(
            song = song,
            onBack = {
                // Gửi thông tin bài hát hiện tại trước khi thoát
                val intent = Intent("MUSIC_EVENT").apply {
                    putExtra("ACTION", "CURRENT_SONG")
                    putExtra("SONG_ID", song.id)
                    putExtra("SONG_TITLE", song.title)
                    putExtra(
                        "SONG_ARTIST",
                        song.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                    )
                    putExtra("SONG_IMAGE", song.imageUrl)
                    putExtra("IS_PLAYING", isPlaying)
                    putExtra("IS_LOOP_ENABLED", isLoopEnabled)
                    putExtra("IS_SHUFFLE_ENABLED", isShuffleEnabled)
                    putExtra("POSITION", currentPosition)
                    putExtra("DURATION", duration)
                }
                context.sendBroadcast(intent)

                // Hiển thị MiniPlayer
                CoroutineScope(Dispatchers.IO).launch {
                    navigationViewModel.preferencesManager.setMiniPlayerVisible(true)
                }
                navController.popBackStack()
            },
            onSeek = { newPosition ->
                musicPlayerViewModel.seekTo(context, newPosition)
            },
            musicPlayerViewModel = musicPlayerViewModel,
            modifier = Modifier
                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0) { // Chỉ cho phép kéo xuống
                                dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                            }
                        },
                        onDragEnd = {
                            if (dragOffset > 100) { // Ngưỡng để thoát
                                // Gửi thông tin bài hát hiện tại trước khi thoát
                                val intent = Intent("MUSIC_EVENT").apply {
                                    putExtra("ACTION", "CURRENT_SONG")
                                    putExtra("SONG_ID", song.id)
                                    putExtra("SONG_TITLE", song.title)
                                    putExtra(
                                        "SONG_ARTIST",
                                        song.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                                    )
                                    putExtra("SONG_IMAGE", song.imageUrl)
                                    putExtra("IS_PLAYING", isPlaying)
                                    putExtra("IS_LOOP_ENABLED", isLoopEnabled)
                                    putExtra("IS_SHUFFLE_ENABLED", isShuffleEnabled)
                                    putExtra("POSITION", currentPosition)
                                    putExtra("DURATION", duration)
                                }
                                context.sendBroadcast(intent)

                                // Hiển thị MiniPlayer
                                CoroutineScope(Dispatchers.IO).launch {
                                    navigationViewModel.preferencesManager.setMiniPlayerVisible(true)
                                }
                                navController.popBackStack()
                            }
                            dragOffset = 0f
                        }
                    )
                }
        )
    } ?: LoadingScreen()
}

@Composable
fun PlaySong(
    song: Song,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit,
    musicPlayerViewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPlaying by musicPlayerViewModel.isPlaying.collectAsState()
    val isFavorite by musicPlayerViewModel.isFavorite.collectAsState()
    var backgroundBrush by remember {
        mutableStateOf(Brush.verticalGradient(listOf(Color.Black, Color.DarkGray)))
    }
    var dragOffset by remember { mutableFloatStateOf(0f) }

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
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        if (dragOffset > 100) {
                            onBack()
                        } else {
                            dragOffset = 0f
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
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
                val newPosition = (progress * duration.longValue).toLong()
                viewModel.seekTo(context, newPosition)
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