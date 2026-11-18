package com.qalculator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The price (dam hishab) tab, opened from the "Price" option of the convert page, with a back arrow on top. */
@Composable
fun PricePage(ctl: PriceController, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    // back: an open unit menu closes first (its own handler), then this page closes
    BackHandler(enabled = !overlays.anyOpen) { onBack() }
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
                SvgIcon(Icons.Back, Q.IconGray, Modifier.size(24.dp))
            }
        }
        PriceScreen(
            ctl = ctl,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
        )
    }
}
