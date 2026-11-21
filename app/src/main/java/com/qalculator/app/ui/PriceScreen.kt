package com.qalculator.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalculator.app.AppState
import com.qalculator.app.logic.FLASH_DUPLICATE
import com.qalculator.app.logic.FLASH_SAVED
import com.qalculator.app.logic.Form
import com.qalculator.app.logic.HistEntry
import com.qalculator.app.logic.MemoView
import com.qalculator.app.logic.SaveResult
import com.qalculator.app.logic.UNIT_OPTS
import com.qalculator.app.logic.TAKA_SUF
import com.qalculator.app.logic.baseParts
import com.qalculator.app.logic.bnDigits
import com.qalculator.app.logic.dimOf
import com.qalculator.app.logic.memoOf
import com.qalculator.app.logic.money
import com.qalculator.app.logic.numf
import com.qalculator.app.logic.pickLabel
import com.qalculator.app.logic.pickUnit
import com.qalculator.app.logic.qty
import com.qalculator.app.logic.reuseForm
import com.qalculator.app.logic.saveEntry
import com.qalculator.app.logic.uid
import com.qalculator.app.logic.whenLabel
import kotlinx.coroutines.delay

/*
 * The দাম হিসাব tab: item name, shop price / quantity / unit, the switch, "you buy" row,
 * the memo box, the history box (paged, never scrolls) and the save button.
 */

/** the state of this tab that is not part of the saved form (history page, the save-button message, open menu) */
class PriceController(private val app: AppState) {
    var hPage by mutableIntStateOf(0)
    var flash by mutableStateOf<String?>(null)
        private set
    var flashSeq by mutableIntStateOf(0)
        private set
    var menuFor by mutableStateOf<String?>(null)

    val form: Form get() = app.form
    val history: List<HistEntry> get() = app.history

    fun setField(id: String, v: String) {
        val f = app.form
        app.updateForm(
            when (id) {
                "item" -> f.copy(item = v)
                "rp" -> f.copy(rp = v)
                "rw" -> f.copy(rw = v)
                "w" -> f.copy(w = v)
                else -> f.copy(amt = v)
            }
        )
    }

    fun setMode(m: String) {
        app.updateForm(app.form.copy(m = m))
    }

    fun onPickUnit(id: String, v: String) {
        app.updateForm(pickUnit(app.form, id, v))
    }

    /** the reset button: everything back to the start */
    fun reset() {
        app.updateForm(Form())
    }

    private fun flashMsg(msg: String) {
        flash = msg
        flashSeq++
    }

    fun clearFlash() {
        flash = null
    }

    /** saveEntry() */
    fun save() {
        when (val r = saveEntry(app.form, app.history, System.currentTimeMillis(), uid())) {
            is SaveResult.Invalid -> {}
            is SaveResult.Duplicate -> flashMsg(FLASH_DUPLICATE)
            is SaveResult.Saved -> {
                app.updateHistory(r.history)
                hPage = 0
                flashMsg(FLASH_SAVED)
            }
        }
    }

    /** "আবার": the saved price goes back into the form */
    fun reuse(id: String) {
        val e = app.history.firstOrNull { it.id == id } ?: return
        app.updateForm(reuseForm(app.form, e))
    }

    fun deleteHistory(id: String) {
        app.updateHistory(app.history.filter { it.id != id })
    }

    fun clearHistory() {
        app.updateHistory(emptyList())
        hPage = 0
    }
}

private const val SAVE_LABEL = "Save to history"

@Composable
fun PriceScreen(ctl: PriceController, modifier: Modifier = Modifier) {
    val compact = LocalCompact.current
    val gap = if (compact) 6.dp else 10.dp
    val overlays = LocalOverlays.current
    val f = ctl.form
    val memo = remember(f) { memoOf(f) }

    // the message on the save button goes back after 2 seconds
    LaunchedEffect(ctl.flashSeq) {
        if (ctl.flash != null) {
            delay(2000)
            ctl.clearFlash()
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val avail = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PriceFlow(avail, gap) {
                // 0: item name
                PriceInput(
                    value = f.item,
                    onChange = { ctl.setField("item", it) },
                    placeholder = "Item name (optional)",
                    numeric = false,
                    money = false,
                    ime = ImeAction.Done,
                    onAction = null,
                    maxLen = 60,
                    modifier = Modifier.fillMaxWidth()
                )
                // 1: shop price | quantity | unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledField("Shop price", Modifier.weight(1f)) {
                        PriceInput(
                            value = f.rp, onChange = { ctl.setField("rp", it) }, placeholder = "0.00 Tk",
                            numeric = true, money = true, ime = ImeAction.Next, onAction = { ctl.save() },
                            maxLen = Int.MAX_VALUE, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    LabeledField("Quantity", Modifier.weight(1f)) {
                        PriceInput(
                            value = f.rw, onChange = { ctl.setField("rw", it) }, placeholder = "0.00",
                            numeric = true, money = false, ime = ImeAction.Next, onAction = { ctl.save() },
                            maxLen = Int.MAX_VALUE, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    LabeledField("Unit", Modifier.weight(1f)) {
                        UnitPick(ctl, "ru", f.ru)
                    }
                }
                // 2: the switch
                SegSwitch(
                    items = listOf(SegItem("w", "Price for quantity"), SegItem("p", "Quantity for money")),
                    selected = f.m,
                    onSelect = { ctl.setMode(it) }
                )
                // 3: "you buy" row (quantity + unit) or "how much money"
                if (f.m != "p") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LabeledField("You are buying (quantity)", Modifier.weight(1.3f)) {
                            PriceInput(
                                value = f.w, onChange = { ctl.setField("w", it) }, placeholder = "0.00",
                                numeric = true, money = false, ime = ImeAction.Done, onAction = { ctl.save() },
                                maxLen = Int.MAX_VALUE, modifier = Modifier.fillMaxWidth()
                            )
                        }
                        LabeledField("Unit", Modifier.weight(1f)) {
                            UnitPick(ctl, "wu", f.wu)
                        }
                    }
                } else {
                    LabeledField("How much money to spend", Modifier.fillMaxWidth()) {
                        PriceInput(
                            value = f.amt, onChange = { ctl.setField("amt", it) }, placeholder = "0.00 Tk",
                            numeric = true, money = true, ime = ImeAction.Done, onAction = { ctl.save() },
                            maxLen = Int.MAX_VALUE, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                // 4: memo
                MemoBox(memo, onReset = { ctl.reset() })
                // 5: history (takes the height that is left)
                HistBox(ctl, overlays)
                // 6: save
                PrimaryButton(
                    text = ctl.flash ?: SAVE_LABEL,
                    onClick = { ctl.save() },
                    enabled = memo.ok
                )
            }
        }
    }
}

/**
 * The tab is a column with gaps; the history box (child 5) gets all the height that is left,
 * but never less than 92. If everything is taller than the area the column scrolls.
 */
@Composable
private fun PriceFlow(avail: Dp, gap: Dp, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, c ->
        val w = c.maxWidth
        val g = gap.roundToPx()
        val flex = 5
        val ps = arrayOfNulls<androidx.compose.ui.layout.Placeable>(measurables.size)
        var fixed = 0
        for (i in measurables.indices) {
            if (i == flex) continue
            ps[i] = measurables[i].measure(
                Constraints(minWidth = w, maxWidth = w, minHeight = 0, maxHeight = Constraints.Infinity)
            )
            fixed += ps[i]!!.height
        }
        fixed += g * (measurables.size - 1)
        val histH = Math.max(92.dp.roundToPx(), avail.roundToPx() - fixed)
        ps[flex] = measurables[flex].measure(Constraints.fixed(w, histH))
        var y = 0
        val ys = IntArray(measurables.size)
        for (i in measurables.indices) {
            ys[i] = y
            y += ps[i]!!.height + g
        }
        val total = y - g
        layout(w, Math.max(0, total)) {
            for (i in measurables.indices) ps[i]!!.placeRelative(0, ys[i])
        }
    }
}

/** .field: a small label above the box */
@Composable
private fun LabeledField(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        FieldLabel(label)
        content()
    }
}

@Composable
private fun UnitPick(ctl: PriceController, id: String, unit: String) {
    val overlays = LocalOverlays.current
    val expanded = overlays.menu != null && ctl.menuFor == id
    PickButton(
        label = pickLabel(unit),
        expanded = expanded,
        onClick = { rect ->
            if (expanded) {
                overlays.closeMenu()
            } else {
                ctl.menuFor = id
                val cur = if (com.qalculator.app.logic.UNITS.containsKey(unit)) unit else "g"
                overlays.openMenu(rect, UNIT_OPTS, cur) { v -> ctl.onPickUnit(id, v) }
            }
        }
    )
}

// ---------------------------------------------------------------- text boxes

/** the ৳ sign right after the typed money (.msuf) */
private object TakaSuffix : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val len = text.text.length
        return TransformedText(
            AnnotatedString(text.text + TAKA_SUF),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset
                override fun transformedToOriginal(offset: Int): Int = Math.min(offset, len)
            }
        )
    }
}

/** input: dark box, thin border, top highlight, the border turns accent when focused */
@Composable
private fun PriceInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    numeric: Boolean,
    money: Boolean,
    ime: ImeAction,
    onAction: (() -> Unit)?,
    maxLen: Int,
    modifier: Modifier
) {
    val compact = LocalCompact.current
    // number boxes show Bengali digits (fillForm)
    val shown = value
    var tf by remember { mutableStateOf(TextFieldValue(shown, TextRange(shown.length))) }
    if (tf.text != shown) tf = TextFieldValue(shown, TextRange(shown.length))
    var focused by remember { mutableStateOf(false) }
    val sel = LocalTextSelectionColors.current
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = sel.handleColor,
            backgroundColor = Color(0x24FFFFFF)
        )
    ) {
        BasicTextField(
            value = tf,
            onValueChange = { nv ->
                var t = nv.text
                if (t.length > maxLen) t = t.take(maxLen)
                tf = nv.copy(text = t)
                onChange(t)
            },
            singleLine = true,
            textStyle = qStyle(1f, 400, Q.Ink, lh = 1.2f),
            cursorBrush = SolidColor(Q.Ink),
            visualTransformation = if (money) TakaSuffix else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
                imeAction = ime
            ),
            keyboardActions = KeyboardActions(
                onNext = { onAction?.invoke() },
                onDone = { onAction?.invoke() }
            ),
            modifier = modifier
                .heightIn(min = if (compact) 38.dp else 44.dp)
                .onFocusChanged { focused = it.isFocused }
                .fieldBox(focused = focused),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compact) 38.dp else 44.dp)
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (tf.text.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = qStyle(1f, 400, Q.Muted.copy(alpha = 0.75f), lh = 1.2f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    inner()
                }
            }
        )
    }
}

// ---------------------------------------------------------------- memo

@Composable
private fun MemoBox(v: MemoView, onReset: () -> Unit) {
    val compact = LocalCompact.current
    val vh = LocalViewportH.current.value
    val totalSize = if (compact) 27.2f else Math.min(Math.max(0.06f * vh, 32f), 43.2f)
    val total = buildAnnotatedString {
        append(v.main)
        if (v.cur) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(TAKA_SUF) }
        }
        val s = v.small
        if (s != null) {
            withStyle(SpanStyle(fontSize = (totalSize * 0.75f).sp, fontWeight = FontWeight.Bold, color = Q.Ink)) {
                append(" ")
                append(s)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .panel(Q.Surface, 20.dp, border = null)
    ) {
        Box(modifier = Modifier.padding(1.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 6.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    BasicText(
                        text = v.label,
                        style = qStyle(0.85f, 600, Q.Muted),
                        maxLines = 1,
                        softWrap = false
                    )
                    BasicText(
                        text = v.detail,
                        style = qStyle(0.82f, 400, Q.Muted, align = TextAlign.End),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                BasicText(
                    text = total,
                    style = qStylePx(totalSize, 700, Q.Ink, lh = 1.15f, tnum = true)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compact) 18.dp else 22.dp)
                        .padding(end = 40.dp)
                ) {
                    BasicText(
                        text = v.sub,
                        style = qStyle(0.9f, 400, Q.Muted, tnum = true)
                    )
                }
            }
            PressBox(
                onClick = onReset,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 6.dp)
                    .size(38.dp)
            ) { pressed ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (pressed) Q.AccentSoft else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    SvgIcon(Icons.Reset, if (pressed) Q.Accent else Q.Muted, Modifier.size(22.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- history

private val TrashRedP = Color(0xFFFF0000)

/** .histbox: the saved prices, a few at a time (pages), never scrolling */
@Composable
private fun HistBox(ctl: PriceController, overlays: Overlays) {
    val items = ctl.history
    Column(
        modifier = Modifier
            .fillMaxSize()
            .panel(Q.Surface, 20.dp, border = null)
            .padding(start = 13.dp, end = 13.dp, top = 7.dp, bottom = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // drawPaged: as many 40px rows as fit in the list part
            val headH = 34.dp
            val listH = maxHeight - headH - 2.dp
            val n = items.size
            val ps = if (listH.value > 0f) Math.max(1, Math.floor((listH.value / 40f).toDouble()).toInt()) else 4
            val pages = Math.max(1, Math.ceil(n.toDouble() / ps).toInt())
            val p = Math.min(Math.max(ctl.hPage, 0), pages - 1)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // header: title | pager | trash
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = headH),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        BasicText("History", style = qStyle(0.78f, 700, Q.Ink))
                    }
                    val showPager = n > 0 && pages > 1
                    Row(
                        modifier = Modifier.graphicsLayer { alpha = if (showPager) 1f else 0f },
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PgMini("‹", enabled = showPager && p > 0) { ctl.hPage = p - 1 }
                        Box(modifier = Modifier.widthIn(min = 42.dp), contentAlignment = Alignment.Center) {
                            BasicText(
                                text = numf((p + 1).toDouble(), 0) + " / " + numf(pages.toDouble(), 0),
                                style = qStyle(0.78f, 400, Q.Muted, tnum = true, align = TextAlign.Center)
                            )
                        }
                        PgMini("›", enabled = showPager && p < pages - 1) { ctl.hPage = p + 1 }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        PressBox(
                            onClick = {
                                overlays.askConfirm(
                                    "Delete all history?",
                                    "All saved prices will be deleted. This cannot be undone.",
                                    "Yes, delete"
                                ) { ctl.clearHistory() }
                            },
                            enabled = n > 0,
                            modifier = Modifier
                                .size(width = 26.dp, height = 32.dp)
                                .graphicsLayer { alpha = if (n > 0) 1f else 0f }
                        ) {
                            SvgIcon(Icons.Trash, TrashRedP, Modifier.size(20.dp))
                        }
                    }
                }
                // list
                if (n == 0) {
                    BasicText(
                        text = "Nothing saved yet. Tap the save button below and it will be kept here.",
                        style = qStyle(0.78f, 400, Q.Muted),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth().clipToBounds()) {
                        val start = p * ps
                        val end = Math.min(n, start + ps)
                        for (i in start until end) {
                            val e = items[i]
                            HistItem(
                                e = e,
                                onReuse = { ctl.reuse(e.id) },
                                onDelete = {
                                    overlays.askConfirm("Delete history item?", "This entry will be deleted.", "Delete") {
                                        ctl.deleteHistory(e.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PgMini(label: String, enabled: Boolean, onClick: () -> Unit) {
    PressBox(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(width = 30.dp, height = 24.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .panel(Q.Field, 8.dp, border = null)
    ) {
        BasicText(label, style = qStylePx(16f, 700, Q.Ink, lh = 1f, align = TextAlign.Center))
    }
}

/** .h-item: one saved price, 40px high */
@Composable
private fun HistItem(e: HistEntry, onReuse: () -> Unit, onDelete: () -> Unit) {
    val itemPrefix = if (e.item != "") e.item + " · " else ""
    val title: String
    val total: AnnotatedString
    val base = 13.12f
    if (e.t == "q") {
        title = itemPrefix + money(e.amt) + TAKA_SUF
        val hp = baseParts(e.grams, dimOf(e.ru))
        total = buildAnnotatedString {
            append(hp.first)
            withStyle(SpanStyle(fontSize = (base * 0.66f).sp, fontWeight = FontWeight.SemiBold, color = Q.Muted)) {
                append(" ")
                append(hp.second)
            }
        }
    } else {
        title = itemPrefix + qty(e.w, e.wu)
        total = buildAnnotatedString {
            append(money(e.total))
            withStyle(SpanStyle(fontSize = (base * 0.95f).sp, fontWeight = FontWeight.Bold, color = Q.Ink)) {
                append(TAKA_SUF)
            }
        }
    }
    val lineColor = Q.Line
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .drawBehind {
                drawRect(
                    color = lineColor,
                    topLeft = Offset(0f, size.height - density),
                    size = Size(size.width, density)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 1.dp)
                .clipToBounds()
                .padding(top = 3.dp, bottom = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicText(
                    text = title,
                    style = qStyle(0.76f, 600, Q.Ink),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline()
                )
                BasicText(
                    text = total,
                    style = qStylePx(base, 700, Q.Ink, tnum = true),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .alignByBaseline()
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = whenLabel(e.ts),
                        style = qStyle(0.54f, 400, Q.Muted),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PressBox(
                            onClick = onReuse,
                            modifier = Modifier.heightIn(min = 20.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp)
                        ) {
                            BasicText("Again", style = qStyle(0.66f, 600, Q.Accent, lh = 1f))
                        }
                        PressBox(
                            onClick = onDelete,
                            modifier = Modifier.size(width = 26.dp, height = 20.dp)
                        ) {
                            SvgIcon(
                                Icons.Trash, TrashRedP,
                                Modifier
                                    .size(13.dp)
                                    .offset(y = (-1.5).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
