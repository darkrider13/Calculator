package com.qalculator.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser

/** An icon from the original: the same SVG path data (24x24 view box), drawn as a stroke. */
class IconDef(val paths: List<String>, val stroke: Float)

object Icons {
    /** unit picker arrow */
    val Chevron = IconDef(listOf("M6 9l6 6 6-6"), 2.2f)

    /** selected row in the unit menu */
    val Check = IconDef(listOf("M5 12.5l4.5 4.5L19 7.5"), 2.6f)

    /** back arrow (history pages) */
    val Back = IconDef(listOf("M15 5l-7 7 7 7"), 2f)

    /** reset button */
    val Reset = IconDef(listOf("M20 11a8 8 0 1 0-2.3 5.7", "M20 4v7h-7"), 2f)

    /** update / sync: two arrows going round */
    val Sync = IconDef(
        listOf("M4.5 12a7.5 7.5 0 0 1 12.8-5.3", "M17.5 3.5v3.5h-3.5", "M19.5 12a7.5 7.5 0 0 1-12.8 5.3", "M6.5 20.5V17H10"),
        1.8f
    )

    /** delete (trash) */
    val Trash = IconDef(
        listOf("M3 6h18", "M8 6V4h8v2", "M19 6l-1 14H6L5 6", "M10 11v6", "M14 11v6"),
        2f
    )

    /** history clock: circle cx12 cy12 r9 + hands */
    val Clock = IconDef(listOf("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M12 7v5l3.5 2"), 2f)

    /** ruler (convert button place) */
    val Ruler = IconDef(
        listOf(
            "M4 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z",
            "M7.5 6v3 M11 6v2 M14.5 6v3 M18 6v2"
        ),
        1.7f
    )

    /** scientific calculator icon (design only) */
    val SciCalc = IconDef(
        listOf(
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            "M6 8l1.2 1.6 1.6-3.8h2.2",
            "M13.5 6.5h4 M14.8 6.5v3.2 M17 6.5v3.2",
            "M6.5 15.2h3a1.5 1.5 0 1 0-.4 1.8",
            "M13 14.2h5 M13 17h5"
        ),
        1.5f
    )

    /** shrink to a floating window: two arrows pointing inwards (the opposite of the full screen icon) */
    val FloatWin = IconDef(
        listOf("M4 14h6v6", "M10 14l-7 7", "M20 10h-6V4", "M14 10l7-7"),
        2f
    )

    /** backspace key */
    val Backspace = IconDef(
        listOf("M21 5H9l-6 7 6 7h12a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1z", "M16 9.5l-5 5M11 9.5l5 5"),
        2f
    )
}

private val pathCache = HashMap<String, Path>()

private fun pathOf(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** Draws the icon filling the size of [modifier] (the original uses square sizes). */
@Composable
fun SvgIcon(icon: IconDef, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val k = size.minDimension / 24f
        scale(k, Offset.Zero) {
            for (d in icon.paths) {
                drawPath(
                    path = pathOf(d),
                    color = color,
                    style = Stroke(width = icon.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
