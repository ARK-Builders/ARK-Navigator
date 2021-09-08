package space.taran.arknavigator.utils

import android.view.View

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