package com.runningcompanion.app.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.runningcompanion.app.domain.math.PaceCalculator
import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.share.ShareCardModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Renders a [ShareCardModel] to a PNG-ready bitmap. Pure Android Canvas
 * drawing: no views, no network, deterministic output.
 */
object ShareCardRenderer {

    const val CARD_WIDTH = 1080

    private const val MARGIN = 54
    private val INK = 0xFF101B3B.toInt()
    private val INK_MUTED = 0xFF4B587A.toInt()
    private val RUN_BLUE = 0xFF2340E6.toInt()
    private val PAPER = 0xFFEDF0F5.toInt()
    private val LINE = 0xFFD5DAE2.toInt()
    private val AMBER = 0xFFFFB200.toInt()
    private val ROUTE_ORANGE = 0xFFFC4C02.toInt()
    private val START_GREEN = 0xFF2E7D32.toInt()
    private val FINISH_RED = 0xFFC62828.toInt()

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK
        textSize = 52f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 30f
    }
    private val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RUN_BLUE
        textSize = 30f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 22f
    }
    private val statValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK
        textSize = 46f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val statUnitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 24f
    }
    private val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 26f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = LINE
        strokeWidth = 2f
    }
    private val trackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PAPER
    }
    private val trackStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = LINE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ROUTE_ORANGE
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val markerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val markerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val emptyTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x332340E6
        style = Paint.Style.FILL
    }
    private val inRangeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RUN_BLUE
        style = Paint.Style.FILL
    }
    private val outOfRangeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AMBER
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 22f
        textAlign = Paint.Align.RIGHT
    }
    private val barLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val consistencyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK
        textSize = 28f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val lapLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK
        textSize = 26f
    }
    private val lapRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 26f
        textAlign = Paint.Align.RIGHT
    }
    private val lapTruncationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 22f
    }
    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK_MUTED
        textSize = 22f
    }

    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())

    private class Stat(val label: String, val value: String, val unit: String, val highlight: Boolean = false)

    fun render(model: ShareCardModel): Bitmap {
        val height = totalHeight(model)
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = MARGIN
        y = drawHeader(canvas, model, y)
        y = drawStatsRow(canvas, model, y)
        y = drawDivider(canvas, y)
        y = drawTrack(canvas, model, y)
        y = drawIntervals(canvas, model, y)
        y = drawLaps(canvas, model, y)
        drawFooter(canvas, model, y)
        return bitmap
    }

    // region section height calculations

    private fun totalHeight(model: ShareCardModel): Int {
        var h = MARGIN
        h += headerHeight(model)
        h += statsRowHeight()
        h += dividerHeight()
        h += trackHeight()
        h += intervalsHeight(model)
        h += lapsHeight(model)
        h += footerHeight()
        return h
    }

    private fun headerHeight(model: ShareCardModel): Int {
        var h = 72 + 40 + 28
        if (model.intervalTag != null) h += 48
        return h
    }

    private fun statsRowHeight(): Int = 30 + 8 + 64 + 36

    private fun dividerHeight(): Int = 2 + 30

    private fun trackHeight(): Int = 26 + 12 + 520 + 28

    private fun intervalsHeight(model: ShareCardModel): Int {
        if (model.bars.isEmpty()) return 0
        var h = 26 + 12 + 300 + 44 // title + chart + bar labels
        if (model.consistency != null) h += 44
        return h + 28
    }

    private fun lapsHeight(model: ShareCardModel): Int {
        if (model.laps.isEmpty()) return 0
        var h = 26 + 12 + model.laps.size * 44
        if (model.lapTruncationCount > 0) h += 40
        return h + 28
    }

    private fun footerHeight(): Int = dividerHeight() + 30 + MARGIN

    // endregion

    // region drawing

    private fun drawHeader(canvas: Canvas, model: ShareCardModel, y: Int): Int {
        var cy = y
        titlePaint.textSize = if (titlePaint.measureText(model.workoutName) > CARD_WIDTH - 2 * MARGIN) 40f else 52f
        canvas.drawText(model.workoutName, MARGIN.toFloat(), cy + titlePaint.textSize * 0.8f, titlePaint)
        cy += 72

        canvas.drawText(dateFormat.format(Date(model.startedAt)), MARGIN.toFloat(), cy + subPaint.textSize * 0.8f, subPaint)
        cy += 40

        if (model.intervalTag != null) {
            canvas.drawText(model.intervalTag, MARGIN.toFloat(), cy + tagPaint.textSize * 0.8f, tagPaint)
            cy += 48
        }
        return cy + 28
    }

    private fun drawStatsRow(canvas: Canvas, model: ShareCardModel, y: Int): Int {
        val metrics = buildList {
            add(Stat("DISTANCE", "%.2f".format(model.distanceM / 1000.0), "km"))
            add(Stat("TIME", PaceCalculator.formatTime(model.durationSec), ""))
            add(
                Stat(
                    "AVG PACE",
                    model.avgPaceSecPerKm?.let { PaceCalculator.formatPace(it) } ?: "--:--",
                    "/km"
                )
            )
            model.elevationGainM?.let { add(Stat("ELEV", "%.0f".format(it), "m")) }
            model.fastestSetPaceSecPerKm?.let {
                add(Stat("FASTEST SET", PaceCalculator.formatPace(it), "/km", highlight = true))
            }
        }
        val cellWidth = (CARD_WIDTH - 2 * MARGIN) / metrics.size
        val cy = y
        val labelY = cy + statLabelPaint.textSize * 0.8f
        val valueY = cy + 30 + 8 + statValuePaint.textSize * 0.75f

        for ((i, stat) in metrics.withIndex()) {
            val x = MARGIN + i * cellWidth
            canvas.drawText(stat.label, x.toFloat(), labelY, statLabelPaint)
            statValuePaint.color = if (stat.highlight) ROUTE_ORANGE else INK
            canvas.drawText(stat.value, x.toFloat(), valueY, statValuePaint)
            if (stat.unit.isNotEmpty()) {
                val valueWidth = statValuePaint.measureText(stat.value)
                canvas.drawText(stat.unit, (x + valueWidth).toFloat(), valueY, statUnitPaint)
            }
        }
        return cy + statsRowHeight()
    }

    private fun drawDivider(canvas: Canvas, y: Int): Int {
        canvas.drawLine(MARGIN.toFloat(), y.toFloat(), CARD_WIDTH - MARGIN.toFloat(), (y + 2f), linePaint)
        return y + dividerHeight()
    }

    private fun drawTrack(canvas: Canvas, model: ShareCardModel, y: Int): Int {
        var cy = y
        canvas.drawText("ROUTE", MARGIN.toFloat(), cy + sectionTitlePaint.textSize * 0.8f, sectionTitlePaint)
        cy += 26 + 12

        val rect = RectF(MARGIN.toFloat(), cy.toFloat(), (CARD_WIDTH - MARGIN).toFloat(), (cy + 520).toFloat())
        val radius = 16f
        canvas.drawRoundRect(rect, radius, radius, trackBgPaint)
        canvas.drawRoundRect(rect, radius, radius, trackStrokePaint)

        val pad = 48f
        val innerLeft = MARGIN + pad
        val innerTop = cy + pad
        val innerRight = CARD_WIDTH - MARGIN - pad
        val innerBottom = cy + 520 - pad

        val track = model.track
        if (model.hasTrack && track.size >= 2) {
            val projected = project(track, innerLeft, innerTop, innerRight, innerBottom)
            for (i in 1 until projected.size) {
                canvas.drawLine(
                    projected[i - 1][0], projected[i - 1][1],
                    projected[i][0], projected[i][1],
                    routePaint
                )
            }
            val start = projected.first()
            val finish = projected.last()
            markerFillPaint.color = START_GREEN
            canvas.drawCircle(start[0], start[1], 18f, markerFillPaint)
            canvas.drawCircle(start[0], start[1], 18f, markerRingPaint)
            markerFillPaint.color = FINISH_RED
            canvas.drawCircle(finish[0], finish[1], 18f, markerFillPaint)
            canvas.drawCircle(finish[0], finish[1], 18f, markerRingPaint)
        } else {
            canvas.drawText(
                "No GPS route recorded",
                (innerLeft + innerRight) / 2,
                (innerTop + innerBottom) / 2,
                emptyTrackPaint
            )
        }
        return cy + 520 + 28
    }

    /** Equirectangular projection of the track into the target box. */
    private fun project(
        points: List<GpsPoint>,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): List<FloatArray> {
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }

        val cosFactor = Math.cos(Math.toRadians((minLat + maxLat) / 2.0)).coerceAtLeast(0.0001)
        val rangeX = (maxLon - minLon) * cosFactor
        val rangeY = maxLat - minLat

        val boxW = (right - left).toDouble()
        val boxH = (bottom - top).toDouble()
        val scaleX = if (rangeX > 1e-9) boxW / rangeX else Double.MAX_VALUE
        val scaleY = if (rangeY > 1e-9) boxH / rangeY else Double.MAX_VALUE
        val scale = min(scaleX, scaleY)
        if (!scale.isFinite() || scale <= 0.0) {
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            return points.map { floatArrayOf(cx, cy) }
        }

        val offsetX = left + (boxW - rangeX * scale) / 2.0
        val offsetY = top + (boxH - rangeY * scale) / 2.0

        return points.map { p ->
            floatArrayOf(
                (offsetX + (p.longitude - minLon) * cosFactor * scale).toFloat(),
                (offsetY + (maxLat - p.latitude) * scale).toFloat()
            )
        }
    }

    private fun drawIntervals(canvas: Canvas, model: ShareCardModel, y: Int): Int {
        val bars = model.bars
        if (bars.isEmpty()) return y
        var cy = y
        canvas.drawText("INTERVALS", MARGIN.toFloat(), cy + sectionTitlePaint.textSize * 0.8f, sectionTitlePaint)
        cy += 26 + 12

        val chartTop = cy
        val chartH = 300
        val chartBottom = chartTop + chartH
        val leftPad = MARGIN + 110f
        val rightPad = CARD_WIDTH - MARGIN - 24f
        val groupW = (rightPad - leftPad) / bars.size
        val barW = min(84f, groupW * 0.62f)

        // y domain (pace: smaller = faster = top of chart)
        val candidates = buildList {
            addAll(bars.map { it.paceSecPerKm })
            bars.forEach {
                it.targetFastSecPerKm?.let { f -> add(f) }
                it.targetSlowSecPerKm?.let { s -> add(s) }
            }
        }
        val domainMin = candidates.min() * 0.94
        val domainMax = candidates.max() * 1.06
        val range = (domainMax - domainMin).coerceAtLeast(1.0)

        fun yFor(pace: Double): Float = (chartTop + ((pace - domainMin) / range * chartH)).toFloat()

        for (tick in listOf(domainMin, (domainMin + domainMax) / 2, domainMax)) {
            val ty = yFor(tick)
            axisPaint.color = LINE
            canvas.drawLine(leftPad, ty, rightPad, ty, axisPaint)
            axisPaint.color = INK_MUTED
            canvas.drawText(PaceCalculator.formatPace(tick), leftPad - 12f, ty + 8f, axisPaint)
        }

        for ((i, bar) in bars.withIndex()) {
            val x = leftPad + i * groupW + (groupW - barW) / 2f

            if (bar.targetFastSecPerKm != null && bar.targetSlowSecPerKm != null) {
                val bandTop = yFor(bar.targetFastSecPerKm)
                val bandBottom = yFor(bar.targetSlowSecPerKm)
                val bandRect = RectF(x - 6f, bandTop, x + barW + 6f, bandBottom)
                canvas.drawRoundRect(bandRect, 8f, 8f, bandPaint)
            }

            val barTop = yFor(bar.paceSecPerKm)
            val barRect = RectF(x, barTop, x + barW, chartBottom.toFloat())
            val paint = if (bar.isOutOfRange) outOfRangeBarPaint else inRangeBarPaint
            canvas.drawRoundRect(barRect, 8f, 8f, paint)
            canvas.drawText(bar.label, (x + barW / 2f).toFloat(), chartBottom + 34f, barLabelPaint)
        }

        cy = chartBottom + 44

        model.consistency?.let { c ->
            val text = "Fastest ${PaceCalculator.formatPace(c.fastestSecPerKm)} · " +
                "Slowest ${PaceCalculator.formatPace(c.slowestSecPerKm)} · CV ${"%.0f".format(c.cvPercent)}%"
            canvas.drawText(text, MARGIN.toFloat(), cy + consistencyPaint.textSize * 0.8f, consistencyPaint)
            cy += 44
        }
        return cy + 28
    }

    private fun drawLaps(canvas: Canvas, model: ShareCardModel, y: Int): Int {
        if (model.laps.isEmpty()) return y
        var cy = y
        canvas.drawText("LAPS", MARGIN.toFloat(), cy + sectionTitlePaint.textSize * 0.8f, sectionTitlePaint)
        cy += 26 + 12

        for (lap in model.laps) {
            val left = if (lap.setLabel != null) "${lap.typeLabel} ${lap.setLabel}" else lap.typeLabel
            val right = listOfNotNull(
                formatDistance(lap.distanceM),
                PaceCalculator.formatTime(lap.durationSec),
                lap.paceSecPerKm?.let { "${PaceCalculator.formatPace(it)}/km" }
            ).joinToString("  ·  ")

            canvas.drawText(left, MARGIN.toFloat(), cy + lapLeftPaint.textSize * 0.8f, lapLeftPaint)
            canvas.drawText(right, (CARD_WIDTH - MARGIN).toFloat(), cy + lapRightPaint.textSize * 0.8f, lapRightPaint)
            cy += 22
            canvas.drawLine(MARGIN.toFloat(), cy.toFloat(), (CARD_WIDTH - MARGIN).toFloat(), cy.toFloat(), linePaint)
            cy += 22
        }
        if (model.lapTruncationCount > 0) {
            canvas.drawText("+${model.lapTruncationCount} more", MARGIN.toFloat(), cy + lapTruncationPaint.textSize * 0.8f, lapTruncationPaint)
            cy += 40
        }
        return cy + 28
    }

    private fun drawFooter(canvas: Canvas, model: ShareCardModel, y: Int) {
        var cy = y
        canvas.drawLine(MARGIN.toFloat(), cy.toFloat(), (CARD_WIDTH - MARGIN).toFloat(), (cy + 2f).toFloat(), linePaint)
        cy += dividerHeight()
        canvas.drawText("Running Companion", MARGIN.toFloat(), cy + footerPaint.textSize * 0.8f, footerPaint)
    }

    // endregion

    private fun formatDistance(m: Double): String =
        if (m >= 1000.0) "%.1f km".format(m / 1000.0) else "${m.toInt()} m"
}
