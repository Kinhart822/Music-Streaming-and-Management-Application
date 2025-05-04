package vn.edu.usth.msma.ui.screen.auth.forgot

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import vn.edu.usth.msma.R

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToOtp: (String, String, String) -> Unit,
    navController: NavController
) {
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(forgotPasswordState.error) {
        forgotPasswordState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            Log.e("ForgotPasswordScreen", "Error: $it")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.forgot_animation)
        )
        val progress by animateLottieCompositionAsState(
            isPlaying = true,
            composition = composition,
            iterations = LottieConstants.IterateForever,
            speed = 0.7f
        )

        LottieAnimation(
            modifier = Modifier
                .width(400.dp)
                .height(350.dp)
                .align(Alignment.CenterHorizontally),
            composition = composition,
            progress = { progress }
        )

        Text(
            text = "Forgot Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = forgotPasswordState.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = {
                Text(
                    forgotPasswordState.emailError ?: "Email",
                    color = if (forgotPasswordState.emailError != null) Color.Red else Color.Unspecified
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.submitEmail { email, sessionId, otpDueDate ->
                    Log.d("ForgotPasswordScreen", "Navigating to otp with email=$email, sessionId=$sessionId, otpDueDate=$otpDueDate")
                    if (otpDueDate.isNotEmpty()) {
                        Toast.makeText(context, "OTP has been sent successfully to your email!", Toast.LENGTH_SHORT).show()
                        navController.navigate("otp/$email/$sessionId/$otpDueDate")
                    } else {
                        Toast.makeText(context, "OTP due date is invalid", Toast.LENGTH_SHORT).show()
                        Log.e("ForgotPasswordScreen", "OTP due date is empty or null")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 90.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !forgotPasswordState.isLoading
        ) {
            if (forgotPasswordState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Send OTP")
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