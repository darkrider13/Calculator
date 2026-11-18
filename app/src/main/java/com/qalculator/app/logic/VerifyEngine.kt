package com.qalculator.app.logic

/*
 * মিলান (verify) sheet logic: step through the numbers of a saved calculation.
 * Ported from the original `vs` object, renderVerify(), vStep(), openVerify(), saveEdit(), delete.
 */

/** vs = { id, src, i, done } */
data class VState(
    val id: String,
    val src: String,
    val i: Int,
    val done: List<Boolean>
)

/** one piece of the top expression: plain text or a number chip */
sealed class VPiece {
    data class Txt(val text: String) : VPiece()
    data class Chip(val i: Int, val text: String, val cur: Boolean, val done: Boolean) : VPiece()
}

/** one line under the buttons: "label <b>value</b>" or the green check line (value = null) */
class VTotLine(val label: String, val value: String?, val ok: Boolean)

class VView(
    val pos: String,
    val num: String,
    val delHidden: Boolean,
    val tot: List<VTotLine>,
    val expr: List<VPiece>
)

private val V_TOK_RE = Regex("([0-9]+\\.?[0-9]*|\\.[0-9]+)|([+\\-*/%()])")

private fun vSym(op: Char): String = when (op) {
    '*' -> "×"
    '/' -> "÷"
    '-' -> "−"
    else -> "+"
}

/** vExprHtml: the expression with each number as a chip (the current one is highlighted) */
fun vExprPieces(src: String, cur: Int, done: List<Boolean>): List<VPiece> {
    val out = ArrayList<VPiece>()
    val sb = StringBuilder()
    var n = 0
    var prev = ""
    fun flush() {
        if (sb.isNotEmpty()) {
            out.add(VPiece.Txt(sb.toString()))
            sb.setLength(0)
        }
    }
    for (m in V_TOK_RE.findAll(src)) {
        val g1 = m.groups[1]
        if (g1 != null) {
            flush()
            out.add(VPiece.Chip(n, numE(jsParseFloat(g1.value), 6), n == cur, done.getOrNull(n) == true))
            n++
            prev = "n"
        } else {
            val op = m.groups[2]!!.value[0]
            val un = op == '-' && (prev == "" || prev == "*" || prev == "/" || prev == "(")
            if (op == '%') {
                sb.append("%"); prev = "n"; continue
            }
            if (op == '(') {
                sb.append("("); prev = "("; continue
            }
            if (op == ')') {
                sb.append(")"); prev = "n"; continue
            }
            if (un) sb.append(vSym(op)) else sb.append(" ").append(vSym(op)).append(" ")
            prev = op.toString()
        }
    }
    flush()
    return out
}

/** renderVerify: the state may move i back into range (like `vs.i = ...` in the original) */
fun vRender(s0: VState): Pair<VState, VView> {
    var s = s0
    val vals = vVals(s.src)
    val n = vals.size
    var i = s.i
    if (i >= n) {
        i = Math.max(0, n - 1)
        s = s.copy(i = i)
    }
    val all = vEval(s.src)
    val isSum = sumNums(s.src) != null
    val tot = ArrayList<VTotLine>()
    if (isSum) {
        var run = 0.0
        for (k in 0..i) run += vals[k]
        run = round8(run)
        tot.add(VTotLine("এ পর্যন্ত যোগফল: ", numE(run, 6), false))
        tot.add(VTotLine("মোট: ", numE(all, 6), false))
    } else {
        tot.add(VTotLine("মোট ফলাফল: ", numE(all, 6), false))
    }
    if (s.done.count { it } == n) tot.add(VTotLine("✓ সব সংখ্যা দেখা শেষ, সব মিলেছে", null, true))
    val expr = ArrayList<VPiece>(vExprPieces(s.src, i, s.done))
    expr.add(VPiece.Txt(" = " + numE(all, 6)))
    val view = VView(
        pos = numE((i + 1).toDouble(), 0) + " / " + numE(n.toDouble(), 0),
        num = numE(if (i < vals.size) vals[i] else Double.NaN, 6),
        delHidden = n <= 2,
        tot = tot,
        expr = expr
    )
    return Pair(s, view)
}

private fun setDone(done: List<Boolean>, i: Int, v: Boolean): List<Boolean> {
    val a = ArrayList<Boolean>(done)
    while (a.size <= i) a.add(false)
    a[i] = v
    return a
}

/** vStep(d): the arrows. null = nothing changes (no render) */
fun vStep(s: VState, d: Int): VState? {
    val n = vVals(s.src).size
    if (n == 0) return null
    var i = s.i
    var done = s.done
    if (d > 0) {
        done = setDone(done, i, true)
        if (i < n - 1) i++
    } else if (i > 0) {
        done = setDone(done, i, false)
        i--
        done = setDone(done, i, false)
    } else return null
    return s.copy(i = i, done = done)
}

/** openVerify(id): null when the entry is missing or cannot be verified */
fun openVerify(calc: List<CalcEntry>, id: String): VState? {
    val e = calc.firstOrNull { it.id == id } ?: return null
    if (!canVerify(e.expr)) return null
    return VState(id, e.expr, 0, emptyList())
}

/** tap on a number chip of the top expression */
fun vSelect(s: VState, i: Int): VState = s.copy(i = i)

/** the number being typed in the edit box */
fun vEditStart(s: VState): String = jsNumStr(vVals(s.src).getOrElse(s.i) { Double.NaN })

/** saveEdit(): null = not valid (the box stays open) */
fun vSaveEdit(s: VState, text: String): VState? {
    val n = parse(text)
    if (!(n >= 0)) return null
    val t = numToks(s.src).getOrNull(s.i) ?: return null
    val cand = s.src.substring(0, t.s) + rawNum(n) + s.src.substring(t.e)
    if (!vEval(cand).isFinite()) return null
    return s.copy(src = cand, done = setDone(s.done, s.i, false))
}

/** the delete button (after the popup says yes): null = not allowed */
fun vDelete(s: VState): VState? {
    val cand = dropNum(s.src, s.i)
    val cnt = numToks(cand).size
    if (cnt < 1 || !vEval(cand).isFinite()) return null
    val done = ArrayList<Boolean>(s.done)
    if (s.i < done.size) done.removeAt(s.i)
    var i = s.i
    if (i >= cnt) i = cnt - 1
    return s.copy(src = cand, done = done, i = i)
}

/** commitVerify(): the saved calculation takes the new expression and result */
fun commitVerify(calc: List<CalcEntry>, s: VState): List<CalcEntry> {
    if (calc.none { it.id == s.id }) return calc
    val r = vEval(s.src)
    return calc.map { if (it.id == s.id) it.copy(expr = s.src, result = r) else it }
}

/** the input text while typing: Bengali digits become English (enDigits) */
fun vInputFilter(text: String): String = enDigits(text)

/** fitV(): font size (rem) and width (em of that font) of the edit box */
fun vInputRem(len: Int): Float = if (len > 9) Math.max(0.8f, 1.3f * 9f / len) else 1.3f

fun vInputWidthEm(len: Int): Float = Math.max(2, len) * 0.7f + 1.2f
