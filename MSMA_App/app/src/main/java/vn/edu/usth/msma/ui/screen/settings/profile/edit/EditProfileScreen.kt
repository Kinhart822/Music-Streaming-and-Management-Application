package vn.edu.usth.msma.ui.screen.settings.profile.edit

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import vn.edu.usth.msma.utils.constants.Gender
import androidx.appcompat.view.ContextThemeWrapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import vn.edu.usth.msma.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onBack: () -> Unit
) {
    Log.d("EditProfileScreen", "Composing EditProfileScreen")
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // File picker launcher for avatar
    val avatarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            viewModel.onAvatarSelected(uri)
        }

    // Permission launcher
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("EditProfileScreen", "Permission granted for avatar")
                avatarLauncher.launch("image/*")
            } else {
                Log.d("EditProfileScreen", "Permission denied for avatar")
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Show toast for errors or success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            Log.d("EditProfileScreen", "Error occurred: $it")
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // Use custom theme for DatePickerDialog
    val datePickerTheme = ContextThemeWrapper(context, R.style.DatePickerDialogTheme)

    // Date picker state
    val datePickerDialog = DatePickerDialog(
        datePickerTheme,
        { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val formattedDate = selectedDate.format(formatter)
            viewModel.onDateOfBirthChanged(formattedDate)
        },
        LocalDate.now().year,
        LocalDate.now().monthValue - 1,
        LocalDate.now().dayOfMonth
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Picker and Display
        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .clickable { viewModel.requestAvatarPicker(permissionLauncher) }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                state.avatarUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected Avatar",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } ?: state.avatar?.let { avatarUrl ->
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Current Avatar",
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
                    text = "Edit Image",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // First Name
        TextField(
            value = state.firstName ?: "",
            onValueChange = { viewModel.onFirstNameChanged(it) },
            label = { Text("First Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // Last Name
        TextField(
            value = state.lastName ?: "",
            onValueChange = { viewModel.onLastNameChanged(it) },
            label = { Text("Last Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // Gender Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            TextField(
                value = state.gender.name,
                onValueChange = {},
                label = { Text("Gender") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = if (state.showGenderDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown",
                        modifier = Modifier.clickable { viewModel.toggleGenderDropdown() }
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            DropdownMenu(
                expanded = state.showGenderDropdown,
                onDismissRequest = { viewModel.toggleGenderDropdown() },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .shadow(4.dp, shape = RoundedCornerShape(8.dp))
            ) {
                Gender.entries.forEach { g ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = g.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        },
                        onClick = {
                            viewModel.onGenderSelected(g)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .background(
                                if (state.gender == g) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }

        // Date of Birth Picker
        TextField(
            value = state.birthDay ?: "",
            onValueChange = {},
            label = { Text("Date of Birth (dd/MM/yyyy)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { datePickerDialog.show() },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select Date",
                    modifier = Modifier.clickable { datePickerDialog.show() }
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // Phone
        TextField(
            value = state.phone ?: "",
            onValueChange = { viewModel.onPhoneChanged(it) },
            label = { Text("Phone") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Submit Button
        Button(
            onClick = { viewModel.updateProfile() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Update Profile")
            }
        }
    }
}