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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/*
 * BMI page: age, gender, height (cm), weight (kg), a short note, and the green Calculate button.
 * The result (BMI number and category) is shown in green.
 */

private val ChipOff = Color(0xFF2C2C2E)
private val ChipOn = Color(0xFF1F3A25)

private fun bmiCategory(bmi: Double): String = when {
    bmi < 18.5 -> "Underweight"
    bmi < 25.0 -> "Normal weight"
    bmi < 30.0 -> "Overweight"
    else -> "Obese"
}

@Composable
fun BmiPage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val overlays = LocalOverlays.current
    BackHandler(enabled = !overlays.anyOpen) { onBack() }
    var age by remember { mutableStateOf("") }
    var male by remember { mutableStateOf(true) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bmiText by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("") }

    fun calculate() {
        val h = height.toDoubleOrNull()
        val w = weight.toDoubleOrNull()
        if (h == null || w == null || h <= 0.0 || w <= 0.0) {
            bmiText = null
            overlays.askConfirm("BMI", "Enter your height (cm) and weight (kg).", "OK") { }
            return
        }
        val m = h / 100.0
        val bmi = w / (m * m)
        bmiText = String.format(Locale.US, "%.1f", bmi)
        category = bmiCategory(bmi)
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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
        ) {
            BasicText(
                "BMI",
                modifier = Modifier.padding(top = 14.dp),
                style = qStylePx(34f, 300, ConvTitleInk, lh = 1.2f),
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) { BmiField("Age", age, { age = it }, false) }
                Spacer(Modifier.width(24.dp))
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        "Gender: " + if (male) "Male" else "Female",
                        style = qStylePx(14f, 300, ConvLabelInk, lh = 1.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenderChip("Male", male) { male = true }
                        GenderChip("Female", !male) { male = false }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            BmiField("Height (cm)", height, { height = it }, true)
            Spacer(Modifier.height(20.dp))
            BmiField("Weight (kg)", weight, { weight = it }, true)
            Spacer(Modifier.height(28.dp))
            val r = bmiText
            if (r != null) {
                BasicText("Your BMI", style = qStylePx(14f, 300, ConvLabelInk, lh = 1.3f))
                BasicText(r, style = qStylePx(48f, 300, Q.KeyGreen, lh = 1.15f), maxLines = 1)
                BasicText(category, style = qStylePx(18f, 400, Q.KeyGreen, lh = 1.3f), maxLines = 1)
                Spacer(Modifier.height(24.dp))
            }
            BasicText("About BMI", style = qStylePx(15f, 300, ConvLabelInk, lh = 1.3f))
            Spacer(Modifier.height(6.dp))
            BasicText(
                "Body mass index (BMI) is a person's weight in kilograms divided by the square of height in meters. " +
                    "BMI is an easy screening method for weight category.",
                style = qStylePx(15f, 300, ConvLabelInk, lh = 1.5f)
            )
            Spacer(Modifier.height(16.dp))
        }
        // the green Calculate button
        PressBox(
            onClick = { calculate() },
            pressedScale = 0.97f,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(Q.KeyEq, RoundedCornerShape(percent = 50))
        ) {
            BasicText("Calculate", style = qStylePx(20f, 500, Color.White, lh = 1.2f, align = TextAlign.Center))
        }
    }
}

@Composable
private fun GenderChip(label: String, on: Boolean, onClick: () -> Unit) {
    PressBox(
        onClick = onClick,
        pressedScale = 0.94f,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .background(if (on) ChipOn else ChipOff, RoundedCornerShape(12.dp)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
    ) {
        BasicText(label, style = qStylePx(14f, 500, if (on) Q.KeyGreen else ConvLabelInk, lh = 1.2f))
    }
}

/** a label over a number field with a thin line under it */
@Composable
private fun BmiField(label: String, value: String, onChange: (String) -> Unit, decimal: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(label, style = qStylePx(14f, 300, ConvLabelInk, lh = 1.3f))
        BasicTextField(
            value = value,
            onValueChange = { v ->
                val ok = if (decimal) v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' }
                else v.all { it.isDigit() }
                if (ok && v.length <= 7) onChange(v)
            },
            singleLine = true,
            textStyle = qStylePx(28f, 300, Color.White, lh = 1.2f),
            cursorBrush = SolidColor(Q.Cursor),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(top = 6.dp)
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ConvDivider))
    }
}
