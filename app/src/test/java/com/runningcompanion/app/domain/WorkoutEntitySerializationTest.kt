package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.data.db.entity.SessionEntity
import com.runningcompanion.app.data.db.entity.WorkoutEntity
import com.runningcompanion.app.domain.engine.ResultsCalculator
import com.runningcompanion.app.domain.model.SegmentResult
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.WorkoutTemplates
import org.junit.Test

class WorkoutEntitySerializationTest {

    @Test
    fun testWorkoutEntity_serializationRoundTrip() {
        val template = WorkoutTemplates.norwegian4x4
        val entity = WorkoutEntity.fromDomain(template)

        assertThat(entity.id).isEqualTo(template.id)
        assertThat(entity.isTemplate).isTrue()

        val restored = entity.toDomain()
        assertThat(restored.id).isEqualTo(template.id)
        assertThat(restored.name).isEqualTo(template.name)
        assertThat(restored.items).hasSize(template.items.size)
        assertThat(restored.flatten()).hasSize(10)
    }

    @Test
    fun testSessionEntity_serializationRoundTrip() {
        val template = WorkoutTemplates.repeats400m
        val segmentResults = listOf(
            SegmentResult(
                index = 0,
                type = SegmentType.WARMUP,
                setNumber = null,
                startedAt = 1000L,
                endedAt = 601000L,
                durationSec = 600L,
                distanceM = 1500.0,
                avgPaceSecPerKm = 400.0
            )
        )
        val session = ResultsCalculator.buildSession(
            workoutSnapshot = template,
            startedAt = 1000L,
            endedAt = 601000L,
            segmentResults = segmentResults,
            allTrackPoints = emptyList()
        )

        val entity = SessionEntity.fromDomain(session)
        assertThat(entity.workoutName).isEqualTo("400m Repeats")
        assertThat(entity.totalDurationSec).isEqualTo(600L)

        val restored = entity.toDomain()
        assertThat(restored.id).isEqualTo(session.id)
        assertThat(restored.results).hasSize(1)
        assertThat(restored.totals.durationSec).isEqualTo(600L)
    }
}
