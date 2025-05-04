package vn.edu.usth.msma

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.navigation.NavigationBar
import vn.edu.usth.msma.ui.screen.SplashScreen
import vn.edu.usth.msma.ui.screen.auth.forgot.ForgotPasswordScreen
import vn.edu.usth.msma.ui.screen.auth.forgot.ForgotPasswordViewModel
import vn.edu.usth.msma.ui.screen.auth.forgot.ForgotPasswordViewModelFactory
import vn.edu.usth.msma.ui.screen.auth.login.LoginScreen
import vn.edu.usth.msma.ui.screen.auth.login.LoginViewModel
import vn.edu.usth.msma.ui.screen.auth.login.LoginViewModelFactory
import vn.edu.usth.msma.ui.screen.auth.otp.OtpScreen
import vn.edu.usth.msma.ui.screen.auth.otp.OtpViewModel
import vn.edu.usth.msma.ui.screen.auth.otp.OtpViewModelFactory
import vn.edu.usth.msma.ui.screen.auth.register.RegisterScreen
import vn.edu.usth.msma.ui.screen.auth.register.RegisterViewModel
import vn.edu.usth.msma.ui.screen.auth.register.RegisterViewModelFactory
import vn.edu.usth.msma.ui.screen.auth.reset.ResetPasswordScreen
import vn.edu.usth.msma.ui.screen.auth.reset.ResetPasswordViewModel
import vn.edu.usth.msma.ui.screen.auth.reset.ResetPasswordViewModelFactory
import vn.edu.usth.msma.ui.theme.MSMATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MSMATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    val navController = rememberNavController()
                    val preferencesManager = PreferencesManager(this)
                    val context = LocalContext.current
                    val isLoggedIn by preferencesManager.isLoggedIn.collectAsState(initial = false)

                    if (showSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        AppNavigation(
                            navController = navController,
                            isLoggedIn = isLoggedIn,
                            preferencesManager = preferencesManager,
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
    preferencesManager: PreferencesManager,
    context: Context
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            val viewModel = LoginViewModelFactory(preferencesManager).create(LoginViewModel::class.java)
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                onNavigateToHome = { navController.navigate("home") }
            )
        }
        composable("register") {
            val viewModel = RegisterViewModelFactory(preferencesManager).create(RegisterViewModel::class.java)
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate("login") }
            )
        }
        composable("forgot_password") {
            val viewModel = ForgotPasswordViewModelFactory().create(ForgotPasswordViewModel::class.java)
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToOtp = { email, sessionId, otpDueDate -> navController.navigate("otp/$email/$sessionId/$otpDueDate") },
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
            val viewModel = OtpViewModelFactory(
                initialEmail = backStackEntry.arguments?.getString("email") ?: "",
                initialSessionId = backStackEntry.arguments?.getString("sessionId") ?: "",
                initialOtpDueDate = backStackEntry.arguments?.getString("otpDueDate") ?: ""
            ).create(OtpViewModel::class.java)
            OtpScreen(
                viewModel = viewModel,
                onNavigateToResetPassword = { sessionId -> navController.navigate("reset_password/$sessionId") },
                onNavigateToLogin = { navController.navigate("login") },
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
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val viewModel = ResetPasswordViewModelFactory(sessionId = sessionId).create(ResetPasswordViewModel::class.java)
            ResetPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate("login") }
            )
        }
        composable("home") {
            NavigationBar(
                context = context,
                parentNavController = navController
            )
        }
    }
}