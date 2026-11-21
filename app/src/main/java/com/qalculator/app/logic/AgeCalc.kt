package com.qalculator.app.logic

import java.util.Calendar

/*
 * Age calculator logic (no UI, no java.time so it works on Android 6 too).
 * A date is typed as 8 digits DDMMYYYY.
 */

class YMD(val y: Int, val m: Int, val d: Int)

fun isLeapYear(y: Int): Boolean = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

fun daysInMonth(y: Int, m: Int): Int = when (m) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (isLeapYear(y)) 29 else 28
}

/** "05111998" -> 5 Nov 1998; null when there are not 8 digits or the date does not exist */
fun parseDateDigits(s: String): YMD? {
    if (s.length != 8 || !s.all { it.isDigit() }) return null
    val d = s.substring(0, 2).toInt()
    val m = s.substring(2, 4).toInt()
    val y = s.substring(4, 8).toInt()
    if (y < 1 || m !in 1..12 || d !in 1..daysInMonth(y, m)) return null
    return YMD(y, m, d)
}

/** today as the 8 typed digits */
fun todayDigits(): String {
    val c = Calendar.getInstance()
    return String.format(
        java.util.Locale.US, "%02d%02d%04d",
        c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)
    )
}

/** days since 1970-01-01 */
fun epochDay(x: YMD): Long {
    val y = (if (x.m <= 2) x.y - 1 else x.y).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (x.m + (if (x.m > 2) -3 else 9)).toLong()
    val doy = (153 * mp + 2) / 5 + x.d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

private val DayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

fun weekdayName(x: YMD): String = DayNames[Math.floorMod(epochDay(x) + 4, 7L).toInt()]

/** years, months, days from [from] to [to] ([from] is not after [to]); a month is added with the day cut to the month's end */
fun periodBetween(from: YMD, to: YMD): Triple<Int, Int, Int> {
    fun anchor(k: Int): YMD {
        val idx = from.y * 12 + (from.m - 1) + k
        val ay = idx / 12
        val am = idx % 12 + 1
        return YMD(ay, am, Math.min(from.d, daysInMonth(ay, am)))
    }
    var tm = (to.y - from.y) * 12 + (to.m - from.m)
    if (epochDay(anchor(tm)) > epochDay(to)) tm -= 1
    val days = (epochDay(to) - epochDay(anchor(tm))).toInt()
    return Triple(tm / 12, tm % 12, days)
}

class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalMonths: Int,
    val totalDays: Long,
    val bornWeekday: String,
    /** the next birthday is today */
    val birthdayToday: Boolean,
    val nextMonths: Int,
    val nextDays: Int,
    val nextWeekday: String
)

/** the age on [on] of someone born on [dob]; null when [dob] is after [on] */
fun ageOn(dob: YMD, on: YMD): AgeResult? {
    if (epochDay(dob) > epochDay(on)) return null
    val (y, m, d) = periodBetween(dob, on)
    fun birthdayIn(year: Int) = YMD(year, dob.m, if (dob.m == 2 && dob.d == 29 && !isLeapYear(year)) 28 else dob.d)
    var next = birthdayIn(on.y)
    if (epochDay(next) < epochDay(on)) next = birthdayIn(on.y + 1)
    val today = epochDay(next) == epochDay(on)
    val (_, nm, nd) = if (today) Triple(0, 0, 0) else periodBetween(on, next)
    val ny = if (today) 0 else periodBetween(on, next).first
    return AgeResult(
        years = y, months = m, days = d,
        totalMonths = y * 12 + m,
        totalDays = epochDay(on) - epochDay(dob),
        bornWeekday = weekdayName(dob),
        birthdayToday = today,
        nextMonths = ny * 12 + nm,
        nextDays = nd,
        nextWeekday = weekdayName(next)
    )
}
