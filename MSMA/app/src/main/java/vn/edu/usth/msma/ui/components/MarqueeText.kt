package vn.edu.usth.msma.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.7f)
) {
    var offsetX by remember { mutableStateOf(0f) }
    val textWidth = remember { mutableStateOf(0f) }
    val containerWidth = remember { mutableStateOf(0f) }
    var isReversed by remember { mutableStateOf(false) }

    LaunchedEffect(textWidth.value, containerWidth.value) {
        if (textWidth.value > containerWidth.value) {
            while (true) {
                if (!isReversed) {
                    offsetX -= 1f
                    if (offsetX <= -textWidth.value) {
                        isReversed = true
                        delay(1000)
                    }
                } else {
                    offsetX += 1f
                    if (offsetX >= 0f) {
                        isReversed = false
                        delay(1000)
                    }
                }
                delay(16) // ~60fps
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                containerWidth.value = coordinates.size.width.toFloat()
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), 0) }
                .onGloballyPositioned { coordinates ->
                    textWidth.value = coordinates.size.width.toFloat()
                }
        )
    }
}