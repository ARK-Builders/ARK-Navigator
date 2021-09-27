package space.taran.arknavigator.utils.extensions

import android.view.View

fun View.changeEnabledStatus(isEnabledStatus: Boolean){
    isEnabled = isEnabledStatus
    isClickable = isEnabledStatus
    isFocusable = isEnabledStatus
}

fun View.makeGone(){
    visibility = View.GONE
}

fun View.makeVisible(){
    visibility = View.VISIBLE
}

fun View.makeVisibleAndSetOnClickListener(action: () -> Unit){
    setOnClickListener{ action() }
    visibility = View.VISIBLE
}