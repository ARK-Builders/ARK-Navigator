package space.taran.arkbrowser.mvp.presenter

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.mvp.model.repo.TagsStorage
import space.taran.arkbrowser.utils.GALLERY_SCREEN
import space.taran.arkbrowser.utils.TAGS_SELECTOR
import space.taran.arkbrowser.utils.Tag
import space.taran.arkbrowser.utils.Tags
import java.lang.AssertionError

typealias Handler = (Set<ResourceId>) -> Unit

class TagsSelector(
    val tags: Tags,
    val resources: Set<ResourceId>,
    val storage: TagsStorage) {

    private lateinit var chips: Map<Tag, Chip>

    private lateinit var chipGroup: ChipGroup

    private val included = mutableSetOf<Tag>()
    private val excluded = mutableSetOf<Tag>()

    // this structure is calculated by included/excluded:
    private lateinit var selection: Set<ResourceId>

    fun draw(chipGroup: ChipGroup, context: Context, update: Handler) {
        this.chipGroup = chipGroup
        chipGroup.removeAllViews()

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

            chipGroup.addView(chip)
            chips[tag] = chip
        }

        this.chips = chips.toMap()
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

        update()
    }

    private fun update() {
        //todo: consider incremental calculation if performance will be not good enough
        val selectionWithTags = resources
            .map { resource ->
                val tags = storage.getTags(resource)
                resource to tags
            }
            .filter { (_, tags) ->
                tags.containsAll(included) && !excluded.any { tags.contains(it) }
            }
            .toMap()

        selection = selectionWithTags.keys

        val checked = included + excluded
        val available = selectionWithTags.values.flatten().toSet() - checked
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

        //todo: tags must be displayed in the following order:
        // checked tags go first
        // then available tags
        // not available tags go last, grayed-out

        //todo: update reset button
        if (tags.size > 1 && (included.isNotEmpty() || excluded.isNotEmpty())) {
//            val chip = Chip(context)
//            chip.chipIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_close)
//            chip.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.black))
//            chip.textStartPadding = 0f
//            chip.textEndPadding = 0f
//            chip.setOnClickListener {
//                //todo: call `onBack`
//                //tags_cg.removeAllViews()
//            }
//            chipGroup.addView(chip)
        }
    }
}
