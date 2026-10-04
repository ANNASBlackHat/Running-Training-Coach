package com.runningcompanion.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.ui.components.SessionStrip
import com.runningcompanion.app.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessions: List<Session>,
    onSessionClicked: (Session) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session History", fontWeight = FontWeight.Bold, color = AppColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Paper)
            )
        },
        containerColor = AppColors.Paper
    ) { padding ->
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No sessions yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Ink
                )
                Text(
                    text = "Start a workout to see your results and interval splits here.",
                    fontSize = 14.sp,
                    color = AppColors.InkMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionClicked(session) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = dateFormat.format(Date(session.startedAt)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.InkMuted
                            )
                            Text(
                                text = session.workoutSnapshot.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Ink,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            SessionStrip(
                                segments = session.workoutSnapshot.flatten(),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "%.2f km".format(session.totals.distanceM / 1000.0),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Ink
                                )
                                Text(
                                    text = PaceCalculator.formatTime(session.totals.durationSec),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Ink
                                )
                                Text(
                                    text = "${PaceCalculator.formatPace(session.totals.avgPaceSecPerKm)}/km",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Run
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
