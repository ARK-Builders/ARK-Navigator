package space.taran.arknavigator.extensions

import android.view.View

fun View.changeEnabledStatus(isEnabledStatus: Boolean){
    isEnabled = isEnabledStatus
    isClickable = isEnabledStatus
    isFocusable = isEnabledStatus
}