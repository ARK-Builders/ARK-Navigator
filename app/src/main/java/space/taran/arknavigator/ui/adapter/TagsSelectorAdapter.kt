package space.taran.arknavigator.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.repo.index.ResourceKind
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagItem
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter

class TagsSelectorAdapter(
    private val context: Context,
    private val checkedChipGroup: ChipGroup,
    private val chipGroup: ChipGroup,
    private val clearChip: Chip,
    private val presenter: TagsSelectorPresenter
) {
    private val chipsByTagItems = mutableMapOf<TagItem, Chip>()
    private val kindToString: Map<ResourceKind, String> by lazy {
        mapOf(
            ResourceKind.IMAGE to context.getString(R.string.kind_image),
            ResourceKind.DOCUMENT
                to context.getString(R.string.kind_document),
            ResourceKind.VIDEO to context.getString(R.string.kind_video)
        )
    }

    fun drawTags() {
        drawClearChip()
        chipGroup.removeAllViews()
        checkedChipGroup.removeAllViews()
        createChips()
        drawIncludedAndExcludedTags()
        drawAvailableTags()
        drawUnavailableTags()
    }

    private fun createChips() {
        chipsByTagItems.clear()
        presenter.includedTagItems.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.BLUE)
            chip.isChecked = true
            chipsByTagItems[tag] = chip
        }
        presenter.excludedTagItems.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.RED)
            chip.isLongClickable = false
            chipsByTagItems[tag] = chip
        }
        presenter.availableTagsForDisplay.forEach { tag ->
            val chip = createDefaultChip(tag)
            chipsByTagItems[tag] = chip
        }
        presenter.unavailableTagsForDisplay.forEach { tag ->
            val chip = createDefaultChip(tag)
            chip.setTextColor(Color.GRAY)
            chip.isLongClickable = false
            chip.isClickable = false
            chip.isCheckable = false
            chipsByTagItems[tag] = chip
        }
    }

    private fun drawIncludedAndExcludedTags() {
        presenter.includedAndExcludedTagsForDisplay.forEach { tag ->
            if (presenter.filterEnabled)
                checkedChipGroup.addView(chipsByTagItems[tag])
            else
                chipGroup.addView(chipsByTagItems[tag])
        }
    }

    private fun drawAvailableTags() {
        presenter.availableTagsForDisplay.forEach { tag ->
            chipGroup.addView(chipsByTagItems[tag])
        }
    }

    private fun drawUnavailableTags() {
        presenter.unavailableTagsForDisplay.forEach { tag ->
            chipGroup.addView(chipsByTagItems[tag])
        }
    }

    private fun drawClearChip() = clearChip.apply {
        if (presenter.isClearBtnEnabled) {
            isEnabled = true
            chipIconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.black)
            )
        } else {
            isEnabled = false
            chipIconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.grayTransparent)
            )
        }
    }

    private fun createDefaultChip(item: TagItem) = Chip(chipGroup.context).apply {
        this.isClickable = true
        this.isLongClickable = true
        this.isCheckable = true
        this.isChecked = false
        this.setTextColor(Color.BLACK)
        when (item) {
            is TagItem.DefaultTagItem -> {
                chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.grayTransparent)
                    )
                text = item.tag
            }
            is TagItem.KindTagItem -> {
                chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.blue)
                    )
                text = kindToString[item.kind]
            }
        }

        this.setOnClickListener {
            presenter.onTagItemClick(item)
        }

        this.setOnLongClickListener {
            presenter.onTagItemLongClick(item)
            true
        }
    }
}
