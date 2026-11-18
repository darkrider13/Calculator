package com.qalculator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/*
 * The convert page: only the list of options (3 per row), same style as the reference picture.
 * Opened by the ruler button under the display. Only "Price" opens a page (the price / dam hishab tab).
 */

private val ConvInk = Color(0xFFD4D4D8)

private class ConvertOption(val label: String, val icon: IconDef)

private val ConvertOptions = listOf(
    ConvertOption(
        "Currency",
        IconDef(listOf("M7 4l5 6 5-6", "M12 10v10", "M7 13.5h10", "M7 17h10"), 1.7f)
    ),
    ConvertOption("Length", Icons.Ruler),
    ConvertOption(
        "Area",
        IconDef(
            listOf(
                "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4z",
                "M8 16L16 8", "M8 16v-3.5", "M8 16h3.5", "M16 8v3.5", "M16 8h-3.5"
            ),
            1.7f
        )
    ),
    ConvertOption(
        "Volume",
        IconDef(
            listOf("M12 3l7.5 4.2v9.6L12 21l-7.5-4.2V7.2z", "M4.5 7.2L12 11.4l7.5-4.2", "M12 11.4V21"),
            1.7f
        )
    ),
    ConvertOption(
        "Temperature",
        IconDef(
            listOf("M10 14.2V5a2 2 0 0 1 4 0v9.2a4 4 0 1 1-4 0z", "M12 8.5v7.5", "M10.4 17.4a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0"),
            1.7f
        )
    ),
    ConvertOption(
        "Speed",
        IconDef(listOf("M6.3 18.7A8 8 0 1 1 17.7 18.7", "M12 13l3.6-3.6"), 1.7f)
    ),
    ConvertOption("Time", Icons.Clock),
    ConvertOption(
        "Mass",
        IconDef(
            listOf(
                "M9.5 6.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0",
                "M8.2 9h7.6l3.2 10a1 1 0 0 1-1 1.3H6a1 1 0 0 1-1-1.3z"
            ),
            1.7f
        )
    ),
    ConvertOption(
        "Numeral system",
        IconDef(
            listOf(
                "M4.5 12a7.5 7.5 0 0 1 12.8-5.3", "M17.5 3.5v3.5h-3.5",
                "M19.5 12a7.5 7.5 0 0 1-12.8 5.3", "M6.5 20.5V17H10",
                "M8.2 12a1.4 2.2 0 1 0 2.8 0a1.4 2.2 0 1 0-2.8 0",
                "M13.3 10.3l1.5-1.2v5.8"
            ),
            1.6f
        )
    ),
    ConvertOption(
        "BMI",
        IconDef(
            listOf(
                "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4z",
                "M7.5 14a4.5 4.5 0 0 1 9 0", "M12 14l-1.8-2.6"
            ),
            1.7f
        )
    ),
    // price tag logo: opens the price (dam hishab) page
    ConvertOption(
        "Price",
        IconDef(
            listOf(
                "M20.6 13.4l-7.2 7.2a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8z",
                "M7.5 7.5h.01"
            ),
            1.7f
        )
    )
)

@Composable
fun ConvertScreen(
    onBack: () -> Unit,
    onPrice: () -> Unit,
    backEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // phone back button / gesture: back to the calculator (off while the price page is open on top)
    BackHandler(enabled = backEnabled) { onBack() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Q.Black)
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 24.dp)
    ) {
        for (rowItems in ConvertOptions.chunked(3)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (opt in rowItems) {
                    Box(modifier = Modifier.weight(1f)) {
                        ConvertCell(opt) { if (opt.label == "Price") onPrice() }
                    }
                }
                for (i in 0 until 3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ConvertCell(opt: ConvertOption, onClick: () -> Unit) {
    PressBox(
        onClick = onClick,
        pressedScale = 0.94f,
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SvgIcon(opt.icon, ConvInk, Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(14.dp))
            BasicText(
                opt.label,
                style = qStylePx(14f, 300, ConvInk, 1.2f, align = TextAlign.Center)
            )
        }
    }
}
