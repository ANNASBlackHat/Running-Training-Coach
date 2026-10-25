package com.runningcompanion.app.domain.model

import java.util.UUID

/**
 * Copies a workout into a fresh, user-owned workout: new workout id, new segment ids
 * and `isTemplate = false`. Used by "duplicate" and by editing a template, so that
 * seeded templates are never overwritten.
 */
fun Workout.duplicateAsCustom(newName: String? = null): Workout {
    val copiedItems = items.map { item ->
        when (item) {
            is WorkoutItem.Single -> WorkoutItem.Single(
                item.segment.copy(id = UUID.randomUUID().toString())
            )
            is WorkoutItem.Repeat -> WorkoutItem.Repeat(
                count = item.count,
                segments = item.segments.map { it.copy(id = UUID.randomUUID().toString()) }
            )
        }
    }

    return copy(
        id = UUID.randomUUID().toString(),
        name = newName ?: "$name (copy)",
        isTemplate = false,
        items = copiedItems
    )
}
