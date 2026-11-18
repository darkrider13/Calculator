package com.bazarhisab.app.logic

/*
 * What the memo box of the price tab shows (the text part of the original renderPrice()).
 */

/** the "total" line: the main text, and what follows it: nothing, the ৳ sign, or a small unit name */
class MemoView(
    val label: String,
    val main: String,
    val cur: Boolean,
    val small: String?,
    val detail: String,
    val sub: String,
    /** the save button is enabled */
    val ok: Boolean
)

fun memoOf(f: Form): MemoView {
    val c = compute(f.rp, f.rw, f.ru, f.w, f.wu, f.amt)
    val q = f.m == "p"
    val item = f.item.trim()
    val label = if (q) "পাবেন" else "মোট দাম"
    var sub = ""
    val ok = if (q) c.rateOk && c.amtOk else c.rateOk && c.weightOk
    val main: String
    var cur = false
    var small: String? = null
    val detail: String
    if (ok && q) {
        val bp = baseParts(c.grams, dimOf(f.ru))
        main = bp.first
        small = bp.second
        detail = (if (item != "") "$item · " else "") + money(c.amt) + "৳"
    } else if (ok) {
        main = money(c.total)
        cur = true
        detail = (if (item != "") "$item · " else "") + qty(c.wv, f.wu)
        val bs = baseOf(dimOf(f.wu))
        if (f.wu != bs.u) sub = qty(c.wv, f.wu) + " = " + numf(c.wG, 3) + " " + bs.n
        else if (c.wG >= 1000) sub = qty(c.wv, bs.u) + " = " + numf(c.wG / 1000, 3) + " " + bs.big
    } else {
        if (q) {
            main = "০.০"
            small = baseOf(dimOf(f.ru)).n
        } else {
            main = "০.০০"
            cur = true
        }
        detail = if (!c.rateOk) "আগে দোকানের রেট লিখুন" else if (q) "কত টাকার কিনবেন, সেটা লিখুন" else "কত পরিমাণ কিনছেন, সেটা লিখুন"
    }
    return MemoView(label, main, cur, small, detail, sub, ok)
}

/** the unit shown on a unit button; an unknown unit shows গ্রাম (setPick) */
fun pickLabel(u: String): String = if (UNITS.containsKey(u)) unitOf(u).n else unitOf("g").n

/** the unit chosen from the menu. A weight / volume mismatch flips the other unit (the menu click handler). */
fun pickUnit(f: Form, id: String, v: String): Form {
    var r = if (id == "ru") f.copy(ru = v) else f.copy(wu = v)
    if (id == "ru" && dimOf(r.wu) != dimOf(v)) r = r.copy(wu = SWAP[r.wu] ?: "g")
    if (id == "wu" && dimOf(r.ru) != dimOf(v)) r = r.copy(ru = SWAP[r.ru] ?: "g")
    return r
}
