package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.SessionTotals
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.share.GpxExporter
import org.junit.Test

class GpxExporterTest {

    private fun buildSession(name: String, pointCount: Int, withAltitude: Boolean = true): Session {
        val workout = Workout(
            id = "w1",
            name = name,
            isTemplate = false,
            items = listOf(
                com.runningcompanion.app.domain.model.WorkoutItem.Single(
                    Segment("s1", SegmentType.WARMUP, SegmentLength.Time(600))
                )
            )
        )
        val track = (1..pointCount).map { i ->
            GpsPoint(
                timestamp = 1_700_000_000_000L + i * 1000L,
                latitude = 52.0 + i * 0.001,
                longitude = 13.0 + i * 0.001,
                accuracy = 1f,
                altitude = if (withAltitude) 50.0 + i else null
            )
        }
        return Session(
            id = "sess1",
            workoutId = "w1",
            workoutSnapshot = workout,
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_600_000L,
            results = emptyList(),
            totals = SessionTotals(600L, 3000.0, 300.0),
            track = track
        )
    }

    @Test
    fun exportContainsStructureAndAllPoints() {
        val gpx = GpxExporter.export(buildSession("Hill Run", 5))
        assertThat(gpx).startsWith("<?xml version=\"1.0\"")
        assertThat(gpx).contains("<gpx version=\"1.1\" creator=\"Running Companion\"")
        assertThat(gpx).contains("<trk>")
        assertThat(Regex("<trkpt ").findAll(gpx).count()).isEqualTo(5)
        assertThat(gpx).contains("52.001")
        assertThat(gpx).contains("<ele>51.0</ele>")
        assertThat(gpx).contains("</gpx>")
    }

    @Test
    fun exportWritesStartAndFinishWaypoints() {
        val gpx = GpxExporter.export(buildSession("Hill Run", 5))
        assertThat(gpx).contains("Hill Run (start)")
        assertThat(gpx).contains("Hill Run (finish)")
    }

    @Test
    fun singlePointTrackHasNoFinishWaypoint() {
        val gpx = GpxExporter.export(buildSession("Solo", 1))
        assertThat(gpx).contains("Solo (start)")
        assertThat(gpx).doesNotContain("(finish)")
    }

    @Test
    fun exportEscapesXmlSpecialCharacters() {
        val gpx = GpxExporter.export(buildSession("Hills & <valleys> \"track\"", 2))
        assertThat(gpx).contains("Hills &amp; &lt;valleys&gt; &quot;track&quot; (start)")
        assertThat(gpx).doesNotContain("<valleys>")
    }

    @Test
    fun exportTimesAreIso8601Utc() {
        val gpx = GpxExporter.export(buildSession("Hill Run", 2))
        // 1_700_000_000_000 ms = 2023-11-14T22:13:20Z
        assertThat(gpx).contains("2023-11-14T22:13:20Z")
        assertThat(gpx).contains("2023-11-14T22:13:21Z")
    }

    @Test
    fun exportOmitsElevationWhenNull() {
        val gpx = GpxExporter.export(buildSession("Flat", 2, withAltitude = false))
        assertThat(gpx).doesNotContain("<ele>")
    }
}
