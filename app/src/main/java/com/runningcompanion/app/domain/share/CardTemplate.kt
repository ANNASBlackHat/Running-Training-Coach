package com.runningcompanion.app.domain.share

/**
 * Layout templates for the share card. The renderer composes the same
 * sections in different combinations:
 *
 * - [FULL]: header, stats, route, intervals, laps
 * - [ROUTE]: header, stats, route (casual Strava-style share)
 * - [LAPS]: header, stats, intervals, laps (training-focused, no map)
 */
enum class CardTemplate(val label: String) {
    FULL("Full"),
    ROUTE("Route"),
    LAPS("Laps")
}
