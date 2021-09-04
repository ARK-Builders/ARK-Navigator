package space.taran.arknavigator.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.presenter.adapter.TagsSelectorPresenter
import space.taran.arknavigator.utils.Tag

class TagsSelectorAdapter(val chipGroup: ChipGroup, val presenter: TagsSelectorPresenter) {
    private val clear = createClearChip()

    fun drawTags() {
        chipGroup.removeAllViews()
        drawIncludedTags()
        drawExcludedTags()
        drawAvailableTags()
        drawClear()
        drawUnavailableTags()
    }

    private fun drawIncludedTags() {
        presenter.includedTags.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.BLUE)
            chip.isChecked = true
            chipGroup.addView(chip)
        }
    }

    private fun drawExcludedTags() {
        presenter.excludedTags.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.RED)
            chip.isLongClickable = false
            chipGroup.addView(chip)
        }
    }

    private fun drawAvailableTags() {
        presenter.availableTags.forEach { tag ->
            val chip = createDefaultChip(tag)
            chipGroup.addView(chip)
        }
    }

    private fun drawUnavailableTags() {
        presenter.unavailableTags.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.GRAY)
            chip.isLongClickable = false
            chip.isClickable = false
            chip.isCheckable = false
            chipGroup.addView(chip)
        }
    }

    private fun drawClear() {
        if (presenter.isClearBtnVisible)
            chipGroup.addView(clear)
    }

    private fun createDefaultChip(tag: Tag) = Chip(chipGroup.context).apply {
        this.isClickable = true
        this.isLongClickable = true
        this.isCheckable = true
        this.isChecked = false
        this.setTextColor(Color.BLACK)
        this.text = tag

        this.setOnClickListener {
            presenter.onTagClick(tag)
        }

        this.setOnLongClickListener {
            presenter.onTagLongClick(tag)
            true
        }
    }

    private fun createClearChip() = Chip(chipGroup.context).apply {
        this.chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_close)
        this.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        this.textStartPadding = 0f
        this.textEndPadding = 0f
        this.setOnClickListener {
            presenter.onClearClick()
        }
    }
}
