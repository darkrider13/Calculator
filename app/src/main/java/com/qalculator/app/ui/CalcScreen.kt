package com.qalculator.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalculator.app.logic.CALC_KEYS
import com.qalculator.app.logic.KeyDef
import com.qalculator.app.logic.Seg
import com.qalculator.app.logic.fitOutRem
import kotlinx.coroutines.delay

/*
 * The calculator tab: the display (.screen), the 4x5 keypad (.keys).
 * Sizes are the CSS ones: px = dp, font sizes in sp, rem = 16.
 */

/** --cl: the height of one row of the expression line */
private const val ROW_DP = 36f

@Composable
fun CalcScreen(ctl: CalcController, onConvert: () -> Unit = {}, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    // Escape / back: the verify sheet closes first, then the history sheet (not while a popup is open)
    BackHandler(enabled = ctl.verify != null && !overlays.anyOpen) { ctl.vBack() }
    BackHandler(enabled = ctl.sheetOpen && ctl.verify == null && !overlays.anyOpen) { ctl.closeSheet() }
    Box(modifier = modifier) {
        // while a sheet is open the screen and keypad are hidden (visibility: hidden)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (ctl.sheetOpen) 0f else 1f }
        ) {
            CalcMain(ctl, onConvert, Modifier.fillMaxSize())
        }
        if (ctl.sheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (ctl.verify != null) 0f else 1f }
            ) {
                CalcHistorySheet(ctl, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
        if (ctl.verify != null) VerifySheet(ctl, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

@Composable
private fun CalcMain(ctl: CalcController, onConvert: () -> Unit, modifier: Modifier) {
    val vh = LocalViewportH.current.value
    BoxWithConstraints(modifier = modifier) {
        // NEW DESIGN: round keys, no display card. Measurements come from the reference picture (px / 2 = dp).
        val sideMargin = 15.dp
        val gapX = 11.dp
        val gapY = 8.dp
        val iconRowH = 56.dp
        val belowIcons = 12.dp + 1.dp + 31.dp // space + divider + space
        val bottomMargin = 15.dp
        val minDisplay = 96.dp
        val byWidth = (maxWidth - sideMargin * 2 - gapX * 3) / 4
        val keysMax = maxHeight - minDisplay - iconRowH - belowIcons - bottomMargin
        val byHeight = (keysMax - gapY * 4) / 5
        val d = Math.max(36f, Math.min(byWidth.value, byHeight.value)).dp
        val keyFont = Math.min(Math.max(d.value * 0.44f, 20f), 34f)
        // the same keys when the scientific pad is open: 5 columns x 7 rows
        val byWidth5 = (maxWidth - sideMargin * 2 - gapX * 4) / 5
        val byHeight7 = (keysMax - gapY * 6) / 7
        val d5 = Math.max(30f, Math.min(byWidth5.value, byHeight7.value)).dp

        Column(modifier = Modifier.fillMaxSize()) {
            CalcDisplay(ctl, Modifier.weight(1f), vh)
            IconRow(ctl, onConvert)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 19.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Q.Divider)
            )
            Spacer(Modifier.height(31.dp))
            CalcKeyPad(ctl, gapX, gapY, d, d5, keyFont, maxWidth)
            Spacer(Modifier.height(bottomMargin))
        }
    }
}

// ---------------------------------------------------------------- display

@Composable
private fun CalcDisplay(ctl: CalcController, modifier: Modifier, vh: Float) {
    // no card / border any more: the numbers sit directly on the black background
    DisplayColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 33.dp, top = 8.dp, bottom = 8.dp),
        expr = { ExprLine(ctl) },
        out = { OutText(ctl, vh) }
    )
}

/** the small icon row under the display: history, ruler (convert, later), scientific (later), backspace */
@Composable
private fun IconRow(ctl: CalcController, onConvert: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // history (same function as before)
        PressBox(
            onClick = { ctl.openSheet() },
            pressedScale = 0.92f,
            modifier = Modifier.size(48.dp)
        ) {
            SvgIcon(Icons.Clock, Q.IconGray, Modifier.size(24.dp))
        }
        Spacer(Modifier.width(9.dp))
        // ruler: the convert button, opens the convert options page
        PressBox(
            onClick = onConvert,
            pressedScale = 0.92f,
            modifier = Modifier.size(48.dp)
        ) {
            SvgIcon(Icons.Ruler, Q.IconGray, Modifier.size(24.dp))
        }
        Spacer(Modifier.width(9.dp))
        // scientific icon: shows / hides the scientific keys (the pad grows, no landscape)
        PressBox(
            onClick = { ctl.toggleSci() },
            pressedScale = 0.92f,
            modifier = Modifier.size(48.dp)
        ) {
            SvgIcon(Icons.SciCalc, if (ctl.sciOpen) Q.KeyGreen else Q.IconGray, Modifier.size(24.dp))
        }
        Spacer(Modifier.weight(1f))
        // backspace (same function as before)
        PressBox(
            onClick = { ctl.onKey("⌫") },
            pressedScale = 0.92f,
            modifier = Modifier.size(48.dp)
        ) {
            SvgIcon(Icons.Backspace, Q.BackGreen, Modifier.size(24.dp))
        }
    }
}

/**
 * .screen is a column that pushes everything to the bottom: the result keeps its size, the expression line
 * takes what is left (36..108), and anything that does not fit is cut at the top.
 */
@Composable
private fun DisplayColumn(
    modifier: Modifier,
    expr: @Composable () -> Unit,
    out: @Composable () -> Unit
) {
    Layout(content = {
        expr()
        out()
    }, modifier = modifier) { measurables, c ->
        val w = c.maxWidth
        val h = c.maxHeight
        val minE = ROW_DP.dp.roundToPx()
        val maxE = (ROW_DP * 3f).dp.roundToPx()
        val o = measurables[1].measure(
            Constraints(minWidth = w, maxWidth = w, minHeight = 0, maxHeight = Constraints.Infinity)
        )
        val avail = h - o.height
        val eMax = Math.max(minE, Math.min(maxE, avail))
        val e = measurables[0].measure(
            Constraints(minWidth = w, maxWidth = w, minHeight = minE, maxHeight = eMax)
        )
        layout(w, h) {
            val total = e.height + o.height
            e.placeRelative(0, h - total)
            o.placeRelative(0, h - o.height)
        }
    }
}

private class NumRange(val k: Int, val start: Int, val end: Int)

private class BuiltExpr(val text: AnnotatedString, val ranges: List<NumRange>, val edOffset: Int)

private fun buildExpr(top: List<Seg>, pressedK: Int): BuiltExpr {
    val ranges = ArrayList<NumRange>()
    var edOffset = -1
    val text = buildAnnotatedString {
        for (seg in top) {
            when (seg) {
                is Seg.Plain -> append(seg.text)
                is Seg.Num -> {
                    val s = length
                    if (seg.k == pressedK) {
                        withStyle(SpanStyle(color = Q.Ink, background = Q.AccentSoft)) { append(seg.text) }
                    } else {
                        append(seg.text)
                    }
                    ranges.add(NumRange(seg.k, s, length))
                }
                is Seg.Edit -> {
                    edOffset = length
                    appendInlineContent("ed", "▯")
                }
            }
        }
    }
    return BuiltExpr(text, ranges, edOffset)
}

/** .c-expr: the expression, 3 rows high, numbers can be tapped to edit them */
@Composable
private fun ExprLine(ctl: CalcController) {
    val st = ctl.state
    val view = st.view
    val density = LocalDensity.current
    var pressedK by remember { mutableIntStateOf(-1) }
    val built = remember(view.top, pressedK) { buildExpr(view.top, pressedK) }
    val builtNow by rememberUpdatedState(built)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportH by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    val progT = remember { longArrayOf(0L) }
    val rowPx = with(density) { ROW_DP.dp.toPx() }
    val rowSp = with(density) { ROW_DP.dp.toSp() }
    val style = remember(rowSp) {
        qStylePx(22.4f, 400, Q.ExprGray, tnum = true, align = TextAlign.End).copy(lineHeight = rowSp)
    }

    // the edit box (inline in the text)
    val editFont = 22.4f * 1.1f
    val inline: Map<String, InlineTextContent> = if (st.ed >= 0) {
        val w = (Math.max(2, st.edText.length) * 0.75f + 1.4f) * editFont
        val hh = 1.6f * editFont
        mapOf(
            "ed" to InlineTextContent(
                Placeholder(w.sp, hh.sp, PlaceholderVerticalAlign.Center)
            ) { EditBox(ctl, st.edText, st.edPos, editFont) }
        )
    } else emptyMap()

    // after each render: show the end of the expression (or the number being edited)
    val edOff = built.edOffset
    LaunchedEffect(view.seq) {
        withFrameNanos { }
        withFrameNanos { }
        progT[0] = System.currentTimeMillis()
        val l = layout
        if (ctl.state.ed >= 0 && edOff >= 0 && l != null && edOff < l.layoutInput.text.length) {
            val b = l.getBoundingBox(edOff)
            val top = b.top.toInt()
            val bottom = Math.ceil(b.bottom.toDouble()).toInt()
            if (top < scroll.value) scroll.scrollTo(top)
            else if (bottom > scroll.value + viewportH) scroll.scrollTo(bottom - viewportH)
        } else {
            val max = scroll.maxValue
            val tgt = Math.ceil((max / rowPx - 0.01f).toDouble()) * rowPx
            scroll.scrollTo(Math.min(tgt.toInt(), max))
        }
    }

    // the expression shows 3 rows; the rest scrolls one whole row at a time
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }.collect { moving ->
            if (!moving) {
                if (System.currentTimeMillis() - progT[0] < 200) return@collect
                delay(90)
                if (scroll.isScrollInProgress) return@collect
                val t = Math.floor(scroll.value / rowPx + 0.5).toFloat() * rowPx
                val target = Math.min(Math.max(t.toInt(), 0), scroll.maxValue)
                if (Math.abs(target - scroll.value) > 1) {
                    progT[0] = System.currentTimeMillis()
                    scroll.animateScrollTo(target)
                }
            }
        }
    }

    val padX = with(density) { 2.dp.toPx() }
    val padY = with(density) { 4.dp.toPx() }
    fun hit(pos: Offset): Int {
        val l = layout ?: return -1
        val len = l.layoutInput.text.length
        for (r in builtNow.ranges) {
            if (r.start >= len || r.end - 1 >= len) continue
            val a = l.getBoundingBox(r.start)
            val b = l.getBoundingBox(r.end - 1)
            val rect = Rect(
                Math.min(a.left, b.left) - padX,
                Math.min(a.top, b.top) - padY,
                Math.max(a.right, b.right) + padX,
                Math.max(a.bottom, b.bottom) + padY
            )
            if (rect.contains(pos)) return r.k
        }
        return -1
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { viewportH = it.height }
            .verticalScroll(scroll)
    ) {
        BasicText(
            text = built.text,
            style = style,
            inlineContent = inline,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { pos ->
                            val k = hit(pos)
                            if (k >= 0) {
                                pressedK = k
                                tryAwaitRelease()
                                pressedK = -1
                            }
                        },
                        onTap = { pos ->
                            val k = hit(pos)
                            if (k >= 0) ctl.onTapNum(k)
                        }
                    )
                }
        )
    }
}

/** .c-ed: the number being edited, with a caret (the keypad types into it) */
@Composable
private fun EditBox(ctl: CalcController, text: String, pos: Int, fontSp: Float) {
    var layoutRef by remember { mutableStateOf<TextLayoutResult?>(null) }
    var boxW by remember { mutableIntStateOf(0) }
    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(text, pos) {
        blink = true
        while (true) {
            delay(500)
            blink = !blink
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .panel(Q.AccentSoft, 6.dp, border = Q.Accent)
            .onSizeChanged { boxW = it.width }
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    val l = layoutRef
                    if (l != null) {
                        val left = (boxW - l.size.width) / 2f
                        ctl.onCaret(l.getOffsetForPosition(Offset(p.x - left, l.size.height / 2f)))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = qStylePx(fontSp, 700, Q.Ink, lh = 1.2f, tnum = true, align = TextAlign.Center),
            maxLines = 1,
            softWrap = false,
            onTextLayout = { layoutRef = it },
            modifier = Modifier.drawWithContent {
                drawContent()
                val l = layoutRef
                if (blink && l != null) {
                    val r = l.getCursorRect(Math.min(Math.max(pos, 0), text.length))
                    drawLine(
                        color = Q.Accent,
                        start = Offset(r.left, r.top),
                        end = Offset(r.left, r.bottom),
                        strokeWidth = density
                    )
                }
            }
        )
    }
}

// ---------------------------------------------------------------- result

/** .c-out: the big result; long-press copies the plain number */
@Composable
private fun OutText(ctl: CalcController, vh: Float) {
    val ctx = LocalContext.current
    val view = ctl.state.view
    val rem = fitOutRem(view.out)
    // clamp(2.6rem, 8vh, 3.6rem), or the fitOut size for long results
    val size = if (rem > 0f) rem * 16f else Math.min(Math.max(0.08f * vh, 41.6f), 57.6f)
    val ctlNow by rememberUpdatedState(ctl)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
    BasicText(
        text = view.out,
        style = qStylePx(size, 700, Q.Ink, lh = 1.2f, tnum = true, align = TextAlign.End),
        modifier = Modifier
            .weight(1f)
            .pointerInput(Unit) {
                val slop = 12.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val x0 = down.position
                    val r = withTimeoutOrNull(450L) {
                        var fin = false
                        while (!fin) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id }
                            if (c == null || !c.pressed) {
                                fin = true
                            } else {
                                val d = c.position - x0
                                if (Math.abs(d.x) + Math.abs(d.y) > slop) fin = true
                            }
                        }
                        true
                    }
                    if (r == null) {
                        val v = ctlNow.state.view.raw
                        if (v != "") {
                            if (copyText(ctx, v)) vibrate15(ctx)
                        }
                    }
                }
            }
    )
    // blinking green cursor after the result (hidden while a number is being edited)
    if (ctl.state.ed < 0) BlinkCursor(size * 0.8f)
    }
}

/** the green blinking line at the end of the result */
@Composable
private fun BlinkCursor(heightSp: Float) {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(530)
            on = !on
        }
    }
    Box(
        modifier = Modifier
            .padding(start = 2.dp)
            .width(2.dp)
            .height(heightSp.dp)
            .graphicsLayer { alpha = if (on) 1f else 0f }
            .background(Q.Cursor)
    )
}



internal fun copyText(ctx: Context, txt: String): Boolean = try {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("", txt))
    true
} catch (x: Throwable) {
    false
}

@Suppress("DEPRECATION")
internal fun vibrate15(ctx: Context) {
    try {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (v == null) return
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(15L)
        }
    } catch (x: Throwable) {
        // like navigator.vibrate: no vibration, no error
    }
}
