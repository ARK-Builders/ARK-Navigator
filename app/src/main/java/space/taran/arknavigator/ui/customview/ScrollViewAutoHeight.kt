package space.taran.arknavigator.ui.customview

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.WindowManager
import android.widget.ScrollView

class ScrollViewAutoHeight @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    companion object {
        private const val PART_OF_SCREEN = 4
    }

    private val defaultDisplay = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    private val size = Point()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        defaultDisplay.getSize(size)
        val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getNewHeight(), MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }

    private fun getNewHeight() = size.y / PART_OF_SCREEN
}