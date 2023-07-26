package dev.arkbuilders.navigator.utils

import android.content.Context
import android.util.TypedValue

fun Context.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        this.resources.displayMetrics
    )
