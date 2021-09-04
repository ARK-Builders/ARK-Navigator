package space.taran.arknavigator.mvp.presenter.adapter

import android.util.Log
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.TAGS_SELECTOR
import space.taran.arknavigator.utils.Tag
import java.lang.AssertionError

class TagsSelectorPresenter(
    val viewState: ResourcesView,
    val onSelectionChangeListener: (Set<ResourceId>) -> Unit
) {
    private var resources: List<ResourceId>? = null
    private var storage: TagsStorage? = null

    private val included = mutableSetOf<Tag>()
    private val excluded = mutableSetOf<Tag>()

    //this data is used by TagsSelectorAdapter
    var includedTags = listOf<Tag>()
    var excludedTags = listOf<Tag>()
    var availableTags = listOf<Tag>()
    var unavailableTags = listOf<Tag>()
    var isClearBtnVisible = false

    fun init(resources: List<ResourceId>, storage: TagsStorage) {
        this.resources = resources
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
        (included + excluded).forEach { uncheckTag(it, needToCalculate = false) }
        calculateTagsAndSelection()
    }

    fun calculateTagsAndSelection() {
        if (storage == null || resources == null)
            return

        val allTags = storage!!.listAllTags()

        val selectionAndComplementWithTags = resources!!
            .map { resource ->
                val tags = storage!!.getTags(resource)
                resource to tags
            }
            .groupBy { (_, tags) ->
                tags.containsAll(included) && !excluded.any { tags.contains(it) }
            }

        val selectionWithTags = (selectionAndComplementWithTags[true] ?: emptyList()).toMap()
        val complementWithTags = (selectionAndComplementWithTags[false] ?: emptyList()).toMap()

        val selection = selectionWithTags.keys
        val tagsOfSelectedResources = selectionWithTags.values.flatten()
        val tagsOfUnselectedResources = complementWithTags.values.flatten()

        val available = tagsOfSelectedResources.toSet() - included - excluded
        val unavailable = allTags - available - included - excluded

        val tagsOfSelectedResPopularity = Popularity.calculate(tagsOfSelectedResources)
        val tagsOfUnselectedResPopularity = Popularity.calculate(tagsOfUnselectedResources)

        includedTags = included.toMutableList().sortedByDescending { tagsOfSelectedResPopularity[it] }
        excludedTags = excluded.toMutableList().sortedByDescending { tagsOfSelectedResPopularity[it] }
        availableTags = available.toMutableList().sortedByDescending { tagsOfSelectedResPopularity[it] }
        unavailableTags = unavailable.toMutableList().sortedByDescending { tagsOfUnselectedResPopularity[it] }

        Log.d(TAGS_SELECTOR, "tags included: $includedTags")
        Log.d(TAGS_SELECTOR, "tags excluded: $excludedTags")
        Log.d(TAGS_SELECTOR, "tags available: $availableTags")
        Log.d(TAGS_SELECTOR, "tags unavailable: $unavailableTags")

        isClearBtnVisible = (included + excluded).size > 1

        onSelectionChangeListener(selection)
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