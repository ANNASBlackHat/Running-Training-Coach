package com.runningcompanion.app.ui.screens.run

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcompanion.app.domain.engine.RunnerSnapshot
import com.runningcompanion.app.domain.engine.RunnerState
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.ui.components.HoldToStopButton
import com.runningcompanion.app.ui.components.PaceLane
import com.runningcompanion.app.ui.components.SessionStrip
import com.runningcompanion.app.ui.theme.AppColors
import com.runningcompanion.app.ui.theme.PaceL
import com.runningcompanion.app.ui.theme.TimerXL

@Composable
fun LiveRunScreen(
    snapshot: RunnerSnapshot?,
    allSegments: List<RuntimeSegment>,
    onPauseClicked: () -> Unit,
    onResumeClicked: () -> Unit,
    onSkipClicked: () -> Unit,
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSeg = snapshot?.currentSegment
    val type = currentSeg?.segment?.type
    val colors = AppColors.forSegment(type)

    // Background color transition over 250ms (deliberate animation moment from spec)
    val animatedBgColor by animateColorAsState(
        targetValue = colors.bg,
        animationSpec = tween(durationMillis = 250),
        label = "bgColorTransition"
    )

    val textColor = colors.fg

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Segment Name & Set counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val segName = when (type) {
                    SegmentType.RUN -> "Run"
                    SegmentType.REST -> "Rest"
                    SegmentType.WARMUP -> "Warm-up"
                    SegmentType.COOLDOWN -> "Cool-down"
                    null -> "Get ready"
                }

                Text(
                    text = segName,
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (currentSeg?.setNumber != null && currentSeg.setTotal != null) {
                    Text(
                        text = "Set ${currentSeg.setNumber} of ${currentSeg.setTotal}",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Center Primary Area: Countdown Timer / Remaining Distance
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val remainingText = if (snapshot?.segmentRemainingSec != null) {
                    PaceCalculator.formatTime(snapshot.segmentRemainingSec)
                } else if (snapshot?.segmentRemainingM != null) {
                    "${snapshot.segmentRemainingM.toInt()}m"
                } else {
                    "--:--"
                }

                Text(
                    text = remainingText,
                    style = TimerXL,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                // Subtitle: e.g. "of 4:00" or "of 400m"
                val totalLengthText = when (val len = currentSeg?.segment?.length) {
                    is SegmentLength.Time -> "of ${PaceCalculator.formatTime(len.seconds.toLong())}"
                    is SegmentLength.Distance -> "of ${len.meters}m"
                    null -> ""
                }
                if (totalLengthText.isNotEmpty()) {
                    Text(
                        text = totalLengthText,
                        color = textColor.copy(alpha = 0.75f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Pace & Pace Lane
            Column(modifier = Modifier.fillMaxWidth()) {
                val paceString = PaceCalculator.formatPace(snapshot?.currentPaceSecPerKm)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = paceString,
                        style = PaceL,
                        color = textColor
                    )
                    Text(
                        text = " /km",
                        color = textColor.copy(alpha = 0.75f),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }

                // Dynamic Pace Lane (shown only if segment has target pace)
                val paceRange = currentSeg?.segment?.paceRange
                if (paceRange != null) {
                    PaceLane(
                        paceRange = paceRange,
                        currentPaceSecPerKm = snapshot?.currentPaceSecPerKm,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    val fastStr = PaceCalculator.formatPace(paceRange.fastSecPerKm.toDouble())
                    val slowStr = PaceCalculator.formatPace(paceRange.slowSecPerKm.toDouble())
                    Text(
                        text = "Target $fastStr to $slowStr /km",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Session Strip & Workout Progress
            Column(modifier = Modifier.fillMaxWidth()) {
                val progressFraction = when (val len = currentSeg?.segment?.length) {
                    is SegmentLength.Time -> {
                        val elapsed = snapshot?.segmentElapsedSec ?: 0L
                        (elapsed.toFloat() / len.seconds.toFloat()).coerceIn(0f, 1f)
                    }
                    is SegmentLength.Distance -> {
                        val dist = snapshot?.segmentDistanceM ?: 0.0
                        (dist.toFloat() / len.meters.toFloat()).coerceIn(0f, 1f)
                    }
                    null -> 0f
                }

                SessionStrip(
                    segments = allSegments,
                    currentIndex = snapshot?.currentSegmentIndex,
                    currentProgress = progressFraction,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Stats: Total Distance & Total Elapsed Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val distKm = (snapshot?.totalDistanceM ?: 0.0) / 1000.0
                    Text(
                        text = "%.2f km".format(distKm),
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = PaceCalculator.formatTime(snapshot?.totalDurationSec ?: 0L),
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bottom Actions: Pause/Resume, Skip, Hold to Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPaused = snapshot?.state == RunnerState.PAUSED

                Button(
                    onClick = if (isPaused) onResumeClicked else onPauseClicked,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == SegmentType.REST) AppColors.Ink else Color.White,
                        contentColor = if (type == SegmentType.REST) Color.White else AppColors.Ink
                    )
                ) {
                    Text(
                        text = if (isPaused) "Resume" else "Pause",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = onSkipClicked,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Skip",
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                HoldToStopButton(
                    onStopConfirmed = onStopConfirmed,
                    modifier = Modifier.weight(1.3f),
                    textColor = textColor
                )
            }
        }
    }
}
