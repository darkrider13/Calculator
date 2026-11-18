package com.qalculator.app.logic

/*
 * Small JSON reader/writer. Writes exactly what JavaScript's JSON.stringify wrote,
 * so the saved data keeps the same format as the original app.
 * Values: null, Boolean, Double, String, List<Any?>, Map<String, Any?> (keeps key order).
 */
class JsonError(msg: String) : Exception(msg)

object Json {

    fun stringify(v: Any?): String {
        val sb = StringBuilder()
        write(sb, v)
        return sb.toString()
    }

    private fun write(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> quote(sb, v)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Double -> sb.append(if (v.isFinite()) jsNumStr(v) else "null")
            is Int -> sb.append(v.toString())
            is Long -> sb.append(v.toString())
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, x) in v) {
                    if (!first) sb.append(',')
                    first = false
                    quote(sb, k.toString())
                    sb.append(':')
                    write(sb, x)
                }
                sb.append('}')
            }
            is List<*> -> {
                sb.append('[')
                var first = true
                for (x in v) {
                    if (!first) sb.append(',')
                    first = false
                    write(sb, x)
                }
                sb.append(']')
            }
            else -> quote(sb, v.toString())
        }
    }

    private fun hex4(c: Int): String {
        val h = Integer.toHexString(c)
        return "\\u" + "0".repeat(4 - h.length) + h
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\b' -> sb.append("\\b")
                c == '\u000c' -> sb.append("\\f")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c.code < 0x20 -> sb.append(hex4(c.code))
                Character.isHighSurrogate(c) -> {
                    if (i + 1 < s.length && Character.isLowSurrogate(s[i + 1])) {
                        sb.append(c).append(s[i + 1])
                        i++
                    } else sb.append(hex4(c.code))
                }
                Character.isLowSurrogate(c) -> sb.append(hex4(c.code))
                else -> sb.append(c)
            }
            i++
        }
        sb.append('"')
    }

    /** Throws JsonError when the text is not valid JSON */
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.ws()
        val v = p.value()
        p.ws()
        if (p.i != text.length) throw JsonError("extra")
        return v
    }

    private class Parser(val s: String) {
        var i = 0

        fun ws() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\t' || s[i] == '\n' || s[i] == '\r')) i++
        }

        private fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) throw JsonError("literal")
            i += word.length
            return v
        }

        fun value(): Any? {
            if (i >= s.length) throw JsonError("end")
            return when (s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> number()
            }
        }

        private fun obj(): Any? {
            val m = LinkedHashMap<String, Any?>()
            i++
            ws()
            if (i < s.length && s[i] == '}') { i++; return m }
            while (true) {
                ws()
                if (i >= s.length || s[i] != '"') throw JsonError("key")
                val k = str()
                ws()
                if (i >= s.length || s[i] != ':') throw JsonError("colon")
                i++
                ws()
                m[k] = value()
                ws()
                if (i >= s.length) throw JsonError("end")
                if (s[i] == ',') { i++; continue }
                if (s[i] == '}') { i++; return m }
                throw JsonError("obj")
            }
        }

        private fun arr(): Any? {
            val l = ArrayList<Any?>()
            i++
            ws()
            if (i < s.length && s[i] == ']') { i++; return l }
            while (true) {
                ws()
                l.add(value())
                ws()
                if (i >= s.length) throw JsonError("end")
                if (s[i] == ',') { i++; continue }
                if (s[i] == ']') { i++; return l }
                throw JsonError("arr")
            }
        }

        private fun str(): String {
            val sb = StringBuilder()
            i++
            while (true) {
                if (i >= s.length) throw JsonError("string")
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c.code < 0x20) throw JsonError("control")
                if (c != '\\') { sb.append(c); continue }
                if (i >= s.length) throw JsonError("escape")
                val e = s[i++]
                when (e) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 4 > s.length) throw JsonError("unicode")
                        val h = s.substring(i, i + 4)
                        val n = h.toIntOrNull(16) ?: throw JsonError("unicode")
                        if (h.startsWith("+") || h.startsWith("-")) throw JsonError("unicode")
                        sb.append(n.toChar())
                        i += 4
                    }
                    else -> throw JsonError("escape")
                }
            }
        }

        private fun number(): Any? {
            val st = i
            if (i < s.length && s[i] == '-') i++
            val d0 = i
            while (i < s.length && s[i] in '0'..'9') i++
            if (i == d0) throw JsonError("number")
            if (s[d0] == '0' && i - d0 > 1) throw JsonError("number")
            if (i < s.length && s[i] == '.') {
                i++
                val f0 = i
                while (i < s.length && s[i] in '0'..'9') i++
                if (i == f0) throw JsonError("number")
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                val e0 = i
                while (i < s.length && s[i] in '0'..'9') i++
                if (i == e0) throw JsonError("number")
            }
            return s.substring(st, i).toDouble()
        }
    }
}
