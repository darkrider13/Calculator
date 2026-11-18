package com.qalculator.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.qalculator.app.logic.Seg
import kotlinx.coroutines.delay

/*
 * The floating calculator (a small window over other apps, shown by FloatService).
 * Same calculator as the app screen (same CalcController), a small display and the same keypad.
 */

/** window width and the height of the top bar (the place to drag the window) */
const val FLOAT_W_DP = 250
const val FLOAT_HEADER_DP = 36

/** the window height while the scientific keys open / close (taller than both sizes, so it needs no resize per frame) */
const val FLOAT_TALL_DP = 450

private val IcOpen = IconDef(listOf("M14 4h6v6", "M20 4l-7 7", "M10 20H4v-6", "M4 20l7-7"), 2f)
private val IcClose = IconDef(listOf("M6 6l12 12", "M18 6L6 18"), 2f)

@Composable
fun FloatCalc(ctl: CalcController, onOpenApp: () -> Unit, onClose: () -> Unit, onAnimating: (Boolean) -> Unit) {
    val dens = LocalDensity.current
    // sp == dp, like in the app
    val fixed = remember(dens.density) { Density(dens.density, 1f) }
    // the window itself must not change its size on every frame of the key animation (that is what made it
    // jump and stutter): it gets a fixed tall size while the keys move, and wraps the content again after
    LaunchedEffect(ctl.sciOpen) {
        onAnimating(true)
        delay(480)
        onAnimating(false)
    }
    CompositionLocalProvider(LocalDensity provides fixed) {
        val shape = RoundedCornerShape(22.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0xF2000000))
                .border(1.dp, Q.Divider, shape)
                .padding(bottom = 10.dp)
        ) {
            // top bar: grip (drag here), scientific keys, back to the app, close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FLOAT_HEADER_DP.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(Q.Divider, RoundedCornerShape(2.dp))
                    )
                }
                PressBox(onClick = { ctl.toggleSci() }, pressedScale = 0.9f, modifier = Modifier.size(32.dp)) {
                    SvgIcon(Icons.SciCalc, if (ctl.sciOpen) Q.KeyGreen else Q.IconGray, Modifier.size(18.dp))
                }
                PressBox(onClick = onOpenApp, pressedScale = 0.9f, modifier = Modifier.size(32.dp)) {
                    SvgIcon(IcOpen, Q.IconGray, Modifier.size(18.dp))
                }
                PressBox(onClick = onClose, pressedScale = 0.9f, modifier = Modifier.size(32.dp)) {
                    SvgIcon(IcClose, Q.KeyRed, Modifier.size(18.dp))
                }
            }
            FloatDisplay(ctl)
            Spacer(Modifier.height(6.dp))
            CalcKeyPad(ctl, 6.dp, 6.dp, 53.dp, 41.dp, 23f, FLOAT_W_DP.dp)
        }
    }
}

/** the expression (one small line) and the result */
@Composable
private fun FloatDisplay(ctl: CalcController) {
    val view = ctl.state.view
    val top = remember(view.top) {
        view.top.joinToString("") {
            when (it) {
                is Seg.Plain -> it.text
                is Seg.Num -> it.text
                is Seg.Edit -> ""
            }
        }
    }
    val shown = if (top.length > 26) "…" + top.takeLast(25) else top
    val out = view.out
    val size = if (out.length > 9) Math.max(18f, 34f * 9f / out.length) else 34f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        BasicText(
            text = shown,
            style = qStylePx(14f, 400, Q.ExprGray, lh = 1.3f, tnum = true, align = TextAlign.End),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        )
        BasicText(
            text = out,
            style = qStylePx(size, 700, Q.Ink, lh = 1.2f, tnum = true, align = TextAlign.End),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
