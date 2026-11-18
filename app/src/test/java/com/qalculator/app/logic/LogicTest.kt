package com.qalculator.app.logic

import org.junit.Assert.assertEquals
import org.junit.Test

/** Optional: `gradle testDebugUnitTest`. Not part of the APK build. */
class LogicTest {
    @Test
    fun matchesOriginalJavaScript() {
        val r = LogicVectors.run() + StateVectors.run()
        assertEquals(r.filter { !it.startsWith("checks run") }.joinToString("\n"), 0, r.count { !it.startsWith("checks run") })
    }
}
