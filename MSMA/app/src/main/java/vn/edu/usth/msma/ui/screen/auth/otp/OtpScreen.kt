package vn.edu.usth.msma.ui.screen.auth.otp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import vn.edu.usth.msma.R
import java.time.ZonedDateTime
import java.time.Duration
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    viewModel: OtpViewModel,
    onNavigateToResetPassword: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    navBackStackEntry: NavBackStackEntry,
    navController: NavController
) {
    val otpState by viewModel.otpState.collectAsState()
    val context = LocalContext.current

    // Extract email, sessionId, and otpDueDate from navigation arguments
    val email = navBackStackEntry.arguments?.getString("email") ?: ""
    val sessionId = navBackStackEntry.arguments?.getString("sessionId") ?: ""
    val otpDueDateStr = navBackStackEntry.arguments?.getString("otpDueDate") ?: ""

    // Parse otpDueDate and handle invalid format
    val otpDueDate = runCatching { ZonedDateTime.parse(otpDueDateStr) }.getOrNull()
    val timeLeftSeconds by produceState(initialValue = 0L, otpDueDate) {
        if (otpDueDate != null) {
            while (true) {
                val now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                val duration = Duration.between(now, otpDueDate).seconds.coerceAtLeast(0)
                value = duration
                Log.d("OtpScreen", "Now: $now, OtpDueDate: $otpDueDate, TimeLeftSeconds: $duration")
                if (duration <= 0) break
                delay(1000L)
            }
        } else {
            Log.e("OtpScreen", "Invalid otpDueDate: $otpDueDateStr")
        }
    }
    val isOtpExpired = timeLeftSeconds <= 0

    LaunchedEffect(otpState.error) {
        otpState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            Log.e("OtpScreen", "Error: $it")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.otp_animation)
        )
        val progress by animateLottieCompositionAsState(
            isPlaying = true,
            composition = composition,
            iterations = LottieConstants.IterateForever,
            speed = 0.7f
        )

        LottieAnimation(
            modifier = Modifier
                .width(200.dp)
                .height(250.dp)
                .align(Alignment.CenterHorizontally),
            composition = composition,
            progress = { progress }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Enter OTP",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = otpState.otp,
            onValueChange = { viewModel.updateOtp(it) },
            label = {
                Text(
                    otpState.otpError ?: "OTP (6 digits)",
                    color = if (otpState.otpError != null) Color.Red else Color.Unspecified
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = "")
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 20.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = !isOtpExpired && !otpState.isVerified
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isOtpExpired && !otpState.isVerified) {
            Text(
                text = "Time remaining: ${timeLeftSeconds / 60}:${
                    (timeLeftSeconds % 60).toString().padStart(2, '0')
                }",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.verifyOtp { sessionId ->
                    Toast.makeText(context, "OTP verified!", Toast.LENGTH_SHORT).show()
                    onNavigateToResetPassword(sessionId)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 90.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !otpState.isLoading && !isOtpExpired && !otpState.isVerified
        ) {
            if (otpState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Verify OTP")
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