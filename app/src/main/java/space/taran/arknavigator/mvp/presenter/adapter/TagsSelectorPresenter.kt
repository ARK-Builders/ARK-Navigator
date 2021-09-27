package space.taran.arknavigator.mvp.presenter.adapter

import android.util.Log
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.TAGS_SELECTOR
import space.taran.arknavigator.utils.Tag
import java.lang.AssertionError
import java.nio.file.Path

class TagsSelectorPresenter(
    private val viewState: ResourcesView,
    private val prefix: Path?,
    private val onSelectionChangeListener: (Set<ResourceId>) -> Unit
) {
    private var index: ResourcesIndex? = null
    private var storage: TagsStorage? = null

    var included = mutableSetOf<Tag>()
        private set
    var excluded = mutableSetOf<Tag>()
        private set

    var selection = setOf<ResourceId>()
        private set

    //this data is used by TagsSelectorAdapter
    var includedAndExcludedTags = listOf<Tag>()
        private set
    var availableTags = listOf<Tag>()
        private set
    var unavailableTags = listOf<Tag>()
        private set
    var isClearBtnVisible = false
        private set

    fun init(index: ResourcesIndex, storage: TagsStorage) {
        this.index = index
        this.storage = storage
    }

    fun onTagClick(tag: Tag) {
        if (excluded.contains(tag) || included.contains(tag)) {
            uncheckTag(tag)
        } else {
            includeTag(tag)
        }
    }

    fun onTagLongClick(tag: Tag) {
        if (!excluded.contains(tag) || included.contains(tag)) {
            excludeTag(tag)
        } else {
            uncheckTag(tag)
        }
    }

    fun onClearClick() {
        included.clear()
        excluded.clear()
        calculateTagsAndSelection()
    }

    fun calculateTagsAndSelection() {
        if (storage == null || index == null)
            return

        val resources = index!!.listIds(prefix)
        val tagsByResources = storage!!.groupTagsByResources(resources)
        val allTags = tagsByResources.values.flatten().toSet()

        //some tags could have been removed from storage
        excluded = excluded.intersect(allTags).toMutableSet()
        included = included.intersect(allTags).toMutableSet()

        val selectionAndComplementWithTags = tagsByResources
            .toList()
            .groupBy { (_, tags) ->
                tags.containsAll(included) && !excluded.any { tags.contains(it) }
            }

        val selectionWithTags = (selectionAndComplementWithTags[true] ?: emptyList()).toMap()
        val complementWithTags = (selectionAndComplementWithTags[false] ?: emptyList()).toMap()

        selection = selectionWithTags.keys
        val tagsOfSelectedResources = selectionWithTags.values.flatten()
        val tagsOfUnselectedResources = complementWithTags.values.flatten()

        val available = tagsOfSelectedResources.toSet() - included - excluded
        val unavailable = allTags - available - included - excluded

        val tagsOfSelectedResPopularity = Popularity.calculate(tagsOfSelectedResources)
        val tagsOfUnselectedResPopularity = Popularity.calculate(tagsOfUnselectedResources)
        val tagsPopularity = Popularity.calculate(tagsByResources.values.flatten())

        includedAndExcludedTags = (included + excluded).sortedByDescending { tagsPopularity[it] }
        availableTags = available.sortedByDescending { tagsOfSelectedResPopularity[it] }
        unavailableTags = unavailable.sortedByDescending { tagsOfUnselectedResPopularity[it] }

        Log.d(TAGS_SELECTOR, "tags included: $included")
        Log.d(TAGS_SELECTOR, "tags excluded: $excluded")
        Log.d(TAGS_SELECTOR, "tags available: $availableTags")
        Log.d(TAGS_SELECTOR, "tags unavailable: $unavailableTags")

        isClearBtnVisible = included.isNotEmpty() || excluded.isNotEmpty()

        onSelectionChangeListener(selection)

        if (allTags.isEmpty())
            viewState.setTagsSelectorHintEnabled(true)
        else
            viewState.setTagsSelectorHintEnabled(false)

        viewState.drawTags()
    }

    private fun includeTag(tag: Tag) {
        Log.d(TAGS_SELECTOR, "including tag $tag")

        included.add(tag)
        excluded.remove(tag)

        calculateTagsAndSelection()
    }

    private fun excludeTag(tag: Tag) {
        Log.d(TAGS_SELECTOR, "excluding tag $tag")

        excluded.add(tag)
        included.remove(tag)

        calculateTagsAndSelection()
    }

    private fun uncheckTag(tag: Tag, needToCalculate: Boolean = true) {
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

        if (needToCalculate)
            calculateTagsAndSelection()
    }
}