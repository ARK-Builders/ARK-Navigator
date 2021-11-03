package space.taran.arknavigator.utils

import android.view.View
import android.view.Window

object FullscreenHelper {
    fun setSystemUIVisibility(isVisible: Boolean, window: Window) {
        if (isVisible) showSystemUI(window) else hideSystemUI(window)
    }

    private fun hideSystemUI(window: Window) {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun showSystemUI(window: Window) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}