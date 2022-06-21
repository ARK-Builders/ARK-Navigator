package space.taran.arknavigator.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.FragmentResourcesBinding
import space.taran.arknavigator.databinding.PopupResourcesTagMenuBinding
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagItem
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.ui.resource.StringProvider
import space.taran.arknavigator.ui.view.DefaultPopup
import javax.inject.Inject

class TagsSelectorAdapter(
    private val fragment: Fragment,
    private val binding: FragmentResourcesBinding,
    private val presenter: TagsSelectorPresenter
) {
    @Inject
    lateinit var stringProvider: StringProvider

    private val checkedChipGroup = binding.cgTagsChecked
    private val chipGroup = binding.tagsCg
    private val clearChip = binding.btnClear
    private val chipsByTagItems = mutableMapOf<TagItem, Chip>()

    fun drawTags() {
        drawClearChip()
        chipGroup.removeAllViews()
        checkedChipGroup.removeAllViews()

        if (checkTagsEmpty()) {
            binding.tvTagsSelectorHint.isVisible = true
            return
        } else
            binding.tvTagsSelectorHint.isVisible = false

        createChips()
        drawIncludedAndExcludedTags()
        drawAvailableTags()
        drawUnavailableTags()
    }

    private fun checkTagsEmpty(): Boolean = with(presenter) {
        return@with includedTagItems.isEmpty() &&
            excludedTagItems.isEmpty() &&
            availableTagsForDisplay.isEmpty() &&
            unavailableTagsForDisplay.isEmpty()
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
        isClickable = true
        isLongClickable = true
        isCheckable = true
        isChecked = false
        setTextColor(Color.BLACK)
        when (item) {
            is TagItem.PlainTagItem -> {
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
                text = stringProvider.kindToString(item.kind)
            }
        }

        setOnClickListener {
            presenter.onTagItemClick(item)
        }

        setOnLongClickListener {
            showTagMenuPopup(item, it)
            true
        }
    }

    private fun showTagMenuPopup(tag: TagItem, tagView: View) {
        val menuBinding = PopupResourcesTagMenuBinding
            .inflate(fragment.requireActivity().layoutInflater)
        val popup = DefaultPopup(menuBinding, R.style.BottomFadeScaleAnimation)
        menuBinding.apply {
            btnInvert.setOnClickListener {
                presenter.onTagItemLongClick(tag)
                popup.popupWindow.dismiss()
            }
        }
        popup.showAbove(tagView)
    }
}
