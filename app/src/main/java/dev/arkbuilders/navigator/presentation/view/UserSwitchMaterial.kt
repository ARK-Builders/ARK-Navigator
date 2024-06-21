package dev.arkbuilders.navigator.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import com.google.android.material.switchmaterial.SwitchMaterial
import dev.arkbuilders.navigator.data.utils.LogTags.SETTINGS_SCREEN
import timber.log.Timber

class UserSwitchMaterial(
    context: Context,
    attrs: AttributeSet
) : SwitchMaterial(context, attrs) {

    private var checkedChangeListener: CustomCheckedChangeListener? = null

    fun setOnUserCheckedChangeListener(
        callback: (isChecked: Boolean) -> Unit
    ) {
        Timber.d(
            SETTINGS_SCREEN,
            "setOnUserCheckedChangeListener: ${this.id}, " + "$isChecked"
        )
        if (checkedChangeListener == null) {
            checkedChangeListener = CustomCheckedChangeListener(callback)
        } else {
            checkedChangeListener?.callback = callback
        }

        this.setOnCheckedChangeListener(checkedChangeListener)
    }

    fun toggleSwitchSilent(mIsChecked: Boolean) {
        if (isChecked != mIsChecked) {
            isChecked = mIsChecked
            jumpDrawablesToCurrentState()
        }
    }

    private class CustomCheckedChangeListener(
        var callback: (isChecked: Boolean) -> Unit
    ) : OnCheckedChangeListener {
        override fun onCheckedChanged(button: CompoundButton, isChecked: Boolean) {
            if (button.isPressed) {
                callback(isChecked)
            }
        }
    }
}
