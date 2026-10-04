package com.runningcompanion.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.ui.components.SessionStrip
import com.runningcompanion.app.ui.theme.AppColors

@Composable
fun HomeScreen(
    workouts: List<Workout>,
    onStartWorkout: (Workout) -> Unit,
    onWorkoutDetail: (Workout) -> Unit,
    onNewWorkout: () -> Unit,
    onHistoryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val templates = workouts.filter { it.isTemplate }
    val myWorkouts = workouts.filter { !it.isTemplate }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Paper,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Button(
                    onClick = onNewWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Run,
                        contentColor = AppColors.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("New workout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 80.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workouts",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Ink
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onHistoryClicked() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = AppColors.Run
                        )
                        Text(
                            text = "History",
                            color = AppColors.Run,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            // Templates Section
            if (templates.isNotEmpty()) {
                item {
                    Text(
                        text = "TEMPLATES",
                        color = AppColors.InkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                items(templates, key = { it.id }) { workout ->
                    WorkoutRowItem(
                        workout = workout,
                        onRowClicked = { onWorkoutDetail(workout) },
                        onStartClicked = { onStartWorkout(workout) }
                    )
                    HorizontalDivider(color = AppColors.Line, thickness = 1.dp)
                }
            }

            // My Workouts Section
            item {
                Text(
                    text = "MY WORKOUTS",
                    color = AppColors.InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
                )
            }

            if (myWorkouts.isEmpty()) {
                item {
                    Text(
                        text = "No custom workouts yet. Tap 'New workout' below to create one.",
                        color = AppColors.InkMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(myWorkouts, key = { it.id }) { workout ->
                    WorkoutRowItem(
                        workout = workout,
                        onRowClicked = { onWorkoutDetail(workout) },
                        onStartClicked = { onStartWorkout(workout) }
                    )
                    HorizontalDivider(color = AppColors.Line, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun WorkoutRowItem(
    workout: Workout,
    onRowClicked: () -> Unit,
    onStartClicked: () -> Unit
) {
    val flattened = workout.flatten()
    val totalSeconds = flattened.sumOf {
        when (val len = it.segment.length) {
            is SegmentLength.Time -> len.seconds.toLong()
            is SegmentLength.Distance -> ((len.meters / 1000.0) * 330.0).toLong()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClicked() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = workout.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Ink
            )
            Text(
                text = "Approx. ${PaceCalculator.formatTime(totalSeconds)} • ${flattened.size} segments",
                fontSize = 13.sp,
                color = AppColors.InkMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            SessionStrip(
                segments = flattened,
                modifier = Modifier.fillMaxWidth().height(10.dp)
            )
        }

        IconButton(
            onClick = onStartClicked,
            modifier = Modifier
                .background(AppColors.Run.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start Workout",
                tint = AppColors.Run
            )
        }
    }
}
