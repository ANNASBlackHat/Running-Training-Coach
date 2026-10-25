package com.runningcompanion.app.ui.components

/**
 * Lays out the blocks of a session strip. Widths are proportional to [weights],
 * but no block is narrower than [minWidth] so repeated intervals stay visible.
 *
 * @param totalWidth full strip width in pixels, gaps included
 * @return one width per block; widths plus gaps sum to [totalWidth]
 */
internal fun computeStripWidths(
    weights: List<Float>,
    totalWidth: Float,
    gap: Float,
    minWidth: Float
): List<Float> {
    if (weights.isEmpty()) return emptyList()

    val count = weights.size
    val available = totalWidth - gap * (count - 1)
    if (available <= 0f) return List(count) { 0f }

    val safeWeights = weights.map { weight ->
        if (weight.isFinite() && weight > 0f) weight else 1f
    }

    // When even the minimum cannot fit, fall back to equal blocks.
    val floor = minWidth.coerceAtMost(available / count)
    val widths = FloatArray(count)
    val pinned = BooleanArray(count)

    for (iteration in 0 until count) {
        val unpinned = (0 until count).filter { !pinned[it] }
        if (unpinned.isEmpty()) break

        val weightSum = unpinned.sumOf { safeWeights[it].toDouble() }
        if (weightSum <= 0.0) break

        val pinnedWidth = floor * (count - unpinned.size)
        val freeSpace = available - pinnedWidth

        var newlyPinned = false
        unpinned.forEach { index ->
            val width = freeSpace * (safeWeights[index] / weightSum).toFloat()
            if (width < floor) {
                pinned[index] = true
                widths[index] = floor
                newlyPinned = true
            } else {
                widths[index] = width
            }
        }
        if (!newlyPinned) break
    }

    pinned.forEachIndexed { index, isPinned ->
        if (isPinned) widths[index] = floor
    }

    // Absorb float rounding into the widest block so the strip fills its width.
    val sum = widths.sum()
    if (sum > 0f && sum != available) {
        val widest = widths.indices.maxByOrNull { widths[it] } ?: 0
        widths[widest] = (widths[widest] + (available - sum)).coerceAtLeast(floor)
    }

    return widths.toList()
}
