package vn.edu.usth.msma.ui.screen.auth.reset

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import vn.edu.usth.msma.R

@Composable
fun ResetPasswordScreen(
    viewModel: ResetPasswordViewModel,
    onNavigateToLogin: () -> Unit
) {
    val resetPasswordState by viewModel.resetPasswordState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(resetPasswordState.error) {
        resetPasswordState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            Log.e("ResetPasswordScreen", "Error: $it")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.reset_password_animation)
        )
        val progress by animateLottieCompositionAsState(
            isPlaying = true,
            composition = composition,
            iterations = LottieConstants.IterateForever,
            speed = 0.7f
        )

        LottieAnimation(
            modifier = Modifier
                .width(250.dp)
                .height(300.dp)
                .align(Alignment.CenterHorizontally),
            composition = composition,
            progress = { progress }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Reset Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = resetPasswordState.newPassword,
            onValueChange = { viewModel.updateNewPassword(it) },
            label = {
                Text(
                    resetPasswordState.newPasswordError ?: "New Password",
                    color = if (resetPasswordState.newPasswordError != null) Color.Red else Color.Unspecified
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = "")
            },
            visualTransformation = if (resetPasswordState.newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    imageVector = if (resetPasswordState.newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "",
                    modifier = Modifier.clickable { viewModel.toggleNewPasswordVisibility() }
                )
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 20.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = resetPasswordState.confirmPassword,
            onValueChange = { viewModel.updateConfirmPassword(it) },
            label = {
                Text(
                    resetPasswordState.confirmPasswordError ?: "Confirm Password",
                    color = if (resetPasswordState.confirmPasswordError != null) Color.Red else Color.Unspecified
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = "")
            },
            visualTransformation = if (resetPasswordState.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    imageVector = if (resetPasswordState.confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "",
                    modifier = Modifier.clickable { viewModel.toggleConfirmPasswordVisibility() }
                )
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 20.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.resetPassword {
                    Toast.makeText(context, "Password reset successful!", Toast.LENGTH_SHORT).show()
                    onNavigateToLogin()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 90.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !resetPasswordState.isLoading
        ) {
            if (resetPasswordState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Reset Password")
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        Row {
            Text(text = "Back to ")
            Text(
                text = "Login!",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}