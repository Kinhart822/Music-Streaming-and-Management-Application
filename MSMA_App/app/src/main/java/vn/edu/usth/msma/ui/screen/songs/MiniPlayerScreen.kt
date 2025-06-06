package vn.edu.usth.msma.ui.screen.songs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.gson.Gson
import vn.edu.usth.msma.R
import vn.edu.usth.msma.ui.components.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerScreen(
    musicPlayerViewModel: MusicPlayerViewModel,
    onCloseClick: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val currentSong by musicPlayerViewModel.currentSong.collectAsState()
    val isPlaying by musicPlayerViewModel.isPlaying.collectAsState()
    val isFavorite by musicPlayerViewModel.isFavorite.collectAsState()
    val currentPosition by musicPlayerViewModel.currentPosition
    val duration by musicPlayerViewModel.duration
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    var dragOffset by remember { mutableFloatStateOf(0f) }

    var backgroundBrush by remember {
        mutableStateOf(Brush.horizontalGradient(listOf(Color(0xFF121212), Color(0xFF2D2D2D))))
    }

    LaunchedEffect(currentSong?.imageUrl) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(currentSong?.imageUrl)
            .allowHardware(false)
            .build()

        val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
        result?.let {
            val bitmap = it.toBitmap()
            Palette.from(bitmap).generate { palette ->
                val dominantColor =
                    palette?.dominantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF121212)
                val secondaryColor =
                    palette?.mutedSwatch?.rgb?.let { Color(it) } ?: Color(0xFF2D2D2D)
                backgroundBrush = Brush.horizontalGradient(listOf(dominantColor, secondaryColor))
            }
        }
    }

    LaunchedEffect(Unit) {
        musicPlayerViewModel.refreshCurrentSongData(context)
        musicPlayerViewModel.registerMusicEventReceiver(context)
    }

    DisposableEffect(Unit) {
        musicPlayerViewModel.registerMusicEventReceiver(context)
        onDispose {
            musicPlayerViewModel.unregisterMusicEventReceiver()
        }
    }

    currentSong?.let { song ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray)
                .offset(y = dragOffset.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount < 0) { // Chỉ cho phép kéo lên
                                dragOffset += dragAmount
                            }
                        },
                        onDragEnd = {
                            if (dragOffset < -50) { // Ngưỡng để mở SongDetails
                                val songJson = Gson().toJson(song)
                                navController.navigate(
                                    ScreenRoute.SongDetails.createRoute(songJson, true)
                                ) {
                                    popUpTo(ScreenRoute.SongDetails.route) { inclusive = true }
                                }
                            }
                            dragOffset = 0f
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundBrush)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val songJson = Gson().toJson(song)
                            navController.navigate(
                                ScreenRoute.SongDetails.createRoute(songJson, true)
                            ) {
                                popUpTo(ScreenRoute.SongDetails.route) { inclusive = true }
                            }
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.imageUrl,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = song.artistNameList?.joinToString(", ") ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }

                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onCloseClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Slider(
                    value = progress,
                    onValueChange = { newValue ->
                        val newPosition = (newValue * duration).toLong()
                        musicPlayerViewModel.seekTo(context, newPosition)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                        .height(2.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    thumb = {},
                    track = { sliderState ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(sliderState.value)
                                    .height(4.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                )
            }
        }
    }
}