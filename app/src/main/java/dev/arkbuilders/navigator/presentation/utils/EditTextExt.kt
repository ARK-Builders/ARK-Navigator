package dev.arkbuilders.navigator.presentation.utils

import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat.getSystemService

fun EditText.showKeyboard() {
    val imm = getSystemService(context, InputMethodManager::class.java)
    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun EditText.closeKeyboard() {
    val imm = getSystemService(context, InputMethodManager::class.java)
    imm?.hideSoftInputFromWindow(this.windowToken, 0)
}

fun EditText.placeCursorToEnd() {
    requestFocus()
    post {
        setSelection(length())
    }
}
