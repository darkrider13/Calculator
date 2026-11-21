package com.qalculator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalculator.app.logic.TAKA_SUF
import com.qalculator.app.logic.UNIT_OPTS
import com.qalculator.app.logic.UNITS
import com.qalculator.app.logic.convPress
import com.qalculator.app.logic.fmtInput
import com.qalculator.app.logic.memoOf
import com.qalculator.app.logic.pickLabel
import kotlinx.coroutines.delay

/*
 * The price (dam hishab) page, in the same design as the converters: big title, rows with the typed numbers in white
 * (the row you type in has a soft box), the result in green, and the same keypad with the tall green AC / red backspace.
 * Reset, History and Save are on the top bar. Everything is saved in the same form / history as before.
 */

@Composable
fun PricePage(ctl: PriceController, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    var showHist by remember { mutableStateOf(false) }
    // back: an open unit menu closes first (its own handler), then the history, then this page
    BackHandler(enabled = !overlays.anyOpen) { if (showHist) showHist = false else onBack() }

    // the message on the save button goes back after 2 seconds
    LaunchedEffect(ctl.flashSeq) {
        if (ctl.flash != null) {
            delay(2000)
            ctl.clearFlash()
        }
    }

    val f = ctl.form
    val memo = remember(f) { memoOf(f) }
    val priceMode = f.m == "p"
    var picked by remember { mutableStateOf("rp") }
    // the row being typed in: in "quantity for money" mode the third row is the money row
    val active = when {
        priceMode && picked == "w" -> "amt"
        !priceMode && picked == "amt" -> "w"
        else -> picked
    }

    if (showHist) {
        Column(modifier = modifier.fillMaxSize().background(Q.Black)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PressBox(onClick = { showHist = false }, pressedScale = 0.92f, modifier = Modifier.size(48.dp)) {
                    SvgIcon(Icons.Back, ConvTitleInk, Modifier.size(24.dp))
                }
            }
            BasicText(
                "Price history",
                modifier = Modifier.padding(start = 26.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                style = qStylePx(34f, 300, ConvTitleInk, lh = 1.2f),
                maxLines = 1
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                HistBox(ctl, overlays, onReused = { showHist = false })
            }
        }
        return
    }

    fun valueOf(id: String) = when (id) {
        "rp" -> f.rp
        "rw" -> f.rw
        "w" -> f.w
        else -> f.amt
    }

    fun press(key: String) {
        val cur = valueOf(active)
        var n = convPress(if (cur.isEmpty()) "0" else cur, key)
        if (n == "0") n = ""
        ctl.setField(active, n)
    }

    fun pickUnit(id: String, anchor: Rect) {
        val cur = if (id == "ru") f.ru else f.wu
        overlays.openMenu(anchor, UNIT_OPTS, if (UNITS.containsKey(cur)) cur else "g") { v -> ctl.onPickUnit(id, v) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Q.Black)
    ) {
        // top bar: back, then reset / history / save
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressBox(onClick = onBack, pressedScale = 0.92f, modifier = Modifier.size(48.dp)) {
                SvgIcon(Icons.Back, ConvTitleInk, Modifier.size(24.dp))
            }
            Spacer(Modifier.weight(1f))
            PressBox(onClick = { ctl.reset() }, pressedScale = 0.92f, modifier = Modifier.size(44.dp)) {
                SvgIcon(Icons.Reset, ConvLabelInk, Modifier.size(22.dp))
            }
            PressBox(onClick = { showHist = true }, pressedScale = 0.92f, modifier = Modifier.size(44.dp)) {
                SvgIcon(Icons.Clock, ConvLabelInk, Modifier.size(22.dp))
            }
            PressBox(
                onClick = { ctl.save() },
                enabled = memo.ok,
                pressedScale = 0.95f,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .graphicsLayer { alpha = if (memo.ok) 1f else 0.35f },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                BasicText(
                    ctl.flash ?: "Save",
                    style = qStylePx(16f, 600, Q.KeyGreen, lh = 1.2f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        BasicText(
            "Price converter",
            modifier = Modifier.padding(start = 26.dp, end = 16.dp, top = 14.dp),
            style = qStylePx(34f, 300, ConvTitleInk, lh = 1.2f),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        ItemNameField(f.item) { ctl.setField("item", it) }
        // the two ways of working it out
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModePill("Price for quantity", !priceMode, Modifier.weight(1f)) { ctl.setMode("w") }
            ModePill("Quantity for money", priceMode, Modifier.weight(1f)) { ctl.setMode("p") }
        }
        Column(modifier = Modifier.weight(1.7f).fillMaxWidth()) {
            PriceRow(
                text = valueOf("rp"), caption = "Shop price", active = active == "rp",
                onSelect = { picked = "rp" }, modifier = Modifier.weight(1f)
            ) { Symbol("৳") }
            PriceRow(
                text = valueOf("rw"), caption = "Shop quantity · " + pickLabel(f.ru), active = active == "rw",
                onSelect = { picked = "rw" }, modifier = Modifier.weight(1f)
            ) { UnitChip(f.ru) { pickUnit("ru", it) } }
            if (!priceMode) {
                PriceRow(
                    text = valueOf("w"), caption = "You are buying · " + pickLabel(f.wu), active = active == "w",
                    onSelect = { picked = "w" }, modifier = Modifier.weight(1f)
                ) { UnitChip(f.wu) { pickUnit("wu", it) } }
            } else {
                PriceRow(
                    text = valueOf("amt"), caption = "Money to spend", active = active == "amt",
                    onSelect = { picked = "amt" }, modifier = Modifier.weight(1f)
                ) { Symbol("৳") }
            }
            ResultRow(memo, Modifier.weight(1.2f))
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(ConvDivider)
        )
        ConvKeypad(
            signed = false,
            onKey = { press(it) },
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun Symbol(s: String) {
    BasicText(s, style = qStylePx(26f, 400, Color(0xFFF2F2F2), lh = 1.2f), maxLines = 1, softWrap = false)
}

/** the optional item name: a plain line with a thin line under it */
@Composable
private fun ItemNameField(value: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 6.dp)) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 60) onChange(it) },
            singleLine = true,
            textStyle = qStylePx(18f, 300, Color.White, lh = 1.2f),
            cursorBrush = SolidColor(Q.Cursor),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        BasicText(
                            "Item name (optional)",
                            style = qStylePx(18f, 300, ConvLabelInk.copy(alpha = 0.7f), lh = 1.2f),
                            maxLines = 1
                        )
                    }
                    inner()
                }
            }
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ConvDivider))
    }
}

/** the round switch button: the chosen one is a dark pill with green text */
@Composable
private fun ModePill(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    PressBox(
        onClick = onClick,
        pressedScale = 0.96f,
        modifier = modifier
            .heightIn(min = 40.dp)
            .background(if (on) Color(0xFF333333) else Color(0xFF161616), RoundedCornerShape(percent = 50)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        BasicText(
            label,
            style = qStylePx(14f, 600, if (on) Q.KeyGreen else ConvLabelInk, lh = 1.2f, align = TextAlign.Center),
            maxLines = 1,
            softWrap = false
        )
    }
}

/** one typed row: the symbol / unit button on the left, the number (white) with a small caption on the right */
@Composable
private fun PriceRow(
    text: String,
    caption: String,
    active: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier,
    left: @Composable () -> Unit
) {
    val shown = if (text.isEmpty()) "0" else fmtInput(text)
    val fontSp = when {
        shown.length <= 8 -> 36f
        shown.length <= 11 -> 31f
        shown.length <= 14 -> 26f
        else -> 22f
    }
    PressBox(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .then(if (active) Modifier.background(Q.AccentSoft, RoundedCornerShape(18.dp)) else Modifier),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            left()
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                BasicText(
                    shown,
                    modifier = Modifier.fillMaxWidth(),
                    style = qStylePx(fontSp, 300, if (text.isEmpty()) Color(0xFF8E8E93) else Color.White, lh = 1.15f, align = TextAlign.End),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                BasicText(
                    caption,
                    modifier = Modifier.fillMaxWidth(),
                    style = qStylePx(13f, 300, ConvLabelInk, lh = 1.3f, align = TextAlign.End),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** the answer, in green */
@Composable
private fun ResultRow(v: com.qalculator.app.logic.MemoView, modifier: Modifier) {
    val vh = LocalViewportH.current.value
    val totalSize = Math.min(Math.max(0.055f * vh, 30f), 42f)
    val total = buildAnnotatedString {
        append(v.main)
        if (v.cur) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(TAKA_SUF) }
        val s = v.small
        if (s != null) {
            withStyle(SpanStyle(fontSize = (totalSize * 0.6f).sp, fontWeight = FontWeight.Medium)) {
                append(" ")
                append(s)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        BasicText(
            v.label + if (v.detail.isNotEmpty()) "  ·  " + v.detail else "",
            modifier = Modifier.fillMaxWidth(),
            style = qStylePx(13f, 300, ConvLabelInk, lh = 1.3f, align = TextAlign.End),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicText(
            total,
            modifier = Modifier.fillMaxWidth(),
            style = qStylePx(totalSize, 300, Q.KeyGreen, lh = 1.15f, tnum = true, align = TextAlign.End),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        if (v.sub.isNotEmpty()) {
            BasicText(
                v.sub,
                modifier = Modifier.fillMaxWidth(),
                style = qStylePx(13f, 300, ConvLabelInk, lh = 1.3f, align = TextAlign.End),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
