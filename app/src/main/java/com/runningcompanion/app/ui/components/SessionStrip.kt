package com.runningcompanion.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.ui.theme.AppColors

@Composable
fun SessionStrip(
    segments: List<RuntimeSegment>,
    currentIndex: Int? = null,
    currentProgress: Float = 0f,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    if (segments.isEmpty()) return

    // Calculate approximate weight for each segment
    val weights = segments.map { seg ->
        when (val len = seg.segment.length) {
            is SegmentLength.Time -> len.seconds.toFloat()
            is SegmentLength.Distance -> (len.meters / 1000f) * 330f // ~5:30/km baseline
        }.coerceAtLeast(10f)
    }
    val totalWeight = weights.sum()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val gapPx = 2.dp.toPx()
        val totalGaps = (segments.size - 1).coerceAtLeast(0) * gapPx
        val availableWidth = (totalWidth - totalGaps).coerceAtLeast(0f)

        var xOffset = 0f

        segments.forEachIndexed { index, seg ->
            val segmentWidth = (weights[index] / totalWeight) * availableWidth
            val isCurrent = currentIndex != null && index == currentIndex
            val isFinished = currentIndex != null && index < currentIndex

            val baseColor = when (seg.segment.type) {
                SegmentType.RUN -> AppColors.Run
                SegmentType.REST -> AppColors.Rest
                SegmentType.WARMUP, SegmentType.COOLDOWN -> AppColors.Base
            }

            val fillColor = when {
                isFinished -> baseColor.copy(alpha = 0.35f)
                else -> baseColor
            }

            // Draw background block
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(xOffset, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // If rest, draw subtle outline
            if (seg.segment.type == SegmentType.REST) {
                drawRoundRect(
                    color = AppColors.Ink.copy(alpha = 0.4f),
                    topLeft = Offset(xOffset, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // If current, draw progress overlay
            if (isCurrent && currentProgress > 0f) {
                val progressWidth = segmentWidth * currentProgress.coerceIn(0f, 1f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.6f),
                    topLeft = Offset(xOffset, 0f),
                    size = Size(progressWidth, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            xOffset += segmentWidth + gapPx
        }
    }
}
