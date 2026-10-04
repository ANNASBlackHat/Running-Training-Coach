package com.runningcompanion.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.RuntimeSegment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.ui.components.SessionStrip
import com.runningcompanion.app.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workout: Workout,
    onStartWorkout: () -> Unit,
    onEditWorkout: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flattened = workout.flatten()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout.name, fontWeight = FontWeight.Bold, color = AppColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.Ink)
                    }
                },
                actions = {
                    IconButton(onClick = onEditWorkout) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Workout", tint = AppColors.Run)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Paper)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Run)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Start workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = AppColors.Paper
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "WORKOUT OVERVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.InkMuted,
                            letterSpacing = 1.sp
                        )
                        SessionStrip(
                            segments = flattened,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .height(18.dp)
                        )
                        Text(
                            text = "${flattened.size} total segments",
                            fontSize = 14.sp,
                            color = AppColors.InkMuted
                        )
                    }
                }
            }

            item {
                Text(
                    text = "SEGMENTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AppColors.InkMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(flattened) { seg ->
                SegmentDetailRow(seg)
                HorizontalDivider(color = AppColors.Line, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun SegmentDetailRow(seg: RuntimeSegment) {
    val type = seg.segment.type
    val color = when (type) {
        SegmentType.RUN -> AppColors.Run
        SegmentType.REST -> AppColors.Rest
        else -> AppColors.Base
    }

    val typeLabel = when (type) {
        SegmentType.RUN -> if (seg.setNumber != null) "Run (Set ${seg.setNumber} of ${seg.setTotal})" else "Run"
        SegmentType.REST -> "Rest"
        SegmentType.WARMUP -> "Warm-up"
        SegmentType.COOLDOWN -> "Cool-down"
    }

    val lengthText = when (val len = seg.segment.length) {
        is SegmentLength.Time -> PaceCalculator.formatTime(len.seconds.toLong())
        is SegmentLength.Distance -> "${len.meters}m"
    }

    val paceRange = seg.segment.paceRange

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = type.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (type == SegmentType.REST) AppColors.Ink else Color.White
                )
            }
            Column {
                Text(
                    text = typeLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Ink
                )
                if (paceRange != null) {
                    val fastStr = PaceCalculator.formatPace(paceRange.fastSecPerKm.toDouble())
                    val slowStr = PaceCalculator.formatPace(paceRange.slowSecPerKm.toDouble())
                    Text(
                        text = "Target $fastStr - $slowStr /km",
                        fontSize = 12.sp,
                        color = AppColors.InkMuted
                    )
                }
            }
        }

        Text(
            text = lengthText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Ink
        )
    }
}
