package com.qalculator.app.logic

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/*
 * Number helpers. Everything here copies what JavaScript did in the original code
 * (Math.round, parseFloat, String(number), Intl.NumberFormat) so results are identical.
 * Note: [0-9] is used instead of \d on purpose (always plain ASCII digits).
 */

const val BN = "০১২৩৪৫৬৭৮৯"

/** JavaScript Math.round: halves go up, -0 kept */
fun jsRound(x: Double): Double {
    val f = Math.floor(x)
    val r = if (x - f >= 0.5) f + 1.0 else f
    return if (r == 0.0 && (x < 0.0 || (x == 0.0 && 1.0 / x < 0.0))) -0.0 else r
}

fun r2(n: Double): Double = jsRound((n + 1e-9) * 100.0) / 100.0
fun r1(n: Double): Double = jsRound((n + 1e-9) * 10.0) / 10.0
fun round8(v: Double): Double = jsRound(v * 1e8) / 1e8

fun toAscii(s: String): String {
    val sb = StringBuilder()
    for (c in s) {
        val i = BN.indexOf(c)
        if (i >= 0) sb.append('0' + i)
        else if (c == ',') { /* removed */ }
        else if (c == '।') sb.append('.')
        else sb.append(c)
    }
    return sb.toString().trim()
}

private val PARSE_RE = Regex("^([0-9]+\\.?[0-9]*|\\.[0-9]+)\\z")

/** parse(): only plain numbers (Bengali digits ok); anything else is NaN */
fun parse(s: String): Double {
    val t = toAscii(s)
    return if (PARSE_RE.matches(t)) t.toDouble() else Double.NaN
}

private val PF_RE = Regex("^[+-]?([0-9]+\\.?[0-9]*|\\.[0-9]+)([eE][+-]?[0-9]+)?")

/** JavaScript parseFloat: reads the longest number at the start, NaN if none */
fun jsParseFloat(s: String): Double {
    val t = s.trimStart()
    val m = PF_RE.find(t) ?: return Double.NaN
    return m.value.toDouble()
}

/**
 * The shortest decimal that reads back as exactly this number (what JavaScript and Intl use).
 * Done with exact BigDecimal rounding, so it does not depend on how the phone's Double.toString behaves.
 * a must be finite and not negative.
 */
fun shortestDecimal(a: Double): BigDecimal {
    if (a == 0.0) return BigDecimal.ZERO
    val exact = BigDecimal(a)
    for (p in 1..17) {
        val r = exact.round(MathContext(p, RoundingMode.HALF_EVEN))
        if (r.toDouble() == a) return r
    }
    return exact
}

/** JavaScript String(number) */
fun jsNumStr(d: Double): String {
    if (d.isNaN()) return "NaN"
    if (d == 0.0) return "0"
    if (d.isInfinite()) return if (d < 0) "-Infinity" else "Infinity"
    if (d < 0) return "-" + jsNumStr(-d)
    val bd = shortestDecimal(d).stripTrailingZeros()
    val digits = bd.unscaledValue().toString()
    val k = digits.length
    val n = k - bd.scale()
    return when {
        n in k..21 -> digits + "0".repeat(n - k)
        n in 1..21 -> digits.substring(0, n) + "." + digits.substring(n)
        n > -6 && n <= 0 -> "0." + "0".repeat(-n) + digits
        else -> {
            val e = n - 1
            val sign = if (e < 0) "-" else "+"
            val ae = Math.abs(e)
            if (k == 1) digits + "e" + sign + ae
            else digits.substring(0, 1) + "." + digits.substring(1) + "e" + sign + ae
        }
    }
}

/**
 * Intl.NumberFormat copy. indian = true groups like 12,34,567 (bn-BD and en-IN), false = no grouping.
 * bengali = true uses ০-৯. Rounds half up on the shortest decimal form of the number (like Intl).
 */
fun formatNumber(x: Double, minFrac: Int, maxFrac: Int, indian: Boolean, bengali: Boolean): String {
    if (x.isNaN()) return "NaN"
    if (x.isInfinite()) return if (x < 0) "-∞" else "∞"
    val neg = x < 0.0 || (x == 0.0 && 1.0 / x < 0.0)
    val dec = shortestDecimal(Math.abs(x)).setScale(maxFrac, RoundingMode.HALF_UP)
    val plain = dec.toPlainString()
    val dot = plain.indexOf('.')
    var intPart = if (dot >= 0) plain.substring(0, dot) else plain
    var frac = if (dot >= 0) plain.substring(dot + 1) else ""
    frac = frac.trimEnd('0')
    while (frac.length < minFrac) frac += "0"
    if (indian && intPart.length > 3) {
        val tail = intPart.substring(intPart.length - 3)
        var rest = intPart.substring(0, intPart.length - 3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.substring(rest.length - 2))
            rest = rest.substring(0, rest.length - 2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        intPart = parts.joinToString(",") + "," + tail
    }
    val out = (if (neg) "-" else "") + intPart + (if (frac.isNotEmpty()) ".$frac" else "")
    return if (bengali) bnDigits(out) else out
}

fun bnDigits(s: String): String {
    val sb = StringBuilder()
    for (c in s) {
        if (c in '0'..'9') sb.append(BN[c - '0']) else sb.append(c)
    }
    return sb.toString()
}

fun enDigits(s: String): String {
    val sb = StringBuilder()
    for (c in s) {
        val i = BN.indexOf(c)
        if (i >= 0) sb.append('0' + i) else sb.append(c)
    }
    return sb.toString()
}

/** money(): Bengali digits, whole numbers without decimals, otherwise 2 decimals */
fun money(n: Double): String {
    if (!n.isFinite()) return "—"
    val r = r2(n)
    val isInt = r.isFinite() && Math.floor(r) == r
    return if (isInt) formatNumber(r, 0, 0, true, true) else formatNumber(r, 2, 2, true, true)
}

/** numf(): Bengali digits, up to d decimals */
fun numf(n: Double, d: Int): String = if (n.isFinite()) formatNumber(n, 0, d, true, true) else "—"

/** numE(): English digits, Indian grouping, up to d decimals */
fun numE(n: Double, d: Int): String = if (n.isFinite()) formatNumber(n, 0, d, true, false) else "—"

/** rawNum(): plain number for copying (no commas, up to 6 decimals) */
fun rawNum(v: Double): String = if (v.isFinite()) formatNumber(v, 0, 6, false, false) else ""

/** the expression as shown on screen: × ÷ − with spaces around operators */
fun dispExpr(s: String): String {
    var t = s.replace("ʳ", "").replace(Regex("a(sin|cos|tan)\\("), "\$1⁻¹(")
    t = t.replace("*", "×").replace("/", "÷").replace("-", "−")
    t = Regex("([+×÷−])").replace(t, " \$1 ")
    t = Regex("[\\s\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]+").replace(t, " ")
    return t.replace("( − ", "(−").replace("^ − ", "^−").trim()
}

/** 1000 or more gram/ml shows as kg/litre, less stays gram/ml */
fun baseParts(g: Double, d: Char): Pair<String, String> {
    val b = baseOf(d)
    return if (g >= 1000) Pair(numf(g / 1000, 3), b.big) else Pair(numf(r1(g), 1), b.n)
}

fun qty(v: Double, u: String?): String = numf(v, 3) + " " + unitOf(u).n

val BN_MON = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে")

/** e.g. "১৬ নভে, ০৭:৫৪ পিএম" (en = true: English digits) */
fun whenLabel(ts: Long, en: Boolean = false): String {
    val c = java.util.GregorianCalendar()
    c.timeInMillis = ts
    var h = c.get(java.util.Calendar.HOUR_OF_DAY)
    val m = c.get(java.util.Calendar.MINUTE)
    val ap = if (h >= 12) "পিএম" else "এএম"
    h %= 12
    if (h == 0) h = 12
    fun bd(n: String): String = if (en) n else bnDigits(n)
    val mm = if (m < 10) "0$m" else "$m"
    return bd(c.get(java.util.Calendar.DAY_OF_MONTH).toString()) + " " + BN_MON[c.get(java.util.Calendar.MONTH)] +
        ", " + bd(h.toString()) + ":" + bd(mm) + " " + ap
}

fun uid(): String {
    val chars = "0123456789abcdefghijklmnopqrstuvwxyz"
    val sb = StringBuilder(java.lang.Long.toString(System.currentTimeMillis(), 36))
    for (x in 0 until 4) sb.append(chars[kotlin.random.Random.nextInt(36)])
    return sb.toString()
}
