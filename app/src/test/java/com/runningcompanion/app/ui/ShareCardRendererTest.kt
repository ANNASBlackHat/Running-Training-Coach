package com.runningcompanion.app.ui

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.model.PaceRange
import com.runningcompanion.app.domain.model.Segment
import com.runningcompanion.app.domain.model.SegmentLength
import com.runningcompanion.app.domain.model.SegmentResult
import com.runningcompanion.app.domain.model.SegmentType
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.model.SessionTotals
import com.runningcompanion.app.domain.model.Workout
import com.runningcompanion.app.domain.model.WorkoutItem
import com.runningcompanion.app.domain.share.CardTemplate
import com.runningcompanion.app.domain.share.ShareCardModel
import com.runningcompanion.app.ui.share.ShareCardRenderer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareCardRendererTest {

    private fun buildSession(withTrack: Boolean, withBars: Boolean = true): Session {
        val workout = Workout(
            id = "w1",
            name = "Intervals",
            isTemplate = false,
            items = listOf(
                WorkoutItem.Single(Segment("s1", SegmentType.WARMUP, SegmentLength.Time(600))),
                WorkoutItem.Repeat(
                    count = 4,
                    segments = listOf(
                        Segment("s2", SegmentType.RUN, SegmentLength.Distance(400), PaceRange(240, 270)),
                        Segment("s3", SegmentType.REST, SegmentLength.Time(90))
                    )
                )
            )
        )
        val results = mutableListOf(
            SegmentResult(
                index = 0,
                type = SegmentType.WARMUP,
                setNumber = null,
                startedAt = 0L,
                endedAt = 600_000L,
                durationSec = 600L,
                distanceM = 3000.0,
                avgPaceSecPerKm = 330.0
            )
        )
        for (set in 1..4) {
            results.add(
                SegmentResult(
                    index = set * 2 - 1,
                    type = SegmentType.RUN,
                    setNumber = set,
                    startedAt = 0L,
                    endedAt = 300_000L,
                    durationSec = 300L,
                    distanceM = 400.0,
                    avgPaceSecPerKm = if (withBars) 250.0 + set * 10 else null
                )
            )
        }
        val track = if (withTrack) {
            (1..20).map { i ->
                GpsPoint(
                    timestamp = i * 1000L,
                    latitude = 52.0 + i * 0.001,
                    longitude = 13.0 + i * 0.001,
                    accuracy = 1f,
                    altitude = 50.0
                )
            }
        } else {
            emptyList()
        }
        return Session(
            id = "sess1",
            workoutId = "w1",
            workoutSnapshot = workout,
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_600_000L,
            results = results,
            totals = SessionTotals(2400L, 4600.0, 320.0),
            track = track
        )
    }

    @Test
    fun renderProducesExpectedBitmapDimensions() {
        val model = ShareCardModel.fromSession(buildSession(withTrack = true))
        val bitmap = ShareCardRenderer.render(model)
        assertThat(bitmap.width).isEqualTo(ShareCardRenderer.CARD_WIDTH)
        assertThat(bitmap.height).isGreaterThan(800)
    }

    @Test
    fun routeTemplateIsShorterAndLapsTemplateDropsTheTrack() {
        val model = ShareCardModel.fromSession(buildSession(withTrack = true))
        val full = ShareCardRenderer.render(model, CardTemplate.FULL)
        val route = ShareCardRenderer.render(model, CardTemplate.ROUTE)
        val laps = ShareCardRenderer.render(model, CardTemplate.LAPS)
        assertThat(route.height).isLessThan(full.height)
        assertThat(laps.height).isLessThan(full.height)
        // ROUTE has no intervals/laps section, LAPS has no track: route is the shortest
        assertThat(route.height).isLessThan(laps.height)
    }

    @Test
    fun allTemplatesRenderWithoutCrash() {
        val model = ShareCardModel.fromSession(buildSession(withTrack = false))
        for (template in CardTemplate.values()) {
            val bitmap = ShareCardRenderer.render(model, template)
            assertThat(bitmap.width).isEqualTo(ShareCardRenderer.CARD_WIDTH)
        }
    }

    @Test
    fun renderWithoutTrackDoesNotCrash() {
        val model = ShareCardModel.fromSession(buildSession(withTrack = false))
        val bitmap = ShareCardRenderer.render(model)
        assertThat(bitmap.width).isEqualTo(ShareCardRenderer.CARD_WIDTH)
    }

    @Test
    fun renderWithNoBarsOrLapsDoesNotCrash() {
        val empty = Session(
            id = "sess-empty",
            workoutId = "w1",
            workoutSnapshot = buildSession(withTrack = false).workoutSnapshot,
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_000_000_000L,
            results = emptyList(),
            totals = SessionTotals(0L, 0.0, null),
            track = emptyList()
        )
        val bitmap = ShareCardRenderer.render(ShareCardModel.fromSession(empty))
        assertThat(bitmap.height).isGreaterThan(500)
    }

    @Test
    fun renderHandlesManyLapsWithTruncation() {
        val many = buildSession(withTrack = false)
        val extraLaps = (0 until 30).map { i ->
            SegmentResult(
                index = i,
                type = if (i % 2 == 0) SegmentType.RUN else SegmentType.REST,
                setNumber = i / 2,
                startedAt = 0L,
                endedAt = 60_000L,
                durationSec = 60L,
                distanceM = 100.0,
                avgPaceSecPerKm = 300.0
            )
        }
        val model = ShareCardModel.fromSession(many.copy(results = extraLaps))
        assertThat(model.laps).hasSize(ShareCardModel.MAX_VISIBLE_LAPS)
        assertThat(model.lapTruncationCount).isEqualTo(30 - ShareCardModel.MAX_VISIBLE_LAPS)
        val bitmap = ShareCardRenderer.render(model)
        assertThat(bitmap.height).isGreaterThan(800)
    }
}
