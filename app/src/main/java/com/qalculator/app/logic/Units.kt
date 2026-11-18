package com.bazarhisab.app.logic

/** Same as UNITS / BASE / SWAP / dimOf in the original code. d = 'w' (weight) or 'v' (volume). */
data class UnitDef(val n: String, val f: Double, val d: Char)

data class BaseDef(val u: String, val n: String, val big: String)

val UNITS: Map<String, UnitDef> = mapOf(
    "g" to UnitDef("গ্রাম", 1.0, 'w'),
    "kg" to UnitDef("কেজি", 1000.0, 'w'),
    "ml" to UnitDef("মিলিলিটার", 1.0, 'v'),
    "l" to UnitDef("লিটার", 1000.0, 'v'),
    "poa" to UnitDef("পোয়া", 250.0, 'w')
)

val BASE: Map<Char, BaseDef> = mapOf(
    'w' to BaseDef("g", "গ্রাম", "কেজি"),
    'v' to BaseDef("ml", "মিলিলিটার", "লিটার")
)

val SWAP: Map<String, String> = mapOf("g" to "ml", "ml" to "g", "kg" to "l", "l" to "kg")

/** UNIT_OPTS (the unit picker list): value + label */
val UNIT_OPTS: List<Pair<String, String>> = listOf(
    "g" to "গ্রাম",
    "kg" to "কেজি",
    "ml" to "মিলিলিটার",
    "l" to "লিটার"
)

/** `UNITS[u] || UNITS.g` */
fun unitOf(u: String?): UnitDef = UNITS[u] ?: UNITS.getValue("g")

fun dimOf(u: String?): Char = unitOf(u).d

fun baseOf(d: Char): BaseDef = BASE.getValue(d)
