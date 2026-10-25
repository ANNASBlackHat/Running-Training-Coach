package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.time.relativeDayLabel
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Test

class RelativeDayTest {

    private val utc = ZoneId.of("UTC")
    private val locale = Locale.ENGLISH
    private val now = Instant.parse("2026-10-25T12:00:00Z").toEpochMilli()

    private fun label(isoInstant: String): String {
        return relativeDayLabel(
            startedAtMs = Instant.parse(isoInstant).toEpochMilli(),
            nowMs = now,
            zone = utc,
            locale = locale
        )
    }

    @Test
    fun testSameDayIsToday() {
        assertThat(label("2026-10-25T06:30:00Z")).isEqualTo("Today")
    }

    @Test
    fun testPreviousDayIsYesterdayEvenIfUnderTwentyFourHours() {
        assertThat(label("2026-10-24T23:55:00Z")).isEqualTo("Yesterday")
    }

    @Test
    fun testOlderDaysUseAbsoluteLabel() {
        assertThat(label("2026-10-23T08:00:00Z")).isEqualTo("Fri, 23 Oct")
        assertThat(label("2026-10-20T08:00:00Z")).isEqualTo("Tue, 20 Oct")
    }

    @Test
    fun testYearBoundaryStillReadsYesterday() {
        val newYear = Instant.parse("2026-01-01T09:00:00Z").toEpochMilli()
        val label = relativeDayLabel(
            startedAtMs = Instant.parse("2025-12-31T22:00:00Z").toEpochMilli(),
            nowMs = newYear,
            zone = utc,
            locale = locale
        )

        assertThat(label).isEqualTo("Yesterday")
    }

    @Test
    fun testZoneDecidesTheCalendarDay() {
        // 2026-10-24T23:30Z is still the 25th in Tokyo (+09:00).
        val tokyo = ZoneId.of("Asia/Tokyo")
        val startedAt = Instant.parse("2026-10-24T23:30:00Z").toEpochMilli()
        val nowInTokyo = Instant.parse("2026-10-25T03:00:00Z").toEpochMilli()

        assertThat(relativeDayLabel(startedAt, nowInTokyo, tokyo, locale)).isEqualTo("Today")
        assertThat(relativeDayLabel(startedAt, nowInTokyo, utc, locale)).isEqualTo("Yesterday")
    }
}
