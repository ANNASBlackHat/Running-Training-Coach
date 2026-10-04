package com.runningcompanion.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.ui.theme.AppColors

@Composable
fun PaceLane(
    paceRange: PaceRange?,
    currentPaceSecPerKm: Double?,
    modifier: Modifier = Modifier
) {
    if (paceRange == null) return

    val marginSec = 30.0
    // Slower pace has HIGHER seconds per km (placed on left: minPace = slowSec + margin)
    // Faster pace has LOWER seconds per km (placed on right: maxPace = fastSec - margin)
    val leftBoundSec = paceRange.slowSecPerKm + marginSec
    val rightBoundSec = (paceRange.fastSecPerKm - marginSec).coerceAtLeast(60.0)
    val totalRangeSec = leftBoundSec - rightBoundSec

    // Fraction along the lane: 0.0 at left (slow), 1.0 at right (fast)
    val targetFraction = if (currentPaceSecPerKm != null && currentPaceSecPerKm > 0) {
        val fraction = (leftBoundSec - currentPaceSecPerKm) / totalRangeSec
        fraction.toFloat().coerceIn(0.02f, 0.98f)
    } else {
        0.5f
    }

    val animatedMarkerFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 350),
        label = "paceMarkerAnimation"
    )

    val isTooFast = currentPaceSecPerKm != null && paceRange.isTooFast(currentPaceSecPerKm)
    val isTooSlow = currentPaceSecPerKm != null && paceRange.isTooSlow(currentPaceSecPerKm)
    val isOutOfRange = isTooFast || isTooSlow

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            val width = size.width
            val height = size.height

            // Target zone calculation
            // Target left: slowSecPerKm
            val targetLeftFraction = ((leftBoundSec - paceRange.slowSecPerKm) / totalRangeSec).toFloat().coerceIn(0f, 1f)
            // Target right: fastSecPerKm
            val targetRightFraction = ((leftBoundSec - paceRange.fastSecPerKm) / totalRangeSec).toFloat().coerceIn(0f, 1f)

            val targetStartX = width * targetLeftFraction
            val targetWidth = (width * targetRightFraction) - targetStartX

            // Draw target zone band
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(targetStartX, 4.dp.toPx()),
                size = Size(targetWidth, height - 8.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw center guide line
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 2.dp.toPx()
            )

            // Draw sliding marker
            if (currentPaceSecPerKm != null) {
                val markerX = width * animatedMarkerFraction
                val markerColor = if (isOutOfRange) AppColors.Alert else Color.White
                drawCircle(
                    color = markerColor,
                    radius = 9.dp.toPx(),
                    center = Offset(markerX, height / 2)
                )
            }
        }

        // Alert Pill overlay
        if (isOutOfRange) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(AppColors.Alert, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val alertText = if (isTooFast) "Too fast →" else "← Too slow"
                Text(
                    text = alertText,
                    color = AppColors.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
