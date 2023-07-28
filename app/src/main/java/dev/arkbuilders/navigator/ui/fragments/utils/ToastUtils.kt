package dev.arkbuilders.navigator.ui.fragments.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import dev.arkbuilders.navigator.R
import java.nio.file.Path

fun Context.toast(
    @StringRes stringId: Int,
    vararg args: Any,
    moreTime: Boolean = false,
) {
    val duration = if (moreTime) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(this, getString(stringId, *args), duration).show()
}

fun Context.toastFailedPaths(failedPaths: List<Path>) {
    if (failedPaths.isEmpty()) return
    val list = failedPaths.joinToString("\n")
    toast(R.string.toast_failed_paths, list, moreTime = true)
}

fun Fragment.toast(
    @StringRes stringId: Int,
    vararg args: Any,
    moreTime: Boolean = false,
) = requireContext().toast(stringId, *args, moreTime = moreTime)

fun Fragment.toastFailedPaths(
    failedPaths: List<Path>
) = requireContext().toastFailedPaths(failedPaths)
