package space.taran.arknavigator.ui.view

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.StyleRes
import androidx.viewbinding.ViewBinding

class DefaultPopup(val binding: ViewBinding, @StyleRes val styleId: Int) {
    val popupWindow: PopupWindow

    init {
        binding.root.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        popupWindow = PopupWindow(binding.root.context)
        popupWindow.apply {
            contentView = binding.root
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            height =
                View.MeasureSpec.makeMeasureSpec(
                    binding.root.measuredHeight,
                    View.MeasureSpec.UNSPECIFIED
                )
            isFocusable = true
            animationStyle = styleId
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    fun showAbove(target: View) {
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
