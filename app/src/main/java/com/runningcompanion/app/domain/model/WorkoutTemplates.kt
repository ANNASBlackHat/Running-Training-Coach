package com.runningcompanion.app.domain.model

import java.util.UUID

object WorkoutTemplates {

    val norwegian4x4 = Workout(
        id = "template_norwegian_4x4",
        name = "Norwegian 4x4",
        isTemplate = true,
        items = listOf(
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.WARMUP,
                    length = SegmentLength.Time(600) // 10 min
                )
            ),
            WorkoutItem.Repeat(
                count = 4,
                segments = listOf(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        type = SegmentType.RUN,
                        length = SegmentLength.Time(240), // 4 min
                        paceRange = PaceRange(fastSecPerKm = 270, slowSecPerKm = 300) // 4:30 - 5:00 min/km
                    ),
                    Segment(
                        id = UUID.randomUUID().toString(),
                        type = SegmentType.REST,
                        length = SegmentLength.Time(180) // 3 min
                    )
                )
            ),
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.COOLDOWN,
                    length = SegmentLength.Time(300) // 5 min
                )
            )
        )
    )

    val repeats400m = Workout(
        id = "template_400m_repeats",
        name = "400m Repeats",
        isTemplate = true,
        items = listOf(
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.WARMUP,
                    length = SegmentLength.Time(600) // 10 min
                )
            ),
            WorkoutItem.Repeat(
                count = 8,
                segments = listOf(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        type = SegmentType.RUN,
                        length = SegmentLength.Distance(400),
                        paceRange = PaceRange(fastSecPerKm = 240, slowSecPerKm = 270) // 4:00 - 4:30 min/km
                    ),
                    Segment(
                        id = UUID.randomUUID().toString(),
                        type = SegmentType.REST,
                        length = SegmentLength.Time(90) // 1.5 min
                    )
                )
            ),
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.COOLDOWN,
                    length = SegmentLength.Time(300) // 5 min
                )
            )
        )
    )

    val tempoRun = Workout(
        id = "template_tempo_run",
        name = "Continuous Tempo (20 min)",
        isTemplate = true,
        items = listOf(
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.WARMUP,
                    length = SegmentLength.Time(600) // 10 min
                )
            ),
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.RUN,
                    length = SegmentLength.Time(1200), // 20 min threshold
                    paceRange = PaceRange(fastSecPerKm = 300, slowSecPerKm = 315) // 5:00 - 5:15 min/km
                )
            ),
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.COOLDOWN,
                    length = SegmentLength.Time(300) // 5 min
                )
            )
        )
    )

    val tempoSplits = Workout(
        id = "template_tempo_splits",
        name = "Tempo Splits (3x 8 min)",
        isTemplate = true,
        items = listOf(
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.WARMUP,
                    length = SegmentLength.Time(600) // 10 min
                )
            ),
            WorkoutItem.Repeat(
                count = 3,
                segments = listOf(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        type = SegmentType.RUN,
                        length = SegmentLength.Time(480), // 8 min threshold
                        paceRange = PaceRange(fastSecPerKm = 300, slowSecPerKm = 315) // 5:00 - 5:15 min/km
                    ),
                    Segment(
                        id = UUID.randomUUID().toString(),
                        type = SegmentType.REST,
                        length = SegmentLength.Time(90) // 1.5 min jog/rest
                    )
                )
            ),
            WorkoutItem.Single(
                Segment(
                    id = UUID.randomUUID().toString(),
                    type = SegmentType.COOLDOWN,
                    length = SegmentLength.Time(300) // 5 min
                )
            )
        )
    )

    val allTemplates = listOf(norwegian4x4, repeats400m, tempoRun, tempoSplits)
}
