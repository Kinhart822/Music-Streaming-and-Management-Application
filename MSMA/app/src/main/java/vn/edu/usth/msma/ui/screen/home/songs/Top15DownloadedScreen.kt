package vn.edu.usth.msma.ui.screen.home.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import vn.edu.usth.msma.ui.components.LoadingScreen
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.components.SongItem

@Composable
fun Top15DownloadScreen(
    viewModel: Top15DownloadedViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val top15DownloadSongs by viewModel.top15DownloadSongs.collectAsState()
    val isTop15Loading by viewModel.isTop15Loading.collectAsState()
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(top15DownloadSongs) { song ->
                    SongItem(
                        song = song,
                        onSongClick = {
                            val songJson = Gson().toJson(song) ?: return@SongItem
                            navController.navigate(
                                ScreenRoute.SongDetails.createRoute(songJson, false)
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}