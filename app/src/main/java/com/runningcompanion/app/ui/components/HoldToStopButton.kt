package com.runningcompanion.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcompanion.app.ui.theme.AppColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HoldToStopButton(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Int = 1000,
    textColor: Color = Color.White
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var holdJob: Job? = null
                    holdJob = scope.launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = holdDurationMs, easing = LinearEasing)
                        )
                        onStopConfirmed()
                    }

                    waitForUpOrCancellation()
                    holdJob.cancel()
                    scope.launch {
                        progress.snapTo(0f)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Fill indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.value)
                .background(AppColors.Alert.copy(alpha = 0.85f))
        )

        // Text label
        Text(
            text = if (progress.value > 0f) "Keep holding..." else "Hold to stop",
            color = if (progress.value > 0.5f) AppColors.Ink else textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
