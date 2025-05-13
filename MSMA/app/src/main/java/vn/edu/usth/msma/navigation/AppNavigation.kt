package vn.edu.usth.msma.navigation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import dagger.hilt.android.lifecycle.HiltViewModel
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
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
import vn.edu.usth.msma.ui.screen.home.HomeNavigation
import vn.edu.usth.msma.ui.screen.library.LibraryScreen
import vn.edu.usth.msma.ui.screen.search.SearchScreen
import vn.edu.usth.msma.ui.screen.settings.SettingScreen
import vn.edu.usth.msma.ui.screen.settings.SettingViewModel
import vn.edu.usth.msma.ui.screen.songs.MiniPlayerScreen
import vn.edu.usth.msma.ui.screen.songs.MiniPlayerViewModel
import vn.edu.usth.msma.ui.screen.songs.MusicPlayerViewModel
import vn.edu.usth.msma.ui.screen.songs.SongDetailsActivity
import javax.inject.Inject

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Otp : Screen("otp/{email}/{sessionId}/{otpDueDate}") {
        fun createRoute(email: String, sessionId: String, otpDueDate: String) =
            "otp/$email/$sessionId/$otpDueDate"
    }
    object ResetPassword : Screen("reset_password/{sessionId}") {
        fun createRoute(sessionId: String) = "reset_password/$sessionId"
    }
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Settings : Screen("settings")
}

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
    val miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val isMiniPlayerVisible by navigationViewModel.preferencesManager.isMiniPlayerVisibleFlow.collectAsState(
        initial = false
    )

    val currentSong by miniPlayerViewModel.currentSong.collectAsState()
    val isPlaying by miniPlayerViewModel.isPlaying.collectAsState()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    // Handle navigation based on isLoggedIn
    LaunchedEffect(isLoggedIn, currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route
        Log.d("MainActivity", "Current route: $currentRoute, isLoggedIn: $isLoggedIn")

        val authRoutes = listOf(
            Screen.Login.route,
            Screen.Register.route,
            Screen.ForgotPassword.route,
            Screen.Otp.route,
            Screen.ResetPassword.route
        )

        if (isLoggedIn && currentRoute != null && authRoutes.contains(currentRoute)) {
            Log.d("MainActivity", "User is logged in, navigating to home")
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!isLoggedIn && currentRoute != null && !authRoutes.contains(currentRoute)) {
            Log.d("MainActivity", "User not logged in, navigating to login")
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (isLoggedIn) {
        DisposableEffect(context) {
            miniPlayerViewModel.registerReceivers(context)
            musicPlayerViewModel.registerPositionReceiver(context)
            onDispose {
                miniPlayerViewModel.unregisterMusicEventReceiver(context)
            }
        }

        // Refresh current song data when route changes
        LaunchedEffect(currentBackStackEntry?.destination?.route) {
            miniPlayerViewModel.refreshCurrentSongData(context)
        }

        Scaffold(
            bottomBar = {
                Column {
                    if (isMiniPlayerVisible) {
                        Box {
                            currentSong?.let {
                                MiniPlayerScreen(
                                    song = it,
                                    isPlaying = isPlaying,
                                    onPlayPauseClick = {
                                        if (isPlaying) {
                                            // Pause music
                                            val intent =
                                                Intent(context, MusicService::class.java).apply {
                                                    action = "PAUSE"
                                                }
                                            context.startService(intent)
                                            miniPlayerViewModel.updatePlaybackState(false)
                                        } else {
                                            // Resume music
                                            val intent =
                                                Intent(context, MusicService::class.java).apply {
                                                    action = "RESUME"
                                                    putExtra("SEEK_POSITION", 0L)
                                                }
                                            context.startService(intent)
                                            miniPlayerViewModel.updatePlaybackState(true)
                                        }
                                    },
                                    musicPlayerViewModel = musicPlayerViewModel,
                                    onMiniPlayerClick = { miniPlayerViewModel.openDetails(context) },
                                    onCloseClick = {
                                        val intent =
                                            Intent(context, MusicService::class.java).apply {
                                                action = "CLOSE"
                                            }
                                        context.startService(intent)
                                    },
                                    miniPlayerViewModel = miniPlayerViewModel
                                )
                            }
                        }
                    }
                    BottomNavigationBar(navController = navController)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = if (isMiniPlayerVisible) 60.dp else 0.dp
                    )
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = modifier
                ) {
                    composable(Screen.Home.route) {
                        HomeNavigation()
                    }
                    composable(Screen.Search.route) {
                        SearchScreen()
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen()
                    }
                    composable(Screen.Settings.route) {
                        val settingViewModel: SettingViewModel = hiltViewModel()
                        SettingScreen(
                            context,
                            navController,
                            settingViewModel
                        )
                    }
                }
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = modifier
        ) {
            composable(Screen.Login.route) {
                val viewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }
            composable(Screen.Register.route) {
                val viewModel: RegisterViewModel = hiltViewModel()
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }
            composable(Screen.ForgotPassword.route) {
                val viewModel: ForgotPasswordViewModel = hiltViewModel()
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    onNavigateToOtp = { email, sessionId, otpDueDate ->
                        navController.navigate(Screen.Otp.createRoute(email, sessionId, otpDueDate))
                    },
                    navController = navController
                )
            }
            composable(
                Screen.Otp.route,
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
                        navController.navigate(Screen.ResetPassword.createRoute(sessionId))
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    navBackStackEntry = backStackEntry,
                    navController = navController
                )
            }
            composable(
                Screen.ResetPassword.route,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val viewModel: ResetPasswordViewModel = hiltViewModel()
                ResetPasswordScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }
        }
    }
}