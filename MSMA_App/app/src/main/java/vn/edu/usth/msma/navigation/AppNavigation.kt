package vn.edu.usth.msma.navigation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import vn.edu.usth.msma.data.Album
import vn.edu.usth.msma.data.Artist
import vn.edu.usth.msma.data.Playlist
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.screen.auth.forgot.ForgotPasswordScreen
import vn.edu.usth.msma.ui.screen.auth.forgot.ForgotPasswordViewModel
import vn.edu.usth.msma.ui.screen.auth.login.LoginScreen
import vn.edu.usth.msma.ui.screen.auth.login.LoginViewModel
import vn.edu.usth.msma.ui.screen.auth.otp.OtpScreen
import vn.edu.usth.msma.ui.screen.auth.otp.OtpViewModel
import vn.edu.usth.msma.ui.screen.auth.register.RegisterScreen
import vn.edu.usth.msma.ui.screen.auth.register.RegisterViewModel
import vn.edu.usth.msma.ui.screen.auth.reset.ResetPasswordScreen
import vn.edu.usth.msma.ui.screen.auth.reset.ResetPasswordViewModel
import vn.edu.usth.msma.ui.screen.home.HomeScreen
import vn.edu.usth.msma.ui.screen.home.HomeViewModel
import vn.edu.usth.msma.ui.screen.home.songs.Top10TrendingScreen
import vn.edu.usth.msma.ui.screen.home.songs.Top10TrendingViewModel
import vn.edu.usth.msma.ui.screen.home.songs.Top15DownloadScreen
import vn.edu.usth.msma.ui.screen.home.songs.Top15DownloadedViewModel
import vn.edu.usth.msma.ui.screen.library.LibraryScreen
import vn.edu.usth.msma.ui.screen.library.LibraryViewModel
import vn.edu.usth.msma.ui.screen.library.songs.FavoriteSongsScreen
import vn.edu.usth.msma.ui.screen.library.songs.FavoriteSongsViewModel
import vn.edu.usth.msma.ui.screen.notification.NotificationScreen
import vn.edu.usth.msma.ui.screen.search.SearchScreen
import vn.edu.usth.msma.ui.screen.search.SearchViewModel
import vn.edu.usth.msma.ui.screen.search.albums.AlbumScreen
import vn.edu.usth.msma.ui.screen.search.artists.ArtistProfileScreen
import vn.edu.usth.msma.ui.screen.search.genres.GenreScreen
import vn.edu.usth.msma.ui.screen.search.genres.GenreViewModel
import vn.edu.usth.msma.ui.screen.search.playlists.PlaylistScreen
import vn.edu.usth.msma.ui.screen.settings.SettingScreen
import vn.edu.usth.msma.ui.screen.settings.SettingViewModel
import vn.edu.usth.msma.ui.screen.settings.changepassword.ChangePasswordScreen
import vn.edu.usth.msma.ui.screen.settings.history_listen.ViewHistoryListenScreen
import vn.edu.usth.msma.ui.screen.settings.history_listen.ViewHistoryListenViewModel
import vn.edu.usth.msma.ui.screen.settings.profile.change_password.ChangePasswordViewModel
import vn.edu.usth.msma.ui.screen.settings.profile.edit.EditProfileScreen
import vn.edu.usth.msma.ui.screen.settings.profile.edit.EditProfileViewModel
import vn.edu.usth.msma.ui.screen.settings.profile.view.ViewProfileScreen
import vn.edu.usth.msma.ui.screen.songs.MiniPlayerScreen
import vn.edu.usth.msma.ui.screen.songs.MusicPlayerViewModel
import vn.edu.usth.msma.ui.screen.songs.SongDetailsScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val songRepository: SongRepository,
    val preferencesManager: PreferencesManager
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    isLoggedIn: Boolean,
    context: Context,
    modifier: Modifier = Modifier
) {
    Log.d("MainActivity", "AppNavigation: isLoggedIn = $isLoggedIn")
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val isMiniPlayerVisible by navigationViewModel.preferencesManager.isMiniPlayerVisibleFlow.collectAsState(
        initial = false
    )

    LaunchedEffect(Unit) {
        musicPlayerViewModel.registerMusicEventReceiver(context)
    }

    DisposableEffect(Unit) {
        musicPlayerViewModel.registerMusicEventReceiver(context)
        onDispose {
            musicPlayerViewModel.unregisterMusicEventReceiver()
        }
    }

    val currentSong by musicPlayerViewModel.currentSong.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Trackers
    val isInNotificationScreen = currentRoute == ScreenRoute.NotificationScreen.route
    val isInFavoriteSongsScreen = currentRoute == ScreenRoute.FavoriteSongs.route
    val isInArtistProfileScreen = currentRoute == ScreenRoute.ArtistDetails.route
    val isInPlaylistScreen = currentRoute == ScreenRoute.PlaylistDetails.route
    val isInAlbumScreen = currentRoute == ScreenRoute.AlbumDetails.route
    val isInGenreScreen = currentRoute == ScreenRoute.Genre.route
    val isInViewProfileScreen = currentRoute == ScreenRoute.ViewProfile.route
    val isInEditProfileScreen = currentRoute == ScreenRoute.EditProfile.route
    val isInChangePasswordScreen = currentRoute == ScreenRoute.ChangePasswordScreen.route
    val isInViewHistoryListenScreen = currentRoute == ScreenRoute.ViewHistoryListen.route
    val isInTop10TrendingScreen = currentRoute == ScreenRoute.Top10TrendingSongs.route
    val isInTop15DownloadedScreen = currentRoute == ScreenRoute.Top15DownloadedSongs.route

    // Handle navigation based on isLoggedIn
    LaunchedEffect(isLoggedIn, currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route
        Log.d("MainActivity", "Current route: $currentRoute, isLoggedIn: $isLoggedIn")

        val authRoutes = listOf(
            ScreenRoute.Login.route,
            ScreenRoute.Register.route,
            ScreenRoute.ForgotPassword.route,
            ScreenRoute.Otp.route,
            ScreenRoute.ResetPassword.route
        )

        if (isLoggedIn && currentRoute != null && authRoutes.contains(currentRoute)) {
            Log.d("MainActivity", "User is logged in, navigating to home")
            navController.navigate(ScreenRoute.Home.route) {
                popUpTo(ScreenRoute.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!isLoggedIn && currentRoute != null && !authRoutes.contains(currentRoute)) {
            Log.d("MainActivity", "User not logged in, navigating to login")
            navController.navigate(ScreenRoute.Login.route) {
                popUpTo(ScreenRoute.Home.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (currentSong != null) {
            navigationViewModel.preferencesManager.setMiniPlayerVisible(true)
        }
    }

    if (isLoggedIn) {
        Scaffold(
            bottomBar = {
                Column {
                    if (isMiniPlayerVisible && currentSong != null &&
                        currentRoute?.contains("songDetails") == false &&
                        !isInNotificationScreen && !isInViewProfileScreen &&
                        !isInEditProfileScreen && !isInChangePasswordScreen &&
                        !isInViewHistoryListenScreen
                    ) {
                        MiniPlayerScreen(
                            musicPlayerViewModel,
                            onCloseClick = {
                                val intent =
                                    Intent(context, MusicService::class.java).apply {
                                        action = "CLOSE"
                                    }
                                context.startService(intent)
                            },
                            navController = navController,
                        )
                    }

                    if (currentRoute?.contains("songDetails") == false &&
                        !isInNotificationScreen && !isInGenreScreen && !isInViewProfileScreen &&
                        !isInEditProfileScreen && !isInChangePasswordScreen &&
                        !isInViewHistoryListenScreen && !isInFavoriteSongsScreen && !isInArtistProfileScreen &&
                        !isInAlbumScreen && !isInPlaylistScreen && !isInTop10TrendingScreen &&
                        !isInTop15DownloadedScreen
                    ) {
                        BottomNavigationBar(navController = navController)
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = if (isMiniPlayerVisible &&
                            currentRoute?.contains("songDetails") == false
                        ) 75.dp else 0.dp
                    )
            ) {
                NavHost(
                    navController = navController,
                    startDestination = ScreenRoute.Home.route,
                    modifier = modifier
                ) {
                    composable(ScreenRoute.Home.route) {
                        val homeViewModel: HomeViewModel = hiltViewModel()
                        HomeScreen(homeViewModel, navController)
                    }
                    composable(ScreenRoute.Search.route) {
                        val searchViewModel: SearchViewModel = hiltViewModel()
                        SearchScreen(searchViewModel, navController)
                    }
                    composable(ScreenRoute.Library.route) {
                        val libraryViewModel: LibraryViewModel = hiltViewModel()
                        LibraryScreen(libraryViewModel, navController)
                    }
                    composable(ScreenRoute.Settings.route) {
                        val settingViewModel: SettingViewModel = hiltViewModel()
                        SettingScreen(context, navController, settingViewModel)
                    }
                    composable(ScreenRoute.FavoriteSongs.route) {
                        val favoriteSongsViewModel: FavoriteSongsViewModel = hiltViewModel()
                        FavoriteSongsScreen(favoriteSongsViewModel, navController)
                    }
                    composable(ScreenRoute.Top10TrendingSongs.route) {
                        val top10TrendingViewModel: Top10TrendingViewModel = hiltViewModel()
                        Top10TrendingScreen(top10TrendingViewModel, navController)
                    }
                    composable(ScreenRoute.Top15DownloadedSongs.route) {
                        val top15DownloadedViewModel: Top15DownloadedViewModel = hiltViewModel()
                        Top15DownloadScreen(top15DownloadedViewModel, navController)
                    }
                    composable(
                        route = ScreenRoute.ArtistDetails.route,
                        arguments = listOf(
                            navArgument("artistDetailsJson") { type = NavType.StringType },
                        )
                    ) { backStackEntry ->
                        val artistDetailsJson =
                            backStackEntry.arguments?.getString("artistDetailsJson") ?: ""
                        val decodedJson =
                            URLDecoder.decode(artistDetailsJson, StandardCharsets.UTF_8.toString())
                        val artist = Gson().fromJson(decodedJson, Artist::class.java)
                        ArtistProfileScreen(
                            artist = artist,
                            navController = navController
                        )
                    }
                    composable(
                        route = ScreenRoute.PlaylistDetails.route,
                        arguments = listOf(
                            navArgument("playlistJson") { type = NavType.StringType },
                        )
                    ) { backStackEntry ->
                        val playlistJson =
                            backStackEntry.arguments?.getString("playlistJson") ?: ""
                        val decodedJson =
                            URLDecoder.decode(playlistJson, StandardCharsets.UTF_8.toString())
                        val playlist = Gson().fromJson(decodedJson, Playlist::class.java)
                        PlaylistScreen(
                            playlist = playlist,
                            navController = navController
                        )
                    }
                    composable(
                        route = ScreenRoute.AlbumDetails.route,
                        arguments = listOf(
                            navArgument("albumJson") { type = NavType.StringType },
                        )
                    ) { backStackEntry ->
                        val albumJson =
                            backStackEntry.arguments?.getString("albumJson") ?: ""
                        val decodedJson =
                            URLDecoder.decode(albumJson, StandardCharsets.UTF_8.toString())
                        val album = Gson().fromJson(decodedJson, Album::class.java)
                        AlbumScreen(
                            album = album,
                            navController = navController
                        )
                    }
                    composable(ScreenRoute.ViewProfile.route) {
                        val viewProfileViewModel: EditProfileViewModel = hiltViewModel()
                        ViewProfileScreen(
                            viewProfileViewModel,
                            onBack = {
                                if (isMiniPlayerVisible) {
                                    val intent = Intent("MUSIC_EVENT").apply {
                                        putExtra("ACTION", "SHOW_MINI_PLAYER")
                                    }
                                    context.sendBroadcast(intent)
                                }
                                navController.popBackStack()
                            })
                    }
                    composable(ScreenRoute.EditProfile.route) {
                        val editProfileViewModel: EditProfileViewModel = hiltViewModel()
                        EditProfileScreen(
                            editProfileViewModel,
                            onBack = {
                                if (isMiniPlayerVisible) {
                                    val intent = Intent("MUSIC_EVENT").apply {
                                        putExtra("ACTION", "SHOW_MINI_PLAYER")
                                    }
                                    context.sendBroadcast(intent)
                                }
                                navController.popBackStack()
                            })
                    }
                    composable(ScreenRoute.ChangePasswordScreen.route) {
                        val changePasswordViewModel: ChangePasswordViewModel = hiltViewModel()
                        ChangePasswordScreen(
                            changePasswordViewModel,
                            onBack = {
                                if (isMiniPlayerVisible) {
                                    val intent = Intent("MUSIC_EVENT").apply {
                                        putExtra("ACTION", "SHOW_MINI_PLAYER")
                                    }
                                    context.sendBroadcast(intent)
                                }
                                navController.popBackStack()
                            })
                    }
                    composable(ScreenRoute.ViewHistoryListen.route) {
                        val viewHistoryListenViewModel: ViewHistoryListenViewModel = hiltViewModel()
                        ViewHistoryListenScreen(
                            viewHistoryListenViewModel,
                            onBack = {
                                if (isMiniPlayerVisible) {
                                    val intent = Intent("MUSIC_EVENT").apply {
                                        putExtra("ACTION", "SHOW_MINI_PLAYER")
                                    }
                                    context.sendBroadcast(intent)
                                }
                                navController.popBackStack()
                            })
                    }
                    composable(ScreenRoute.NotificationScreen.route) {
                        NotificationScreen(
                            onBackClick = {
                                if (isMiniPlayerVisible) {
                                    val intent = Intent("MUSIC_EVENT").apply {
                                        putExtra("ACTION", "SHOW_MINI_PLAYER")
                                    }
                                    context.sendBroadcast(intent)
                                }
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        ScreenRoute.SongDetails.route,
                        arguments = listOf(
                            navArgument("songJson") { type = NavType.StringType },
                            navArgument("fromMiniPlayer") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val songJson = backStackEntry.arguments?.getString("songJson") ?: ""
                        val fromMiniPlayer =
                            backStackEntry.arguments?.getBoolean("fromMiniPlayer") ?: false
                        val decodedJson =
                            URLDecoder.decode(songJson, StandardCharsets.UTF_8.toString())
                        val song = Gson().fromJson(decodedJson, Song::class.java)
                        SongDetailsScreen(
                            song = song,
                            onBack = { navController.popBackStack() },
                            fromMiniPlayer = fromMiniPlayer,
                            songRepository = navigationViewModel.songRepository,
                            context = context,
                            navController = navController,
                        )
                    }
                    composable(
                        ScreenRoute.Genre.route,
                        arguments = listOf(
                            navArgument("genreJson") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val genreJson = backStackEntry.arguments?.getString("genreJson") ?: ""
                        val genre = try {
                            val decodedJson =
                                URLDecoder.decode(genreJson, StandardCharsets.UTF_8.toString())
                            Gson().fromJson(decodedJson, GenreResponse::class.java)
                                ?: GenreResponse(
                                    id = 0,
                                    name = "Error",
                                    briefDescription = "",
                                    fullDescription = "",
                                    imageUrl = "",
                                    createdDate = "",
                                    lastModifiedDate = ""
                                )
                        } catch (e: Exception) {
                            Log.e("GenreScreen", "Failed to decode genreJson: $e")
                            GenreResponse(
                                id = 0,
                                name = "Error",
                                briefDescription = "",
                                fullDescription = "",
                                imageUrl = "",
                                createdDate = "",
                                lastModifiedDate = ""
                            )
                        }

                        val viewModel: GenreViewModel = hiltViewModel()
                        LaunchedEffect(genre) {
                            viewModel.init(genre)
                        }

                        GenreScreen(
                            genre = genre,
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                }
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = ScreenRoute.Login.route,
            modifier = modifier
        ) {
            composable(ScreenRoute.Login.route) {
                val viewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = {
                        navController.navigate(ScreenRoute.Register.route)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(ScreenRoute.ForgotPassword.route)
                    }
                )
            }
            composable(ScreenRoute.Register.route) {
                val viewModel: RegisterViewModel = hiltViewModel()
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(ScreenRoute.Login.route)
                    }
                )
            }
            composable(ScreenRoute.ForgotPassword.route) {
                val viewModel: ForgotPasswordViewModel = hiltViewModel()
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(ScreenRoute.Login.route)
                    },
                    onNavigateToOtp = { email, sessionId, otpDueDate ->
                        navController.navigate(
                            ScreenRoute.Otp.createRoute(
                                email,
                                sessionId,
                                otpDueDate
                            )
                        )
                    },
                    navController = navController
                )
            }
            composable(
                ScreenRoute.Otp.route,
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType },
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("otpDueDate") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val viewModel: OtpViewModel = hiltViewModel()
                OtpScreen(
                    viewModel = viewModel,
                    onNavigateToResetPassword = { sessionId ->
                        navController.navigate(ScreenRoute.ResetPassword.createRoute(sessionId))
                    },
                    onNavigateToLogin = {
                        navController.navigate(ScreenRoute.Login.route)
                    },
                    navBackStackEntry = backStackEntry,
                    navController = navController
                )
            }
            composable(
                ScreenRoute.ResetPassword.route,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val viewModel: ResetPasswordViewModel = hiltViewModel()
                ResetPasswordScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(ScreenRoute.Login.route)
                    }
                )
            }
        }
    }
}