package space.taran.arkbrowser.utils

import android.view.View
import android.view.Window

object FullscreenModeHelper {
    fun setSystemUIVisibility(isVisible: Boolean, window: Window) {
        if (isVisible) showSystemUI(window) else hideSystemUI(window)
    }

    fun hideSystemUI(window: Window) {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
 //                   or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    fun showSystemUI(window: Window) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}