package com.runningcompanion.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.runningcompanion.app.domain.model.SegmentType

object AppColors {
    val Ink = Color(0xFF101B3B)
    val InkMuted = Color(0xFF4B587A)
    val Run = Color(0xFF2340E6)
    val Rest = Color(0xFFCFE6DB)
    val Base = Color(0xFF55657A)
    val Paper = Color(0xFFF4F6F9)
    val Line = Color(0xFFD5DAE2)
    val Alert = Color(0xFFFFB200)
    val White = Color(0xFFFFFFFF)

    fun forSegment(type: SegmentType?): SegmentColorPair {
        return when (type) {
            SegmentType.RUN -> SegmentColorPair(bg = Run, fg = White)
            SegmentType.REST -> SegmentColorPair(bg = Rest, fg = Ink)
            SegmentType.WARMUP, SegmentType.COOLDOWN, null -> SegmentColorPair(bg = Base, fg = White)
        }
    }
}

data class SegmentColorPair(
    val bg: Color,
    val fg: Color
)
