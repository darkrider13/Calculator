package com.qalculator.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.qalculator.app.ui.CalcController
import com.qalculator.app.ui.CalcScreen
import com.qalculator.app.ui.LocalCompact
import com.qalculator.app.ui.LocalViewportH
import com.qalculator.app.ui.PriceController
import com.qalculator.app.ui.PriceScreen
import com.qalculator.app.ui.SegItem
import com.qalculator.app.ui.TabBar
import com.qalculator.app.ui.LocalOverlays
import com.qalculator.app.ui.OverlayHost
import com.qalculator.app.ui.Overlays
import com.qalculator.app.ui.pageBackground

/** Root of the app. Later steps put the calculator / price screens here. */
@Composable
fun QalculatorApp(state: AppState) {
    val overlays = remember { Overlays() }
    val calcCtl = remember { CalcController(state) }
    // CSS px never follow the phone's font-size setting (the old WebView app ignored it too), so sp == dp here:
    // this keeps every fixed-height row from overlapping when the system font size is large
    val dens = LocalDensity.current
    val fixedDensity = remember(dens.density) { Density(dens.density, 1f) }
    CompositionLocalProvider(
        LocalOverlays provides overlays,
        LocalDensity provides fixedDensity
    ) {
        // the system bars area stays black; everything else lives inside it
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // @media (max-height: 640px)
                val compact = maxHeight <= 640.dp
                CompositionLocalProvider(
                    LocalCompact provides compact,
                    LocalViewportH provides maxHeight
                ) {
                    OverlayHost(overlays) {
                        // plain black page (no glow), no tab bar (price screen is hidden for now), max 460 wide
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            CalcScreen(
                                ctl = calcCtl,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxHeight()
                                    .widthIn(max = 460.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
