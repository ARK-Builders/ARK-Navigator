package space.taran.arknavigator.ui.fragments.utils

import android.content.Context
import android.widget.Toast
import java.nio.file.Path
import space.taran.arknavigator.mvp.view.NotifiableView

object Notifications {

    fun notifyUser(context: Context?, message: String, moreTime: Boolean) {
        val duration = if (moreTime) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context!!, message, duration).show()
    }

    fun notifyIfFailedPaths(view: NotifiableView, failed: List<Path>) {
        if (failed.isNotEmpty()) {
            view.notifyUser(
                message = "Failed to verify the following paths:\n" +
                    failed.joinToString("\n"),
                moreTime = true
            )
        }
    }
}
