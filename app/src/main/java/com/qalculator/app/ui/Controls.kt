package com.qalculator.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A tappable box without the Android ripple. Like CSS :active it can shrink a little while pressed
 * (pressedScale). content gets the pressed state.
 */
@Composable
fun PressBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = 1f,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .graphicsLayer {
                val s = if (pressed) pressedScale else 1f
                scaleX = s
                scaleY = s
            }
            .then(modifier)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content(pressed)
    }
}

/** label above a field: .72rem, 500, muted, one line with … */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier,
        style = qStyle(0.72f, 500, Q.Muted),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false
    )
}

/** .primary: the big light button (disabled = 45% visible and not tappable) */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val compact = LocalCompact.current
    PressBox(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 44.dp else 48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .panel(Q.Primary, 12.dp, border = null)
    ) {
        BasicText(
            text = text,
            style = qStyle(1f, 600, Q.AccentInk, align = TextAlign.Center)
        )
    }
}

/** the item shown in tabs / segments */
class SegItem(val key: String, val label: String)

/**
 * Shared look of the bottom tabs and the segmented switch:
 * a dark rounded bar with equal buttons; the selected one is lighter with a top highlight.
 */
@Composable
private fun SegBar(
    items: List<SegItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
    pad: Int,
    barRadius: Int,
    itemRadius: Int,
    minHeight: Int,
    fontRem: Float,
    itemPad: Int
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .panel(Q.Field2, barRadius.dp, border = null, highlight = Q.HlField)
            .padding(pad.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (item in items) {
            val on = item.key == selected
            PressBox(
                onClick = { onSelect(item.key) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = minHeight.dp)
                    .then(
                        if (on) Modifier.panel(Q.Selected, itemRadius.dp, border = null, highlight = Q.HlSelected)
                        else Modifier
                    ),
                contentPadding = PaddingValues(itemPad.dp)
            ) {
                BasicText(
                    text = item.label,
                    style = qStyle(fontRem, 600, if (on) Q.Ink else Q.Muted, align = TextAlign.Center)
                )
            }
        }
    }
}

/** the two bottom tabs (.tabs) */
@Composable
fun TabBar(items: List<SegItem>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val compact = LocalCompact.current
    SegBar(
        items, selected, onSelect, modifier,
        pad = 4, barRadius = 14, itemRadius = 10,
        minHeight = if (compact) 40 else 44, fontRem = 0.92f, itemPad = 6
    )
}

/** the two-way switch (.seg), for example পরিমাণ দিয়ে টাকা / টাকা দিয়ে পরিমাণ */
@Composable
fun SegSwitch(items: List<SegItem>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val compact = LocalCompact.current
    SegBar(
        items, selected, onSelect, modifier,
        pad = 3, barRadius = 12, itemRadius = 9,
        minHeight = if (compact) 32 else 38, fontRem = 0.88f, itemPad = 0
    )
}

/** how a text field / unit button looks: dark box, thin border, top highlight, focus ring inside */
fun Modifier.fieldBox(focused: Boolean = false, expanded: Boolean = false): Modifier = this.drawBehind {
    val r = 10.dp.toPx()
    val bw = density
    drawRoundRect(color = Q.Field, cornerRadius = CornerRadius(r))
    val edge = if (focused || expanded) Q.Accent else Q.Line
    drawRoundRect(
        color = edge,
        topLeft = Offset(bw / 2f, bw / 2f),
        size = Size(size.width - bw, size.height - bw),
        cornerRadius = CornerRadius(r - bw / 2f),
        style = Stroke(width = bw)
    )
    drawTopHighlight(r, bw, Q.HlField)
    if (focused) {
        // outline: 2px solid accent; outline-offset: -2px  (inside the edge)
        val rw = 2f * density
        drawRoundRect(
            color = Q.Accent,
            topLeft = Offset(rw / 2f, rw / 2f),
            size = Size(size.width - rw, size.height - rw),
            cornerRadius = CornerRadius(r - rw / 2f),
            style = Stroke(width = rw)
        )
    }
}

/**
 * The unit button (.pick): shows the chosen unit and an arrow. onClick gives the button's place on the
 * screen so the menu can open right below (or above) it.
 */
@Composable
fun PickButton(
    label: String,
    expanded: Boolean,
    onClick: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = LocalCompact.current
    val bounds = remember { arrayOf(Rect.Zero) }
    PressBox(
        onClick = { onClick(bounds[0]) },
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 38.dp else 44.dp)
            .onGloballyPositioned { bounds[0] = it.boundsInWindow() }
            .fieldBox(expanded = expanded),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicText(
                text = label,
                modifier = Modifier.weight(1f),
                style = qStyle(1f, 400, Q.Ink, align = TextAlign.Left),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SvgIcon(Icons.Chevron, Q.Muted, Modifier.size(16.dp))
        }
    }
}
