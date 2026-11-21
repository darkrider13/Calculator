package com.qalculator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qalculator.app.logic.ConvUnit
import com.qalculator.app.logic.fmtInput
import java.math.BigInteger

/*
 * Numeral system page: two rows (BIN / OCT / DEC / HEX, pick with the arrows), the keypad with the
 * letters A-F. The row you type in is white, the other one shows the converted number in green.
 * AC is green and backspace is red, like on every converter page. Whole numbers only.
 */

private val BaseUnits = listOf(
    ConvUnit("2", "BIN", "Binary", 1.0),
    ConvUnit("8", "OCT", "Octal", 1.0),
    ConvUnit("10", "DEC", "Decimal", 1.0),
    ConvUnit("16", "HEX", "Hexadecimal", 1.0)
)

private val BaseMenu = BaseUnits.map { it.key to (it.symbol + "  –  " + it.name) }

private val KeyOff = Color(0xFF3A3A3C)

private const val MaxDigits = 20

private class NumeralState {
    var input by mutableStateOf("0")
    var topBase by mutableStateOf(10)
    var bottomBase by mutableStateOf(2)
    var topActive by mutableStateOf(true)

    private fun baseOf(top: Boolean) = if (top) topBase else bottomBase
    private val activeBase get() = baseOf(topActive)

    private fun value(): BigInteger = try {
        BigInteger(input, activeBase)
    } catch (e: NumberFormatException) {
        BigInteger.ZERO
    }

    fun textOf(top: Boolean): String {
        val base = baseOf(top)
        val s = value().toString(base).uppercase()
        return if (base == 10) fmtInput(s) else s
    }

    fun activate(top: Boolean) {
        if (topActive == top) return
        input = value().toString(baseOf(top)).uppercase()
        topActive = top
    }

    fun setBase(top: Boolean, base: Int) {
        if (top == topActive) {
            input = value().toString(base).uppercase()
        }
        if (top) topBase = base else bottomBase = base
    }

    fun validDigit(k: String): Boolean = Character.digit(k[0], 16) in 0 until activeBase

    fun press(k: String) {
        when (k) {
            "AC" -> input = "0"
            "⌫" -> {
                val t = input.dropLast(1)
                input = if (t.isEmpty()) "0" else t
            }
            else -> {
                if (!validDigit(k)) return
                if (input.length >= MaxDigits) return
                input = if (input == "0") k else input + k
            }
        }
    }
}

@Composable
fun NumeralPage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    BackHandler(enabled = !overlays.anyOpen) { onBack() }
    val st = remember { NumeralState() }

    fun unitOf(base: Int) = BaseUnits.first { it.key == base.toString() }

    fun pickBase(top: Boolean, anchor: Rect) {
        val cur = if (top) st.topBase else st.bottomBase
        overlays.openMenu(anchor, BaseMenu, cur.toString()) { st.setBase(top, it.toInt()) }
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
            PressBox(onClick = onBack, pressedScale = 0.92f, modifier = Modifier.size(48.dp)) {
                SvgIcon(Icons.Back, ConvTitleInk, Modifier.size(24.dp))
            }
        }
        BasicText(
            "Numeral system",
            modifier = Modifier.padding(start = 26.dp, end = 16.dp, top = 14.dp),
            style = qStylePx(34f, 300, ConvTitleInk, lh = 1.2f),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ConvRow(
                unit = unitOf(st.topBase),
                text = st.textOf(true),
                active = st.topActive,
                onSelect = { st.activate(true) },
                onUnit = { pickBase(true, it) },
                modifier = Modifier.weight(1f)
            )
            ConvRow(
                unit = unitOf(st.bottomBase),
                text = st.textOf(false),
                active = !st.topActive,
                onSelect = { st.activate(false) },
                onUnit = { pickBase(false, it) },
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
        Column(
            modifier = Modifier
                .weight(1.6f)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // row 1: AC (green), backspace (red; hold = clear all), F, E
            val haptic = LocalHapticFeedback.current
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                PressBox(onClick = { st.press("AC") }, pressedScale = 0.88f, modifier = Modifier.weight(1f).fillMaxHeight()) { AcText() }
                PressBox(
                    onClick = { st.press("⌫") },
                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); st.press("AC") },
                    pressedScale = 0.88f,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) { BackIcon() }
                NumKey("F", st, Modifier.weight(1f).fillMaxHeight())
                NumKey("E", st, Modifier.weight(1f).fillMaxHeight())
            }
            for (row in listOf(listOf("7", "8", "9", "D"), listOf("4", "5", "6", "C"), listOf("1", "2", "3", "B"))) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (k in row) NumKey(k, st, Modifier.weight(1f).fillMaxHeight())
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                NumKey("0", st, Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.weight(1f))
                NumKey("A", st, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/** a digit / letter key: dim and not clickable when it is not valid in the base being typed */
@Composable
private fun NumKey(k: String, st: NumeralState, modifier: Modifier) {
    val ok = st.validDigit(k)
    PressBox(
        onClick = { st.press(k) },
        enabled = ok,
        pressedScale = 0.88f,
        modifier = modifier
    ) {
        BasicText(
            k,
            style = qStylePx(38f, 300, if (ok) Color.White else KeyOff, lh = 1.2f, align = TextAlign.Center),
            maxLines = 1,
            softWrap = false
        )
    }
}
