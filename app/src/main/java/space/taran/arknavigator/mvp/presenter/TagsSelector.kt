package space.taran.arknavigator.mvp.presenter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import space.taran.arknavigator.R
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.TAGS_SELECTOR
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags
import java.lang.AssertionError

typealias Handler = (Set<ResourceId>) -> Unit

class TagsSelector(
    private val tags: Tags,
    private val resources: Collection<ResourceId>,
    private val storage: TagsStorage) {

    private lateinit var chips: Map<Tag, Chip>

    private lateinit var chipGroup: ChipGroup

    private val included = mutableSetOf<Tag>()
    private val excluded = mutableSetOf<Tag>()

    // this structure is calculated by included/excluded:
    private lateinit var selection: Set<ResourceId>

    private lateinit var clear: Chip

    fun drawChips(chipGroup: ChipGroup, context: Context, update: Handler) {
        this.chipGroup = chipGroup

        val chips = mutableMapOf<Tag, Chip>()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.isClickable = true
            chip.isLongClickable = true
            chip.isCheckable = true
            chip.isChecked = false
            chip.text = tag

            chip.setOnClickListener {
                if (!chip.isClickable) {
                    Log.d(TAGS_SELECTOR, "the chip isn't clickable")
                    return@setOnClickListener
                }

                if (excluded.contains(tag) || included.contains(tag)) {
                    uncheckTag(tag)
                } else {
                    includeTag(tag)
                }
                update(selection)
            }

            chip.setOnLongClickListener {
                if (!chip.isLongClickable) {
                    Log.d(TAGS_SELECTOR, "the chip isn't clickable")
                    return@setOnLongClickListener false
                }

                if (!excluded.contains(tag) || included.contains(tag)) {
                    excludeTag(tag)
                } else {
                    uncheckTag(tag)
                }
                update(selection)
                true
            }

            chips[tag] = chip
        }

        this.chips = chips.toMap()

        clear = Chip(context)
        clear.chipIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_close)
        clear.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.black))
        clear.textStartPadding = 0f
        clear.textEndPadding = 0f
        clear.setOnClickListener {
            //todo optimize multiple calls to update()
            (included + excluded).forEach { uncheckTag(it) }
            update(selection)
        }

        update()
    }

    private fun includeTag(tag: Tag) {
        Log.d(TAGS_SELECTOR, "including tag $tag")

        chips[tag]!!.setTextColor(Color.BLUE)
        included.add(tag)
        excluded.remove(tag)

        update()
    }

    private fun excludeTag(tag: Tag) {
        Log.d(TAGS_SELECTOR, "excluding tag $tag")

        chips[tag]!!.setTextColor(Color.RED)
        excluded.add(tag)
        included.remove(tag)

        update()
    }

    private fun uncheckTag(tag: Tag) {
        Log.d(TAGS_SELECTOR, "un-checking tag $tag")

        if (included.contains(tag) && excluded.contains(tag)) {
            throw AssertionError("The tag is both included and excluded")
        }
        if (!included.contains(tag) && !excluded.contains(tag)) {
            throw AssertionError("The tag is neither included nor excluded")
        }

        if (!included.remove(tag)) {
            excluded.remove(tag)
        }

        chips[tag]!!.isChecked = false

        update()
    }

    private fun update() {
        val selectionAndComplementWithTags = resources
            .map { resource ->
                val tags = storage.getTags(resource)
                resource to tags
            }
            .groupBy { (_, tags) ->
                tags.containsAll(included) && !excluded.any { tags.contains(it) }
            }

        val selectionWithTags = (selectionAndComplementWithTags[true] ?: emptyList()).toMap()
        val complementWithTags = (selectionAndComplementWithTags[false] ?: emptyList()).toMap()

        selection = selectionWithTags.keys
        val tagsOfSelectedResources = selectionWithTags.values.flatten()
        val tagsOfUnselectedResources = complementWithTags.values.flatten()

        val checked = included + excluded
        val available = tagsOfSelectedResources.toSet() - checked
        val unavailable = tags - available - checked

        Log.d(TAGS_SELECTOR, "tags checked: $checked")
        Log.d(TAGS_SELECTOR, "tags available: $available")
        Log.d(TAGS_SELECTOR, "tags unavailable: $unavailable")

        available.forEach { tag ->
            val chip = chips[tag]!!
            chip.setTextColor(Color.BLACK)
            chip.isLongClickable = true
            chip.isClickable = true
            chip.isCheckable = true
        }

        unavailable.forEach { tag ->
            val chip = chips[tag]!!
            chip.setTextColor(Color.GRAY)
            chip.isLongClickable = false
            chip.isClickable = false
            chip.isCheckable = false
        }

        chipGroup.removeAllViews()

        //todo: this ordering algorithm makes more sense, but with current selector style
        // visually it feels too annoying. original algorithm should be used when alternative
        // "tag cloud" style selector will be adopted
        //pushChips(checked, Popularity.calculate(tagsOfSelectedResources))
        //pushChips(available, Popularity.calculate(tagsOfSelectedResources))
        //pushChips(unavailable, Popularity.calculate(tagsOfUnselectedResources))

        val allTags = tagsOfSelectedResources + tagsOfUnselectedResources
        val popularity = Popularity.calculate(allTags)

        pushChips(checked + available, popularity)
        if ((included + excluded).size > 1) {
            chipGroup.addView(clear)
        }
        pushChips(unavailable, popularity)
    }

    private fun pushChips(tags: Tags, popularity: Map<Tag, Int>) {
        tags.sortedByDescending { popularity[it] }
            .forEach { chipGroup.addView(chips[it]) }
    }
}
