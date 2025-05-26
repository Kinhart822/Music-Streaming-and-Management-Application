package vn.edu.usth.msma

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.navigation.MainScreen
import vn.edu.usth.msma.network.ApiClient
import vn.edu.usth.msma.network.CustomAuthenticator
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.screen.SplashScreen
import vn.edu.usth.msma.ui.screen.songs.MusicPlayerViewModel
import vn.edu.usth.msma.ui.theme.MSMATheme
import vn.edu.usth.msma.utils.eventbus.Event.*
import vn.edu.usth.msma.utils.eventbus.EventBus
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var songRepository: SongRepository

    @Inject
    lateinit var customAuthenticator: CustomAuthenticator

    private var showSplash by mutableStateOf(true)
    private val tag: String = javaClass.simpleName

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate called")

        val userId = "123" // Replace with actual user ID from your auth system
        FirebaseMessaging.getInstance().subscribeToTopic("user_$userId")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Subscribed to topic: user_$userId")
                } else {
                    println("Failed to subscribe to topic: user_$userId")
                }
            }

        // Check if there's a running music service and clear mini player if not
        lifecycleScope.launch {
            val isMiniPlayerVisible = preferencesManager.isMiniPlayerVisibleFlow.first()
            if (isMiniPlayerVisible) {
                // Check if MusicService is actually running
                val isServiceRunning = isServiceRunning(MusicService::class.java)
                if (!isServiceRunning) {
                    // Clear miniplayer state if service is not running
                    preferencesManager.setMiniPlayerVisible(false)
                }
            }
        }

        // Request notification permission for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

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
                        preferencesManager.getIsLoggedInFlow(currentUserEmail!!)
                            .collectAsState(initial = false).value
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
                                else -> {}
                            }
                        }
                    }

                    if (showSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MainScreen(
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

    @SuppressLint("ServiceCast")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            if (preferencesManager.isMiniPlayerVisibleFlow.first()) {
                val intent = Intent(this@MainActivity, MusicService::class.java).apply {
                    action = "CLOSE"
                }
                startService(intent)
                // Clear miniplayer state when app is destroyed
                preferencesManager.setMiniPlayerVisible(false)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            if (preferencesManager.isMiniPlayerVisibleFlow.first()) {
                val intent = Intent(this@MainActivity, MusicService::class.java).apply {
                    action = "CLOSE"
                }
                startService(intent)
                // Clear miniplayer state when app is stopped
                preferencesManager.setMiniPlayerVisible(false)
            }
        }
    }
}