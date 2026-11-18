package com.qalculator.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qalculator.app.logic.CalcEntry
import com.qalculator.app.logic.Form
import com.qalculator.app.logic.HistEntry
import com.qalculator.app.logic.STORAGE_KEY
import com.qalculator.app.logic.loadState
import com.qalculator.app.logic.serializeState

/**
 * The saved data (price form, price history, calculator history).
 * Same as `state` + localStorage in the original: loaded at start, saved 300ms after a change,
 * and saved right away when the app goes to the background.
 */
class AppState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("qalculator", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { saveNow() }
    private var pending = false

    var form by mutableStateOf(Form())
        private set
    var history by mutableStateOf<List<HistEntry>>(emptyList())
        private set
    var calc by mutableStateOf<List<CalcEntry>>(emptyList())
        private set

    init {
        val d = loadState(prefs.getString(STORAGE_KEY, null))
        form = d.form
        history = d.history
        calc = d.calc
    }

    /** price form changed (saveLocal) */
    fun updateForm(f: Form) {
        form = f
        saveLater()
    }

    /** price history changed (persistAll) */
    fun updateHistory(h: List<HistEntry>) {
        history = h
        saveLater()
    }

    /** calculator history changed (persistAll) */
    fun updateCalc(c: List<CalcEntry>) {
        calc = c
        saveLater()
    }

    private fun saveLater() {
        if (!pending) {
            pending = true
            handler.postDelayed(saveRunnable, 300)
        }
    }

    private fun saveNow() {
        handler.removeCallbacks(saveRunnable)
        pending = false
        try {
            prefs.edit().putString(STORAGE_KEY, serializeState(form, history, calc)).apply()
        } catch (x: Exception) {
            // like the original: saving problems are ignored
        }
    }

    /** app goes to the background: write now if a save is waiting */
    fun flushIfPending() {
        if (pending) saveNow()
    }
}
