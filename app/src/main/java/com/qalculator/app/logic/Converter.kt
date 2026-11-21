package com.qalculator.app.logic

import java.math.BigDecimal
import java.math.MathContext

/*
 * The unit converters (Temperature, Length, Area, Volume, Speed, Time, Mass).
 * Pure logic, no UI. Every unit has a factor to the base unit of its group (value * factor = base value),
 * so any unit converts to any other: value * from.factor / to.factor.
 * Temperature is not a plain factor (it has an offset), so it has its own formulas.
 */

class ConvUnit(val key: String, val symbol: String, val name: String, val factor: Double)

class ConvCategory(
    val id: String,
    val title: String,
    val units: List<ConvUnit>,
    val defTop: String,
    val defBottom: String,
    /** temperature: the keypad gets a +/− key (values can be below zero) */
    val signed: Boolean = false
) {
    fun unit(key: String): ConvUnit = units.firstOrNull { it.key == key } ?: units.first()

    /** the unit menu list: key + label */
    val menuOptions: List<Pair<String, String>> get() = units.map { it.key to (it.symbol + "  –  " + it.name) }
}

object Converters {
    val Temperature = ConvCategory(
        "temperature", "Temperature converter",
        listOf(
            ConvUnit("c", "°C", "Celsius", 1.0),
            ConvUnit("f", "°F", "Fahrenheit", 1.0),
            ConvUnit("k", "K", "Kelvin", 1.0)
        ),
        "c", "f", signed = true
    )

    // base: meter
    val Length = ConvCategory(
        "length", "Length converter",
        listOf(
            ConvUnit("km", "km", "Kilometer", 1000.0),
            ConvUnit("m", "m", "Meter", 1.0),
            ConvUnit("dm", "dm", "Decimeter", 0.1),
            ConvUnit("cm", "cm", "Centimeter", 0.01),
            ConvUnit("mm", "mm", "Millimeter", 0.001),
            ConvUnit("um", "µm", "Micrometer", 1e-6),
            ConvUnit("nm", "nm", "Nanometer", 1e-9),
            ConvUnit("mi", "mi", "Mile", 1609.344),
            ConvUnit("yd", "yd", "Yard", 0.9144),
            ConvUnit("ft", "ft", "Foot", 0.3048),
            ConvUnit("in", "in", "Inch", 0.0254),
            ConvUnit("nmi", "nmi", "Nautical mile", 1852.0)
        ),
        "m", "cm"
    )

    // base: square meter
    val Area = ConvCategory(
        "area", "Area converter",
        listOf(
            ConvUnit("km2", "km²", "Square kilometer", 1e6),
            ConvUnit("ha", "ha", "Hectare", 1e4),
            ConvUnit("m2", "m²", "Square meter", 1.0),
            ConvUnit("dm2", "dm²", "Square decimeter", 0.01),
            ConvUnit("cm2", "cm²", "Square centimeter", 1e-4),
            ConvUnit("mm2", "mm²", "Square millimeter", 1e-6),
            ConvUnit("mi2", "mi²", "Square mile", 2589988.110336),
            ConvUnit("ac", "ac", "Acre", 4046.8564224),
            ConvUnit("yd2", "yd²", "Square yard", 0.83612736),
            ConvUnit("ft2", "ft²", "Square foot", 0.09290304),
            ConvUnit("in2", "in²", "Square inch", 0.00064516)
        ),
        "m2", "cm2"
    )

    // base: cubic meter
    val Volume = ConvCategory(
        "volume", "Volume converter",
        listOf(
            ConvUnit("m3", "m³", "Cubic meter", 1.0),
            ConvUnit("l", "L", "Liter", 1e-3),
            ConvUnit("ml", "mL", "Milliliter", 1e-6),
            ConvUnit("cm3", "cm³", "Cubic centimeter", 1e-6),
            ConvUnit("mm3", "mm³", "Cubic millimeter", 1e-9),
            ConvUnit("gal", "gal", "Gallon (US)", 0.003785411784),
            ConvUnit("qt", "qt", "Quart (US)", 0.000946352946),
            ConvUnit("pt", "pt", "Pint (US)", 0.000473176473),
            ConvUnit("cup", "cup", "Cup (US)", 0.0002365882365),
            ConvUnit("floz", "fl oz", "Fluid ounce (US)", 2.95735295625e-5),
            ConvUnit("ft3", "ft³", "Cubic foot", 0.028316846592),
            ConvUnit("in3", "in³", "Cubic inch", 1.6387064e-5)
        ),
        "m3", "cm3"
    )

    // base: meter per second
    val Speed = ConvCategory(
        "speed", "Speed converter",
        listOf(
            ConvUnit("ms", "m/s", "Meter per second", 1.0),
            ConvUnit("kms", "km/s", "Kilometer per second", 1000.0),
            ConvUnit("kmh", "km/h", "Kilometer per hour", 1000.0 / 3600.0),
            ConvUnit("mph", "mph", "Mile per hour", 0.44704),
            ConvUnit("kn", "kn", "Knot", 1852.0 / 3600.0),
            ConvUnit("fts", "ft/s", "Foot per second", 0.3048)
        ),
        "ms", "kms"
    )

    // base: second (month = 30 days, year = 365 days)
    val Time = ConvCategory(
        "time", "Time converter",
        listOf(
            ConvUnit("ns", "ns", "Nanosecond", 1e-9),
            ConvUnit("us", "µs", "Microsecond", 1e-6),
            ConvUnit("msec", "ms", "Millisecond", 1e-3),
            ConvUnit("s", "s", "Second", 1.0),
            ConvUnit("min", "min", "Minute", 60.0),
            ConvUnit("h", "h", "Hour", 3600.0),
            ConvUnit("d", "d", "Day", 86400.0),
            ConvUnit("wk", "wk", "Week", 604800.0),
            ConvUnit("mo", "mo", "Month (30 days)", 2592000.0),
            ConvUnit("yr", "yr", "Year (365 days)", 31536000.0)
        ),
        "min", "s"
    )

    // base: kilogram
    val Mass = ConvCategory(
        "mass", "Mass converter",
        listOf(
            ConvUnit("t", "t", "Tonne", 1000.0),
            ConvUnit("kg", "kg", "Kilogram", 1.0),
            ConvUnit("g", "g", "Gram", 1e-3),
            ConvUnit("mg", "mg", "Milligram", 1e-6),
            ConvUnit("ug", "µg", "Microgram", 1e-9),
            ConvUnit("lb", "lb", "Pound", 0.45359237),
            ConvUnit("oz", "oz", "Ounce", 0.028349523125)
        ),
        "kg", "g"
    )

    /** the convert page option label -> converter */
    fun byLabel(label: String): ConvCategory? = when (label) {
        "Temperature" -> Temperature
        "Length" -> Length
        "Area" -> Area
        "Volume" -> Volume
        "Speed" -> Speed
        "Time" -> Time
        "Mass" -> Mass
        else -> null
    }
}

/** Converts [v] from unit [from] to unit [to] of the same category. */
fun convertValue(cat: ConvCategory, v: Double, from: String, to: String): Double {
    if (cat.id == "temperature") {
        val c = when (from) {
            "f" -> (v - 32.0) * 5.0 / 9.0
            "k" -> v - 273.15
            else -> v
        }
        return when (to) {
            "f" -> c * 9.0 / 5.0 + 32.0
            "k" -> c + 273.15
            else -> c
        }
    }
    return v * cat.unit(from).factor / cat.unit(to).factor
}

/** how many digits the keypad accepts in one number */
const val CONV_MAX_DIGITS = 12

/**
 * One key press on the converter keypad. [cur] is the typed text ("1", "12.", "-5"), the result is the new text.
 * Keys: 0-9, ".", "AC", "⌫", "±".
 */
fun convPress(cur: String, key: String): String {
    val neg = cur.startsWith("-")
    val body = if (neg) cur.substring(1) else cur
    return when (key) {
        "AC" -> "0"
        "⌫" -> {
            val t = cur.dropLast(1)
            if (t == "" || t == "-") "0" else t
        }
        "±" -> if (neg) body else "-$cur"
        "." -> if (body.contains('.')) cur else if (body == "") (if (neg) "-0." else "0.") else "$cur."
        else -> {
            // a digit
            if (body == "0") (if (neg) "-" else "") + key
            else if (body.count { it.isDigit() } >= CONV_MAX_DIGITS) cur
            else cur + key
        }
    }
}

private fun groupThousands(intPart: String): String {
    val sb = StringBuilder()
    for ((i, c) in intPart.withIndex()) {
        if (i > 0 && (intPart.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return sb.toString()
}

/** The text being typed, shown with thousands commas: "1234.5" -> "1,234.5", "12." stays "12.". */
fun fmtInput(s: String): String {
    val neg = s.startsWith("-")
    val body = if (neg) s.substring(1) else s
    val dot = body.indexOf('.')
    val ip = if (dot >= 0) body.substring(0, dot) else body
    val rest = if (dot >= 0) body.substring(dot) else ""
    return (if (neg) "-" else "") + groupThousands(ip) + rest
}

private fun rounded(x: Double): BigDecimal = BigDecimal(x).round(MathContext(12)).stripTrailingZeros()

/** The plain number (no commas) for [x], 12 significant digits, so floating point noise is gone. Used for the typed text. */
fun plainNumber(x: Double): String {
    if (!x.isFinite() || x == 0.0) return "0"
    val s = rounded(x).toPlainString()
    return if (s == "-0") "0" else s
}

/** A converted result as shown: thousands commas, no floating point noise, very big/small ones as 1E-12. */
fun fmtResult(x: Double): String {
    if (!x.isFinite()) return "—"
    if (x == 0.0) return "0"
    val a = Math.abs(x)
    if (a >= 1e15 || a < 1e-9) {
        val bd = BigDecimal(x).round(MathContext(7)).stripTrailingZeros()
        val digits = bd.unscaledValue().abs().toString()
        val exp = digits.length - 1 - bd.scale()
        val mant = digits.substring(0, 1) + (if (digits.length > 1) "." + digits.substring(1) else "")
        return (if (x < 0) "-" else "") + mant + "E" + exp
    }
    return fmtInput(plainNumber(x))
}

/** The typed text as a number (NaN if it is not one). */
fun inputValue(s: String): Double = s.toDoubleOrNull() ?: Double.NaN
