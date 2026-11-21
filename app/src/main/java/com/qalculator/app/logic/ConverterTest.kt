package com.qalculator.app.logic

import org.junit.Assert.assertEquals
import org.junit.Test

/** Optional: `gradle testDebugUnitTest`. Checks the converter pages (the same numbers as the reference screenshots). */
class ConverterTest {
    private fun conv(c: ConvCategory, v: Double, a: String, b: String) = fmtResult(convertValue(c, v, a, b))

    @Test
    fun defaultPairs() {
        assertEquals("33.8", conv(Converters.Temperature, 1.0, "c", "f"))
        assertEquals("100", conv(Converters.Length, 1.0, "m", "cm"))
        assertEquals("10,000", conv(Converters.Area, 1.0, "m2", "cm2"))
        assertEquals("1,000,000", conv(Converters.Volume, 1.0, "m3", "cm3"))
        assertEquals("0.001", conv(Converters.Speed, 1.0, "ms", "kms"))
        assertEquals("60", conv(Converters.Time, 1.0, "min", "s"))
        assertEquals("1,000", conv(Converters.Mass, 1.0, "kg", "g"))
    }

    @Test
    fun temperatureBothWays() {
        assertEquals("212", conv(Converters.Temperature, 100.0, "c", "f"))
        assertEquals("-40", conv(Converters.Temperature, -40.0, "f", "c"))
        assertEquals("-273.15", conv(Converters.Temperature, 0.0, "k", "c"))
    }

    @Test
    fun keypad() {
        var s = "1"
        for (k in listOf("2", ".", "5", "⌫", "⌫")) s = convPress(s, k)
        assertEquals("1", s)
        assertEquals("0", convPress("5", "⌫"))
        assertEquals("-5", convPress("5", "±"))
        assertEquals("0", convPress("123", "AC"))
    }
}
