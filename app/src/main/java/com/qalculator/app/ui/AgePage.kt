package com.qalculator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.qalculator.app.logic.AgeResult
import com.qalculator.app.logic.ageOn
import com.qalculator.app.logic.fmtInput
import com.qalculator.app.logic.parseDateDigits
import com.qalculator.app.logic.todayDigits

/*
 * Age calculator page, same look as the converters: two date rows (date of birth, age at - today by default),
 * the keypad with the tall AC / backspace buttons, and the result in green.
 */

private val DateDim = Color(0xFF4A4A4C)

/** 05-11-1998 while typing: the typed digits in white, the missing ones as DD / MM / YYYY in dim gray */
private fun dateText(d: String, typed: Color) = buildAnnotatedString {
    var idx = 0
    for (c in "DD-MM-YYYY") {
        if (c == '-') {
            withStyle(SpanStyle(color = if (idx > 0 && idx <= d.length) typed else DateDim)) { append('-') }
        } else {
            if (idx < d.length) withStyle(SpanStyle(color = typed)) { append(d[idx]) }
            else withStyle(SpanStyle(color = DateDim)) { append(c) }
            idx++
        }
    }
}

/** typing check: day 01-31 and month 01-12 only, a digit that would make them invalid is not accepted */
private fun dateTypingOk(d: String): Boolean {
    if (d.isNotEmpty() && d[0] > '3') return false
    if (d.length >= 2) {
        val day = d.substring(0, 2).toInt()
        if (day < 1 || day > 31) return false
    }
    if (d.length >= 3 && d[2] > '1') return false
    if (d.length >= 4) {
        val month = d.substring(2, 4).toInt()
        if (month < 1 || month > 12) return false
    }
    return true
}

private fun plural(n: Int, one: String) = "$n $one" + if (n == 1) "" else "s"

@Composable
fun AgePage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    BackHandler(enabled = !overlays.anyOpen) { onBack() }
    var dob by remember { mutableStateOf("") }
    var asOf by remember { mutableStateOf(todayDigits()) }
    var dobActive by remember { mutableStateOf(true) }

    fun press(k: String) {
        val cur = if (dobActive) dob else asOf
        val n = when (k) {
            "AC" -> ""
            "⌫" -> cur.dropLast(1)
            else -> if (cur.length < 8 && k[0].isDigit() && dateTypingOk(cur + k)) cur + k else cur
        }
        if (dobActive) dob = n else asOf = n
    }

    val dobDate = parseDateDigits(dob)
    val onDate = parseDateDigits(asOf)
    val result: AgeResult? = if (dobDate != null && onDate != null) ageOn(dobDate, onDate) else null

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
            "Age calculator",
            modifier = Modifier.padding(start = 26.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            style = qStylePx(34f, 300, ConvTitleInk, lh = 1.2f),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Column(modifier = Modifier.weight(1.7f).fillMaxWidth()) {
            AgeDateRow(
                title = "Born", sub = "Date of birth", subAction = null,
                digits = dob, active = dobActive, onSelect = { dobActive = true },
                modifier = Modifier.heightIn(min = 72.dp).weight(0.5f)
            )
            AgeDateRow(
                title = "Age at", sub = "Today", subAction = { asOf = todayDigits() },
                digits = asOf, active = !dobActive, onSelect = { dobActive = false },
                modifier = Modifier.heightIn(min = 72.dp).weight(0.5f)
            )
            val resScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .scrollFade(resScroll)
                    .verticalScroll(resScroll)
                    .padding(horizontal = 26.dp, vertical = 8.dp)
            ) {
                AgeResultView(dob.length == 8 && dobDate == null, asOf.length == 8 && onDate == null, dobDate != null, onDate != null, result)
            }
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
                .weight(1.25f)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            dot = false
        )
    }
}

/** one date row: the label on the left ("Today" is a button on the second row), the typed date on the right */
@Composable
private fun AgeDateRow(
    title: String,
    sub: String,
    subAction: (() -> Unit)?,
    digits: String,
    active: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier
) {
    val copy = rememberCopier()
    PressBox(
        onClick = onSelect,
        onLongClick = { if (digits.length == 8) copy(dateText(digits, Color.White).text) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .then(if (active) Modifier.background(Q.AccentSoft, RoundedCornerShape(18.dp)) else Modifier),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                BasicText(title, style = qStylePx(22f, 500, Color(0xFFF2F2F2), lh = 1.2f), maxLines = 1, softWrap = false)
                if (subAction != null) {
                    PressBox(onClick = subAction, pressedScale = 0.95f, modifier = Modifier.heightIn(min = 28.dp)) {
                        BasicText(sub, style = qStylePx(14f, 600, Q.KeyGreen, lh = 1.2f), maxLines = 1, softWrap = false)
                    }
                } else {
                    BasicText(sub, style = qStylePx(13f, 300, ConvLabelInk, lh = 1.4f), maxLines = 1, softWrap = false)
                }
            }
            Spacer(Modifier.width(8.dp))
            BasicText(
                dateText(digits, if (active) Color.White else Color(0xFFB4B4B9)),
                modifier = Modifier.weight(1f),
                style = qStylePx(32f, 300, Color.White, lh = 1.15f, align = TextAlign.End),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun AgeResultView(dobBad: Boolean, onBad: Boolean, dobOk: Boolean, onOk: Boolean, r: AgeResult?) {
    if (r == null) {
        val msg = when {
            dobBad -> "Date of birth is not a real date."
            onBad -> "The \"Age at\" date is not a real date."
            dobOk && onOk -> "Date of birth is after the \"Age at\" date."
            else -> "Type your date of birth (day, month, year) to see your age."
        }
        val bad = dobBad || onBad || (dobOk && onOk)
        BasicText(msg, style = qStylePx(15f, 300, if (bad) Q.Bad else ConvLabelInk, lh = 1.5f))
        return
    }
    BasicText("Age", style = qStylePx(13f, 300, ConvLabelInk, lh = 1.3f))
    val ageText = plural(r.years, "year") + "  " + plural(r.months, "month") + "  " + plural(r.days, "day")
    val copy = rememberCopier()
    PressBox(
        onClick = { },
        onLongClick = { copy(ageText) },
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicText(ageText, style = qStylePx(28f, 300, Q.KeyGreen, lh = 1.3f))
    }
    Spacer(Modifier.height(10.dp))
    val next = if (r.birthdayToday) "Today (${r.nextWeekday})"
    else "in " + plural(r.nextMonths, "month") + " " + plural(r.nextDays, "day") + " (${r.nextWeekday})"
    val weeks = r.totalDays / 7
    val restDays = (r.totalDays % 7).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatLine("Next birthday", next)
        StatLine("Born on", r.bornWeekday)
        StatLine("Total months", fmtInput(r.totalMonths.toString()))
        StatLine("Total weeks", fmtInput(weeks.toString()) + " weeks " + plural(restDays, "day"))
        StatLine("Total days", fmtInput(r.totalDays.toString()))
        StatLine("Total hours", fmtInput((r.totalDays * 24).toString()))
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(label, style = qStylePx(14f, 300, ConvLabelInk, lh = 1.3f), maxLines = 1, softWrap = false)
        BasicText(
            value,
            modifier = Modifier.weight(1f),
            style = qStylePx(16f, 500, Q.KeyGreen, lh = 1.3f, align = TextAlign.End)
        )
    }
}


/** fades the top / bottom edge of a scrolling area while there is more content in that direction */
private fun Modifier.scrollFade(state: ScrollState, fade: androidx.compose.ui.unit.Dp = 28.dp): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val h = fade.toPx().coerceAtMost(size.height / 2f)
            if (state.canScrollBackward) {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = h),
                    size = androidx.compose.ui.geometry.Size(size.width, h),
                    blendMode = BlendMode.DstIn
                )
            }
            if (state.canScrollForward) {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - h, endY = size.height),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h),
                    size = androidx.compose.ui.geometry.Size(size.width, h),
                    blendMode = BlendMode.DstIn
                )
            }
        }
