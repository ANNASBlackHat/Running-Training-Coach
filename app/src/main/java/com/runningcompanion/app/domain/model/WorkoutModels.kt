package com.runningcompanion.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SegmentType {
    WARMUP,
    RUN,
    REST,
    COOLDOWN
}

@Serializable
sealed interface SegmentLength {
    @Serializable
    data class Time(val seconds: Int) : SegmentLength

    @Serializable
    data class Distance(val meters: Int) : SegmentLength
}

@Serializable
data class PaceRange(
    val fastSecPerKm: Int,
    val slowSecPerKm: Int
) {
    init {
        require(fastSecPerKm <= slowSecPerKm) {
            "fastSecPerKm ($fastSecPerKm) must be <= slowSecPerKm ($slowSecPerKm)"
        }
    }

    fun isTooFast(paceSecPerKm: Double): Boolean = paceSecPerKm < fastSecPerKm
    fun isTooSlow(paceSecPerKm: Double): Boolean = paceSecPerKm > slowSecPerKm
    fun isInRange(paceSecPerKm: Double): Boolean = !isTooFast(paceSecPerKm) && !isTooSlow(paceSecPerKm)
}

@Serializable
data class Segment(
    val id: String,
    val type: SegmentType,
    val length: SegmentLength,
    val paceRange: PaceRange? = null
)

@Serializable
sealed interface WorkoutItem {
    @Serializable
    data class Single(val segment: Segment) : WorkoutItem

    @Serializable
    data class Repeat(val count: Int, val segments: List<Segment>) : WorkoutItem
}

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val isTemplate: Boolean = false,
    val items: List<WorkoutItem>
) {
    fun flatten(): List<RuntimeSegment> {
        val result = mutableListOf<RuntimeSegment>()
        var index = 0

        for (item in items) {
            when (item) {
                is WorkoutItem.Single -> {
                    result.add(
                        RuntimeSegment(
                            index = index++,
                            segment = item.segment,
                            setNumber = null,
                            setTotal = null
                        )
                    )
                }
                is WorkoutItem.Repeat -> {
                    for (set in 1..item.count) {
                        for (seg in item.segments) {
                            result.add(
                                RuntimeSegment(
                                    index = index++,
                                    segment = seg,
                                    setNumber = if (seg.type == SegmentType.RUN || seg.type == SegmentType.REST) set else null,
                                    setTotal = if (seg.type == SegmentType.RUN || seg.type == SegmentType.REST) item.count else null
                                )
                            )
                        }
                    }
                }
            }
        }
        return result
    }
}

@Serializable
data class RuntimeSegment(
    val index: Int,
    val segment: Segment,
    val setNumber: Int? = null,
    val setTotal: Int? = null
)
