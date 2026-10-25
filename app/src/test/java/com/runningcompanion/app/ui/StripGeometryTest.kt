package com.runningcompanion.app.ui

import com.google.common.truth.Truth.assertThat
import com.runningcompanion.app.ui.components.computeStripWidths
import org.junit.Test

class StripGeometryTest {

    private val gap = 2f
    private val minWidth = 4f

    @Test
    fun testWidthsAndGapsFillTheStrip() {
        val widths = computeStripWidths(
            weights = listOf(600f, 102f, 90f, 102f, 90f),
            totalWidth = 1000f,
            gap = gap,
            minWidth = minWidth
        )

        val total = widths.sum() + gap * (widths.size - 1)
        assertThat(total).isWithin(0.01f).of(1000f)
    }

    @Test
    fun testLongerSegmentsGetWiderBlocks() {
        val widths = computeStripWidths(
            weights = listOf(600f, 102f, 90f),
            totalWidth = 1000f,
            gap = gap,
            minWidth = minWidth
        )

        assertThat(widths[0]).isGreaterThan(widths[1])
        assertThat(widths[1]).isGreaterThan(widths[2])
    }

    @Test
    fun testShortSegmentsNeverCollapse() {
        // 36 intervals would otherwise shrink below a pixel each.
        val widths = computeStripWidths(
            weights = List(36) { 10f },
            totalWidth = 328f,
            gap = gap,
            minWidth = minWidth
        )

        assertThat(widths).hasSize(36)
        widths.forEach { assertThat(it).isAtLeast(minWidth) }
    }

    @Test
    fun testMixOfLongAndShortSegmentsKeepsMinimumForShortOnes() {
        val weights = listOf(600f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
        val widths = computeStripWidths(weights, totalWidth = 328f, gap = gap, minWidth = minWidth)

        weights.indices.drop(1).forEach { index ->
            assertThat(widths[index]).isAtLeast(minWidth)
        }
        assertThat(widths[0]).isGreaterThan(widths[1])
    }

    @Test
    fun testSingleSegmentFillsTheStrip() {
        val widths = computeStripWidths(listOf(240f), totalWidth = 328f, gap = gap, minWidth = minWidth)

        assertThat(widths).hasSize(1)
        assertThat(widths.first()).isWithin(0.01f).of(328f)
    }

    @Test
    fun testEqualWeightsProduceEqualWidths() {
        val widths = computeStripWidths(List(4) { 90f }, totalWidth = 318f, gap = gap, minWidth = minWidth)

        widths.forEach { assertThat(it).isWithin(0.01f).of(widths.first()) }
    }

    @Test
    fun testInvalidWeightsAreTreatedAsPositive() {
        val widths = computeStripWidths(
            weights = listOf(0f, Float.NaN, 100f, -5f),
            totalWidth = 400f,
            gap = gap,
            minWidth = minWidth
        )

        widths.forEach {
            assertThat(it.isFinite()).isTrue()
            assertThat(it).isAtLeast(minWidth)
        }
        assertThat(widths.sum() + gap * (widths.size - 1)).isWithin(0.01f).of(400f)
    }

    @Test
    fun testEmptyInputProducesNoBlocks() {
        assertThat(computeStripWidths(emptyList(), totalWidth = 300f, gap = gap, minWidth = minWidth))
            .isEmpty()
    }

    @Test
    fun testWidthTooNarrowForGapsDoesNotCrash() {
        val widths = computeStripWidths(List(100) { 10f }, totalWidth = 100f, gap = gap, minWidth = minWidth)

        assertThat(widths).hasSize(100)
        widths.forEach { assertThat(it).isEqualTo(0f) }
    }
}
