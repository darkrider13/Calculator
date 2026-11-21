package com.qalculator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qalculator.app.logic.ConvCategory
import com.qalculator.app.logic.convPress
import com.qalculator.app.logic.convertValue
import com.qalculator.app.logic.fmtInput
import com.qalculator.app.logic.fmtResult
import com.qalculator.app.logic.inputValue
import com.qalculator.app.logic.plainNumber

/*
 * One converter page (Temperature, Length, Area, Volume, Speed, Time, Mass): back arrow, big title,
 * two unit rows (the one you type in is white, the other one shows the converted number in green) and the keypad
 * with the tall AC and backspace buttons. The numbers come from logic/Converter.kt.
 */

private val ConvOrange = Color(0xFFD2693C)
internal val ConvTitleInk = Color(0xFFD4D4D8)
internal val ConvLabelInk = Color(0xFF8E8E93)
private val ConvPill = Color(0xFF333333)
internal val ConvDivider = Color(0xFF2A2A2C)

/** the small up / down arrows next to the unit */
internal val UpDownIcon = IconDef(listOf("M8 9.5l4-4 4 4", "M8 14.5l4 4 4-4"), 2f)

/** plus over minus with a slash (the +/− key of the temperature keypad) */
private val PlusMinusIcon = IconDef(listOf("M8 4.5v7", "M4.5 8h7", "M13 18.5h7", "M5.5 20L18.5 4"), 1.8f)

/** what the page remembers: the typed number, the two units and which row is being typed in */
private const val FT_IN = "ftin"

/** "5.7" (feet.inches) -> feet as a decimal number; the digits after the dot are whole inches */
private fun ftInValue(s: String): Double {
    val parts = s.split('.')
    val ft = parts[0].toDoubleOrNull() ?: 0.0
    val inch = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    return ft + inch / 12.0
}

/** feet as a decimal number -> the typed text "feet.inches" (inches rounded to a whole number) */
private fun ftInInput(v: Double): String {
    if (!v.isFinite() || v <= 0.0) return "0"
    var ft = Math.floor(v + 1e-9).toLong()
    var inch = Math.round((v - ft) * 12.0).toInt()
    if (inch >= 12) { ft += 1; inch = 0 }
    return if (inch == 0) ft.toString() else ft.toString() + "." + inch
}

/** the typed text as shown: 5′ 7″ */
private fun ftInTyped(s: String): String {
    val parts = s.split('.')
    val ft = parts[0].ifEmpty { "0" }
    val inch = parts.getOrNull(1)?.ifEmpty { "0" } ?: "0"
    return fmtInput(ft) + "′ " + inch + "″"
}

/** feet as a decimal number shown as 5′ 7.3″ */
private fun ftInResult(v: Double): String {
    if (!v.isFinite()) return "—"
    var ft = Math.floor(v + 1e-9)
    var inch = Math.round((v - ft) * 12.0 * 10.0) / 10.0
    if (inch >= 12.0) { ft += 1.0; inch = 0.0 }
    val inStr = if (inch == Math.floor(inch)) inch.toLong().toString() else inch.toString()
    return fmtInput(plainNumber(ft)) + "′ " + inStr + "″"
}

/** what the page remembers: the typed number, the two units and which row is being typed in */
private class ConverterState(val cat: ConvCategory) {
    var input by mutableStateOf("0")
    var topUnit by mutableStateOf(cat.defTop)
    var bottomUnit by mutableStateOf(cat.defBottom)
    var topActive by mutableStateOf(true)

    private fun unitOfRow(top: Boolean) = if (top) topUnit else bottomUnit

    /** the typed number as a plain value in the unit of the row being typed in */
    private fun activeValue(): Double =
        if (unitOfRow(topActive) == FT_IN) ftInValue(input) else inputValue(input)

    /** a value shown in the typed form of [unit] */
    private fun inputFor(unit: String, v: Double): String =
        if (unit == FT_IN) ftInInput(v) else plainNumber(v)

    fun textOf(top: Boolean): String {
        val u = unitOfRow(top)
        if (topActive == top) return if (u == FT_IN) ftInTyped(input) else fmtInput(input)
        val v = convertValue(cat, activeValue(), unitOfRow(topActive), u)
        return if (u == FT_IN) ftInResult(v) else fmtResult(v)
    }

    /** tapping the other row: it becomes the one you type in, starting from its converted number */
    fun activate(top: Boolean) {
        if (topActive == top) return
        val v = convertValue(cat, activeValue(), unitOfRow(topActive), unitOfRow(top))
        input = inputFor(unitOfRow(top), v)
        topActive = top
    }

    fun setUnit(top: Boolean, key: String) {
        val old = unitOfRow(top)
        // the row being typed in changes to or from feet + inches: keep the same length, written the new way
        if (top == topActive && old != key && (old == FT_IN || key == FT_IN)) {
            input = inputFor(key, convertValue(cat, activeValue(), old, key))
        }
        if (top) topUnit = key else bottomUnit = key
    }

    fun press(key: String) {
        if (unitOfRow(topActive) != FT_IN) {
            input = convPress(input, key)
            return
        }
        // feet + inches: the dot separates feet from inches (5.7 = 5 ft 7 in)
        input = when (key) {
            "AC" -> "0"
            "⌫" -> input.dropLast(1).let { if (it.isEmpty()) "0" else it }
            "." -> if (input.contains('.')) input else "$input."
            else -> {
                val dot = input.indexOf('.')
                when {
                    dot >= 0 && input.length - dot - 1 >= 2 -> input
                    dot < 0 && input.length >= 5 -> input
                    input == "0" -> key
                    else -> input + key
                }
            }
        }
    }
}

@Composable
fun ConverterPage(cat: ConvCategory, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    // back: an open unit menu closes first (its own handler), then this page closes
    BackHandler(enabled = !overlays.anyOpen) { onBack() }
    val st = remember(cat.id) { ConverterState(cat) }

    fun pickUnit(top: Boolean, anchor: Rect) {
        val cur = if (top) st.topUnit else st.bottomUnit
        overlays.openMenu(anchor, cat.menuOptions, cur) { st.setUnit(top, it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Q.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressBox(
                onClick = onBack,
                pressedScale = 0.92f,
                modifier = Modifier.size(48.dp)
            ) {
                SvgIcon(Icons.Back, ConvTitleInk, Modifier.size(24.dp))
            }
        }
        BasicText(
            cat.title,
            modifier = Modifier.padding(start = 26.dp, end = 16.dp, top = 14.dp),
            style = qStylePx(34f, 300, ConvTitleInk, lh = 1.2f),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val top = cat.unit(st.topUnit)
            val bottom = cat.unit(st.bottomUnit)
            ConvRow(
                unit = top,
                text = st.textOf(true),
                active = st.topActive,
                onSelect = { st.activate(true) },
                onUnit = { pickUnit(true, it) },
                modifier = Modifier.weight(1f)
            )
            ConvRow(
                unit = bottom,
                text = st.textOf(false),
                active = !st.topActive,
                onSelect = { st.activate(false) },
                onUnit = { pickUnit(false, it) },
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(ConvDivider)
        )
        ConvKeypad(
            signed = cat.signed,
            onKey = { st.press(it) },
            modifier = Modifier
                .weight(1.25f)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }
}

/** long press on a number copies it: returns the function that puts a text on the clipboard and says "Copied" */
@Composable
internal fun rememberCopier(): (String) -> Unit {
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    return remember(clip, ctx, haptic) {
        { text: String ->
            clip.setText(AnnotatedString(text))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
        }
    }
}

/** the number as it is copied: no thousands commas (feet + inches stays as shown) */
internal fun copyText(shown: String): String = if (shown.contains('′')) shown else shown.replace(",", "")

/** one unit row: the unit button on the left, the number on the right with the unit name under it */
@Composable
internal fun ConvRow(
    unit: com.qalculator.app.logic.ConvUnit,
    text: String,
    active: Boolean,
    onSelect: () -> Unit,
    onUnit: (Rect) -> Unit,
    modifier: Modifier
) {
    // the longer the number, the smaller it gets, so it always fits
    val fontSp = when {
        text.length <= 8 -> 40f
        text.length <= 11 -> 34f
        text.length <= 14 -> 28f
        text.length <= 17 -> 24f
        else -> 20f
    }
    val copy = rememberCopier()
    PressBox(
        onClick = onSelect,
        onLongClick = { copy(copyText(text)) },
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 26.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnitChip(unit.symbol, onUnit)
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                BasicText(
                    text,
                    modifier = Modifier.fillMaxWidth(),
                    style = qStylePx(fontSp, 300, if (active) Color.White else Q.KeyGreen, lh = 1.15f, align = TextAlign.End),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                BasicText(
                    unit.name,
                    modifier = Modifier.fillMaxWidth(),
                    style = qStylePx(13f, 300, ConvLabelInk, lh = 1.3f, align = TextAlign.End),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** the unit symbol with the up / down arrows; tapping it opens the unit menu */
@Composable
internal fun UnitChip(label: String, onClick: (Rect) -> Unit) {
    val bounds = remember { arrayOf(Rect.Zero) }
    PressBox(
        onClick = { onClick(bounds[0]) },
        pressedScale = 0.95f,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BasicText(
                label,
                style = qStylePx(22f, 500, Color(0xFFF2F2F2), lh = 1.2f),
                maxLines = 1,
                softWrap = false
            )
            SvgIcon(UpDownIcon, ConvLabelInk, Modifier.size(18.dp))
        }
    }
}

/**
 * The keypad: 3 columns of digits (0 and . in the bottom row, under 2 and 3) and a 4th column with
 * the tall AC and backspace buttons. The temperature keypad has no tall buttons: AC, backspace and +/− in a column.
 */
@Composable
internal fun ConvKeypad(signed: Boolean, onKey: (String) -> Unit, modifier: Modifier, dot: Boolean = true) {
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(3f).fillMaxHeight()) {
            for (row in listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"))) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (k in row) DigitKey(k, Modifier.weight(1f).fillMaxHeight(), onKey)
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                DigitKey("0", Modifier.weight(1f).fillMaxHeight(), onKey)
                if (dot) DigitKey(".", Modifier.weight(1f).fillMaxHeight(), onKey) else Spacer(Modifier.weight(1f))
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (signed) {
                // temperature: plain keys, one per row
                SideKey(Modifier.weight(1f).fillMaxWidth(), { onKey("AC") }) { AcText() }
                SideKey(Modifier.weight(1f).fillMaxWidth(), { onKey("⌫") }, { onKey("AC") }) { BackIcon() }
                SideKey(Modifier.weight(1f).fillMaxWidth(), { onKey("±") }) {
                    SvgIcon(PlusMinusIcon, ConvOrange, Modifier.size(28.dp))
                }
                Spacer(Modifier.weight(1f))
            } else {
                // the tall pills: AC over the top two rows, backspace over the bottom two
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Pill({ onKey("AC") }) { AcText() } }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Pill({ onKey("⌫") }, { onKey("AC") }) { BackIcon() } }
            }
        }
    }
}

@Composable
private fun DigitKey(k: String, modifier: Modifier, onKey: (String) -> Unit) {
    PressBox(
        onClick = { onKey(k) },
        pressedScale = 0.88f,
        modifier = modifier
    ) {
        BasicText(
            k,
            style = qStylePx(38f, 300, Color.White, lh = 1.2f, align = TextAlign.Center),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SideKey(modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    PressBox(
        onClick = onClick,
        onLongClick = onLongClick?.let { { haptic.performHapticFeedback(HapticFeedbackType.LongPress); it() } },
        pressedScale = 0.88f,
        modifier = modifier
    ) { content() }
}

/** the tall dark button (AC / backspace) */
@Composable
private fun Pill(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    PressBox(
        onClick = onClick,
        onLongClick = onLongClick?.let { { haptic.performHapticFeedback(HapticFeedbackType.LongPress); it() } },
        pressedScale = 0.95f,
        modifier = Modifier
            .fillMaxSize()
            .background(ConvPill, RoundedCornerShape(percent = 50))
    ) { content() }
}

@Composable
internal fun AcText() {
    BasicText(
        "AC",
        style = qStylePx(24f, 500, Q.KeyGreen, lh = 1.2f, align = TextAlign.Center),
        maxLines = 1,
        softWrap = false
    )
}

/** the cross (backspace) icon of the main calculator, in red */
@Composable
internal fun BackIcon() {
    SvgIcon(Icons.Backspace, Q.KeyRed, Modifier.size(28.dp))
}
