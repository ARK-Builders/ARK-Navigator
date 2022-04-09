package space.taran.arknavigator.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.CompoundButton
import com.google.android.material.switchmaterial.SwitchMaterial
import space.taran.arknavigator.utils.LogTags.SETTINGS_SCREEN

class UserSwitchMaterial(
    context: Context,
    attrs: AttributeSet
) : SwitchMaterial(context, attrs) {

    private var checkedChangeListener: CustomCheckedChangeListener? = null

    fun setOnUserCheckedChangeListener(
        callback: (isChecked: Boolean) -> Unit
    ) {
        Log.d(
            SETTINGS_SCREEN,
            "setOnUserCheckedChangeListener: ${this.id}, " + "$isChecked"
        )
        if (checkedChangeListener == null)
            checkedChangeListener = CustomCheckedChangeListener(callback)
        else checkedChangeListener?.callback = callback

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
            if (button.isPressed)
                callback(isChecked)
        }
    }
}
