package com.qalculator.app.logic

/*
 * Calculator + price logic. Line by line copy of the original functions:
 * compute, evalExpr, trimTail, closeParens, hasOp, numToks, normNum, stripLead,
 * tidyExpr, dropNum, sumNums, vVals, vEval, canVerify.
 */

// ---------------------------------------------------------------- price (compute)

data class PriceCalc(
    val rateOk: Boolean,
    val weightOk: Boolean,
    val amtOk: Boolean,
    val perG: Double,
    val total: Double,
    val grams: Double,
    val rp: Double,
    val rwv: Double,
    val wv: Double,
    val wG: Double,
    val amt: Double
)

fun compute(rpS: String, rwS: String, ruS: String, wS: String, wuS: String, amtS: String): PriceCalc {
    val rp = parse(rpS)
    val rwv = parse(rwS)
    val wv = parse(wS)
    val amt = parse(amtS)
    val ru = unitOf(ruS)
    val wu = unitOf(wuS)
    val rwG = rwv * ru.f
    val wG = wv * wu.f
    val rateOk = rp > 0 && rwG > 0
    val weightOk = wG > 0
    val amtOk = amt > 0
    val perG = if (rateOk) rp / rwG else Double.NaN
    return PriceCalc(
        rateOk = rateOk,
        weightOk = weightOk,
        amtOk = amtOk,
        perG = perG,
        total = if (rateOk && weightOk) perG * wG else Double.NaN,
        grams = if (rateOk && amtOk) amt / perG else Double.NaN,
        rp = rp, rwv = rwv, wv = wv, wG = wG, amt = amt
    )
}

// ---------------------------------------------------------------- expression evaluator

class ExprError(msg: String) : Exception(msg)

/** the function names of the scientific keys (the expression text has them as "sin(" etc.) */
internal const val FN_SRC = "a?(?:sin|cos|tan)ʳ?|lg|ln|√"
private val FN_NAMES = setOf("sin", "cos", "tan", "asin", "acos", "atan", "lg", "ln", "√")

/** any character that only a scientific key can put into the expression */
private val SCI_RE = Regex("[a-zπ!²¹^√]")

fun isSci(t: String): Boolean = SCI_RE.containsMatchIn(t)

private fun factorial(v: Double): Double {
    if (v.isNaN() || v < 0 || Math.abs(v - Math.rint(v)) > 1e-9) return Double.NaN
    val n = Math.rint(v).toInt()
    if (n > 170) return Double.POSITIVE_INFINITY
    var r = 1.0
    for (k in 2..n) r *= k
    return r
}

/** one function applied to a; trig works in degrees unless rad */
private fun applyFn(name: String, a: Double, rad: Boolean): Double {
    fun ang(x: Double) = if (rad) x else Math.toRadians(x)
    fun back(x: Double) = if (rad) x else Math.toDegrees(x)
    return when (name) {
        "sin" -> Math.sin(ang(a))
        "cos" -> Math.cos(ang(a))
        "tan" -> {
            if (!rad && Math.abs(((a % 180.0) + 180.0) % 180.0 - 90.0) < 1e-9) Double.NaN
            else Math.tan(ang(a))
        }
        "asin" -> if (a < -1.0 || a > 1.0) Double.NaN else back(Math.asin(a))
        "acos" -> if (a < -1.0 || a > 1.0) Double.NaN else back(Math.acos(a))
        "atan" -> back(Math.atan(a))
        "lg" -> if (a <= 0.0) Double.NaN else Math.log10(a)
        "ln" -> if (a <= 0.0) Double.NaN else Math.log(a)
        "√" -> if (a < 0.0) Double.NaN else Math.sqrt(a)
        else -> Double.NaN
    }
}

private class Evaluator(private val s: String) {
    var i = 0
    private var isPct = false
    private var tp = false

    private fun at(k: Int): Char = if (k < s.length) s[k] else '\u0000'

    private fun num(): Double {
        val st = i
        while (i < s.length && (s[i] in '0'..'9' || s[i] == '.')) i++
        if (st == i) throw ExprError("num")
        val v = jsParseFloat(s.substring(st, i))
        if (v.isNaN()) throw ExprError("num")
        if (at(i) == '%') {
            i++
            isPct = true
            return v / 100
        }
        isPct = false
        return v
    }

    /** one value: a bracket, a constant (π, e), a function call (sin(...)) or a number */
    private fun atom(): Double {
        val c = at(i)
        if (c == '(') {
            i++
            val pv = expr()
            if (at(i) == ')') i++
            else if (i < s.length) throw ExprError("paren")
            if (at(i) == '%') {
                i++
                isPct = true
                return pv / 100
            }
            isPct = false
            return pv
        }
        if (c == 'π') {
            i++
            isPct = false
            return Math.PI
        }
        if (c == 'e') {
            i++
            isPct = false
            return Math.E
        }
        if ((c in 'a'..'z') || c == '√') return call()
        return num()
    }

    /** sin( cos( tan( asin( acos( atan( lg( ln( √( ; a trailing ʳ means radians */
    private fun call(): Double {
        var j = i
        val name: String
        if (s[j] == '√') {
            j++
            name = "√"
        } else {
            while (j < s.length && s[j] in 'a'..'z') j++
            name = s.substring(i, j)
        }
        var rad = false
        if (j < s.length && s[j] == 'ʳ') {
            rad = true
            j++
        }
        if (j >= s.length || s[j] != '(') throw ExprError("fn")
        if (name !in FN_NAMES) throw ExprError("fn")
        i = j + 1
        val a = expr()
        if (at(i) == ')') i++
        else if (i < s.length) throw ExprError("paren")
        isPct = false
        return applyFn(name, a, rad)
    }

    /** a value followed by any number of ! ² ⁻¹ */
    private fun postfix(): Double {
        var v = atom()
        while (true) {
            val c = at(i)
            if (c == '!') {
                i++
                v = factorial(v)
                isPct = false
            } else if (c == '²') {
                i++
                v *= v
                isPct = false
            } else if (c == '⁻' && at(i + 1) == '¹') {
                i += 2
                v = 1.0 / v
                isPct = false
            } else break
        }
        return v
    }

    /** x^y, right to left: 2^3^2 = 2^(3^2) */
    private fun power(): Double {
        val b = postfix()
        if (at(i) == '^') {
            i++
            val x = factor()
            isPct = false
            return Math.pow(b, x)
        }
        return b
    }

    private fun factor(): Double {
        if (at(i) == '-') {
            i++
            return -factor()
        }
        return power()
    }

    private fun term(): Double {
        var v = factor()
        tp = isPct
        while (at(i) == '*' || at(i) == '/') {
            val o = s[i++]
            val r = factor()
            v = if (o == '*') v * r else v / r
            tp = false
        }
        return v
    }

    fun expr(): Double {
        var v = term()
        while (at(i) == '+' || at(i) == '-') {
            val o = s[i++]
            var r = term()
            if (tp) r = v * r
            v = if (o == '+') v + r else v - r
        }
        return v
    }
}

/** Throws ExprError when the text is not a valid expression */
fun evalExpr(s: String): Double {
    val e = Evaluator(s)
    val v = e.expr()
    if (e.i != s.length) throw ExprError("end")
    return v
}

/** evalExpr that gives NaN instead of an error (the original try/catch) */
fun evalOrNaN(s: String): Double = try {
    evalExpr(s)
} catch (x: Exception) {
    Double.NaN
}

// ---------------------------------------------------------------- expression text helpers

private val TRIM_TAIL_RE = Regex("(?:[+\\-*/^.]|(?:$FN_SRC)?\\()+\\z")
private val OP_ANY_RE = Regex("[+*/\\-^]")
private val NUM_TOK_RE = Regex("[0-9]+\\.?[0-9]*|\\.[0-9]+")
private val NORM_RE = Regex("^([0-9]+\\.?[0-9]*|\\.[0-9]+)\\z")
private val TRAIL_OPS_RE = Regex("[+\\-*/^]+\\z")
private val LEAD_ZEROS_RE = Regex("^0+(?=[0-9])")

fun trimTail(e: String): String = TRIM_TAIL_RE.replace(e, "")

fun closeParens(t: String): String {
    val o = t.count { it == '(' }
    val c = t.count { it == ')' }
    return if (o > c) t + ")".repeat(o - c) else t
}

fun hasOp(t: String): Boolean = OP_ANY_RE.containsMatchIn(t.drop(1)) || t.endsWith("%") || isSci(t)

/** start (s) and end (e, exclusive) of one number inside an expression */
data class NumTok(val s: Int, val e: Int)

fun numToks(src: String): List<NumTok> =
    NUM_TOK_RE.findAll(src).map { NumTok(it.range.first, it.range.last + 1) }.toList()

fun normNum(a0: String): String {
    var a = a0
    if (!NORM_RE.matches(a)) return ""
    if (a.startsWith(".")) a = "0$a"
    a = a.replace(Regex("\\.\\z"), "")
    return LEAD_ZEROS_RE.replace(a, "")
}

/** leading zeros are dropped unless a "." follows; the cursor position moves with the text */
fun stripLead(v: String, p: Int): Pair<String, Int> {
    var z = 0
    while (z < v.length - 1 && v[z] == '0' && v[z + 1] in '0'..'9') z++
    return if (z > 0) Pair(v.substring(z), Math.max(0, p - z)) else Pair(v, p)
}

private val TIDY_1 = Regex("(?:$FN_SRC)?\\(\\)%?")
private val TIDY_2 = Regex("\\(([+*/^])")
private val TIDY_3 = Regex("[+\\-*/^]\\)")
private val TIDY_4 = Regex("[+\\-*/^]([+*/^])")
private val TIDY_5 = Regex("^[+*/^]+")

/** clean up leftovers after a number is taken out: (), "(+", "+)", "++" */
fun tidyExpr(s0: String): String {
    var s = s0
    var p: String
    do {
        p = s
        s = TIDY_1.replace(s, "")
        s = TIDY_2.replace(s, "(")
        s = TIDY_3.replace(s, ")")
        s = TIDY_4.replace(s, "\$1")
        s = TIDY_5.replace(s, "")
    } while (s != p)
    return s
}

/** the expression with number k (and the operator that joined it) taken out */
fun dropNum(src: String, k: Int): String {
    val toks = numToks(src)
    val t = toks.getOrNull(k) ?: return src
    var e = t.e
    if (src.getOrNull(e) == '%') e++
    var before = src.substring(0, t.s)
    var after = src.substring(e)
    val m = TRAIL_OPS_RE.find(before)
    val core = if (m != null) before.substring(0, before.length - m.value.length) else before
    if (core != "" && !core.endsWith("(")) {
        before = core
    } else {
        before = core
        after = after.replace(Regex("^[+\\-*/]"), "")
    }
    return tidyExpr(before + after)
}

// ---------------------------------------------------------------- verify (মিলান) helpers

private val SUM_RE = Regex("^[0-9]+(\\.[0-9]+)?(\\+[0-9]+(\\.[0-9]+)?)+\\z")

/** "10+20+30" gives [10, 20, 30], any other expression gives null */
fun sumNums(expr: String): List<Double>? =
    if (SUM_RE.matches(expr)) expr.split("+").map { jsParseFloat(it) } else null

/** every number in the expression, in order */
fun vVals(src: String): List<Double> = numToks(src).map { jsParseFloat(src.substring(it.s, it.e)) }

fun vEval(src: String): Double {
    val t = closeParens(trimTail(src))
    if (t == "") return Double.NaN
    val v = evalOrNaN(t)
    return if (v.isFinite()) round8(v) else Double.NaN
}

fun canVerify(expr: String?): Boolean {
    val e = expr ?: ""
    return numToks(e).size >= 2 && vEval(e).isFinite()
}
