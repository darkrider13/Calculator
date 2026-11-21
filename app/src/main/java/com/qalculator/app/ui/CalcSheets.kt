package com.qalculator.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import com.qalculator.app.Updater
import com.qalculator.app.logic.CalcEntry
import com.qalculator.app.logic.VPiece
import com.qalculator.app.logic.VView
import com.qalculator.app.logic.canVerify
import com.qalculator.app.logic.dispExpr
import com.qalculator.app.logic.numE
import com.qalculator.app.logic.vInputRem
import com.qalculator.app.logic.vInputWidthEm
import com.qalculator.app.logic.whenLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/*
 * The two sheets that open over the calculator: the history list (#csheet) and the মিলান verify sheet (#vsheet).
 * Both are transparent, cover the whole calculator area and hide what is under them.
 */

/** a sheet takes every touch (nothing reaches the screen under it) */
private fun Modifier.blockTouches(): Modifier = this.pointerInput(Unit) { detectTapGestures { } }

/** .sheet-head: the back arrow, the title, and something on the right */
@Composable
private fun SheetHead(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressBox(onClick = onBack, modifier = Modifier.size(42.dp)) {
            SvgIcon(Icons.Back, Q.Ink, Modifier.size(20.dp))
        }
        BasicText(
            text = title,
            style = qStyle(1.05f, 700, Q.KeyGreen),
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

// ================================================================ history sheet

private val TrashRed = Color(0xFFFF0000)

@Composable
fun CalcHistorySheet(ctl: CalcController, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    val entries = ctl.entries
    val listState = rememberLazyListState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .blockTouches(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SheetHead("ক্যালকুলেটর হিস্টরি", onBack = { ctl.closeSheet() }) {
            UpdateButton(overlays)
            if (entries.isNotEmpty()) {
                PressBox(
                    onClick = {
                        overlays.askConfirm(
                            "সব হিসাব মুছবেন?",
                            "ক্যালকুলেটরের সব হিস্টরি মুছে যাবে। এটা আর ফেরত আনা যাবে না।",
                            "হ্যাঁ, মুছুন"
                        ) { ctl.clearEntries() }
                    },
                    modifier = Modifier.size(width = 36.dp, height = 32.dp)
                ) {
                    SvgIcon(Icons.Trash, TrashRed, Modifier.size(20.dp))
                }
            }
        }
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                BasicText(
                    text = "এখনো কোনো হিসাব নেই। ক্যালকুলেটরে = চাপলে হিসাবটা এখানে জমা হবে।",
                    style = qStylePx(15f, 400, Q.Muted),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                items(entries, key = { it.id }) { e ->
                    CalcItem(
                        e = e,
                        onOpen = { ctl.openVerifyFor(e.id) },
                        onUse = { ctl.useEntry(e.id) },
                        onDelete = {
                            overlays.askConfirm("হিসাব মুছবেন?", "এই হিসাবটি মুছে ফেলা হবে।", "মুছুন") {
                                ctl.deleteEntry(e.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

/** the update icon: looks at the latest GitHub release and offers to download it if it is newer than this app */
@Composable
private fun UpdateButton(overlays: Overlays) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    PressBox(
        onClick = {
            if (!busy) {
                busy = true
                Updater.check { info, failed ->
                    busy = false
                    val cur = Updater.currentBuild(ctx)
                    if (failed) {
                        overlays.askConfirm("আপডেট চেক করা যায়নি", "ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।", "ঠিক আছে") { }
                    } else if (info != null && info.build > cur) {
                        overlays.askConfirm("নতুন আপডেট আছে", "নতুন ভার্সন " + info.tag + " পাওয়া গেছে। ডাউনলোড করবেন?", "ডাউনলোড") {
                            Updater.download(ctx, info)
                        }
                    } else {
                        overlays.askConfirm("আপডেট", "আপনি সর্বশেষ ভার্সনেই আছেন।", "ঠিক আছে") { }
                    }
                }
            }
        },
        pressedScale = 0.92f,
        modifier = Modifier
            .size(width = 36.dp, height = 32.dp)
            .graphicsLayer { alpha = if (busy) 0.4f else 1f }
    ) {
        SvgIcon(Icons.Sync, Q.IconGray, Modifier.size(22.dp))
    }
}

/** .c-item: one saved calculation. Tapping it opens মিলান (when it has two or more numbers). */
@Composable
private fun CalcItem(e: CalcEntry, onOpen: () -> Unit, onUse: () -> Unit, onDelete: () -> Unit) {
    val verifiable = remember(e.expr) { canVerify(e.expr) }
    var pressed by remember { mutableStateOf(false) }
    val onOpenNow by rememberUpdatedState(onOpen)
    val lineColor = Q.Line
    val base = Modifier
        .fillMaxWidth()
        .heightIn(min = 72.dp)
        .drawBehind {
            if (pressed) drawRect(Q.AccentSoft)
            drawRect(
                color = lineColor,
                topLeft = Offset(0f, size.height - density),
                size = androidx.compose.ui.geometry.Size(size.width, density)
            )
        }
    val m = if (verifiable) {
        base.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { onOpenNow() }
            )
        }
    } else base
    Box(modifier = m.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val maxR = maxWidth * 0.55f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    BasicText(
                        text = dispExpr(e.expr),
                        style = qStyle(0.88f, 400, Color.White, lh = 1.35f, tnum = true, align = TextAlign.Start),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                    )
                    BasicText(
                        text = "= " + numE(e.result, 6),
                        style = qStyle(0.95f, 700, Q.KeyGreen, tnum = true),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .widthIn(max = maxR)
                            .padding(horizontal = 2.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = whenLabel(e.ts, true),
                    style = qStyle(0.74f, 400, Q.Muted),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PressBox(
                        onClick = onUse,
                        modifier = Modifier.heightIn(min = 32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        BasicText("ব্যবহার", style = qStyle(0.85f, 600, Q.KeyGreen))
                    }
                    PressBox(onClick = onDelete, modifier = Modifier.size(width = 26.dp, height = 20.dp)) {
                        SvgIcon(Icons.Trash, TrashRed, Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}

// ================================================================ verify sheet

/** .v-n chips and plain text, split into words (a word is never broken, lines break at the spaces) */
private class VWord(val pieces: List<VPiece>, val cur: Boolean)

private fun toWords(expr: List<VPiece>): List<VWord> {
    val words = ArrayList<VWord>()
    var cur = ArrayList<VPiece>()
    var hasCur = false
    val sb = StringBuilder()
    fun flushText() {
        if (sb.isNotEmpty()) {
            cur.add(VPiece.Txt(sb.toString()))
            sb.setLength(0)
        }
    }
    fun endWord() {
        flushText()
        if (cur.isNotEmpty()) {
            words.add(VWord(cur, hasCur))
            cur = ArrayList()
            hasCur = false
        }
    }
    for (p in expr) {
        when (p) {
            is VPiece.Txt -> for (ch in p.text) {
                if (ch == ' ') endWord() else sb.append(ch)
            }
            is VPiece.Chip -> {
                flushText()
                cur.add(p)
                if (p.cur) hasCur = true
            }
        }
    }
    endWord()
    return words
}

private val V_ROW = 28.dp

/** lays the words out like running text: left to right, next line when the word does not fit */
@Composable
private fun WordFlow(spacePx: Int, curRow: IntArray, curWord: Int, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, c ->
        val maxW = c.maxWidth
        val rowPx = V_ROW.roundToPx()
        val ps = measurables.map { it.measure(Constraints(minWidth = 0, maxWidth = maxW, minHeight = 0, maxHeight = rowPx)) }
        val xs = IntArray(ps.size)
        val rs = IntArray(ps.size)
        var x = 0
        var row = 0
        for (k in ps.indices) {
            val w = ps[k].width
            if (x > 0 && x + spacePx + w > maxW) {
                row++
                x = 0
            } else if (x > 0) {
                x += spacePx
            }
            xs[k] = x
            rs[k] = row
            if (k == curWord) curRow[0] = row
            x += w
        }
        layout(maxW, (row + 1) * rowPx) {
            for (k in ps.indices) ps[k].placeRelative(xs[k], rs[k] * rowPx)
        }
    }
}

@Composable
private fun VChip(p: VPiece.Chip, onClick: () -> Unit, did: BooleanArray) {
    val bg: Color
    val fg: Color
    if (p.cur && p.done) {
        bg = Q.Good; fg = Color(0xFF04130C)
    } else if (p.cur) {
        bg = Q.Accent; fg = Q.AccentInk
    } else if (p.done) {
        bg = Color.Transparent; fg = Q.Good
    } else {
        bg = Color.Transparent; fg = Q.Ink
    }
    Box(
        modifier = Modifier
            .height(24.dp)
            .panel(bg, 8.dp, border = null)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (did[0]) did[0] = false else onClick()
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = p.text,
            style = qStylePx(15f, 700, fg, lh = 1.2f),
            maxLines = 1,
            softWrap = false
        )
    }
}

/** .v-sum: the whole expression with its total. Tap a number to jump to it, long-press to copy everything. */
@Composable
private fun VSum(ctl: CalcController, view: VView) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = remember(density) {
        qStylePx(15f, 700, Q.Ink).copy(lineHeight = with(density) { V_ROW.toSp() })
    }
    val spacePx = remember(style) { measurer.measure(AnnotatedString(" "), style).size.width }
    val words = remember(view.expr) { toWords(view.expr) }
    val curWord = words.indexOfFirst { it.cur }
    val curRow = remember { intArrayOf(0) }
    val scroll = rememberScrollState()
    val rowPx = with(density) { V_ROW.toPx() }
    val did = remember { booleanArrayOf(false) }
    val ctlNow by rememberUpdatedState(ctl)

    // vShow(): keep the current number in the 5 visible rows
    LaunchedEffect(view) {
        withFrameNanos { }
        withFrameNanos { }
        val row = curRow[0]
        val first = Math.floor(scroll.value / rowPx + 0.5).toInt()
        if (row < first) scroll.scrollTo((row * rowPx).toInt())
        else if (row > first + 4) scroll.scrollTo(((row - 4) * rowPx).toInt())
    }
    // the rows scroll one whole row at a time
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }.collect { moving ->
            if (!moving) {
                delay(90)
                if (scroll.isScrollInProgress) return@collect
                val t = Math.floor(scroll.value / rowPx + 0.5).toFloat() * rowPx
                val target = Math.min(Math.max(t.toInt(), 0), scroll.maxValue)
                if (Math.abs(target - scroll.value) > 1) scroll.animateScrollTo(target)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .panel(Q.Surface, 20.dp, border = null)
            .padding(horizontal = 15.dp, vertical = 11.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = V_ROW * 5)
                .pointerInput(Unit) {
                    val slop = 12.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        did[0] = false
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
                            did[0] = true
                            val v = ctlNow.vView
                            val s = ctlNow.verify
                            if (v != null && s != null) {
                                val txt = copyTextOf(s.src)
                                if (copyText(ctx, txt)) vibrate15(ctx)
                            }
                        }
                    }
                }
                .verticalScroll(scroll)
        ) {
            WordFlow(spacePx, curRow, curWord) {
                for (w in words) {
                    Row(
                        modifier = Modifier.height(V_ROW),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (p in w.pieces) {
                            when (p) {
                                is VPiece.Txt -> BasicText(p.text, style = style)
                                is VPiece.Chip -> VChip(p, onClick = { ctlNow.vSelectChip(p.i) }, did = did)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** the text a long-press copies: the expression as shown, = and the plain result */
private fun copyTextOf(src: String): String {
    val all = com.qalculator.app.logic.vEval(src)
    return dispExpr(src) + " = " + com.qalculator.app.logic.rawNum(all)
}

/** the left / right arrow button */
@Composable
private fun VArrow(label: String, onClick: () -> Unit, modifier: Modifier) {
    PressBox(
        onClick = onClick,
        pressedScale = 0.96f,
        modifier = modifier
            .width(56.dp)
            .panel(Q.KeyOp, 22.dp, border = null),
        contentPadding = PaddingValues(bottom = 4.dp)
    ) {
        BasicText(label, style = qStylePx(32f, 600, Q.Accent, lh = 1f, align = TextAlign.Center))
    }
}

/** the middle box: position, the big number (tap to change it), swipe left / right to step */
@Composable
private fun VCurrent(ctl: CalcController, view: VView, modifier: Modifier) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val editing = ctl.vEditing
    val ctlNow by rememberUpdatedState(ctl)
    var boxW by remember { mutableIntStateOf(0) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .panel(Q.Surface, 22.dp, border = null)
            .onSizeChanged { boxW = it.width }
            .pointerInput(Unit) {
                val slop = 10.dp.toPx()
                val swipe = 40.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val x0 = down.position.x
                    val p0 = down.position
                    var last = down.position
                    var up = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        last = c.position
                        if (c.changedToUp()) {
                            up = true
                            break
                        }
                        if (!c.pressed) break
                    }
                    if (up) {
                        val dx = last.x - x0
                        if (Math.abs(dx) > swipe) {
                            ctlNow.vStepBy(if (dx < 0) 1 else -1)
                        } else if (Math.abs(last.x - p0.x) + Math.abs(last.y - p0.y) <= slop) {
                            if (!ctlNow.vEditing) ctlNow.vOpenEdit()
                        }
                    }
                }
            }
            .padding(9.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BasicText(
                text = view.pos,
                style = qStyle(0.85f, 600, Q.Muted, tnum = true, align = TextAlign.Center)
            )
            if (!editing) {
                // fitNum(): shrink a long number so it stays on one line
                val avail = boxW - 22f * density.density
                var fs = 24f
                var style = qStylePx(fs, 700, Q.Ink, lh = 1.15f, tnum = true, align = TextAlign.Center)
                var w = measurer.measure(view.num, style, softWrap = false).size.width
                if (w > 0 && avail > 0f) {
                    while (w > avail && fs > 12f) {
                        fs -= 1f
                        style = qStylePx(fs, 700, Q.Ink, lh = 1.15f, tnum = true, align = TextAlign.Center)
                        w = measurer.measure(view.num, style, softWrap = false).size.width
                    }
                }
                BasicText(text = view.num, style = style, maxLines = 1, softWrap = false)
            } else {
                VEditField(ctl)
            }
        }
    }
}

/** #v-in: the box where the number is changed (English digits only) */
@Composable
private fun VEditField(ctl: CalcController) {
    val tf = ctl.vEdit
    val focus = remember { FocusRequester() }
    val n = tf.text.length
    val fontPx = vInputRem(n) * 16f
    val widthDp = vInputWidthEm(n) * fontPx
    LaunchedEffect(Unit) {
        focus.requestFocus()
    }
    BasicTextField(
        value = tf,
        onValueChange = { nv -> ctl.vEditChange(nv) },
        singleLine = true,
        textStyle = qStylePx(fontPx, 700, Q.Ink, lh = 1.2f, align = TextAlign.Center),
        cursorBrush = SolidColor(Q.Accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { ctl.vSave(tf.text) }),
        modifier = Modifier
            .focusRequester(focus)
            .width(widthDp.dp)
            .height((1.7f * fontPx).dp)
            .panel(Q.AccentSoft, 6.dp, border = null),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (tf.text.isEmpty()) {
                    BasicText(
                        "সঠিক সংখ্যা",
                        style = qStylePx(fontPx, 700, Color(0xFF757575), lh = 1.2f, align = TextAlign.Center),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun VButton(text: String, bg: Color, fg: Color, onClick: () -> Unit, modifier: Modifier) {
    PressBox(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .panel(bg, 12.dp, border = null)
    ) {
        BasicText(text, style = qStyle(1f, 600, fg, align = TextAlign.Center))
    }
}

@Composable
fun VerifySheet(ctl: CalcController, modifier: Modifier = Modifier) {
    val view = ctl.vView ?: return
    val overlays = LocalOverlays.current
    val editing = ctl.vEditing
    val state = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .blockTouches()
            .verticalScroll(state),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SheetHead("মিলিয়ে দেখুন", onBack = { ctl.vBack() })
        VSum(ctl, view)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VArrow("‹", { ctl.vStepBy(-1) }, Modifier.fillMaxHeight().heightIn(min = 72.dp))
            VCurrent(ctl, view, Modifier.weight(1f).fillMaxHeight())
            VArrow("›", { ctl.vStepBy(1) }, Modifier.fillMaxHeight().heightIn(min = 72.dp))
        }
        BasicText(
            text = "কোনো সংখ্যা ভুল হলে মাঝের বড় বক্সে চাপ দিয়ে ঠিক করুন",
            style = qStyle(0.72f, 500, Q.Muted, align = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
        if (!editing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!view.delHidden) {
                    PressBox(
                        onClick = {
                            overlays.askConfirm(
                                "সংখ্যাটা বাদ দিবেন?",
                                "এই সংখ্যাটা হিসাব থেকে সরে যাবে আর ফলাফল নতুন করে হবে।",
                                "বাদ দিন"
                            ) { ctl.vDeleteNumber() }
                        },
                        modifier = Modifier.heightIn(min = 32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        BasicText("বাদ দিন", style = qStyle(0.85f, 600, Q.Bad))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VEditSave(ctl, Modifier.weight(1f))
                VButton("বাতিল", Q.Field2, Q.Ink, { ctl.vCancelEdit() }, Modifier.weight(1f))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (t in view.tot) {
                if (t.ok) {
                    BasicText(t.label, style = qStyle(0.86f, 700, Q.Good, tnum = true))
                } else {
                    val text = buildAnnotatedString {
                        append(t.label)
                        withStyle(SpanStyle(color = Q.Ink, fontWeight = FontWeight.Bold)) { append(t.value ?: "") }
                    }
                    BasicText(text, style = qStyle(0.86f, 400, Q.Muted, tnum = true))
                }
            }
        }
    }
}

/** "সেভ" */
@Composable
private fun VEditSave(ctl: CalcController, modifier: Modifier) {
    VButton("সেভ", Q.Primary, Q.AccentInk, { ctl.vSave(ctl.vEdit.text) }, modifier)
}
