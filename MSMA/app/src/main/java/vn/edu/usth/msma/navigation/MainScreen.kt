package vn.edu.usth.msma.navigation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import vn.edu.usth.msma.data.Album
import vn.edu.usth.msma.data.Artist
import vn.edu.usth.msma.data.Playlist
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.ui.components.ScreenRoute
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    screenTitle: String?,
    onNotificationClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomEnd = 15.dp,
                    bottomStart = 15.dp
                )
            ),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            onBackClick?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        title = { Text(screenTitle.toString()) },
        actions = {
            onNotificationClick?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    )
}

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean,
    context: Context
) {
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = currentBackStackEntry?.destination?.route

    // Show top and bottom bars
    val showTopAndBottomBar = currentRoute?.startsWith(ScreenRoute.Login.route) == false &&
            currentRoute.startsWith(ScreenRoute.Register.route) == false &&
            currentRoute.startsWith(ScreenRoute.ForgotPassword.route) == false &&
            currentRoute.startsWith(ScreenRoute.Otp.route) == false &&
            currentRoute.startsWith(ScreenRoute.ResetPassword.route) == false &&
            currentRoute.contains("songDetails") == false

    // Determine ScreenRoute title
    val screenTitle = when {
        currentRoute?.contains("genre") == true -> {
            // Extract genreJson from arguments and decode
            val genreJson = currentBackStackEntry.arguments?.getString("genreJson") ?: ""
            try {
                val genre = Gson().fromJson(genreJson, GenreResponse::class.java)
                genre.name
            } catch (e: Exception) {
                "Genre"
            }
        }

        currentRoute?.contains("artist") == true -> {
            // Extract genreJson from arguments and decode
            val artistDetailsJson =
                currentBackStackEntry.arguments?.getString("artistDetailsJson") ?: ""
            try {
                val decodedJson =
                    URLDecoder.decode(artistDetailsJson, StandardCharsets.UTF_8.toString())
                val artist = Gson().fromJson(decodedJson, Artist::class.java)
                artist.artistName + " Profile"
            } catch (e: Exception) {
                "Artist Profile"
            }
        }

        currentRoute?.contains("playlist") == true -> {
            // Extract genreJson from arguments and decode
            val playlistJson = currentBackStackEntry.arguments?.getString("playlistJson") ?: ""
            try {
                val decodedJson =
                    URLDecoder.decode(playlistJson, StandardCharsets.UTF_8.toString())
                val playlist = Gson().fromJson(decodedJson, Playlist::class.java)
                playlist.playlistName + " Playlist"
            } catch (e: Exception) {
                "Playlist"
            }
        }

        currentRoute?.contains("album") == true -> {
            // Extract genreJson from arguments and decode
            val albumJson = currentBackStackEntry.arguments?.getString("albumJson") ?: ""
            try {
                val decodedJson =
                    URLDecoder.decode(albumJson, StandardCharsets.UTF_8.toString())
                val album = Gson().fromJson(decodedJson, Album::class.java)
                album.albumName + " Album"
            } catch (e: Exception) {
                "Album"
            }
        }

        currentRoute == ScreenRoute.Home.route -> "Home"
        currentRoute == ScreenRoute.Search.route -> "Search"
        currentRoute == ScreenRoute.Library.route -> "Library"
        currentRoute == ScreenRoute.Settings.route -> "Settings"
        currentRoute == ScreenRoute.NotificationScreen.route -> "Notifications"
        currentRoute == ScreenRoute.ViewProfile.route -> "View Profile"
        currentRoute == ScreenRoute.EditProfile.route -> "Edit Profile"
        currentRoute == ScreenRoute.ChangePasswordScreen.route -> "Change Password"
        currentRoute == ScreenRoute.ViewHistoryListen.route -> "History Listen"
        currentRoute == ScreenRoute.FavoriteSongs.route -> "Favourite Songs"

        else -> "Home"
    }

    // Trackers
    val isInNotificationScreen = currentRoute == ScreenRoute.NotificationScreen.route
    val isInFavoriteSongsScreen = currentRoute == ScreenRoute.FavoriteSongs.route
    val isInGenreScreen = currentRoute == ScreenRoute.Genre.route
    val isInViewProfileScreen = currentRoute == ScreenRoute.ViewProfile.route
    val isInEditProfileScreen = currentRoute == ScreenRoute.EditProfile.route
    val isInChangePasswordScreen = currentRoute == ScreenRoute.ChangePasswordScreen.route
    val isInViewHistoryListenScreen = currentRoute == ScreenRoute.ViewHistoryListen.route
    val isInArtistProfileScreen = currentRoute == ScreenRoute.ArtistDetails.route
    val isInPlaylistScreen = currentRoute == ScreenRoute.PlaylistDetails.route
    val isInAlbumScreen = currentRoute == ScreenRoute.AlbumDetails.route
    val isInTop15DownloadedScreen = currentRoute == ScreenRoute.Top15DownloadedSongs.route
    val isInTop10TrendingScreen = currentRoute == ScreenRoute.Top10TrendingSongs.route

    Scaffold(
        topBar = {
            if (showTopAndBottomBar) {
                TopBar(
                    screenTitle = screenTitle,
                    onNotificationClick = if (currentRoute.startsWith(ScreenRoute.Genre.route) == false
                        && !isInNotificationScreen && !isInGenreScreen && !isInViewProfileScreen
                        && !isInEditProfileScreen && !isInChangePasswordScreen &&
                        !isInViewHistoryListenScreen && !isInFavoriteSongsScreen && !isInArtistProfileScreen &&
                        !isInPlaylistScreen && !isInAlbumScreen && !isInTop10TrendingScreen &&
                        !isInTop15DownloadedScreen
                    ) {
                        {
                            navController.navigate(ScreenRoute.NotificationScreen.route)
                        }
                    } else null,
                    onBackClick = if (currentRoute.contains("genre") == true ||
                        isInNotificationScreen || isInViewProfileScreen || isInEditProfileScreen ||
                        isInChangePasswordScreen || isInViewHistoryListenScreen || isInFavoriteSongsScreen || isInArtistProfileScreen ||
                        isInPlaylistScreen || isInAlbumScreen || isInTop10TrendingScreen || isInTop15DownloadedScreen
                    ) {
                        { navController.popBackStack() }
                    } else null
                )
            }
        },
        bottomBar = {
            if (showTopAndBottomBar) {
                if (!isInNotificationScreen && !isInGenreScreen && !isInViewProfileScreen
                    && !isInEditProfileScreen && !isInChangePasswordScreen &&
                    !isInViewHistoryListenScreen && !isInFavoriteSongsScreen && !isInArtistProfileScreen &&
                    !isInPlaylistScreen && !isInAlbumScreen && !isInTop10TrendingScreen &&
                    !isInTop15DownloadedScreen
                ) {
                    BottomNavigationBar(navController)
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            isLoggedIn = isLoggedIn,
            context = context,
            modifier = Modifier.padding(innerPadding)
        )
    }
}