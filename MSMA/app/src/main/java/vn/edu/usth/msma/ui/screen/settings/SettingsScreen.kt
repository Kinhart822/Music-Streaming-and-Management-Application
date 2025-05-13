package vn.edu.usth.msma.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import vn.edu.usth.msma.ui.screen.settings.history_listen.ViewHistoryListenActivity
import vn.edu.usth.msma.ui.screen.settings.profile.change_password.ChangePasswordActivity
import vn.edu.usth.msma.ui.screen.settings.profile.edit.EditAccountInfoActivity
import vn.edu.usth.msma.ui.screen.settings.profile.view.ViewProfileActivity

data class SettingItem(
    val title: String,
    val userName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val isUserInfo: Boolean = false,
    val icon: @Composable () -> Unit = {},
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

    // Trạng thái cho dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Navigate to login screen when sign-out is successful
    LaunchedEffect(settingState.isSignedOut) {
        if (settingState.isSignedOut) {
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // Navigate to login screen when account is deleted
    LaunchedEffect(settingState.isAccountDeleted) {
        if (settingState.isAccountDeleted) {
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

    // Show error toast when delete account fails
    LaunchedEffect(settingState.deleteAccountError) {
        settingState.deleteAccountError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val accountItems = listOf(
        SettingItem(
            title = "User Info",
            userName = settingState.userName,
            email = settingState.email,
            avatarUrl = settingState.avatar,
            firstName = settingState.firstName,
            lastName = settingState.lastName,
            isUserInfo = true,
            onClick = {
                context.startActivity(Intent(context, ViewProfileActivity::class.java))
            }
        )
    )

    val settingItems = listOf(
        SettingItem(
            title = "Edit / Update",
            icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
            onClick = {
                context.startActivity(Intent(context, EditAccountInfoActivity::class.java))
            }
        ),
        SettingItem(
            title = "Change Password",
            icon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
            onClick = {
                context.startActivity(Intent(context, ChangePasswordActivity::class.java))
            }
        ),
        SettingItem(
            title = "View History Listen",
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            onClick = {
                context.startActivity(Intent(context, ViewHistoryListenActivity::class.java))
            }
        ),
        SettingItem(
            title = "Delete Account",
            icon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
            onClick = { showDeleteDialog = true },
            isLoading = settingState.isDeleteLoading
        ),
        SettingItem(
            title = "Sign Out",
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out") },
            onClick = { showSignOutDialog = true },
            isLoading = settingState.isSignOutLoading
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Account section (no title)
        accountItems.forEach { item ->
            SettingCard(item)
        }

        // Settings section
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        settingItems.forEach { item ->
            SettingCard(item)
        }

        // Add extra space at the bottom for better scrolling
        Spacer(modifier = Modifier.height(65.dp))
    }

    // Dialog xác nhận xóa tài khoản
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog xác nhận đăng xuất
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingCard(item: SettingItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.isUserInfo) Modifier
                else Modifier.clickable(enabled = !item.isLoading) { item.onClick() }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        if (item.isUserInfo) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        item.firstName != null && item.lastName != null -> "${item.firstName} ${item.lastName}"
                        item.firstName != null -> item.firstName
                        item.lastName != null -> item.lastName
                        else -> "user"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                item.avatarUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View Profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { item.onClick() }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.email ?: "No email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    item.icon()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item.title in listOf("Delete Account", "Sign Out")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                if (item.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "Navigate to ${item.title}",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}