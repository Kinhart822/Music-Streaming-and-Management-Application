package vn.edu.usth.msma.ui.screen.auth.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import vn.edu.usth.msma.R

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onNavigateToLogin: () -> Unit) {
    val registerState by viewModel.registerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.register_animation)
        )
        val progress by animateLottieCompositionAsState(
            isPlaying = true,
            composition = composition,
            iterations = LottieConstants.IterateForever,
            speed = 0.7f
        )

        Spacer(modifier = Modifier.height(30.dp))

        LottieAnimation(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally),
            composition = composition,
            progress = { progress }
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Create Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Please enter your details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = registerState.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = {
                Text(
                    registerState.emailError ?: "Email",
                    color = if (registerState.emailError != null) Color.Red else Color.Unspecified
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.AccountCircle, contentDescription = "")
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 20.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = registerState.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = {
                Text(
                    registerState.passwordError ?: "Password",
                    color = if (registerState.passwordError != null) Color.Red else Color.Unspecified
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = "")
            },
            visualTransformation = if (registerState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    imageVector = if (registerState.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "",
                    modifier = Modifier.clickable { viewModel.togglePasswordVisibility() }
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
            value = registerState.confirmPassword,
            onValueChange = { viewModel.updateConfirmPassword(it) },
            label = {
                Text(
                    registerState.confirmPasswordError ?: "Confirm Password",
                    color = if (registerState.confirmPasswordError != null) Color.Red else Color.Unspecified
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = "")
            },
            visualTransformation = if (registerState.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    imageVector = if (registerState.confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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
            onClick = { viewModel.register() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 90.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !registerState.isLoading
        ) {
            if (registerState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Register")
            }
        }

        registerState.registerError?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (registerState.isRegistered) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Registration successful!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        Row {
            Text(text = "Already have an account? ")
            Text(
                text = "Login now!",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}