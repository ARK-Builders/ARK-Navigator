package space.taran.arknavigator.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.textfield.TextInputEditText

class BackButtonEditText(context: Context, attrs: AttributeSet?) :
    TextInputEditText(context, attrs) {

    private var onBackPressedListener: (() -> Unit)? = null

    fun setOnBackPressedListener(onBackPressedListener: () -> Unit) {
        this.onBackPressedListener = onBackPressedListener
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action != KeyEvent.ACTION_DOWN)
            onBackPressedListener?.let { it() }

        return super.onKeyPreIme(keyCode, event)
    }
}
