package vn.edu.usth.msma

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vn.edu.usth.msma.data.preferences.UserPreferences
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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

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
                    val userPreferences = UserPreferences(dataStore)

                    if (showSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        NavHost(navController = navController, startDestination = "login") {
                            composable("login") {
                                val viewModel = LoginViewModelFactory(userPreferences).create(LoginViewModel::class.java)
                                LoginScreen(
                                    viewModel = viewModel,
                                    onNavigateToRegister = { navController.navigate("register") },
                                    onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                                    onNavigateToHome = { navController.navigate("home") }
                                )
                            }
                            composable("register") {
                                val viewModel = RegisterViewModelFactory().create(RegisterViewModel::class.java)
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
                                    onNavigateToOtp = { navController.navigate("otp") }
                                )
                            }
                            composable("otp") { backStackEntry ->
                                val viewModel = OtpViewModelFactory().create(OtpViewModel::class.java)
                                OtpScreen(
                                    viewModel = viewModel,
                                    onNavigateToResetPassword = { navController.navigate("reset_password") },
                                    onNavigateToLogin = { navController.navigate("login") }
                                )
                            }
                            composable("reset_password") { backStackEntry ->
                                val viewModel = ResetPasswordViewModelFactory().create(ResetPasswordViewModel::class.java)
                                ResetPasswordScreen(
                                    viewModel = viewModel,
                                    onNavigateToLogin = { navController.navigate("login") }
                                )
                            }
                            composable("home") {
                                Text("Home Screen")
                            }
                        }
                    }
                }
            }
        }
    }
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun DefaultPreview() {
//    MSMATheme {
//        // Preview content
//    }
//}