package vn.edu.usth.msma.ui.screen.settings.changepassword

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.edu.usth.msma.R
import vn.edu.usth.msma.ui.screen.settings.profile.change_password.ChangePasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel,
    onBack: () -> Unit
) {
    Log.d("ChangePasswordScreen", "Composing ChangePasswordScreen")
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Show toast for errors or success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            Log.d("ChangePasswordScreen", "Error occurred: $it")
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // New Password
        TextField(
            value = state.newPassword ?: "",
            onValueChange = { viewModel.onNewPasswordChanged(it) },
            label = { Text("New Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            visualTransformation = if (state.newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            isError = state.newPasswordError != null,
            supportingText = {
                state.newPasswordError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            },
            trailingIcon = {
                val image = if (state.newPasswordVisible)
                    painterResource(id = R.drawable.visibility_24)
                else
                    painterResource(id = R.drawable.visibility_off_24)
                Icon(
                    painter = image,
                    contentDescription = "Toggle password visibility",
                    modifier = Modifier.clickable { viewModel.toggleNewPasswordVisibility() }
                )
            }
        )

        // Confirm Password
        TextField(
            value = state.confirmPassword ?: "",
            onValueChange = { viewModel.onConfirmPasswordChanged(it) },
            label = { Text("Confirm Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            visualTransformation = if (state.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            isError = state.confirmPasswordError != null,
            supportingText = {
                state.confirmPasswordError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            },
            trailingIcon = {
                val image = if (state.confirmPasswordVisible)
                    painterResource(id = R.drawable.visibility_24)
                else
                    painterResource(id = R.drawable.visibility_off_24)
                Icon(
                    painter = image,
                    contentDescription = "Toggle confirm password visibility",
                    modifier = Modifier.clickable { viewModel.toggleConfirmPasswordVisibility() }
                )
            }
        )

        // Submit Button
        Button(
            onClick = { viewModel.changePassword() },
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
                Text("Change Password")
            }
        }
    }
}