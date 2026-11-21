package com.qalculator.app.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/** What the confirm dialog shows (the original confirmBox) */
class ConfirmRequest(
    val title: String,
    val message: String,
    val yesLabel: String,
    val onYes: () -> Unit
)

/** The unit menu: where the button is on screen, the choices, the chosen one, what to do on pick */
class MenuRequest(
    val anchor: Rect,
    val options: List<Pair<String, String>>,
    val selected: String,
    val onPick: (String) -> Unit
)

/** One confirm dialog or one menu at a time, shown above the whole app. */
@Stable
class Overlays {
    var confirm by mutableStateOf<ConfirmRequest?>(null)
        private set
    var menu by mutableStateOf<MenuRequest?>(null)
        private set

    val anyOpen: Boolean get() = confirm != null || menu != null

    fun askConfirm(title: String, message: String, yesLabel: String, onYes: () -> Unit) {
        menu = null
        confirm = ConfirmRequest(title, message, yesLabel, onYes)
    }

    fun closeConfirm() {
        confirm = null
    }

    fun openMenu(anchor: Rect, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
        confirm = null
        menu = MenuRequest(anchor, options, selected, onPick)
    }

    fun closeMenu() {
        menu = null
    }

    /** back button: closes the open dialog or menu */
    fun dismiss() {
        confirm = null
        menu = null
    }
}

val LocalOverlays = staticCompositionLocalOf { Overlays() }

private val CssEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** blur behind glass works on Android 12+ (same look as backdrop-filter); older phones get a more solid glass */
private val canBlur: Boolean get() = Build.VERSION.SDK_INT >= 31

private val glassBg: Color
    get() = if (canBlur) rgba(40, 46, 60, 0.45f) else rgba(40, 46, 60, 0.94f)

/** soft black glass for the unit menus (blur behind on Android 12+, a more solid black on older phones) */
private val blackGlassBg: Color
    get() = if (canBlur) rgba(8, 8, 10, 0.62f) else rgba(12, 12, 14, 0.96f)

/** box-shadow: 0 12px 40px rgba(0,0,0,.45) around a rounded box (outside only) */
private fun Modifier.glassShadow(radius: Float): Modifier = this.drawBehind {
    if (Build.VERSION.SDK_INT < 28) return@drawBehind
    val r = radius * density
    val box = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
    }
    clipPath(box, clipOp = ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val p = android.graphics.Paint()
            p.isAntiAlias = true
            p.color = android.graphics.Color.BLACK
            p.setShadowLayer(24f * density, 0f, 12f * density, android.graphics.Color.argb(115, 0, 0, 0))
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
        }
    }
}

/** the glass box used by the dialog and the menu */
private fun Modifier.glass(radius: Float, bg: Color? = null): Modifier = this
    .glassShadow(radius)
    .drawBehind {
        val r = radius * density
        val bw = density
        drawRoundRect(color = bg ?: glassBg, cornerRadius = CornerRadius(r))
        drawRoundRect(
            color = Q.GlassBorder,
            topLeft = Offset(bw / 2f, bw / 2f),
            size = Size(size.width - bw, size.height - bw),
            cornerRadius = CornerRadius(r - bw / 2f),
            style = Stroke(width = bw)
        )
        drawTopHighlight(r, bw, Q.HlGlass)
    }

/**
 * Puts the app content and, above it, the confirm dialog / unit menu.
 * While one is open the content behind is blurred and darkened (backdrop-filter in the original).
 */
@Composable
fun OverlayHost(overlays: Overlays, content: @Composable () -> Unit) {
    val open = overlays.anyOpen
    val spec: AnimationSpec<androidx.compose.ui.unit.Dp> = if (open) tween(180, easing = CssEase) else snap()
    val blurDp by animateDpAsState(targetValue = if (open) 14.dp else 0.dp, animationSpec = spec, label = "backdropBlur")
    BackHandler(enabled = open) { overlays.dismiss() }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurDp > 0.dp) Modifier.blur(blurDp) else Modifier)
        ) {
            content()
        }
        val cfm = overlays.confirm
        if (cfm != null) ConfirmDialog(cfm, onClose = { overlays.closeConfirm() })
        val mn = overlays.menu
        if (mn != null) UnitMenu(mn, onClose = { overlays.closeMenu() })
    }
}

@Composable
private fun ConfirmDialog(req: ConfirmRequest, onClose: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(180, easing = CssEase)) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(200, easing = CssEase)) }
    val noSource = remember { MutableInteractionSource() }
    val boxSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Q.Scrim.copy(alpha = Q.Scrim.alpha * fade.value)) }
            .clickable(interactionSource = noSource, indication = null, onClick = onClose)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = pop.value
                    val s = 0.96f + 0.04f * pop.value
                    scaleX = s
                    scaleY = s
                }
                .glass(16f)
                .clickable(interactionSource = boxSource, indication = null, onClick = {})
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 12.dp)
        ) {
            BasicText(
                text = req.title,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                style = qStyle(1f, 700, Q.Ink, align = TextAlign.Center)
            )
            BasicText(
                text = req.message,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                style = qStyle(0.88f, 400, Q.Muted, lh = 1.5f, align = TextAlign.Center)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CfmButton(text = "No", yes = false, onClick = onClose)
                CfmButton(text = req.yesLabel, yes = true, onClick = {
                    onClose()
                    req.onYes()
                })
            }
        }
    }
}

@Composable
private fun CfmButton(text: String, yes: Boolean, onClick: () -> Unit) {
    PressBox(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .drawBehind {
                val r = 10.dp.toPx()
                if (yes) {
                    drawRoundRect(color = Q.BadBg, cornerRadius = CornerRadius(r))
                } else {
                    val bw = density
                    drawRoundRect(color = Q.Field, cornerRadius = CornerRadius(r))
                    drawRoundRect(
                        color = Q.Line,
                        topLeft = Offset(bw / 2f, bw / 2f),
                        size = Size(size.width - bw, size.height - bw),
                        cornerRadius = CornerRadius(r - bw / 2f),
                        style = Stroke(width = bw)
                    )
                }
            },
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        BasicText(
            text = text,
            style = qStyle(0.9f, 600, if (yes) Q.Bad else Q.Ink, align = TextAlign.Center)
        )
    }
}

@Composable
private fun UnitMenu(req: MenuRequest, onClose: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(180, easing = CssEase)) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(160, easing = CssEase)) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val bdSource = remember { MutableInteractionSource() }
    val menuSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .drawBehind { drawRect(Q.Scrim.copy(alpha = Q.Scrim.alpha * fade.value)) }
            .clickable(interactionSource = bdSource, indication = null, onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    // the menu opens in the middle of the screen
                    val minW = Math.min(220.dp.roundToPx(), constraints.maxWidth)
                    val maxW = Math.min(320.dp.roundToPx(), constraints.maxWidth - 48.dp.roundToPx())
                    val p = measurable.measure(
                        Constraints(
                            minWidth = minW,
                            maxWidth = Math.max(minW, maxW),
                            minHeight = 0,
                            maxHeight = constraints.maxHeight
                        )
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        p.place(
                            (constraints.maxWidth - p.width) / 2,
                            (constraints.maxHeight - p.height) / 2
                        )
                    }
                }
                .graphicsLayer {
                    alpha = pop.value
                    val s = 0.96f + 0.04f * pop.value
                    scaleX = s
                    scaleY = s
                }
                .glass(16f, blackGlassBg)
                .clickable(interactionSource = menuSource, indication = null, onClick = {})
                .width(IntrinsicSize.Max)
                // 4 rows are shown (4 x 44dp + 3 gaps of 2dp + 6dp padding top and bottom), the rest scrolls
                .heightIn(max = 194.dp)
                .padding(6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (opt in req.options) {
                val on = opt.first == req.selected
                PressBox(
                    onClick = {
                        onClose()
                        req.onPick(opt.first)
                    },
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .then(if (on) Modifier.background(Q.GlassSel, RoundedCornerShape(11.dp)) else Modifier),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BasicText(
                            text = opt.second,
                            modifier = Modifier.weight(1f),
                            style = qStylePx(15f, 600, Q.Ink, align = TextAlign.Left)
                        )
                        if (on) SvgIcon(Icons.Check, Q.Accent, Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
