package vn.edu.usth.msma.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(val title: String, val icon: ImageVector) {
    object Home : NavigationItem("Home", Icons.Filled.Home)
    object Search : NavigationItem("Search", Icons.Filled.Search)
    object Library : NavigationItem("Library", Icons.Filled.LibraryMusic)
    object Settings : NavigationItem("Settings", Icons.Filled.Settings)

    companion object {
        val bottomNavItems: List<NavigationItem> = listOf(
            Home,
            Search,
            Library,
            Settings
        )
    }
}
