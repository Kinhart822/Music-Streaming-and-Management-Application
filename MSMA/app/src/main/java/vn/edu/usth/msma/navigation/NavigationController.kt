package vn.edu.usth.msma.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.ui.screen.notification.NotificationActivity
import vn.edu.usth.msma.ui.screen.home.HomeScreen
import vn.edu.usth.msma.ui.screen.library.LibraryScreen
import vn.edu.usth.msma.ui.screen.search.SearchNavigation
import vn.edu.usth.msma.ui.screen.settings.SettingScreen
import vn.edu.usth.msma.ui.screen.settings.SettingViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val songRepository: SongRepository
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationBar(
    context: Context,
    parentNavController: NavHostController,
    navController: NavHostController = rememberNavController(),
    settingViewModel: SettingViewModel = hiltViewModel()
) {
    val items = listOf(
        NavigationItems("Home", Icons.Filled.Home),
        NavigationItems("Search", Icons.Filled.Search),
        NavigationItems("Library", Icons.Filled.LibraryMusic),
        NavigationItems("Settings", Icons.Filled.Settings)
    )

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    navController.addOnDestinationChangedListener { _, destination, _ ->
        selectedItemIndex = when (destination.route) {
            "Home" -> 0
            "Search" -> 1
            "Library" -> 2
            "Settings" -> 3
            else -> 0
        }
    }

    val screenTitle = when (selectedItemIndex) {
        0 -> "Home"
        1 -> "Search"
        2 -> "Library"
        3 -> "Settings"
        else -> "Home"
    }

    Scaffold(
        topBar = {
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = { Text(screenTitle) },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, NotificationActivity::class.java))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            modifier = Modifier
                                .height(30.dp)
                                .width(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = {
                            selectedItemIndex = index
                            val route = when (index) {
                                0 -> "Home"
                                1 -> "Search"
                                2 -> "Library"
                                3 -> "Settings"
                                else -> "Home"
                            }
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.logo,
                                contentDescription = item.name,
                                tint = if (selectedItemIndex == index) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                },
                                modifier = Modifier.size(if (selectedItemIndex == index) 30.dp else 25.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedItemIndex == index) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                }
                            )
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "Home",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("Home") { HomeScreen() }
            composable("Search") { SearchNavigation() }
            composable("Library") { LibraryScreen() }
            composable("Settings") { SettingScreen(context, parentNavController, settingViewModel) }
        }
    }
}