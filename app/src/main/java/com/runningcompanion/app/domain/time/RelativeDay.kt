package com.runningcompanion.app.domain.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ABSOLUTE_DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

/**
 * "Today", "Yesterday", or an absolute day label such as "Mon, 20 Oct".
 * All inputs are explicit so callers (and tests) control the clock and zone.
 */
fun relativeDayLabel(
    startedAtMs: Long,
    nowMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val started = Instant.ofEpochMilli(startedAtMs).atZone(zone).toLocalDate()
    val now = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

    return when {
        started == now -> "Today"
        started == now.minusDays(1) -> "Yesterday"
        else -> started.format(ABSOLUTE_DAY_FORMAT.withLocale(locale))
    }
}
