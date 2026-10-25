package com.runningcompanion.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.summary.WorkoutSummary
import com.runningcompanion.app.domain.time.relativeDayLabel
import com.runningcompanion.app.ui.components.SessionStrip
import com.runningcompanion.app.ui.theme.AppColors
import com.runningcompanion.app.ui.theme.AppShapes
import com.runningcompanion.app.ui.theme.AppSpacing
import com.runningcompanion.app.ui.theme.ChipLabel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    workouts: List<Workout>,
    recentSession: Session?,
    activeWorkout: Workout?,
    onStartWorkout: (Workout) -> Unit,
    onResumeWorkout: () -> Unit,
    onWorkoutDetail: (Workout) -> Unit,
    onNewWorkout: () -> Unit,
    onHistoryClicked: () -> Unit,
    onSessionClicked: (Session) -> Unit,
    onDuplicateWorkout: (Workout) -> Unit,
    onRenameWorkout: (Workout, String) -> Unit,
    onDeleteWorkout: (Workout) -> Unit,
    modifier: Modifier = Modifier
) {
    val templates = workouts.filter { it.isTemplate }
    val myWorkouts = workouts.filter { !it.isTemplate }

    var renameTarget by remember { mutableStateOf<Workout?>(null) }
    var deleteTarget by remember { mutableStateOf<Workout?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Paper,
        bottomBar = { NewWorkoutBar(onNewWorkout = onNewWorkout) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = AppSpacing.gutter,
                end = AppSpacing.gutter,
                top = AppSpacing.xl,
                bottom = AppSpacing.listBottomInset
            )
        ) {
            item(key = "header") {
                HomeHeader(onHistoryClicked = onHistoryClicked)
            }

            if (activeWorkout != null) {
                item(key = "resume_run") {
                    ResumeRunBar(
                        workout = activeWorkout,
                        onResume = onResumeWorkout,
                        modifier = Modifier.padding(bottom = AppSpacing.md)
                    )
                }
            }

            if (recentSession != null) {
                item(key = "recent_session") {
                    LastRunCard(
                        session = recentSession,
                        onClick = { onSessionClicked(recentSession) },
                        modifier = Modifier.padding(bottom = AppSpacing.lg)
                    )
                }
            }

            if (templates.isNotEmpty()) {
                stickyHeader(key = "templates_header") {
                    SectionHeader(title = "TEMPLATES")
                }

                item(key = "templates_rail") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        contentPadding = PaddingValues(bottom = AppSpacing.sm)
                    ) {
                        items(templates, key = { it.id }) { template ->
                            TemplateCard(
                                workout = template,
                                onClick = { onWorkoutDetail(template) },
                                onStart = { onStartWorkout(template) }
                            )
                        }
                    }
                }
            }

            stickyHeader(key = "my_workouts_header") {
                SectionHeader(
                    title = "MY WORKOUTS",
                    topPadding = if (templates.isEmpty()) AppSpacing.sm else AppSpacing.xl
                )
            }

            if (myWorkouts.isEmpty()) {
                item(key = "my_workouts_empty") {
                    EmptyWorkoutsCard()
                }
            } else {
                items(myWorkouts, key = { it.id }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        onClick = { onWorkoutDetail(workout) },
                        onStart = { onStartWorkout(workout) },
                        onRename = { renameTarget = workout },
                        onDuplicate = { onDuplicateWorkout(workout) },
                        onDelete = { deleteTarget = workout }
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                }
            }
        }
    }

    renameTarget?.let { workout ->
        RenameWorkoutDialog(
            workout = workout,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                renameTarget = null
                if (newName != workout.name) onRenameWorkout(workout, newName)
            }
        )
    }

    deleteTarget?.let { workout ->
        DeleteWorkoutDialog(
            workout = workout,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteWorkout(workout)
            }
        )
    }
}

@Composable
private fun HomeHeader(onHistoryClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppSpacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Workouts",
            style = MaterialTheme.typography.displaySmall,
            color = AppColors.Ink
        )

        Surface(
            shape = AppShapes.pill,
            color = AppColors.White,
            border = BorderStroke(1.dp, AppColors.Line)
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onHistoryClicked)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = AppColors.Run,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Run,
                    modifier = Modifier.padding(start = AppSpacing.xs)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, topPadding: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Paper)
            .padding(top = topPadding, bottom = AppSpacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.InkMuted
        )
    }
}

@Composable
private fun ResumeRunBar(
    workout: Workout,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("resume_run_bar")
            .clip(AppShapes.card)
            .clickable(onClick = onResume),
        shape = AppShapes.card,
        color = AppColors.Run.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = AppColors.Run
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppSpacing.sm)
            ) {
                Text(
                    text = "WORKOUT IN PROGRESS",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Run
                )
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "Resume",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Run,
                modifier = Modifier.padding(start = AppSpacing.sm)
            )
        }
    }
}

@Composable
private fun LastRunCard(
    session: Session,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distanceKm = session.totals.distanceM / 1000.0
    val pace = PaceCalculator.formatPace(session.totals.avgPaceSecPerKm)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_session_card"),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = AppColors.White)
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LAST RUN",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.InkMuted
                )
                Text(
                    text = session.workoutSnapshot.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = AppSpacing.xs)
                )
                Text(
                    text = "${relativeDayLabel(session.startedAt)} · %.2f km · %s/km"
                        .format(distanceKm, pace),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.InkMuted,
                    modifier = Modifier.padding(top = AppSpacing.xs)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.InkMuted
            )
        }
    }
}

@Composable
private fun TemplateCard(
    workout: Workout,
    onClick: () -> Unit,
    onStart: () -> Unit
) {
    val segments = remember(workout) { workout.flatten() }

    Card(
        onClick = onClick,
        modifier = Modifier.width(240.dp),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = AppColors.White)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.lg)) {
            TemplateBadge()
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = AppSpacing.sm)
            )
            Text(
                text = WorkoutSummary.metaLine(workout),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.InkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            SessionStrip(
                segments = segments,
                height = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.md)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.sm),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .clickable(onClick = onStart)
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start ${workout.name}",
                        tint = AppColors.Run,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Run,
                        modifier = Modifier.padding(start = AppSpacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateBadge() {
    Box(
        modifier = Modifier
            .clip(AppShapes.chip)
            .background(AppColors.Line)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "TEMPLATE",
            style = ChipLabel,
            color = AppColors.InkMuted
        )
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val segments = remember(workout) { workout.flatten() }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = AppColors.White)
    ) {
        Row(
            modifier = Modifier.padding(start = AppSpacing.lg, top = AppSpacing.lg, bottom = AppSpacing.lg, end = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = AppSpacing.sm)
            ) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = WorkoutSummary.metaLine(workout),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, bottom = AppSpacing.sm)
                )
                SessionStrip(
                    segments = segments,
                    height = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(
                onClick = onStart,
                modifier = Modifier
                    .background(AppColors.Run.copy(alpha = 0.1f), AppShapes.button)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start ${workout.name}",
                    tint = AppColors.Run
                )
            }

            WorkoutMenu(
                workout = workout,
                onRename = onRename,
                onDuplicate = onDuplicate,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun WorkoutMenu(
    workout: Workout,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options for ${workout.name}",
                tint = AppColors.InkMuted
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                onClick = {
                    expanded = false
                    onDuplicate()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun EmptyWorkoutsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.card,
        color = Color.Transparent,
        border = BorderStroke(1.dp, AppColors.Line)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.xl)) {
            Text(
                text = "No custom workouts yet",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Ink
            )
            Text(
                text = "Build your own intervals, or start from a template and adjust it to your target paces.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.InkMuted,
                modifier = Modifier.padding(top = AppSpacing.xs)
            )
        }
    }
}

@Composable
private fun NewWorkoutBar(onNewWorkout: () -> Unit) {
    Surface(
        color = AppColors.Paper,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.md)
        ) {
            Button(
                onClick = onNewWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = AppShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Run,
                    contentColor = AppColors.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = AppSpacing.sm)
                )
                Text("New workout", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun RenameWorkoutDialog(
    workout: Workout,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.card,
            color = AppColors.White
        ) {
            Column(modifier = Modifier.padding(AppSpacing.xl)) {
                Text(
                    text = "Rename workout",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.Ink
                )
                RenameWorkoutForm(
                    initialName = workout.name,
                    onCancel = onDismiss,
                    onConfirm = onConfirm,
                    modifier = Modifier.padding(top = AppSpacing.md)
                )
            }
        }
    }
}

/**
 * The editable part of the rename dialog, kept separate so it can be exercised
 * as plain content (dialogs containing text fields cannot be driven under Robolectric).
 */
@Composable
internal fun RenameWorkoutForm(
    initialName: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    Column(modifier) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Workout name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rename_field")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.sm),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_rename")
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun DeleteWorkoutDialog(
    workout: Workout,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete workout?") },
        text = {
            Text("\"${workout.name}\" will be removed permanently.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete")
            ) {
                Text("Delete", color = Color(0xFFC62828))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
