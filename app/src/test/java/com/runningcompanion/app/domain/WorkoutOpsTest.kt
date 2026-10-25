package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.model.WorkoutItem
import com.runningcompanion.app.domain.model.WorkoutTemplates
import com.runningcompanion.app.domain.model.duplicateAsCustom
import org.junit.Test

class WorkoutOpsTest {

    @Test
    fun testDuplicateCreatesNewUserOwnedWorkout() {
        val template = WorkoutTemplates.norwegian4x4
        val copy = template.duplicateAsCustom()

        assertThat(copy.id).isNotEqualTo(template.id)
        assertThat(copy.isTemplate).isFalse()
        assertThat(copy.name).isEqualTo("Norwegian 4x4 (copy)")
        assertThat(copy.items).hasSize(template.items.size)
        assertThat(copy.flatten().map { it.segment.type })
            .isEqualTo(template.flatten().map { it.segment.type })
    }

    @Test
    fun testDuplicateAssignsFreshSegmentIdsAndKeepsTargets() {
        val template = WorkoutTemplates.repeats400m
        val copy = template.duplicateAsCustom()

        val originalIds = template.flatten().map { it.segment.id }.toSet()
        val copiedSegments = copy.flatten().map { it.segment }
        val copiedIds = copiedSegments.map { it.id }.toSet()

        // A repeat block shares one segment definition across its sets.
        assertThat(copiedIds).hasSize(originalIds.size)
        assertThat(copiedIds.intersect(originalIds)).isEmpty()
        assertThat(copiedSegments.none { it.id in originalIds }).isTrue()
        assertThat(copiedSegments.map { it.paceRange })
            .isEqualTo(template.flatten().map { it.segment.paceRange })
    }

    @Test
    fun testDuplicateKeepsRepeatStructure() {
        val copy = WorkoutTemplates.repeats400m.duplicateAsCustom()
        val repeat = copy.items.filterIsInstance<WorkoutItem.Repeat>().single()

        assertThat(repeat.count).isEqualTo(8)
        assertThat(repeat.segments).hasSize(2)
    }

    @Test
    fun testDuplicateAcceptsExplicitName() {
        val copy = WorkoutTemplates.tempoRun.duplicateAsCustom(newName = "Tuesday Tempo")

        assertThat(copy.name).isEqualTo("Tuesday Tempo")
        assertThat(copy.id).isNotEqualTo(WorkoutTemplates.tempoRun.id)
    }

    @Test
    fun testDuplicateLeavesTheOriginalUntouched() {
        val template = WorkoutTemplates.tempoSplits
        val originalIds = template.flatten().map { it.segment.id }
        val originalName = template.name

        template.duplicateAsCustom()

        assertThat(template.flatten().map { it.segment.id }).isEqualTo(originalIds)
        assertThat(template.name).isEqualTo(originalName)
        assertThat(template.isTemplate).isTrue()
    }
}
