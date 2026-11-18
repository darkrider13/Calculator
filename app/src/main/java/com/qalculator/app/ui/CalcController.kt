package com.bazarhisab.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.bazarhisab.app.AppState
import com.bazarhisab.app.logic.CalcEntry
import com.bazarhisab.app.logic.CalcState
import com.bazarhisab.app.logic.VState
import com.bazarhisab.app.logic.VView
import com.bazarhisab.app.logic.commitVerify
import com.bazarhisab.app.logic.jsNumStr
import com.bazarhisab.app.logic.openVerify
import com.bazarhisab.app.logic.vDelete
import com.bazarhisab.app.logic.vEditStart
import com.bazarhisab.app.logic.vInputFilter
import com.bazarhisab.app.logic.vRender
import com.bazarhisab.app.logic.vSaveEdit
import com.bazarhisab.app.logic.vSelect
import com.bazarhisab.app.logic.vStep
import com.bazarhisab.app.logic.addCalcEntry
import com.bazarhisab.app.logic.commitEdit
import com.bazarhisab.app.logic.moveCaret
import com.bazarhisab.app.logic.press
import com.bazarhisab.app.logic.renderCalc
import com.bazarhisab.app.logic.tapNum

/**
 * Holds the `calc` object of the original (the calculator tab state). It lives as long as the app screen,
 * so switching tabs keeps the expression, like the original page.
 */
class CalcController(private val app: AppState) {
    var state by mutableStateOf(renderCalc(CalcState()))
        private set

    /** a keypad key (press) */
    fun onKey(k: String) {
        val r = press(state, k)
        state = r.state
        val e = r.entry
        if (e != null) app.updateCalc(addCalcEntry(app.calc, e)) // addCalc
    }

    /** tap on number k of the expression: edit it */
    fun onTapNum(k: Int) {
        state = tapNum(state, k)
    }

    /** tap inside the number box: move the cursor */
    fun onCaret(pos: Int) {
        state = moveCaret(state, pos)
    }

    /** end the number edit (openSheet calls commitEdit) */
    fun commit() {
        state = commitEdit(state).first
    }

    // ------------------------------------------------------------ calculator history sheet (#csheet)

    /** the saved calculations (state.calc) */
    val entries: List<CalcEntry> get() = app.calc

    var sheetOpen by mutableStateOf(false)
        private set

    /** openSheet(): the number edit ends, then the sheet shows */
    fun openSheet() {
        commit()
        sheetOpen = true
    }

    /** closeSheet(): both sheets hide */
    fun closeSheet() {
        verify = null
        vView = null
        vEditing = false
        sheetOpen = false
    }

    /** "ব্যবহার": the saved result goes back to the display. Results written with an exponent are skipped. */
    fun useEntry(id: String) {
        val e = app.calc.firstOrNull { it.id == id } ?: return
        val str = jsNumStr(e.result)
        if (str.contains('e') || str.contains('E')) return
        state = renderCalc(state.copy(expr = str, last = e.expr, fresh = true, err = ""))
        closeSheet()
    }

    fun deleteEntry(id: String) {
        app.updateCalc(app.calc.filter { it.id != id })
    }

    fun clearEntries() {
        app.updateCalc(emptyList())
    }

    // ------------------------------------------------------------ মিলান sheet (#vsheet)

    var verify by mutableStateOf<VState?>(null)
        private set
    var vView by mutableStateOf<VView?>(null)
        private set

    /** the edit row (সেভ / বাতিল) is open */
    var vEditing by mutableStateOf(false)
        private set

    private fun setV(s: VState) {
        val r = vRender(s)
        verify = r.first
        vView = r.second
    }

    fun openVerifyFor(id: String) {
        val s = openVerify(app.calc, id) ?: return
        vEditing = false
        setV(s)
    }

    fun vStepBy(d: Int) {
        val s = verify ?: return
        vEditing = false
        val n = vStep(s, d) ?: return
        setV(n)
    }

    fun vSelectChip(i: Int) {
        val s = verify ?: return
        vEditing = false
        setV(vSelect(s, i))
    }

    /** the text in the edit box (#v-in) */
    var vEdit by mutableStateOf(TextFieldValue(""))
        private set

    /** openEditRow(): the box shows the number as text, the cursor at the end */
    fun vOpenEdit() {
        val s = verify ?: return
        val t = vEditStart(s)
        vEdit = TextFieldValue(t, TextRange(t.length))
        vEditing = true
    }

    /** the "input" event: Bengali digits become English */
    fun vEditChange(nv: TextFieldValue) {
        vEdit = nv.copy(text = vInputFilter(nv.text))
    }

    fun vCancelEdit() {
        vEditing = false
    }

    /** saveEdit(): false = the number is not valid, the box stays open */
    fun vSave(text: String): Boolean {
        val s = verify ?: return false
        val n = vSaveEdit(s, text) ?: return false
        vEditing = false
        app.updateCalc(commitVerify(app.calc, n))
        setV(n)
        return true
    }

    /** the delete button after the popup says yes */
    fun vDeleteNumber() {
        val s = verify ?: return
        val n = vDelete(s) ?: return
        app.updateCalc(commitVerify(app.calc, n))
        setV(n)
    }

    /** the back arrow of the verify sheet */
    fun vBack() {
        verify = null
        vView = null
    }
}
