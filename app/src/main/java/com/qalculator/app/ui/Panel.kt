package com.bazarhisab.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp

/** box-shadow: inset 0 1px 0 color  (thin light line along the top edge, inside the border) */
fun DrawScope.drawTopHighlight(radius: Float, inset: Float, color: Color) {
    val dy = density
    val w = size.width
    val h = size.height
    val r = if (radius - inset > 0f) radius - inset else 0f
    val outer = Path().apply {
        addRoundRect(RoundRect(inset, inset, w - inset, h - inset, CornerRadius(r)))
    }
    val inner = Path().apply {
        addRoundRect(RoundRect(inset, inset + dy, w - inset, h - inset + dy, CornerRadius(r)))
    }
    clipPath(outer) {
        clipPath(inner, clipOp = ClipOp.Difference) {
            drawRect(color)
        }
    }
}

/**
 * A box like the original cards: background, 1px border (inside), rounded corners and the optional
 * top highlight. [border] = null means no border.
 */
fun Modifier.panel(
    bg: Color,
    radius: Dp,
    border: Color? = Q.Line,
    highlight: Color? = null
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val bw = if (border != null) density else 0f
    drawRoundRect(color = bg, cornerRadius = CornerRadius(r))
    if (border != null) {
        drawRoundRect(
            color = border,
            topLeft = Offset(bw / 2f, bw / 2f),
            size = Size(size.width - bw, size.height - bw),
            cornerRadius = CornerRadius(if (r - bw / 2f > 0f) r - bw / 2f else 0f),
            style = Stroke(width = bw)
        )
    }
    if (highlight != null) drawTopHighlight(r, bw, highlight)
}
