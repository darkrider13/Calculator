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
 * two unit rows (the one you type in is orange, the other one shows the converted number) and the keypad
 * with the tall AC and backspace buttons. The numbers come from logic/Converter.kt.
 */

private val ConvOrange = Color(0xFFD2693C)
private val ConvTitleInk = Color(0xFFD4D4D8)
private val ConvLabelInk = Color(0xFF8E8E93)
private val ConvPill = Color(0xFF333333)
private val ConvDivider = Color(0xFF2A2A2C)

/** the small up / down arrows next to the unit */
private val UpDownIcon = IconDef(listOf("M8 9.5l4-4 4 4", "M8 14.5l4 4 4-4"), 2f)

/** plus over minus with a slash (the +/− key of the temperature keypad) */
private val PlusMinusIcon = IconDef(listOf("M8 4.5v7", "M4.5 8h7", "M13 18.5h7", "M5.5 20L18.5 4"), 1.8f)

/** what the page remembers: the typed number, the two units and which row is being typed in */
private class ConverterState(val cat: ConvCategory) {
    var input by mutableStateOf("1")
    var topUnit by mutableStateOf(cat.defTop)
    var bottomUnit by mutableStateOf(cat.defBottom)
    var topActive by mutableStateOf(true)

    private fun unitOfRow(top: Boolean) = if (top) topUnit else bottomUnit

    fun textOf(top: Boolean): String {
        if (topActive == top) return fmtInput(input)
        val v = convertValue(cat, inputValue(input), unitOfRow(topActive), unitOfRow(top))
        return fmtResult(v)
    }

    /** tapping the other row: it becomes the one you type in, starting from its converted number */
    fun activate(top: Boolean) {
        if (topActive == top) return
        val v = convertValue(cat, inputValue(input), unitOfRow(topActive), unitOfRow(top))
        input = plainNumber(v)
        topActive = top
    }

    fun setUnit(top: Boolean, key: String) {
        if (top) topUnit = key else bottomUnit = key
    }

    fun press(key: String) {
        input = convPress(input, key)
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

/** one unit row: the unit button on the left, the number on the right with the unit name under it */
@Composable
private fun ConvRow(
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
    PressBox(
        onClick = onSelect,
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
                    style = qStylePx(fontSp, 300, if (active) ConvOrange else Color.White, lh = 1.15f, align = TextAlign.End),
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
private fun UnitChip(label: String, onClick: (Rect) -> Unit) {
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
private fun ConvKeypad(signed: Boolean, onKey: (String) -> Unit, modifier: Modifier) {
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
                DigitKey(".", Modifier.weight(1f).fillMaxHeight(), onKey)
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (signed) {
                // temperature: plain keys, one per row
                SideKey(Modifier.weight(1f).fillMaxWidth(), { onKey("AC") }) { AcText() }
                SideKey(Modifier.weight(1f).fillMaxWidth(), { onKey("⌫") }) { BackIcon() }
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
                ) { Pill({ onKey("⌫") }) { BackIcon() } }
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
private fun SideKey(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    PressBox(onClick = onClick, pressedScale = 0.88f, modifier = modifier) { content() }
}

/** the tall dark button (AC / backspace) */
@Composable
private fun Pill(onClick: () -> Unit, content: @Composable () -> Unit) {
    PressBox(
        onClick = onClick,
        pressedScale = 0.95f,
        modifier = Modifier
            .fillMaxSize()
            .background(ConvPill, RoundedCornerShape(percent = 50))
    ) { content() }
}

@Composable
private fun AcText() {
    BasicText(
        "AC",
        style = qStylePx(24f, 500, ConvOrange, lh = 1.2f, align = TextAlign.Center),
        maxLines = 1,
        softWrap = false
    )
}

/** the cross (backspace) icon of the main calculator */
@Composable
private fun BackIcon() {
    SvgIcon(Icons.Backspace, Q.BackGreen, Modifier.size(28.dp))
}
