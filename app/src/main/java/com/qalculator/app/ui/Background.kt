package com.qalculator.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/** one CSS radial-gradient(RX RY at CX CY, inner, outer STOP) */
private fun DrawScope.glow(
    cx: Float, cy: Float, rx: Float, ry: Float,
    inner: Color, outer: Color, stop: Float
) {
    val c = Offset(size.width * cx, size.height * cy)
    val radiusX = size.width * rx
    val radiusY = size.height * ry
    withTransform({ scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = c) }) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to inner,
                stop to outer,
                center = c,
                radius = radiusX
            ),
            radius = radiusX,
            center = c
        )
    }
}

/**
 * The page background of the original (body background):
 * black, three soft light patches, and a black fade at the top and bottom.
 */
fun Modifier.pageBackground(): Modifier = this.drawBehind {
    drawRect(Color.Black)
    // radial-gradient(60% 22% at 55% 50%, rgba(255,255,255,.025), rgba(255,255,255,0) 70%)
    glow(0.55f, 0.50f, 0.60f, 0.22f, rgba(255, 255, 255, 0.025f), rgba(255, 255, 255, 0f), 0.70f)
    // radial-gradient(80% 32% at 90% 64%, rgba(150,160,195,.09), rgba(130,140,170,0) 74%)
    glow(0.90f, 0.64f, 0.80f, 0.32f, rgba(150, 160, 195, 0.09f), rgba(130, 140, 170, 0f), 0.74f)
    // radial-gradient(90% 34% at 15% 36%, rgba(175,185,210,.12), rgba(160,170,195,0) 72%)
    glow(0.15f, 0.36f, 0.90f, 0.34f, rgba(175, 185, 210, 0.12f), rgba(160, 170, 195, 0f), 0.72f)
    // linear-gradient(180deg, #000 0%, rgba(0,0,0,0) 15%, rgba(0,0,0,0) 85%, #000 100%)
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Black,
            0.15f to Color(0f, 0f, 0f, 0f),
            0.85f to Color(0f, 0f, 0f, 0f),
            1f to Color.Black,
            startY = 0f,
            endY = size.height
        )
    )
}
