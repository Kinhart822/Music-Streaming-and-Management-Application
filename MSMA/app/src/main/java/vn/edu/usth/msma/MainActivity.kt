package vn.edu.usth.msma

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.navigation.NavigationBar
import vn.edu.usth.msma.network.ApiClient
import vn.edu.usth.msma.network.CustomAuthenticator
import vn.edu.usth.msma.ui.screen.SplashScreen
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
import vn.edu.usth.msma.ui.theme.MSMATheme
import vn.edu.usth.msma.utils.eventbus.Event.ProfileUpdatedEvent
import vn.edu.usth.msma.utils.eventbus.Event.SessionExpiredEvent
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesManager: PreferencesManager
    @Inject
    lateinit var apiClient: ApiClient
    @Inject
    lateinit var customAuthenticator: CustomAuthenticator

    private var showSplash by mutableStateOf(true)
    private val tag: String = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate called")

        setContent {
            MSMATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val currentUserEmail by preferencesManager.currentUserEmailFlow.collectAsState(
                        initial = null
                    )
                    val isLoggedIn = if (currentUserEmail != null) {
                        preferencesManager.getIsLoggedInFlow(currentUserEmail!!).collectAsState(initial = false).value
                    } else {
                        false
                    }


                    // Handle session expired event
                    LaunchedEffect(Unit) {
                        EventBus.events.collect { event ->
                            when (event) {
                                is SessionExpiredEvent -> {
                                    Log.d(tag, "Session expired, navigating to login")
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }

                                is ProfileUpdatedEvent -> {
                                    ""
                                }
                            }
                        }
                    }

                    if (showSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        AppNavigation(
                            navController = navController,
                            isLoggedIn = isLoggedIn,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(
    navController: NavHostController,
    isLoggedIn: Boolean,
    context: Context
) {
    Log.d("MainActivity", "AppNavigation: isLoggedIn = $isLoggedIn")
    
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            Log.d("MainActivity", "Composing login screen")
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { 
                    Log.d("MainActivity", "Navigating to register")
                    navController.navigate("register") 
                },
                onNavigateToForgotPassword = { 
                    Log.d("MainActivity", "Navigating to forgot password")
                    navController.navigate("forgot_password") 
                },
                onNavigateToHome = { 
                    Log.d("MainActivity", "Navigating to home")
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            Log.d("MainActivity", "Composing register screen")
            val viewModel: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { 
                    Log.d("MainActivity", "Navigating back to login")
                    navController.navigate("login") 
                }
            )
        }
        composable("forgot_password") {
            Log.d("MainActivity", "Composing forgot password screen")
            val viewModel: ForgotPasswordViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { 
                    Log.d("MainActivity", "Navigating back to login from forgot password")
                    navController.navigate("login") 
                },
                onNavigateToOtp = { email, sessionId, otpDueDate ->
                    Log.d("MainActivity", "Navigating to OTP screen with email=$email, sessionId=$sessionId")
                    navController.navigate("otp/$email/$sessionId/$otpDueDate")
                },
                navController = navController
            )
        }
        composable(
            "otp/{email}/{sessionId}/{otpDueDate}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("otpDueDate") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            Log.d("MainActivity", "Composing OTP screen")
            val viewModel: OtpViewModel = hiltViewModel()
            OtpScreen(
                viewModel = viewModel,
                onNavigateToResetPassword = { sessionId ->
                    Log.d("MainActivity", "Navigating to reset password with sessionId=$sessionId")
                    navController.navigate("reset_password/$sessionId")
                },
                onNavigateToLogin = { 
                    Log.d("MainActivity", "Navigating back to login from OTP")
                    navController.navigate("login") 
                },
                navBackStackEntry = backStackEntry,
                navController = navController
            )
        }
        composable(
            "reset_password/{sessionId}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            Log.d("MainActivity", "Composing reset password screen")
            val viewModel: ResetPasswordViewModel = hiltViewModel()
            ResetPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { 
                    Log.d("MainActivity", "Navigating back to login from reset password")
                    navController.navigate("login") 
                }
            )
        }
        composable("home") {
            Log.d("MainActivity", "Composing home screen")
            NavigationBar(
                context = context,
                parentNavController = navController
            )
        }
    }

    // Handle navigation based on isLoggedIn after NavHost is set up
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(isLoggedIn, currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route
        Log.d("MainActivity", "Current route: $currentRoute, isLoggedIn: $isLoggedIn")
        
        // List of authentication routes that don't require login
        val authRoutes = listOf("login", "register", "forgot_password", "otp", "reset_password")
        
        if (!isLoggedIn && currentRoute != null && !authRoutes.any { currentRoute.startsWith(it) }) {
            Log.d("MainActivity", "User not logged in, navigating to login")
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        } else if (isLoggedIn && currentRoute == "login") {
            Log.d("MainActivity", "User is logged in, navigating to home")
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }
}