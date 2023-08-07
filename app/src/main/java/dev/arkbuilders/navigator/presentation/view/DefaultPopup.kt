package dev.arkbuilders.navigator.presentation.view

import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.viewbinding.ViewBinding
import dev.arkbuilders.navigator.presentation.utils.dpToPx

class DefaultPopup(
    val binding: ViewBinding,
    @StyleRes val animationId: Int? = null,
    @DrawableRes val bgId: Int? = null,
    val elevation: Float = 0f,
) {
    lateinit var popupWindow: PopupWindow

    private fun init() {
        val context = binding.root.context
        binding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        popupWindow = PopupWindow(context)
        popupWindow.apply {
            contentView = binding.root
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            height =
                View.MeasureSpec.makeMeasureSpec(
                    binding.root.measuredHeight,
                    View.MeasureSpec.UNSPECIFIED
                )
            isFocusable = true
            animationId?.let { animationStyle = it }
            bgId?.let {
                val drawable = ResourcesCompat.getDrawable(
                    context.resources,
                    it,
                    null
                )
                setBackgroundDrawable(drawable)
            }
            elevation = context.dpToPx(this@DefaultPopup.elevation)
        }
    }

    fun showAbove(target: View) {
        init()
        val targetRect = getViewRectOnScreen(target)
        val xOffset = (target.width - binding.root.measuredWidth) / 2
        val popupLeft = targetRect.left + xOffset
        popupWindow.showAtLocation(
            target,
            Gravity.NO_GRAVITY,
            popupLeft,
            targetRect.top - binding.root.measuredHeight
        )
    }

    fun showBelow(target: View) {
        init()
        val targetRect = getViewRectOnScreen(target)
        val xOffset = (target.width - binding.root.measuredWidth) / 2
        val popupLeft = targetRect.left + xOffset
        popupWindow.showAtLocation(
            target,
            Gravity.NO_GRAVITY,
            popupLeft,
            targetRect.top + targetRect.height()
        )
    }

    private fun getViewRectOnScreen(view: View): Rect {
        val location = IntArray(2).apply {
            view.getLocationInWindow(this)
        }
        return Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
    }
}
