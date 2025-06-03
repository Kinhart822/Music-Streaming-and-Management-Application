package vn.edu.usth.msma.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.7f),
    scrollSpeed: Float = 100f // pixels per second
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var textWidth by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var isReversed by remember { mutableStateOf(false) }

    LaunchedEffect(textWidth, containerWidth) {
        if (textWidth > containerWidth) {
            while (true) {
                val frameTimeMillis = 16L
                val distance = scrollSpeed * frameTimeMillis / 1000f

                if (!isReversed) {
                    offsetX -= distance
                    if (offsetX <= -(textWidth - containerWidth)) {
                        offsetX = -(textWidth - containerWidth)
                        isReversed = true
                        delay(500)
                    }
                } else {
                    offsetX += distance
                    if (offsetX >= 0f) {
                        offsetX = 0f
                        isReversed = false
                        delay(500)
                    }
                }

                delay(frameTimeMillis)
            }
        } else {
            offsetX = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), 0) }
                .onGloballyPositioned { coordinates ->
                    textWidth = coordinates.size.width.toFloat()
                }
        )
    }
}
