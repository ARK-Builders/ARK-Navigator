package space.taran.arknavigator.utils

import android.view.View
import android.view.Window
import android.view.WindowManager

object FullscreenHelper {
    fun setSystemUIVisibility(isVisible: Boolean, window: Window, hasNavigationBar: Boolean) {
        if (isVisible) showSystemUI(window) else hideSystemUI(window, hasNavigationBar)
    }

    private fun hideSystemUI(window: Window, hasNavigation: Boolean) {
        if (hasNavigation){
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
        else window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    private fun showSystemUI(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}