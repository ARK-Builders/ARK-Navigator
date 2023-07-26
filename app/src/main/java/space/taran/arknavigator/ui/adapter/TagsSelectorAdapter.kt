package dev.arkbuilders.navigator.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.flexbox.FlexboxLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.FragmentResourcesBinding
import dev.arkbuilders.navigator.databinding.ItemTagBinding
import dev.arkbuilders.navigator.databinding.PopupResourcesTagMenuBinding
import dev.arkbuilders.navigator.mvp.presenter.adapter.tagsselector.TagItem
import dev.arkbuilders.navigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import dev.arkbuilders.navigator.ui.resource.StringProvider
import dev.arkbuilders.navigator.ui.view.DefaultPopup
import javax.inject.Inject

class TagsSelectorAdapter(
    private val fragment: Fragment,
    private val binding: FragmentResourcesBinding,
    private val presenter: TagsSelectorPresenter
) {
    @Inject
    lateinit var stringProvider: StringProvider

    private val tagsAdapter = ItemAdapter<TagItemView>()
    private val filterTagsAdapter = ItemAdapter<TagItemView>()

    private val clearChip = binding.btnClear

    init {
        binding.rvTags.apply {
            layoutManager = FlexboxLayoutManager(fragment.requireContext())
            adapter = FastAdapter.with(tagsAdapter)
            itemAnimator = null
        }

        binding.rvTagsFilter.apply {
            layoutManager = FlexboxLayoutManager(fragment.requireContext())
            adapter = FastAdapter.with(filterTagsAdapter)
            itemAnimator = null
        }
    }

    fun drawTags() {
        drawClearChip()

        if (checkTagsEmpty()) {
            binding.tvTagsSelectorHint.isVisible = true
            return
        } else
            binding.tvTagsSelectorHint.isVisible = false

        val includedTagItemView = presenter.includedTagItems.map {
            createTagItemView(it, TagSelectType.Included)
        }
        val excludedTagItemView = presenter.excludedTagItems.map {
            createTagItemView(it, TagSelectType.Excluded)
        }
        val availableTagItemView = presenter.availableTagsForDisplay.map {
            createTagItemView(it, TagSelectType.Available)
        }
        val unavailableTagItemView = presenter.unavailableTagsForDisplay.map {
            createTagItemView(it, TagSelectType.Unavailable)
        }

        val includedAndExcluded = includedTagItemView + excludedTagItemView
        val includedAndExcludedSorted =
            presenter.includedAndExcludedTagsForDisplay.map { tagItem ->
                includedAndExcluded.find { it.tagItem == tagItem }
                    ?: error("TagsSelectorPresenter: Tag inconsistency detected")
            }

        if (presenter.filterEnabled) {
            filterTagsAdapter.set(includedAndExcludedSorted)
            tagsAdapter.set(
                availableTagItemView +
                    unavailableTagItemView
            )
        } else {
            filterTagsAdapter.set(emptyList())
            tagsAdapter.set(
                includedAndExcludedSorted +
                    availableTagItemView +
                    unavailableTagItemView
            )
        }
    }

    private fun checkTagsEmpty(): Boolean = with(presenter) {
        return@with includedTagItems.isEmpty() &&
            excludedTagItems.isEmpty() &&
            availableTagsForDisplay.isEmpty() &&
            unavailableTagsForDisplay.isEmpty()
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

    private fun createTagItemView(tagItem: TagItem, tagType: TagSelectType) =
        TagItemView(
            fragment,
            fragment.requireContext(),
            presenter,
            stringProvider,
            tagItem,
            tagType
        )
}

private enum class TagSelectType {
    Included, Excluded, Available, Unavailable
}

private class TagItemView(
    private val fragment: Fragment,
    private val ctx: Context,
    private val presenter: TagsSelectorPresenter,
    private val stringProvider: StringProvider,
    val tagItem: TagItem,
    private val tagType: TagSelectType
) : AbstractBindingItem<ItemTagBinding>() {

    override val type = R.id.fastadapter_item

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ItemTagBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ItemTagBinding,
        payloads: List<Any>
    ) = with(binding.chipTag) {

        resetTagView(binding)

        setOnClickListener {
            presenter.onTagItemClick(tagItem)
        }

        setOnLongClickListener {
            showTagMenuPopup(tagItem, it)
            true
        }

        when (tagItem) {
            is TagItem.PlainTagItem -> {
                chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.grayTransparent)
                    )
                text = tagItem.tag
            }
            is TagItem.KindTagItem -> {
                chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.blue)
                    )
                text = stringProvider.kindToString(tagItem.kind)
            }
        }

        when (tagType) {
            TagSelectType.Included -> {
                setTextColor(Color.BLUE)
                isChecked = true
            }
            TagSelectType.Excluded -> {
                setTextColor(Color.RED)
                isLongClickable = false
            }
            TagSelectType.Available -> {}
            TagSelectType.Unavailable -> {
                setTextColor(Color.GRAY)
                isLongClickable = false
                isClickable = false
                isCheckable = false
            }
        }
    }

    private fun resetTagView(binding: ItemTagBinding) = binding.chipTag.apply {
        isClickable = true
        isLongClickable = true
        isCheckable = true
        isChecked = false
        setTextColor(Color.BLACK)
    }

    private fun showTagMenuPopup(tag: TagItem, tagView: View) {
        val menuBinding = PopupResourcesTagMenuBinding
            .inflate(fragment.requireActivity().layoutInflater)
        val popup = DefaultPopup(
            menuBinding,
            R.style.FadeAnimation,
            R.drawable.bg_rounded_8,
            24f
        )
        menuBinding.apply {
            btnInvert.setOnClickListener {
                presenter.onTagItemLongClick(tag)
                popup.popupWindow.dismiss()
            }
        }
        popup.showAbove(tagView)
    }
}
