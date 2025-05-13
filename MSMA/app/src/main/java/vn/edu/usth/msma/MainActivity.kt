package vn.edu.usth.msma

import android.content.Intent
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
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.navigation.MainScreen
import vn.edu.usth.msma.network.ApiClient
import vn.edu.usth.msma.network.CustomAuthenticator
import vn.edu.usth.msma.service.MusicService
import vn.edu.usth.msma.ui.screen.SplashScreen
import vn.edu.usth.msma.ui.theme.MSMATheme
import vn.edu.usth.msma.utils.eventbus.Event
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

                                is ProfileUpdatedEvent -> {
                                    ""
                                }

                                is Event.SongFavouriteUpdateEvent -> {
                                    ""
                                }

                                is Event.InitializeDataLibrary -> {
                                    ""
                                }

                                is Event.MediaNotificationCancelSongEvent -> {
                                    ""
                                }
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

    override fun onDestroy() {
        super.onDestroy()

        lifecycleScope.launch {
            if (preferencesManager.isMiniPlayerVisibleFlow.first()) {
                val intent = Intent(this@MainActivity, MusicService::class.java).apply {
                    action = "CLOSE"
                }
                startService(intent)
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
            }
        }
    }
}