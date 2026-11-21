package com.qalculator.app.logic

/** Same as UNITS / BASE / SWAP / dimOf in the original code. d = 'w' (weight) or 'v' (volume). */
data class UnitDef(val n: String, val f: Double, val d: Char)

data class BaseDef(val u: String, val n: String, val big: String)

val UNITS: Map<String, UnitDef> = mapOf(
    "g" to UnitDef("Gram", 1.0, 'w'),
    "kg" to UnitDef("Kilogram", 1000.0, 'w'),
    "ml" to UnitDef("Milliliter", 1.0, 'v'),
    "l" to UnitDef("Liter", 1000.0, 'v'),
    "poa" to UnitDef("Poa", 250.0, 'w')
)

val BASE: Map<Char, BaseDef> = mapOf(
    'w' to BaseDef("g", "Gram", "Kilogram"),
    'v' to BaseDef("ml", "Milliliter", "Liter")
)

val SWAP: Map<String, String> = mapOf("g" to "ml", "ml" to "g", "kg" to "l", "l" to "kg")

/** UNIT_OPTS (the unit picker list): value + label */
val UNIT_OPTS: List<Pair<String, String>> = listOf(
    "g" to "Gram",
    "kg" to "Kilogram",
    "ml" to "Milliliter",
    "l" to "Liter"
)

/** `UNITS[u] || UNITS.g` */
fun unitOf(u: String?): UnitDef = UNITS[u] ?: UNITS.getValue("g")

fun dimOf(u: String?): Char = unitOf(u).d

fun baseOf(d: Char): BaseDef = BASE.getValue(d)
