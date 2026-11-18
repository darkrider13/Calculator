package com.qalculator.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.qalculator.app.logic.CALC_KEYS
import com.qalculator.app.logic.KeyDef

/*
 * The keypad. Closed: the normal 4x5 round keys. Open (scientific icon): the keys shrink and move,
 * a column (sqrt, x!, 1/x, pi, e) comes in on the left and two rows (2nd deg sin cos tan / xy lg ln ( ))
 * come in on top, like a 5x7 pad. Everything is one progress value p (0 = closed, 1 = open), read only
 * in the layout / draw phase, so the animation does not recompose the keys.
 */

/** a scientific key: label = base text + small raised text, code = what press() gets */
private class SciKey(val base: String, val sup: String, val code: String)

private fun k(base: String, sup: String, code: String) = SciKey(base, sup, code)

@Composable
fun CalcKeyPad(
    ctl: CalcController,
    gapX: Dp,
    gapY: Dp,
    d0: Dp,
    d5: Dp,
    fontSp: Float,
    gridW: Dp
) {
    val prog = remember { Animatable(0f) }
    var sciVisible by remember { mutableStateOf(false) }
    LaunchedEffect(ctl.sciOpen) {
        if (ctl.sciOpen) {
            sciVisible = true
            prog.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        } else {
            prog.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            sciVisible = false
        }
    }

    val second = ctl.second
    val rad = ctl.state.rad

    // sizes in dp (plain floats), as functions of p
    val d0f = d0.value
    val d5f = Math.min(d5.value, d0f)
    val gx = gapX.value
    val gy = gapY.value
    val gw = gridW.value
    fun dOf(p: Float) = d0f + (d5f - d0f) * p
    fun startOf(p: Float): Float {
        val dd = dOf(p)
        val w = (4f + p) * dd + (3f + p) * gx
        return (gw - w) / 2f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .layout { m, c ->
                val p = prog.value
                val h = ((5f + 2f * p) * dOf(p) + (4f + 2f * p) * gy).dp.roundToPx()
                val pl = m.measure(Constraints(c.maxWidth, c.maxWidth, h, h))
                layout(pl.width, h) { pl.place(0, 0) }
            }
    ) {
        // the normal 4x5 keys
        for (r in 0 until 5) {
            for (c in 0 until 4) {
                val def = CALC_KEYS[r * 4 + c]
                Placed(
                    prog = { prog.value },
                    d0 = d0f,
                    cx = { p -> startOf(p) + (c + p) * (dOf(p) + gx) + dOf(p) / 2f },
                    cy = { p -> (r + 2f * p) * (dOf(p) + gy) + dOf(p) / 2f },
                    scale = { p -> dOf(p) / d0f },
                    alphaOf = { 1f }
                ) {
                    CalcKeyRound(def, fontSp, Modifier.size(d0)) { ctl.onKey(def.k) }
                }
            }
        }

        if (sciVisible) {
            // top two rows: 2nd deg sin cos tan / xy lg ln ( )
            for (j in 0 until 2) {
                for (i in 0 until 5) {
                    val key = topKey(j, i, second)
                    val isDeg = j == 0 && i == 1
                    val on = j == 0 && i == 0 && second
                    Placed(
                        prog = { prog.value },
                        d0 = d0f,
                        cx = { p -> startOf(p) + i * (dOf(p) + gx) + dOf(p) / 2f },
                        cy = { p -> j * (dOf(p) + gy) + dOf(p) / 2f },
                        scale = { p -> dOf(p) / d0f * (0.6f + 0.4f * p) },
                        alphaOf = { it }
                    ) {
                        if (isDeg) {
                            SciKeyRound(SciKey(if (rad) "rad" else "deg", "", "deg"), fontSp, Modifier.size(d0), Q.KeyGreen, Q.KeyNum) { ctl.toggleAngle() }
                        } else {
                            SciKeyRound(key, fontSp, Modifier.size(d0), if (on) Color.White else Q.KeyText, if (on) Q.KeyEq else Q.KeyNum) {
                                if (key.code == "2nd") ctl.toggleSecond() else ctl.onKey(key.code)
                            }
                        }
                    }
                }
            }
            // left column: sqrt x! 1/x pi e, next to the 5 normal rows
            for (r in 0 until 5) {
                val key = leftKey(r, second)
                Placed(
                    prog = { prog.value },
                    d0 = d0f,
                    cx = { p -> startOf(p) + (-1f + p) * (dOf(p) + gx) + dOf(p) / 2f },
                    cy = { p -> (r + 2f * p) * (dOf(p) + gy) + dOf(p) / 2f },
                    scale = { p -> dOf(p) / d0f * (0.6f + 0.4f * p) },
                    alphaOf = { it }
                ) {
                    SciKeyRound(key, fontSp, Modifier.size(d0), Q.KeyText, Q.KeyNum) { ctl.onKey(key.code) }
                }
            }
        }
    }
}

private fun topKey(j: Int, i: Int, second: Boolean): SciKey = if (j == 0) {
    when (i) {
        0 -> k("2nd", "", "2nd")
        1 -> k("deg", "", "deg")
        2 -> if (second) k("sin", "−1", "asin") else k("sin", "", "sin")
        3 -> if (second) k("cos", "−1", "acos") else k("cos", "", "cos")
        else -> if (second) k("tan", "−1", "atan") else k("tan", "", "tan")
    }
} else {
    when (i) {
        0 -> k("x", "y", "^")
        1 -> if (second) k("10", "x", "10x") else k("lg", "", "lg")
        2 -> if (second) k("e", "x", "ex") else k("ln", "", "ln")
        3 -> k("(", "", "(")
        else -> k(")", "", ")")
    }
}

private fun leftKey(r: Int, second: Boolean): SciKey = when (r) {
    0 -> if (second) k("x", "2", "²") else k("√", "", "√")
    1 -> k("x!", "", "!")
    2 -> k("1/x", "", "inv")
    3 -> k("π", "", "π")
    else -> k("e", "", "e")
}

/**
 * Puts a key of fixed size d0 so that its centre is (cx, cy) dp from the top left of the pad, scaled
 * around its centre. The numbers come from lambdas of p so only layout / draw run while it animates.
 */
@Composable
private fun Placed(
    prog: () -> Float,
    d0: Float,
    cx: (Float) -> Float,
    cy: (Float) -> Float,
    scale: (Float) -> Float,
    alphaOf: (Float) -> Float,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset {
                val p = prog()
                IntOffset((cx(p) - d0 / 2f).dp.roundToPx(), (cy(p) - d0 / 2f).dp.roundToPx())
            }
            .graphicsLayer {
                val p = prog()
                val sc = scale(p)
                scaleX = sc
                scaleY = sc
                alpha = alphaOf(p)
            }
    ) { content() }
}

/** a normal round key (same look as before) */
@Composable
private fun CalcKeyRound(def: KeyDef, fontSp: Float, modifier: Modifier, onClick: () -> Unit) {
    val bg: Color
    val fg: Color
    when (def.cls) {
        "op" -> { bg = Q.KeyOp; fg = Q.KeyGreen }
        "clr" -> { bg = Q.KeyOp; fg = Q.KeyRed }
        "eq" -> { bg = Q.KeyEq; fg = Color.White }
        else -> { bg = Q.KeyNum; fg = Q.KeyText }
    }
    val size = if (def.label == "+/−") fontSp * 0.8f else fontSp
    PressBox(
        onClick = onClick,
        pressedScale = 0.94f,
        modifier = modifier.background(bg, CircleShape)
    ) {
        BasicText(
            text = def.label,
            style = qStylePx(size, 600, fg, lh = 1.2f, align = TextAlign.Center),
            maxLines = 1,
            softWrap = false
        )
    }
}

/** a scientific round key: smaller text, the raised part (y of x^y, -1 of sin^-1) drawn as a superscript */
@Composable
private fun SciKeyRound(key: SciKey, fontSp: Float, modifier: Modifier, fg: Color, bg: Color, onClick: () -> Unit) {
    val size = fontSp * (if (key.base.length + key.sup.length >= 4) 0.62f else 0.78f)
    val text = buildAnnotatedString {
        append(key.base)
        if (key.sup != "") {
            withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = (size * 0.68f).sp)) {
                append(key.sup)
            }
        }
    }
    PressBox(
        onClick = onClick,
        pressedScale = 0.94f,
        modifier = modifier.background(bg, CircleShape)
    ) {
        if (key.code == "2nd") {
            // alternative function key: the option (⌥) icon (green circle while the 2nd functions are on)
            SvgIcon(SWAP_ICON, fg, Modifier.size((fontSp * 0.95f).dp))
        } else {
            BasicText(
                text = text,
                style = qStylePx(size, 600, fg, lh = 1.2f, align = TextAlign.Center),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** the option sign (the ⌥ sign): switch the keys to their alternative functions (sin to sin-1, lg to 10^x ...) */
private val SWAP_ICON = IconDef(
    listOf("M3 6h5l8 12h5", "M14 6h7"),
    2.2f
)
