package com.runningcompanion.app.ui.screens.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem
import com.runningcompanion.app.ui.components.SessionStrip
import com.runningcompanion.app.ui.theme.AppColors
import java.util.UUID

// Data holder for identifying which segment is being edited
private data class EditingTarget(
    val itemIndex: Int,
    val subIndex: Int? = null, // null if Single, index if inside Repeat
    val segment: Segment
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreen(
    initialWorkout: Workout? = null,
    onSaveWorkout: (Workout) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workoutName by remember { mutableStateOf(initialWorkout?.name ?: "Custom Workout") }
    val workoutItems = remember {
        mutableStateListOf<WorkoutItem>().apply {
            if (initialWorkout != null) {
                addAll(initialWorkout.items)
            } else {
                add(
                    WorkoutItem.Single(
                        Segment(
                            id = UUID.randomUUID().toString(),
                            type = SegmentType.WARMUP,
                            length = SegmentLength.Time(300)
                        )
                    )
                )
                add(
                    WorkoutItem.Repeat(
                        count = 4,
                        segments = listOf(
                            Segment(
                                id = UUID.randomUUID().toString(),
                                type = SegmentType.RUN,
                                length = SegmentLength.Time(240),
                                paceRange = PaceRange(270, 300)
                            ),
                            Segment(
                                id = UUID.randomUUID().toString(),
                                type = SegmentType.REST,
                                length = SegmentLength.Time(180)
                            )
                        )
                    )
                )
                add(
                    WorkoutItem.Single(
                        Segment(
                            id = UUID.randomUUID().toString(),
                            type = SegmentType.COOLDOWN,
                            length = SegmentLength.Time(300)
                        )
                    )
                )
            }
        }
    }

    var editingTarget by remember { mutableStateOf<EditingTarget?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val previewWorkout = Workout(
        id = initialWorkout?.id ?: UUID.randomUUID().toString(),
        name = workoutName,
        isTemplate = false,
        items = workoutItems.toList()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Builder", fontWeight = FontWeight.Bold, color = AppColors.Ink) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.Ink)
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
                    onClick = {
                        if (workoutName.isNotBlank() && workoutItems.isNotEmpty()) {
                            onSaveWorkout(previewWorkout)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Run)
                ) {
                    Text("Save workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = AppColors.Paper
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = workoutName,
                    onValueChange = { workoutName = it },
                    label = { Text("Workout Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text(
                    text = "WORKOUT PREVIEW (TAP SEGMENT TO EDIT)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.InkMuted
                )
                SessionStrip(
                    segments = previewWorkout.flatten(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(18.dp)
                )
            }

            // Structured Workout Items List
            itemsIndexed(workoutItems) { itemIndex, item ->
                when (item) {
                    is WorkoutItem.Single -> {
                        SegmentCard(
                            segment = item.segment,
                            subtitle = null,
                            onEditClicked = {
                                editingTarget = EditingTarget(itemIndex, null, item.segment)
                            },
                            onDeleteClicked = {
                                workoutItems.removeAt(itemIndex)
                            }
                        )
                    }
                    is WorkoutItem.Repeat -> {
                        RepeatBlockCard(
                            repeatItem = item,
                            onCountChanged = { newCount ->
                                workoutItems[itemIndex] = item.copy(count = newCount)
                            },
                            onEditSegment = { subIdx, seg ->
                                editingTarget = EditingTarget(itemIndex, subIdx, seg)
                            },
                            onAddSegmentToRepeat = { newType ->
                                val newSeg = Segment(
                                    id = UUID.randomUUID().toString(),
                                    type = newType,
                                    length = SegmentLength.Time(if (newType == SegmentType.RUN) 180 else 120),
                                    paceRange = if (newType == SegmentType.RUN) PaceRange(270, 300) else null
                                )
                                val updatedList = item.segments.toMutableList().apply { add(newSeg) }
                                workoutItems[itemIndex] = item.copy(segments = updatedList)
                            },
                            onDeleteSegmentFromRepeat = { subIdx ->
                                if (item.segments.size > 1) {
                                    val updatedList = item.segments.toMutableList().apply { removeAt(subIdx) }
                                    workoutItems[itemIndex] = item.copy(segments = updatedList)
                                } else {
                                    workoutItems.removeAt(itemIndex)
                                }
                            },
                            onDeleteRepeatBlock = {
                                workoutItems.removeAt(itemIndex)
                            }
                        )
                    }
                }
            }

            // Action Buttons to Add Sections
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Add Repeat Block (Garmin style)
                    Button(
                        onClick = {
                            workoutItems.add(
                                WorkoutItem.Repeat(
                                    count = 4,
                                    segments = listOf(
                                        Segment(
                                            id = UUID.randomUUID().toString(),
                                            type = SegmentType.RUN,
                                            length = SegmentLength.Time(240),
                                            paceRange = PaceRange(270, 300)
                                        ),
                                        Segment(
                                            id = UUID.randomUUID().toString(),
                                            type = SegmentType.REST,
                                            length = SegmentLength.Time(120)
                                        )
                                    )
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Run.copy(alpha = 0.12f),
                            contentColor = AppColors.Run
                        )
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Add Repeat Set (e.g. 4x Run + Rest)", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                workoutItems.add(
                                    WorkoutItem.Single(
                                        Segment(
                                            id = UUID.randomUUID().toString(),
                                            type = SegmentType.WARMUP,
                                            length = SegmentLength.Time(300)
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Warm-up", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                workoutItems.add(
                                    WorkoutItem.Single(
                                        Segment(
                                            id = UUID.randomUUID().toString(),
                                            type = SegmentType.COOLDOWN,
                                            length = SegmentLength.Time(300)
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Cool-down", fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Interactive Segment Editor Bottom Sheet
    editingTarget?.let { target ->
        SegmentEditorBottomSheet(
            initialSegment = target.segment,
            onDismiss = { editingTarget = null },
            onSave = { updatedSegment ->
                if (target.subIndex == null) {
                    workoutItems[target.itemIndex] = WorkoutItem.Single(updatedSegment)
                } else {
                    val repeatItem = workoutItems[target.itemIndex] as WorkoutItem.Repeat
                    val updatedList = repeatItem.segments.toMutableList()
                    updatedList[target.subIndex] = updatedSegment
                    workoutItems[target.itemIndex] = repeatItem.copy(segments = updatedList)
                }
                editingTarget = null
            }
        )
    }
}

@Composable
private fun SegmentCard(
    segment: Segment,
    subtitle: String?,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val color = when (segment.type) {
        SegmentType.RUN -> AppColors.Run
        SegmentType.REST -> AppColors.Rest
        else -> AppColors.Base
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClicked() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = segment.type.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (segment.type == SegmentType.REST) AppColors.Ink else Color.White
                    )
                }
                Column {
                    Text(
                        text = formatLength(segment.length),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Ink
                    )
                    val pace = segment.paceRange
                    if (pace != null) {
                        Text(
                            text = "Target: ${PaceCalculator.formatPace(pace.fastSecPerKm.toDouble())} to ${PaceCalculator.formatPace(pace.slowSecPerKm.toDouble())}/km",
                            fontSize = 12.sp,
                            color = AppColors.InkMuted
                        )
                    } else if (subtitle != null) {
                        Text(text = subtitle, fontSize = 12.sp, color = AppColors.InkMuted)
                    }
                }
            }

            Row {
                IconButton(onClick = onEditClicked) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppColors.Run)
                }
                IconButton(onClick = onDeleteClicked) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun RepeatBlockCard(
    repeatItem: WorkoutItem.Repeat,
    onCountChanged: (Int) -> Unit,
    onEditSegment: (Int, Segment) -> Unit,
    onAddSegmentToRepeat: (SegmentType) -> Unit,
    onDeleteSegmentFromRepeat: (Int) -> Unit,
    onDeleteRepeatBlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Repeat Group Header with Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = AppColors.Run, modifier = Modifier.padding(end = 6.dp))
                    Text(
                        text = "REPEAT GROUP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Run,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (repeatItem.count > 1) onCountChanged(repeatItem.count - 1) },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                    }
                    Text(
                        text = "${repeatItem.count}x",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Ink
                    )
                    IconButton(
                        onClick = { if (repeatItem.count < 30) onCountChanged(repeatItem.count + 1) },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                    }

                    IconButton(onClick = onDeleteRepeatBlock) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }

            HorizontalDivider(color = AppColors.Line, modifier = Modifier.padding(vertical = 8.dp))

            // Segments inside the repeat group
            repeatItem.segments.forEachIndexed { subIndex, seg ->
                SegmentCard(
                    segment = seg,
                    subtitle = "Set step ${subIndex + 1}",
                    onEditClicked = { onEditSegment(subIndex, seg) },
                    onDeleteClicked = { onDeleteSegmentFromRepeat(subIndex) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Quick add to this group
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAddSegmentToRepeat(SegmentType.RUN) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add Run Step", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onAddSegmentToRepeat(SegmentType.REST) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add Rest Step", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentEditorBottomSheet(
    initialSegment: Segment,
    onDismiss: () -> Unit,
    onSave: (Segment) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialSegment.type) }
    var isDistanceMode by remember { mutableStateOf(initialSegment.length is SegmentLength.Distance) }

    // Length inputs
    var minutesText by remember {
        mutableStateOf(
            if (initialSegment.length is SegmentLength.Time) (initialSegment.length.seconds / 60).toString() else "4"
        )
    }
    var secondsText by remember {
        mutableStateOf(
            if (initialSegment.length is SegmentLength.Time) (initialSegment.length.seconds % 60).toString() else "0"
        )
    }
    var metersText by remember {
        mutableStateOf(
            if (initialSegment.length is SegmentLength.Distance) initialSegment.length.meters.toString() else "400"
        )
    }

    // Pace target inputs
    var enablePaceTarget by remember { mutableStateOf(initialSegment.paceRange != null) }
    var fastMinText by remember {
        mutableStateOf(
            if (initialSegment.paceRange != null) (initialSegment.paceRange.fastSecPerKm / 60).toString() else "4"
        )
    }
    var fastSecText by remember {
        mutableStateOf(
            if (initialSegment.paceRange != null) "%02d".format(initialSegment.paceRange.fastSecPerKm % 60) else "30"
        )
    }
    var slowMinText by remember {
        mutableStateOf(
            if (initialSegment.paceRange != null) (initialSegment.paceRange.slowSecPerKm / 60).toString() else "5"
        )
    }
    var slowSecText by remember {
        mutableStateOf(
            if (initialSegment.paceRange != null) "%02d".format(initialSegment.paceRange.slowSecPerKm % 60) else "00"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Paper,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Segment",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Ink
            )

            // Segment Type Chips
            Text(text = "TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.InkMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SegmentType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Run,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Length Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "TARGET BY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.InkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isDistanceMode,
                        onClick = { isDistanceMode = false },
                        label = { Text("Time") }
                    )
                    FilterChip(
                        selected = isDistanceMode,
                        onClick = { isDistanceMode = true },
                        label = { Text("Distance") }
                    )
                }
            }

            if (!isDistanceMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = metersText,
                    onValueChange = { metersText = it.filter { c -> c.isDigit() } },
                    label = { Text("Meters (e.g. 400 or 1000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(color = AppColors.Line)

            // Target Pace Range (Run segments)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "TARGET PACE RANGE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                    Text(text = "App will alert when outside range", fontSize = 12.sp, color = AppColors.InkMuted)
                }
                Switch(checked = enablePaceTarget, onCheckedChange = { enablePaceTarget = it })
            }

            if (enablePaceTarget) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Fast Pace (e.g. 4:30 min/km)", fontSize = 12.sp, color = AppColors.InkMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = fastMinText,
                            onValueChange = { fastMinText = it.filter { c -> c.isDigit() } },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fastSecText,
                            onValueChange = { fastSecText = it.filter { c -> c.isDigit() } },
                            label = { Text("Sec") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(text = "Slow Pace (e.g. 5:00 min/km)", fontSize = 12.sp, color = AppColors.InkMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = slowMinText,
                            onValueChange = { slowMinText = it.filter { c -> c.isDigit() } },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = slowSecText,
                            onValueChange = { slowSecText = it.filter { c -> c.isDigit() } },
                            label = { Text("Sec") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    val length = if (!isDistanceMode) {
                        val mins = minutesText.toIntOrNull() ?: 0
                        val secs = secondsText.toIntOrNull() ?: 0
                        SegmentLength.Time((mins * 60 + secs).coerceAtLeast(5))
                    } else {
                        val meters = metersText.toIntOrNull() ?: 100
                        SegmentLength.Distance(meters.coerceAtLeast(10))
                    }

                    val paceRange = if (enablePaceTarget) {
                        val fMin = fastMinText.toIntOrNull() ?: 4
                        val fSec = fastSecText.toIntOrNull() ?: 30
                        val sMin = slowMinText.toIntOrNull() ?: 5
                        val sSec = slowSecText.toIntOrNull() ?: 0
                        val fastSecTotal = fMin * 60 + fSec
                        val slowSecTotal = sMin * 60 + sSec
                        if (fastSecTotal <= slowSecTotal) {
                            PaceRange(fastSecTotal, slowSecTotal)
                        } else {
                            PaceRange(slowSecTotal, fastSecTotal)
                        }
                    } else null

                    onSave(
                        initialSegment.copy(
                            type = selectedType,
                            length = length,
                            paceRange = paceRange
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Run)
            ) {
                Text("Apply Changes", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatLength(length: SegmentLength): String {
    return when (length) {
        is SegmentLength.Time -> PaceCalculator.formatTime(length.seconds.toLong())
        is SegmentLength.Distance -> "${length.meters}m"
    }
}
