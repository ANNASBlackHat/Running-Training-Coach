package com.runningcompanion.app.domain

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.domain.math.GeoUtils
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.GpsPoint
import org.junit.Test

class MathAndPaceTest {

    @Test
    fun testHaversineDistance_MonasToBundaranHI() {
        // Monas (Jakarta) approx -6.1754, 106.8272
        // Bundaran HI approx -6.1925, 106.8236
        // Distance is ~1.9 km to 2.0 km
        val dist = GeoUtils.haversineDistanceMeters(
            -6.1754, 106.8272,
            -6.1925, 106.8236
        )
        assertThat(dist).isGreaterThan(1800.0)
        assertThat(dist).isLessThan(2100.0)
    }

    @Test
    fun testGpsPointAcceptance_rejectsInaccuratePoints() {
        val goodPoint = GpsPoint(
            timestamp = 1000L,
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 10.0f
        )
        assertThat(GeoUtils.isPointAcceptable(goodPoint, null)).isTrue()

        val inaccuratePoint = GpsPoint(
            timestamp = 2000L,
            latitude = 0.0001,
            longitude = 0.0001,
            accuracy = 35.0f // > 25m threshold
        )
        assertThat(GeoUtils.isPointAcceptable(inaccuratePoint, goodPoint)).isFalse()
    }

    @Test
    fun testGpsPointAcceptance_rejectsImpossibleSpeedJump() {
        val p1 = GpsPoint(timestamp = 0L, latitude = 0.0, longitude = 0.0, accuracy = 5.0f)
        // 1 second later, 100 meters away => 100 m/s (far exceeds 10 m/s threshold)
        val p2 = GpsPoint(timestamp = 1000L, latitude = 0.0009, longitude = 0.0, accuracy = 5.0f)
        assertThat(GeoUtils.isPointAcceptable(p2, p1)).isFalse()
    }

    @Test
    fun testPaceCalculator_computesCorrectPace() {
        val paceCalc = PaceCalculator(windowDurationMs = 15_000L)
        val t0 = 100_000L

        // Run at 5:00 min/km = 300 sec/km = 3.33 m/s
        // In 12 seconds, distance should be 40 meters
        val p1 = GpsPoint(timestamp = t0, latitude = 0.0, longitude = 0.0, accuracy = 5f)
        // Move ~40m north in 12s (approx 0.00036 deg lat)
        val p2 = GpsPoint(timestamp = t0 + 12_000L, latitude = 0.00036, longitude = 0.0, accuracy = 5f)

        paceCalc.addPoint(p1)
        paceCalc.addPoint(p2)

        val pace = paceCalc.calculateLivePaceSecPerKm(t0 + 12_000L)
        assertThat(pace).isNotNull()
        // 12s for ~40m = 300 s/km (approx 5:00 min/km)
        assertThat(pace!!).isGreaterThan(280.0)
        assertThat(pace).isLessThan(320.0)

        assertThat(PaceCalculator.formatPace(300.0)).isEqualTo("5:00")
        assertThat(PaceCalculator.formatTime(245)).isEqualTo("04:05")
        assertThat(PaceCalculator.formatTime(3665)).isEqualTo("1:01:05")
    }

    @Test
    fun testPaceCalculator_withDopplerSpeed() {
        val paceCalc = PaceCalculator()
        val t0 = 100_000L

        // Points with hardware Doppler speed = 3.70 m/s (~4:30 min/km = 270 s/km)
        val p1 = GpsPoint(timestamp = t0, latitude = 0.0, longitude = 0.0, accuracy = 5f, speed = 3.70f)
        val p2 = GpsPoint(timestamp = t0 + 2000L, latitude = 0.00007, longitude = 0.0, accuracy = 5f, speed = 3.70f)

        paceCalc.addPoint(p1)
        paceCalc.addPoint(p2)

        val pace = paceCalc.calculateLivePaceSecPerKm(t0 + 2000L)
        assertThat(pace).isNotNull()
        assertThat(pace!!).isGreaterThan(265.0)
        assertThat(pace).isLessThan(275.0)

        // Speech tests
        assertThat(PaceCalculator.formatPaceSpeech(300.0)).isEqualTo("5")
        assertThat(PaceCalculator.formatPaceSpeech(270.0)).isEqualTo("4 30")
        assertThat(PaceCalculator.formatPaceSpeech(255.0)).isEqualTo("4 15")
        assertThat(PaceCalculator.formatPaceSpeech(305.0)).isEqualTo("5 oh 5")
    }
}
