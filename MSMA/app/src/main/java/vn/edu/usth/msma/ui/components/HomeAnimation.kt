package vn.edu.usth.msma.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import vn.edu.usth.msma.R
import java.util.Calendar

fun getGreeting(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 6..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        in 16..23 -> "Good Evening"
        else -> "Good Night"
    }
}

private fun getAnimationForGreeting(greeting: String): Int {
    return when (greeting) {
        "Good Morning" -> R.raw.morning
        "Good Afternoon" -> R.raw.afternoon
        "Good Evening" -> R.raw.night
        "Good Night" -> R.raw.night
        else -> R.raw.home_loading
    }
}

@Composable
fun HomeAnimation(
    modifier: Modifier = Modifier,
    greetingText: String = getGreeting()
) {
    val animationRes = getAnimationForGreeting(greetingText)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 5.dp, end = 5.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = greetingText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(16.dp))
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(150.dp)
            )
        }
    }
}