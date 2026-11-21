package com.qalculator.app.logic

/*
 * Saved data + the pure rules around it (fixUnits, uniq, normalize, loadLocal, saveEntry, reuse).
 * Same JSON format as the original app: { form, history, calc }.
 */

const val MAX_HIST = 300
const val MAX_CALC = 300
const val STORAGE_KEY = "qalculator-v1"

/** the price tab inputs. m = "w" (পরিমাণ দিয়ে টাকা) or "p" (টাকা দিয়ে পরিমাণ) */
data class Form(
    val item: String = "",
    val rp: String = "",
    val rw: String = "",
    val ru: String = "g",
    val w: String = "",
    val wu: String = "g",
    val m: String = "w",
    val amt: String = ""
)

/** one saved price: t = "p" (total price) or "q" (quantity for an amount) */
data class HistEntry(
    val id: String,
    val t: String,
    val ts: Long,
    val item: String,
    val rp: Double,
    val rw: Double,
    val ru: String,
    val w: Double = Double.NaN,
    val wu: String = "",
    val total: Double = Double.NaN,
    val amt: Double = Double.NaN,
    val grams: Double = Double.NaN
)

/** one saved calculator result */
data class CalcEntry(
    val id: String,
    val ts: Long,
    val expr: String,
    val result: Double
)

data class AppData(val form: Form, val history: List<HistEntry>, val calc: List<CalcEntry>)

/** units: গ্রাম, কেজি, মিলিলিটার, লিটার; turn any old পোয়া value into grams */
fun fixUnits(f: Form): Form {
    var rw = f.rw
    var ru = f.ru
    var w = f.w
    var wu = f.wu
    if (ru == "poa") {
        val a = parse(rw)
        if (!a.isNaN()) rw = jsNumStr(a * 250)
        ru = "g"
    }
    if (wu == "poa") {
        val b = parse(w)
        if (!b.isNaN()) w = jsNumStr(b * 250)
        wu = "g"
    }
    if (!UNITS.containsKey(ru)) ru = "g"
    if (!UNITS.containsKey(wu)) wu = "g"
    if (dimOf(wu) != dimOf(ru)) wu = SWAP[wu] ?: "g"
    return f.copy(rw = rw, ru = ru, w = w, wu = wu)
}

/** drop entries without id and repeated ids, newest first */
private fun <T> uniq(a: List<T>, id: (T) -> String, ts: (T) -> Long): List<T> {
    val seen = HashSet<String>()
    val out = ArrayList<T>()
    for (e in a) {
        val k = id(e)
        if (k.isNotEmpty() && seen.add(k)) out.add(e)
    }
    return out.sortedByDescending { ts(it) }
}

fun normalizeHistory(a: List<HistEntry>): List<HistEntry> = uniq(a, { it.id }, { it.ts }).take(MAX_HIST)

fun normalizeCalc(a: List<CalcEntry>): List<CalcEntry> = uniq(a, { it.id }, { it.ts }).take(MAX_CALC)

// ---------------------------------------------------------------- JSON <-> data

private fun str(v: Any?, def: String = ""): String = when (v) {
    is String -> v
    is Double -> jsNumStr(v)
    else -> def
}

private fun dbl(v: Any?): Double = if (v is Double) v else Double.NaN

private fun lng(v: Any?): Long = if (v is Double && v.isFinite()) v.toLong() else 0L

internal fun histFrom(m: Map<*, *>): HistEntry = HistEntry(
    id = str(m["id"]),
    t = str(m["t"]),
    ts = lng(m["ts"]),
    item = str(m["item"]),
    rp = dbl(m["rp"]),
    rw = dbl(m["rw"]),
    ru = str(m["ru"]),
    w = dbl(m["w"]),
    wu = str(m["wu"]),
    total = dbl(m["total"]),
    amt = dbl(m["amt"]),
    grams = dbl(m["grams"])
)

private fun calcFrom(m: Map<*, *>): CalcEntry = CalcEntry(
    id = str(m["id"]),
    ts = lng(m["ts"]),
    expr = str(m["expr"]),
    result = dbl(m["result"])
)

/**
 * loadLocal(): text is what was saved last time (null = nothing saved yet).
 * Bad or missing data gives the defaults. Old calculator results kept inside the price history are moved to the calculator list.
 */
fun loadState(text: String?): AppData {
    var form = Form()
    var hist: List<HistEntry> = emptyList()
    var calc: List<CalcEntry> = emptyList()
    if (text == null || text.isEmpty()) return AppData(form, hist, calc)
    try {
        val d = Json.parse(text)
        if (d is Map<*, *>) {
            val f = d["form"]
            if (f is Map<*, *>) {
                val def = Form()
                form = fixUnits(
                    Form(
                        item = str(f["item"], def.item), rp = str(f["rp"], def.rp), rw = str(f["rw"], def.rw),
                        ru = str(f["ru"], def.ru), w = str(f["w"], def.w), wu = str(f["wu"], def.wu),
                        m = str(f["m"], def.m), amt = str(f["amt"], def.amt)
                    )
                )
            }
            val h = d["history"]
            val c = d["calc"]
            val calcList = ArrayList<CalcEntry>()
            if (c is List<*>) for (e in c) if (e is Map<*, *>) calcList.add(calcFrom(e))
            val histList = ArrayList<HistEntry>()
            if (h is List<*>) for (e in h) {
                if (e is Map<*, *>) {
                    if (e["t"] == "c") calcList.add(calcFrom(e)) else histList.add(histFrom(e))
                }
            }
            hist = histList
            calc = calcList
        }
    } catch (x: Exception) {
        // keep whatever was read before the problem, like the original try/catch
    }
    return AppData(form, normalizeHistory(hist), normalizeCalc(calc))
}

private fun formMap(f: Form): Map<String, Any?> = linkedMapOf(
    "item" to f.item, "rp" to f.rp, "rw" to f.rw, "ru" to f.ru,
    "w" to f.w, "wu" to f.wu, "m" to f.m, "amt" to f.amt
)

private fun histMap(e: HistEntry): Map<String, Any?> =
    if (e.t == "q") linkedMapOf(
        "id" to e.id, "t" to e.t, "ts" to e.ts, "item" to e.item, "rp" to e.rp, "rw" to e.rw,
        "ru" to e.ru, "amt" to e.amt, "grams" to e.grams
    ) else linkedMapOf(
        "id" to e.id, "t" to e.t, "ts" to e.ts, "item" to e.item, "rp" to e.rp, "rw" to e.rw,
        "ru" to e.ru, "w" to e.w, "wu" to e.wu, "total" to e.total
    )

private fun calcMap(e: CalcEntry): Map<String, Any?> =
    linkedMapOf("id" to e.id, "t" to "c", "ts" to e.ts, "expr" to e.expr, "result" to e.result)

fun histListJson(a: List<HistEntry>): String = Json.stringify(a.map { histMap(it) })

/** what is written to storage: JSON.stringify(state) */
fun serializeState(form: Form, history: List<HistEntry>, calc: List<CalcEntry>): String =
    Json.stringify(
        linkedMapOf(
            "form" to formMap(form),
            "history" to history.map { histMap(it) },
            "calc" to calc.map { calcMap(it) }
        )
    )

// ---------------------------------------------------------------- price actions

sealed class SaveResult {
    /** nothing to save (rate/weight/amount missing) */
    object Invalid : SaveResult()

    /** same as the newest saved entry: show "এটা আগেই সেভ করা আছে" */
    object Duplicate : SaveResult()

    /** saved: show "Saved ✓" */
    data class Saved(val history: List<HistEntry>) : SaveResult()
}

const val FLASH_SAVED = "Saved ✓"
const val FLASH_DUPLICATE = "Already saved"

/** sig(): the entry without id and time */
private fun sameEntry(a: HistEntry, b: HistEntry): Boolean = a.copy(id = "", ts = 0L) == b.copy(id = "", ts = 0L)

/** saveEntry() */
fun saveEntry(form: Form, history: List<HistEntry>, ts: Long, id: String): SaveResult {
    val c = compute(form.rp, form.rw, form.ru, form.w, form.wu, form.amt)
    val q = form.m == "p"
    val item = form.item.trim().take(60)
    val e: HistEntry
    if (q) {
        if (!(c.rateOk && c.amtOk)) return SaveResult.Invalid
        e = HistEntry(
            id = id, t = "q", ts = ts, item = item, rp = c.rp, rw = c.rwv, ru = form.ru,
            amt = c.amt, grams = r1(c.grams)
        )
    } else {
        if (!(c.rateOk && c.weightOk)) return SaveResult.Invalid
        e = HistEntry(
            id = id, t = "p", ts = ts, item = item, rp = c.rp, rw = c.rwv, ru = form.ru,
            w = c.wv, wu = form.wu, total = r2(c.total)
        )
    }
    val top = history.firstOrNull()
    if (top != null && top.t == e.t && sameEntry(top, e)) return SaveResult.Duplicate
    return SaveResult.Saved((listOf(e) + history).take(MAX_HIST))
}

/** the "আবার" button in the price history: put a saved price back into the form */
fun reuseForm(form: Form, e: HistEntry): Form {
    var f = form.copy(item = e.item, rp = jsNumStr(e.rp), rw = jsNumStr(e.rw), ru = e.ru)
    f = if (e.t == "q") f.copy(m = "p", amt = jsNumStr(e.amt))
    else f.copy(m = "w", w = jsNumStr(e.w), wu = e.wu)
    return fixUnits(f)
}

/** addCalc(): newest first, at most 300 */
fun addCalcEntry(list: List<CalcEntry>, e: CalcEntry): List<CalcEntry> = (listOf(e) + list).take(MAX_CALC)
