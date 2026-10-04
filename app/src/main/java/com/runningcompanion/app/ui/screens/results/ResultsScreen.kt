package com.runningcompanion.app.ui.screens.results

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.runningcompanion.app.domain.model.SegmentResult
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.ui.components.SessionStrip
import com.runningcompanion.app.ui.components.WorkoutMapView
import com.runningcompanion.app.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultsScreen(
    session: Session,
    onHomeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", Locale.getDefault())

    // Find the fastest run segment to highlight
    val runSegments = session.results.filter { it.type == SegmentType.RUN && (it.avgPaceSecPerKm ?: Double.MAX_VALUE) > 0 }
    val fastestRunSegment = runSegments.minByOrNull { it.avgPaceSecPerKm ?: Double.MAX_VALUE }

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
                    onClick = onHomeClicked,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Run)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Back to Workouts", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Strava-Style Hero Header
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32), // Emerald Green
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "WORKOUT COMPLETED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = session.workoutSnapshot.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Ink
                    )
                    Text(
                        text = dateFormat.format(Date(session.startedAt)),
                        fontSize = 14.sp,
                        color = AppColors.InkMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Strava-Style 4-Box Key Metrics Grid
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HeroMetricItem(
                                label = "DISTANCE",
                                value = "%.2f".format(session.totals.distanceM / 1000.0),
                                unit = "km",
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricItem(
                                label = "ACTIVE TIME",
                                value = PaceCalculator.formatTime(session.totals.durationSec),
                                unit = "",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = AppColors.Line, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HeroMetricItem(
                                label = "AVG PACE",
                                value = PaceCalculator.formatPace(session.totals.avgPaceSecPerKm),
                                unit = "/km",
                                modifier = Modifier.weight(1f)
                            )
                            val bestPaceStr = if (fastestRunSegment?.avgPaceSecPerKm != null) {
                                "${PaceCalculator.formatPace(fastestRunSegment.avgPaceSecPerKm)}"
                            } else "--:--"
                            HeroMetricItem(
                                label = "FASTEST SET",
                                value = bestPaceStr,
                                unit = "/km",
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // OpenStreetMap Route Map Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GPS ROUTE MAP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.InkMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "OpenStreetMap",
                                fontSize = 11.sp,
                                color = AppColors.InkMuted
                            )
                        }

                        WorkoutMapView(track = session.track)
                    }
                }
            }

            // Session Strip Visual Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INTERVAL TIMELINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.InkMuted,
                            letterSpacing = 1.sp
                        )
                        SessionStrip(
                            segments = session.workoutSnapshot.flatten(),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(16.dp)
                        )
                    }
                }
            }

            // Segment Splits Table
            item {
                Text(
                    text = "INTERVAL BREAKDOWN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AppColors.InkMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(session.results) { res ->
                val isFastest = res == fastestRunSegment
                SegmentResultRow(res, isFastest = isFastest)
                HorizontalDivider(color = AppColors.Line, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun HeroMetricItem(
    label: String,
    value: String,
    unit: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.InkMuted,
            letterSpacing = 0.5.sp
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) Color(0xFFFC4C02) else AppColors.Ink // Strava orange highlight
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.InkMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun SegmentResultRow(result: SegmentResult, isFastest: Boolean) {
    val typeName = when (result.type) {
        SegmentType.RUN -> if (result.setNumber != null) "Run (Set ${result.setNumber})" else "Run"
        SegmentType.REST -> "Rest"
        SegmentType.WARMUP -> "Warm-up"
        SegmentType.COOLDOWN -> "Cool-down"
    }

    val typeColor = when (result.type) {
        SegmentType.RUN -> AppColors.Run
        SegmentType.REST -> AppColors.Rest
        else -> AppColors.Base
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = result.type.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.type == SegmentType.REST) AppColors.Ink else Color.White
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Ink
                        )
                        if (isFastest) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFF3E0))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFC4C02),
                                        modifier = Modifier.width(11.dp).height(11.dp)
                                    )
                                    Text(
                                        text = "Fastest",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFC4C02),
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "${PaceCalculator.formatTime(result.durationSec)}  •  ${result.distanceM.toInt()}m  •  ${PaceCalculator.formatPace(result.avgPaceSecPerKm)}/km",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isFastest) Color(0xFFFC4C02) else AppColors.Ink
            )
        }

        // Show per-km splits for warmup or cooldown
        if (result.kmSplits.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, top = 6.dp)
            ) {
                result.kmSplits.forEach { split ->
                    Text(
                        text = "↳ Km ${split.km}: ${PaceCalculator.formatPace(split.paceSecPerKm)}/km",
                        fontSize = 12.sp,
                        color = AppColors.InkMuted
                    )
                }
            }
        }
    }
}
