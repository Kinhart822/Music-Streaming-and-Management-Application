package vn.edu.usth.msma.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.edu.usth.msma.R
import vn.edu.usth.msma.ui.theme.MSMATheme

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startFade by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (startFade) 0f else 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "Fade Animation"
    )

    LaunchedEffect(Unit) {
        delay(2000L)
        startFade = true
        delay(1000L)
        onTimeout()
    }

    MSMATheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(200.dp) // Icon lớn
                    .graphicsLayer(alpha = alpha)
            )
        }
    }
}