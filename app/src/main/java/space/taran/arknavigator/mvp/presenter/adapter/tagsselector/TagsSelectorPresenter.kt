package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

import android.util.Log
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.TAGS_SELECTOR
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags
import java.nio.file.Path

class TagsSelectorPresenter(
    private val viewState: ResourcesView,
    private val prefix: Path?,
    private val onSelectionChangeListener: (Set<ResourceId>) -> Unit
) {
    private var index: ResourcesIndex? = null
    private var storage: TagsStorage? = null
    private val actions = ArrayDeque<TagsSelectorAction>()
    private var filter = ""
    var filterEnabled = false
        private set

    var includedTags = mutableSetOf<Tag>()
        private set
    var excludedTags = mutableSetOf<Tag>()
        private set
    private var availableTags = listOf<Tag>()
    private var unavailableTags = listOf<Tag>()

    var selection = setOf<ResourceId>()
        private set

    //this data is used by TagsSelectorAdapter
    var includedAndExcludedTagsForDisplay = listOf<Tag>()
        private set
    var availableTagsForDisplay = listOf<Tag>()
        private set
    var unavailableTagsForDisplay = listOf<Tag>()
        private set
    var isClearBtnVisible = false
        private set

    fun init(index: ResourcesIndex, storage: TagsStorage) {
        this.index = index
        this.storage = storage
    }

    fun onTagClick(tag: Tag) {
        when {
            excludedTags.contains(tag) -> {
                actions.addLast(UncheckExcluded(tag))
                uncheckTag(tag)
            }
            includedTags.contains(tag) -> {
                actions.addLast(UncheckIncluded(tag))
                uncheckTag(tag)
            }
            else -> {
                actions.addLast(Include(tag))
                if (filterEnabled) resetFilter()
                includeTag(tag)
            }
        }
    }

    fun onTagLongClick(tag: Tag) {
        when {
            includedTags.contains(tag) -> {
                actions.addLast(UncheckAndExclude(tag))
                excludeTag(tag)
            }
            !excludedTags.contains(tag) -> {
                actions.addLast(Exclude(tag))
                if (filterEnabled) resetFilter()
                excludeTag(tag)
            }
            else -> {
                actions.addLast(UncheckExcluded(tag))
                uncheckTag(tag)
            }
        }
    }

    fun onClearClick() {
        actions.addLast(Clear(includedTags.toSet(), excludedTags.toSet()))
        includedTags.clear()
        excludedTags.clear()
        calculateTagsAndSelection()
    }

    fun onFilterChanged(filter: String) {
        this.filter = filter
        filterTags()
        viewState.drawTags()
    }

    fun onFilterToggle(enabled: Boolean) {
        if (filterEnabled != enabled) {
            viewState.setTagsFilterEnabled(enabled)
            filterEnabled = enabled
            if (enabled) {
                viewState.setTagsFilterText(filter)
                filterTags()
                viewState.drawTags()
            } else {
                resetTags()
                viewState.drawTags()
            }
        }
    }

    fun calculateTagsAndSelection() {
        Log.d(TAGS_SELECTOR, "calculating tags and selection")
        if (storage == null || index == null)
            return

        val resources = index!!.listIds(prefix)
        val tagsByResources = storage!!.groupTagsByResources(resources)
        val allTags = tagsByResources.values.flatten().toSet()

        //some tags could have been removed from storage
        excludedTags = excludedTags.intersect(allTags).toMutableSet()
        includedTags = includedTags.intersect(allTags).toMutableSet()

        val selectionAndComplementWithTags = tagsByResources
            .toList()
            .groupBy { (_, tags) ->
                tags.containsAll(includedTags) && !excludedTags.any { tags.contains(it) }
            }

        val selectionWithTags = (selectionAndComplementWithTags[true] ?: emptyList()).toMap()
        val complementWithTags = (selectionAndComplementWithTags[false] ?: emptyList()).toMap()

        selection = selectionWithTags.keys
        val tagsOfSelectedResources = selectionWithTags.values.flatten()
        val tagsOfUnselectedResources = complementWithTags.values.flatten()

        availableTags = (tagsOfSelectedResources.toSet() - includedTags - excludedTags).toList()
        unavailableTags = (allTags - availableTags - includedTags - excludedTags).toList()

        val tagsOfSelectedResPopularity = Popularity.calculate(tagsOfSelectedResources)
        val tagsOfUnselectedResPopularity = Popularity.calculate(tagsOfUnselectedResources)
        val tagsPopularity = Popularity.calculate(tagsByResources.values.flatten())
        availableTags = availableTags.sortedByDescending { tagsOfSelectedResPopularity[it] }
        unavailableTags = unavailableTags.sortedByDescending { tagsOfUnselectedResPopularity[it] }

        includedAndExcludedTagsForDisplay = (includedTags + excludedTags).sortedByDescending { tagsPopularity[it] }

        if (filterEnabled) filterTags()
        else resetTags()

        Log.d(TAGS_SELECTOR, "tags included: $includedTags")
        Log.d(TAGS_SELECTOR, "tags excluded: $excludedTags")
        Log.d(TAGS_SELECTOR, "tags available: $availableTagsForDisplay")
        Log.d(TAGS_SELECTOR, "tags unavailable: $unavailableTagsForDisplay")

        isClearBtnVisible = includedTags.isNotEmpty() || excludedTags.isNotEmpty()

        onSelectionChangeListener(selection)

        if (allTags.isEmpty())
            viewState.setTagsSelectorHintEnabled(true)
        else
            viewState.setTagsSelectorHintEnabled(false)

        viewState.drawTags()
    }

    fun onBackClick(): Boolean {
        if (actions.isEmpty())
            return false

        val action = findLastActualAction() ?: return false

        when(action) {
            is Include -> {
                includedTags.remove(action.tag!!)
            }
            is Exclude -> {
                excludedTags.remove(action.tag!!)
            }
            is UncheckIncluded -> {
                includedTags.add(action.tag!!)
            }
            is UncheckExcluded -> {
                excludedTags.add(action.tag!!)
            }
            is UncheckAndExclude -> {
                excludedTags.remove(action.tag!!)
                includedTags.add(action.tag)
            }
            is Clear -> {
                includedTags = action.included.toMutableSet()
                excludedTags = action.excluded.toMutableSet()
            }
        }

        actions.removeLast()

        calculateTagsAndSelection()

        return true
    }

    private fun findLastActualAction(): TagsSelectorAction? {
        val allTags = storage!!.getTags(index!!.listIds(prefix))

        while (actions.lastOrNull() != null) {
            val lastAction = actions.last()
            if (isActionActual(lastAction, allTags)) {
                return lastAction
            } else
                actions.removeLast()
        }

        return null
    }

    private fun isActionActual(action: TagsSelectorAction, allTags: Tags): Boolean {
        action.tag?.let { tag ->
            if (!allTags.contains(tag))
                return false
        } ?: let {
            action as Clear
            if (action.excluded.intersect(allTags).isEmpty() &&
                action.included.intersect(allTags).isEmpty())
                return false
        }
        return true
    }

    private fun resetFilter() {
        filter = ""
        viewState.setTagsFilterText(filter)
    }

    private fun filterTags() {
        availableTagsForDisplay = availableTags.filter { it.startsWith(filter, false) }
        unavailableTagsForDisplay = unavailableTags.filter { it.startsWith(filter, false) }
    }

    private fun resetTags() {
        availableTagsForDisplay = availableTags
        unavailableTagsForDisplay = unavailableTags
    }

    private fun includeTag(tag: Tag) {
        Log.d(TAGS_SELECTOR, "including tag $tag")

        includedTags.add(tag)
        excludedTags.remove(tag)

        calculateTagsAndSelection()
    }

    private fun excludeTag(tag: Tag) {
        Log.d(TAGS_SELECTOR, "excluding tag $tag")

        excludedTags.add(tag)
        includedTags.remove(tag)

        calculateTagsAndSelection()
    }

    private fun uncheckTag(tag: Tag, needToCalculate: Boolean = true) {
        Log.d(TAGS_SELECTOR, "un-checking tag $tag")

        if (includedTags.contains(tag) && excludedTags.contains(tag)) {
            throw AssertionError("The tag is both included and excluded")
        }
        if (!includedTags.contains(tag) && !excludedTags.contains(tag)) {
            throw AssertionError("The tag is neither included nor excluded")
        }

        if (!includedTags.remove(tag)) {
            excludedTags.remove(tag)
        }

        if (needToCalculate)
            calculateTagsAndSelection()
    }
}