package space.taran.arknavigator.utils.extensions

import android.view.View

fun View.changeEnabledStatus(isEnabledStatus: Boolean){
    isEnabled = isEnabledStatus
    isClickable = isEnabledStatus
    isFocusable = isEnabledStatus
}