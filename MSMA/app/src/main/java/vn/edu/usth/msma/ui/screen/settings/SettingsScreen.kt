package vn.edu.usth.msma.ui.screen.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

data class SettingItem(
    val title: String,
    val onClick: () -> Unit,
    val isLoading: Boolean = false
)

@Composable
fun SettingScreen(
    context: Context,
    navController: NavHostController,
    viewModel: SettingViewModel
) {
    val settingState by viewModel.settingState.collectAsState()

    // Navigate to login screen when sign-out is successful
    LaunchedEffect(settingState.isSignedOut) {
        if (settingState.isSignedOut) {
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // Show error toast when sign-out fails
    LaunchedEffect(settingState.signOutError) {
        settingState.signOutError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val settingItems = listOf(
        SettingItem(
            title = "Details",
            onClick = {
                Toast.makeText(context, "Details clicked", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItem(
            title = "Edit / Update",
            onClick = {
                Toast.makeText(context, "Edit / Update clicked", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItem(
            title = "Change Password",
            onClick = {
                Toast.makeText(context, "Change Password clicked", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItem(
            title = "View History",
            onClick = {
                Toast.makeText(context, "View History clicked", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItem(
            title = "Delete Account",
            onClick = {
                Toast.makeText(context, "Delete Account clicked", Toast.LENGTH_SHORT).show()
            }
        ),
        SettingItem(
            title = "Sign Out",
            onClick = { viewModel.signOut() },
            isLoading = settingState.isLoading
        )
    )

    // Split items into two
    val dividerIndex = settingItems.indexOfFirst { it.title == "Delete Account" } + 1
    val itemsBeforeDivider = settingItems.subList(0, dividerIndex)
    val itemsAfterDivider = settingItems.subList(dividerIndex, settingItems.size)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(itemsBeforeDivider) { item ->
            SettingCard(item)
        }
        item {
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                thickness = 1.dp
            )
        }
        items(itemsAfterDivider) { item ->
            SettingCard(item)
        }
    }
}

@Composable
fun SettingCard(item: SettingItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isLoading) { item.onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.title in listOf("Delete Account", "Sign Out")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (item.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Navigate to ${item.title}",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}