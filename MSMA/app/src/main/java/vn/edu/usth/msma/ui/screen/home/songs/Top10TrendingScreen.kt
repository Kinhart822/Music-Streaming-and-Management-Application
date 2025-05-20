package vn.edu.usth.msma.ui.screen.home.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.gson.Gson
import vn.edu.usth.msma.ui.components.HomeAnimation
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.components.SongItem

@Composable
fun Top10TrendingScreen(
    viewModel: Top10TrendingViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val top10TrendingSongs by viewModel.top10TrendingSongs.collectAsState()
    val isTop10Loading by viewModel.isTop10Loading.collectAsState()
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
        ) {
            Text(
                text = "Top 10 Trending Songs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (isTop10Loading) {
                LoadingScreen(message = "Loading trending songs...")
            } else if (top10TrendingSongs.isEmpty()) {
                Text(
                    text = "No trending songs found",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(top10TrendingSongs) { song ->
                        SongItem(
                            song = song,
                            onSongClick = {
                                val songJson = Gson().toJson(song) ?: return@SongItem
                                navController.navigate(
                                    ScreenRoute.SongDetails.createRoute(songJson, true)
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