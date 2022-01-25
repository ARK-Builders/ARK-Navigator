package space.taran.arknavigator.ui.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible

class LoadingTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    private var isLoading = false
    private var isMakingDots = false

    var loadingText: String = ""
        set(value) {
            field = value
            text = loadingText
            if (text.isNotEmpty() && !isMakingDots)
                makeLoadingDots()
        }

    private var dotCount = 0
        set(value) {
            field = if (value >= 4) 0
            else value
        }

    fun setVisibilityAndLoadingStatus(visibility: Int) {
        this.visibility = visibility
        dotCount = 0
        isLoading = isVisible
    }

    private fun makeLoadingDots() {
        isMakingDots = true
        Handler(Looper.getMainLooper()).postDelayed({
            if (isLoading) {
                dotCount++

                val textToDisplay = "$loadingText${".".repeat(dotCount)}"
                this.text = textToDisplay
                makeLoadingDots()
            } else isMakingDots = false
        }, 500)
    }
}
