package space.taran.arknavigator.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.ui.fragments.GalleryFragment.Companion.KIND_TAGS_PREFIX_KEY
import space.taran.arknavigator.utils.Tag

class TagsSelectorAdapter(
    private val checkedChipGroup: ChipGroup,
    private val chipGroup: ChipGroup,
    private val presenter: TagsSelectorPresenter,
    private var selectedTag: Tag
) {
    private val clearChip = createClearChip()
    private val chipsByTags = mutableMapOf<Tag, Chip>()

    fun drawTags() {
        chipGroup.removeAllViews()
        checkedChipGroup.removeAllViews()
        createChips()
        drawIncludedAndExcludedTags()
        drawClearBtn()
        drawUnavailableTags()
        drawAvailableTags()
    }

    private fun createChips() {
        chipsByTags.clear()
        presenter.includedTags.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.BLUE)
            chip.isChecked = true
            chipsByTags[tag] = chip
        }
        presenter.excludedTags.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.RED)
            chip.isLongClickable = false
            chipsByTags[tag] = chip
        }
        presenter.availableTagsForDisplay.forEach { tag ->
            val chip = createDefaultChip(tag)
            chipsByTags[tag] = chip
        }
        presenter.unavailableTagsForDisplay.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.GRAY)
            chip.isLongClickable = false
            chip.isClickable = false
            chip.isCheckable = false
            chipsByTags[tag] = chip
        }
    }

    private fun drawIncludedAndExcludedTags() {
        presenter.includedAndExcludedTagsForDisplay.forEach { tag ->
            if (presenter.filterEnabled)
                checkedChipGroup.addView(chipsByTags[tag])
            else
                chipGroup.addView(chipsByTags[tag])
        }
    }

    private fun drawAvailableTags() {
        presenter.availableTagsForDisplay.forEach { tag ->
            chipGroup.addView(chipsByTags[tag])
        }
        val tag = selectedTag
        if (selectedTag != "") {
            selectedTag = ""
            presenter.onTagClick(tag)
        }
    }

    private fun drawUnavailableTags() {
        presenter.unavailableTagsForDisplay.forEach { tag ->
            chipGroup.addView(chipsByTags[tag])
        }
    }

    private fun drawClearBtn() {
        if (presenter.isClearBtnVisible) {
            if (presenter.filterEnabled)
                checkedChipGroup.addView(clearChip)
            else
                chipGroup.addView(clearChip)
        }
    }

    private fun createDefaultChip(tag: Tag) = Chip(chipGroup.context).apply {
        this.isClickable = true
        this.isLongClickable = true
        this.isCheckable = true
        this.isChecked = false
        this.setTextColor(Color.BLACK)
        if (tag.contains(KIND_TAGS_PREFIX_KEY, ignoreCase = true)) {
            this.chipBackgroundColor =
                ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.blue)
                )
        } else {
            this.chipBackgroundColor =
                ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.grayTransparent)
                )
        }
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
        this.chipIconTint = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                R.color.black
            )
        )
        this.textStartPadding = 0f
        this.textEndPadding = 0f
        this.setOnClickListener {
            presenter.onClearClick()
        }
    }
}
