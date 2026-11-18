package com.qalculator.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** rgba(r,g,b,a) like in CSS */
fun rgba(r: Int, g: Int, b: Int, a: Float): Color = Color(r / 255f, g / 255f, b / 255f, a)

/** All colors of the original dark theme (the :root variables + the later overrides). */
object Q {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF121212)
    val Field = Color(0xFF101010)
    val Field2 = Color(0xFF0C0C0C)
    val Ink = Color(0xFFF3F6FB)
    val Muted = Color(0xFF8E99AE)
    val Line = rgba(255, 255, 255, 0.12f)
    val LineStrong = rgba(255, 255, 255, 0.24f)
    val Accent = Color(0xFFDDE3F0)
    val AccentSoft = Color(0xFF1F1F1F)
    val AccentInk = Color(0xFF0A0D13)
    val Good = Color(0xFF00FF00)
    val Bad = Color(0xFFFF9C93)
    val BadBg = Color(0xFF2A0F0E)

    /** .primary button and the "=" key */
    val Primary = Color(0xFFCFD3DC)

    /** selected tab / selected segment */
    val Selected = Color(0xFF262626)

    // glass (dialog + menu)
    val GlassSel = rgba(255, 255, 255, 0.14f)
    val GlassBorder = rgba(255, 255, 255, 0.18f)
    val Scrim = rgba(0, 0, 0, 0.35f)

    // top inner highlights (inset 0 1px 0 ...)
    val HlPanel = rgba(255, 255, 255, 0.10f)
    val HlField = rgba(255, 255, 255, 0.06f)
    val HlSelected = rgba(255, 255, 255, 0.18f)
    val HlGlass = rgba(255, 255, 255, 0.22f)
}

/** true on short screens: the original @media (max-height: 640px) rules */
val LocalCompact = compositionLocalOf { false }

/** the height of the app area (CSS 100vh): font sizes like clamp(.., 8vh, ..) use it */
val LocalViewportH = compositionLocalOf<Dp> { 800.dp }

/**
 * Text style. [rem] is the CSS size in rem (1rem = 16sp), weight = CSS font-weight,
 * lh = CSS line-height (unitless, default 1.4 like the body).
 */
fun qStyle(
    rem: Float,
    weight: Int = 400,
    color: Color = Q.Ink,
    lh: Float = 1.4f,
    tnum: Boolean = false,
    align: TextAlign = TextAlign.Unspecified
): TextStyle = qStylePx(rem * 16f, weight, color, lh, tnum, align)

/** same as qStyle but the size is given directly in sp (CSS px) */
fun qStylePx(
    size: Float,
    weight: Int = 400,
    color: Color = Q.Ink,
    lh: Float = 1.4f,
    tnum: Boolean = false,
    align: TextAlign = TextAlign.Unspecified
): TextStyle = TextStyle(
    color = color,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    fontFamily = FontFamily.Default,
    fontFeatureSettings = if (tnum) "tnum" else null,
    textAlign = align,
    lineHeight = (size * lh).sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)
