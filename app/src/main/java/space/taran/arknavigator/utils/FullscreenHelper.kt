package space.taran.arknavigator.utils

import android.os.Build
import android.util.Log
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object FullscreenHelper {
    fun setSystemUIVisibility(
        isVisible: Boolean,
        window: Window,
        hasNavigationBar: Boolean
    ) {
        if (isVisible) showSystemUINew(window) else hideSystemUINew(window, hasNavigationBar)
    }

    private fun hideSystemUINew(window: Window, hasNavigation: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController ?: return
            windowInsetsController.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsets.Type.systemBars())
        } else {
            if (hasNavigation){
                window.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_IMMERSIVE
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN)
            } else window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun showSystemUINew(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController ?: return
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}