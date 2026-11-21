package com.qalculator.app.logic

/*
 * The calculator tab logic (the `calc` object, press(), renderCalc(), the number edit mode).
 * Ported line by line from the original JavaScript. No Android code here, so it can be tested.
 */

/** one piece of the expression line: plain text, a tappable number (k = its index) or the number being edited */
sealed class Seg {
    data class Plain(val text: String) : Seg()
    data class Num(val k: Int, val text: String) : Seg()
    object Edit : Seg()
}

/** what the screen shows (the DOM of the original): top line, big result, and the plain number for copy */
data class CalcView(
    val top: List<Seg> = emptyList(),
    val out: String = "",
    val raw: String = "",
    /** counts the renders (every renderCalc call), so the screen can scroll to the end each time */
    val seq: Int = 0
)

/** calc = { expr, last, fresh, err, ed } + the text/cursor of the number input while editing (ed >= 0) */
data class CalcState(
    val expr: String = "",
    val last: String = "",
    val fresh: Boolean = false,
    val err: String = "",
    val ed: Int = -1,
    val edText: String = "",
    val edPos: Int = 0,
    val view: CalcView = CalcView(),
    /** angle mode of sin/cos/tan: false = degrees, true = radians */
    val rad: Boolean = false
)

class PressResult(val state: CalcState, val entry: CalcEntry? = null)

const val ERR_DIV = "Can't divide"
const val ERR_DOM = "Invalid input"

/** the keys: label, key code, class, (aria label is not needed) */
class KeyDef(val label: String, val k: String, val cls: String)

val CALC_KEYS: List<KeyDef> = listOf(
    KeyDef("C", "C", "clr"), KeyDef("( )", "()", "op"), KeyDef("%", "%", "op"), KeyDef("÷", "/", "op"),
    KeyDef("7", "7", ""), KeyDef("8", "8", ""), KeyDef("9", "9", ""), KeyDef("×", "*", "op"),
    KeyDef("4", "4", ""), KeyDef("5", "5", ""), KeyDef("6", "6", ""), KeyDef("−", "-", "op"),
    KeyDef("1", "1", ""), KeyDef("2", "2", ""), KeyDef("3", "3", ""), KeyDef("+", "+", "op"),
    KeyDef("+/−", "±", ""), KeyDef("0", "0", ""), KeyDef(".", ".", ""), KeyDef("=", "=", "eq")
)

// ---------------------------------------------------------------- expression line

private val TOK_RE = Regex("([0-9]+\\.?[0-9]*|\\.[0-9]+)|([+\\-*/%()])|((?:$FN_SRC)\\(|π|e|!|²|⁻¹|\\^)")

private fun sym(op: Char): String = when (op) {
    '*' -> "×"
    '/' -> "÷"
    '-' -> "−"
    else -> "+"
}

private fun isJsSpace(c: Char): Boolean = c == ' ' || c == ' '

/** exprHtml: the expression as tappable numbers; number k (if k >= 0) is the edit box */
fun exprSegs(src: String, k: Int, tail: String): List<Seg> {
    val out = ArrayList<Seg>()
    val sb = StringBuilder()
    var n = 0
    var prev = ""
    fun flush() {
        if (sb.isNotEmpty()) {
            out.add(Seg.Plain(sb.toString()))
            sb.setLength(0)
        }
    }
    for (m in TOK_RE.findAll(src)) {
        val g1 = m.groups[1]
        val g3 = m.groups[3]
        if (g3 != null) {
            val t = g3.value
            if (t == "^") {
                sb.append("^"); prev = "*"
            } else if (t == "π" || t == "e" || t == "!" || t == "²" || t == "⁻¹") {
                sb.append(t); prev = "n"
            } else {
                // a function: asin( is shown as sin⁻¹( and the radians mark is hidden
                sb.append(t.replace("ʳ", "").replace(Regex("^a(sin|cos|tan)"), "\$1⁻¹")); prev = "("
            }
            continue
        }
        if (g1 != null) {
            flush()
            if (n == k) out.add(Seg.Edit) else out.add(Seg.Num(n, g1.value))
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
            if (un) sb.append(sym(op)) else sb.append(" ").append(sym(op)).append(" ")
            prev = op.toString()
        }
    }
    flush()
    // .replace(/^\s+|\s+$/g, "") + tail
    if (out.isNotEmpty()) {
        val f = out.first()
        if (f is Seg.Plain) {
            val t = f.text.dropWhile { isJsSpace(it) }
            if (t.isEmpty()) out.removeAt(0) else out[0] = Seg.Plain(t)
        }
    }
    if (out.isNotEmpty()) {
        val l = out.last()
        if (l is Seg.Plain) {
            val t = l.text.dropLastWhile { isJsSpace(it) }
            if (t.isEmpty()) out.removeAt(out.size - 1) else out[out.size - 1] = Seg.Plain(t)
        }
    }
    if (tail.isNotEmpty()) {
        val l = out.lastOrNull()
        if (l is Seg.Plain) out[out.size - 1] = Seg.Plain(l.text + tail) else out.add(Seg.Plain(tail))
    }
    return out
}

// ---------------------------------------------------------------- edit mode helpers

/** editCand: the expression as it would be if the edit ended now */
fun editCand(s: CalcState): String {
    val t = numToks(s.expr).getOrNull(s.ed) ?: return s.expr
    val v = normNum(toAscii(s.edText))
    return if (v == "") dropNum(s.expr, s.ed) else s.expr.substring(0, t.s) + v + s.expr.substring(t.e)
}

/** updateEditOut: the big result while a number is being edited */
fun updateEditOut(s: CalcState): CalcState {
    val c = trimTail(editCand(s))
    val v = if (c == "") 0.0 else evalOrNaN(c)
    val out: String
    val raw: String
    if (v.isFinite()) {
        out = numE(round8(v), 6)
        raw = rawNum(round8(v))
    } else {
        out = "—"
        raw = ""
    }
    return s.copy(view = s.view.copy(out = out, raw = raw))
}

private val PLAIN_NUM_RE = Regex("^-?([0-9]+\\.?[0-9]*|\\.[0-9]+)\\z")
private val DOT_END_RE = Regex("\\.\\z")

/** renderCalc: work out what the screen shows from the state */
fun renderCalc(s: CalcState): CalcState {
    val e = s.expr
    val t = trimTail(e)
    val top: List<Seg>
    var out: String
    var raw = ""
    if (s.ed >= 0) {
        top = exprSegs(e, s.ed, "")
        out = "0"
    } else if (s.err != "") {
        top = if (s.last != "") exprSegs(s.last, -1, "") else emptyList()
        out = s.err
    } else if (s.fresh) {
        top = exprSegs(s.last, -1, " =")
        out = numE(jsParseFloat(e), 6)
        raw = rawNum(jsParseFloat(e))
    } else if (hasOp(t) || t.contains('(') || isSci(e)) {
        top = exprSegs(e, -1, "")
        val v = evalOrNaN(t)
        if (v.isFinite()) {
            out = numE(round8(v), 6)
            raw = rawNum(round8(v))
        } else {
            out = "—"
            raw = ""
        }
    } else {
        top = emptyList()
        out = if (e == "") "" else e.replace('*', '×').replace('/', '÷').replace('-', '−')
        raw = if (PLAIN_NUM_RE.matches(e)) DOT_END_RE.replace(e, "") else ""
    }
    var r = s.copy(view = CalcView(top, out, raw, s.view.seq + 1))
    if (r.ed >= 0) {
        // the caret goes to the end of the number box
        r = updateEditOut(r.copy(edPos = r.edText.length))
    }
    return r
}

/** the big result's text size in rem (fitOut), or 0 for the default size */
fun fitOutRem(out: String): Float {
    val n = out.length
    return if (n > 8) Math.max(1.7f, 3.2f * 8f / n) else 0f
}

/** commitEdit: end the edit. Returns the new state and true if a number was removed */
fun commitEdit(s: CalcState): Pair<CalcState, Boolean> {
    if (s.ed < 0) return Pair(s, false)
    val k = s.ed
    val t = numToks(s.expr).getOrNull(k)
    val v = normNum(toAscii(s.edText))
    var gone = false
    var expr = s.expr
    if (t != null) {
        if (v != "") {
            expr = expr.substring(0, t.s) + v + expr.substring(t.e)
        } else {
            expr = dropNum(expr, k)
            gone = true
        }
    }
    return Pair(renderCalc(s.copy(expr = expr, ed = -1, edText = "", edPos = 0)), gone)
}

private val OPS_END_RE = Regex("[+\\-*/^]+\\z")

/** editOp: an operator key while editing changes the operator before the number. null = not handled */
private fun editOp(s: CalcState, k: String): CalcState? {
    val t = numToks(s.expr).getOrNull(s.ed) ?: return null
    val before = s.expr.substring(0, t.s)
    val m = OPS_END_RE.find(before) ?: return s
    val head = before.substring(0, before.length - m.value.length)
    if (head == "" || head.endsWith("(")) return s
    val typed = s.edText
    val ns = s.copy(expr = head + k + s.expr.substring(t.s))
    val r = renderCalc(ns)
    return updateEditOut(r.copy(edText = typed, edPos = typed.length))
}

/** editKey: keypad keys typed into the number being edited, at the cursor. null = not handled */
private fun editKey(s: CalcState, k: String): CalcState? {
    val isDigit = k.length == 1 && k[0] in '0'..'9'
    if (!isDigit && k != "." && k != "⌫") return null
    val v = s.edText
    val s0 = Math.min(Math.max(s.edPos, 0), v.length)
    val nv: String
    val np: Int
    if (k == ".") {
        if (v.indexOf('.') >= 0) return s
        nv = v.substring(0, s0) + "." + v.substring(s0)
        np = s0 + 1
    } else if (k == "⌫") {
        if (s0 > 0) {
            nv = v.substring(0, s0 - 1) + v.substring(s0)
            np = s0 - 1
        } else return s
    } else {
        nv = v.substring(0, s0) + k + v.substring(s0)
        np = s0 + 1
    }
    val f = stripLead(nv, np)
    return updateEditOut(s.copy(edText = f.first, edPos = f.second))
}

/** tap on number k of the expression line starts (or moves) the number edit */
fun tapNum(s0: CalcState, k0: Int): CalcState {
    var s = s0
    var k = k0
    if (s.ed >= 0) {
        val prevK = s.ed
        val r = commitEdit(s)
        s = r.first
        if (r.second && k > prevK) k--
    } else if (s.fresh || s.err != "") {
        if (s.last == "") return s
        s = s.copy(expr = s.last, last = "", fresh = false, err = "")
    }
    val toks = numToks(s.expr)
    if (k >= toks.size) return s
    val t = toks[k]
    return renderCalc(s.copy(ed = k, edText = s.expr.substring(t.s, t.e), edPos = 0))
}

/** a click on the number box moves the caret (same as a tap inside the <input>) */
fun moveCaret(s: CalcState, pos: Int): CalcState {
    if (s.ed < 0) return s
    return s.copy(edPos = Math.min(Math.max(pos, 0), s.edText.length))
}

// ---------------------------------------------------------------- press

private val END_DIGIT_PCT_PAREN = Regex("[0-9%)πe!²¹]\\z")
private val END_PCT_PAREN = Regex("[%)πe!²¹]\\z")
private val END_DIGIT_PAREN = Regex("[0-9)]\\z")
private val ZERO_AT_END = Regex("(^|[+\\-*/^(])0\\z")
private val END_MUL_DIV = Regex("[*/^]\\z")
private val END_OP = Regex("[+\\-*/^]\\z")
private val NUM_PCT_END = Regex("([0-9]+\\.?[0-9]*|\\.[0-9]+)(%?)\\z")
private val UNARY_MINUS_END = Regex("(^|[*/^(])-\\z")
private val OP_START_END = Regex("[*/^(]\\z")
private val CUR_NUM_END = Regex("[0-9.]*\\z")

// ---------------------------------------------------------------- scientific keys

private val CAN_MUL_END = Regex("[0-9%)πe!²¹.]\\z")
private val POST_OK = Regex("[0-9)πe!²¹]\\z")
private val CLOSE_OK = Regex("[0-9%)πe!²¹]\\z")
private val TOKEN_END_RE = Regex("(?:(?:$FN_SRC)\\(|⁻¹)\\z")
private val TRIG_NO_R = Regex("(a?(?:sin|cos|tan))\\(")

/** key code to the text it puts in the expression (a "function": it opens a bracket) */
private val SCI_FN = mapOf(
    "sin" to "sin", "cos" to "cos", "tan" to "tan",
    "asin" to "asin", "acos" to "acos", "atan" to "atan",
    "lg" to "lg", "ln" to "ln", "√" to "√",
    "10x" to "10^", "ex" to "e^"
)
private val TRIG_KEYS = setOf("sin", "cos", "tan", "asin", "acos", "atan")

/** a number or bracket before a new function / constant / "(" gets a × in between */
private fun mulJoin(e: String, tok: String): String =
    if (CAN_MUL_END.containsMatchIn(e)) "$e*$tok" else e + tok

/** backspace takes a whole function name (sin() or ⁻¹ at once */
private fun dropLastToken(e: String): String {
    val m = TOKEN_END_RE.find(e)
    return if (m != null) e.substring(0, m.range.first) else e.dropLast(1)
}

/** the deg / rad key: the trig functions already typed change with it */
fun setAngle(s0: CalcState, rad: Boolean): CalcState {
    var s = s0
    if (s.ed >= 0) s = commitEdit(s).first
    val ne = if (rad) TRIG_NO_R.replace(s.expr) { it.groupValues[1] + "ʳ(" } else s.expr.replace("ʳ", "")
    return renderCalc(s.copy(expr = ne, rad = rad))
}

/** press(k): the same steps as the original, state in, state out (+ the saved entry when "=" works) */
fun press(s0: CalcState, k: String, ts: Long = System.currentTimeMillis(), id: String = uid()): PressResult {
    var s = s0
    if (s.ed >= 0) {
        if (k == "C") {
            s = s.copy(ed = -1, edText = "", edPos = 0)
        } else {
            val h = editKey(s, k)
            if (h != null) return PressResult(h)
            if (k == "+" || k == "-" || k == "*" || k == "/") {
                val o = editOp(s, k)
                if (o != null) return PressResult(o)
                s = commitEdit(s).first
            } else {
                s = commitEdit(s).first
            }
        }
    }
    var e = s.expr
    var expr = s.expr
    var last = s.last
    var fresh = s.fresh
    val err0 = ""
    fun st(err: String = err0): CalcState = s.copy(expr = expr, last = last, fresh = fresh, err = err)
    fun done(): PressResult = PressResult(renderCalc(st()))

    if (k == "C") {
        expr = ""; last = ""; fresh = false
    } else if (k == "⌫") {
        if (fresh) {
            expr = ""; fresh = false; last = ""
        } else expr = dropLastToken(e)
    } else if (k == "=") {
        if (fresh) return PressResult(st())
        val t = closeParens(trimTail(e))
        if (t == "" || t == "-" || !hasOp(t)) return PressResult(st())
        var v = evalOrNaN(t)
        if (!v.isFinite()) {
            last = t; expr = ""
            return PressResult(renderCalc(st(if (isSci(t) && !t.contains('/')) ERR_DOM else ERR_DIV)))
        }
        v = round8(v)
        val str = jsNumStr(v)
        val entry = CalcEntry(id, ts, t, v)
        last = t
        if (str.contains('e') || str.contains('E')) {
            expr = ""; fresh = false
        } else {
            expr = str; fresh = true
        }
        return PressResult(renderCalc(st()), entry)
    } else if (k == "()") {
        if (fresh) {
            e = ""; fresh = false; last = ""
        }
        val op = e.count { it == '(' }
        val cl = e.count { it == ')' }
        if (END_DIGIT_PCT_PAREN.containsMatchIn(e)) {
            expr = if (op > cl) "$e)" else "$e*("
        } else expr = "$e("
    } else if (k == "±") {
        if (fresh) {
            fresh = false; last = ""
        }
        val mm = NUM_PCT_END.find(e)
        if (e == "") expr = "-"
        else if (mm != null) {
            val head = e.substring(0, e.length - mm.value.length)
            val m0 = mm.value
            if (head == "") expr = "-$m0"
            else if (UNARY_MINUS_END.containsMatchIn(head)) expr = head.dropLast(1) + m0
            else if (OP_START_END.containsMatchIn(head)) expr = "$head-$m0"
            else if (head.endsWith("+")) expr = head.dropLast(1) + "-" + m0
            else if (head.endsWith("-")) expr = head.dropLast(1) + "+" + m0
            else expr = e
        } else if (UNARY_MINUS_END.containsMatchIn(e)) expr = e.dropLast(1)
        else if (OP_START_END.containsMatchIn(e)) expr = "$e-"
    } else if (k.length == 1 && k[0] in '0'..'9') {
        if (fresh) {
            e = ""; fresh = false; last = ""
        }
        if (END_PCT_PAREN.containsMatchIn(e)) return PressResult(st())
        if (ZERO_AT_END.containsMatchIn(e)) e = e.dropLast(1)
        expr = e + k
    } else if (k == ".") {
        if (fresh) {
            e = "0"; fresh = false; last = ""
        }
        if (END_PCT_PAREN.containsMatchIn(e)) return PressResult(st())
        val cur = CUR_NUM_END.find(e)!!.value
        if (cur.indexOf('.') >= 0) return PressResult(st())
        expr = e + (if (cur == "") "0." else ".")
    } else if (k == "%") {
        if (!END_DIGIT_PAREN.containsMatchIn(e)) return done()
        fresh = false
        expr = "$e%"
    } else if (SCI_FN.containsKey(k)) {
        // sin cos tan ... lg ln √ 10^ e^ : opens "name(" ; after a result it wraps the result
        val base = SCI_FN.getValue(k)
        val name = if (s.rad && k in TRIG_KEYS) base + "ʳ" else base
        val tok = if (base.endsWith("^")) "$base(" else "$name("
        if (fresh) {
            expr = tok + e + ")"; fresh = false
        } else expr = mulJoin(e, tok)
    } else if (k == "π" || k == "e") {
        if (fresh) {
            e = ""; fresh = false; last = ""
        }
        expr = mulJoin(e, k)
    } else if (k == "!" || k == "²" || k == "inv") {
        if (!POST_OK.containsMatchIn(e)) return done()
        fresh = false
        expr = e + (if (k == "inv") "⁻¹" else k)
    } else if (k == "(") {
        if (fresh) {
            e = ""; fresh = false; last = ""
        }
        expr = mulJoin(e, "(")
    } else if (k == ")") {
        val op = e.count { it == '(' }
        val cl = e.count { it == ')' }
        if (op <= cl || !CLOSE_OK.containsMatchIn(e)) return done()
        fresh = false
        expr = "$e)"
    } else {
        fresh = false
        if (e == "" || e.endsWith("(")) {
            if (k == "-") expr = "$e-"
        } else if (END_MUL_DIV.containsMatchIn(e) && k == "-") expr = e + k
        else {
            if (END_OP.containsMatchIn(e)) {
                e = OPS_END_RE.replace(e, "")
                if (e == "" || e.endsWith("(")) return done()
            }
            expr = e + k
        }
    }
    return done()
}
