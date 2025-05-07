package vn.edu.usth.msma.ui.screen.search.songs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import vn.edu.usth.msma.R
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService

@Composable
fun DetailSongScreen(
    songId: Long,
    onBack: () -> Unit,
    songRepository: SongRepository,
    context: Context
) {
    val scope = rememberCoroutineScope()
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Fetch song and initialize playback
    LaunchedEffect(songId) {
        currentSong = songRepository.getSongById(songId)
        currentSong?.let { song ->
            duration = parseDuration(song.duration.toString())
            val intent = Intent(context, MusicService::class.java).apply {
                action = "PLAY"
                putExtra("SONG_PATH", song.mp3Url)
                putExtra("SONG_TITLE", song.title)
                putExtra("SONG_ARTIST", song.artistNameList?.joinToString(", ") ?: "")
                putExtra("SONG_IMAGE", song.imageUrl)
                putExtra("SONG_ID", song.id)
            }
            context.startService(intent)
            isPlaying = true
        }
    }

    // Broadcast receiver for playback updates
    DisposableEffect(Unit) {
        val songUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "MUSIC_EVENT") {
                    when (intent.getStringExtra("ACTION")) {
                        "NEXT", "PREVIOUS" -> {
                            scope.launch {
                                val newSongId = intent.getStringExtra("SONG_ID")?.toLongOrNull() ?: return@launch
                                val newSong = songRepository.getSongById(newSongId)
                                newSong?.let {
                                    currentSong = it
                                    duration = parseDuration(it.duration.toString())
                                    currentPosition = 0L
                                    isPlaying = true
                                }
                            }
                        }
                        "PLAY" -> isPlaying = true
                        "PAUSE" -> isPlaying = false
                        "POSITION_UPDATE" -> {
                            currentPosition = intent.getLongExtra("POSITION", 0L)
                            duration = intent.getLongExtra("DURATION", duration)
                        }
                        "DOWNLOAD_COMPLETE" -> {
                            Toast.makeText(context, "Lưu nhạc thành công ❤", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter("MUSIC_EVENT")
        ContextCompat.registerReceiver(context, songUpdateReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        onDispose {
            context.unregisterReceiver(songUpdateReceiver)
        }
    }

    currentSong?.let { song ->
        PlaySong(
            song = song,
            songRepository = songRepository,
            context = context,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onBack = onBack,
            onPlayPause = {
                val intent = Intent(context, MusicService::class.java).apply {
                    action = if (isPlaying) "PAUSE" else "RESUME"
                }
                context.startService(intent)
                isPlaying = !isPlaying
            },
            onNext = {
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "NEXT"
                }
                context.startService(intent)
            },
            onPrevious = {
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "PREVIOUS"
                }
                context.startService(intent)
            },
            onSeek = { newPosition ->
                currentPosition = newPosition
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "SEEK"
                    putExtra("POSITION", newPosition)
                }
                context.startService(intent)
            }
        )
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PlaySong(
    song: Song,
    songRepository: SongRepository,
    context: Context,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit
) {
    var backgroundBrush by remember {
        mutableStateOf(Brush.verticalGradient(listOf(Color.Black, Color.DarkGray)))
    }

    LaunchedEffect(song.imageUrl) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(song.imageUrl)
            .allowHardware(false)
            .build()

        val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
        result?.let {
            val bitmap = it.toBitmap()
            Palette.from(bitmap).generate { palette ->
                val dominantColor = palette?.dominantSwatch?.rgb?.let { Color(it) } ?: Color.DarkGray
                val secondaryColor = palette?.mutedSwatch?.rgb?.let { Color(it) } ?: Color.Black
                backgroundBrush = Brush.verticalGradient(listOf(dominantColor, secondaryColor))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(
                onClick = {
                    Toast.makeText(context, "Đang tải bài hát ${song.title}", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = "DOWNLOAD_SONG"
                        putExtra("SONG_ID", song.id)
                    }
                    context.startService(intent)
                },
                modifier = Modifier
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 56.dp)
        ) {
            LoadImage(song.imageUrl ?: "")

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = song.title,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = song.artistNameList?.joinToString(", ") ?: "Unknown Artist",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )

                IconButton(
                    onClick = {
                        // Favorite functionality removed as per provided code
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.favorite_border),
                        contentDescription = "Favorite/Unfavorite",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFE91E63)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            MusicControls(
                isPlaying = isPlaying,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = onSeek
            )
        }
    }
}

@Composable
fun LoadImage(url: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build(),
        placeholder = painterResource(R.drawable.logo),
        error = painterResource(R.drawable.logo),
        contentDescription = "Song Image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(20.dp))
    )
}

@Composable
fun MusicControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var isLoopEnabled by remember { mutableStateOf(false) }
    var isRandomEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                inactiveTrackColor = Color.Gray
            ),
            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
            onValueChange = { newValue ->
                val newPosition = (newValue * duration).toLong()
                onSeek(newPosition)
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = formatTime(duration),
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isLoopEnabled = !isLoopEnabled
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = if (isLoopEnabled) "LOOP_ON" else "LOOP_OFF"
                    }
                    context.startService(intent)
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isLoopEnabled) R.drawable.repeat_one else R.drawable.repeat
                    ),
                    contentDescription = "Loop",
                    tint = if (isLoopEnabled) Color(0xFFFCFFFC) else Color.White,
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
                    contentDescription = "Play/Pause",
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
                onClick = {
                    isRandomEnabled = !isRandomEnabled
                    val intent = Intent(context, MusicService::class.java).apply {
                        action = if (isRandomEnabled) "SHUFFLE_ON" else "SHUFFLE_OFF"
                    }
                    context.startService(intent)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (isRandomEnabled) Color(0xFF03A9F4) else Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val minutes = (timeMs / 1000) / 60
    val seconds = (timeMs / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun parseDuration(duration: String): Long {
    val parts = duration.split(":")
    val minutes = parts[0].toLong()
    val seconds = parts[1].toLong()
    return (minutes * 60 + seconds) * 1000
}