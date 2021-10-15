package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

import android.util.Log
import space.taran.arknavigator.mvp.model.IndexCache
import space.taran.arknavigator.mvp.model.RootAndFav
import space.taran.arknavigator.mvp.model.TagsCache
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.TAGS_SELECTOR
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags
import javax.inject.Inject

class TagsSelectorPresenter(
    private val viewState: ResourcesView,
    private val rootAndFav: RootAndFav,
    private val onSelectionChangeListener: (Set<ResourceId>) -> Unit
) {
    @Inject
    lateinit var indexCache: IndexCache

    @Inject
    lateinit var tagsCache: TagsCache

    private val actions = ArrayDeque<TagsSelectorAction>()

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

    fun onTagClick(tag: Tag) {
        when {
            excluded.contains(tag) -> {
                actions.addLast(UncheckExcluded(tag))
                uncheckTag(tag)
            }
            included.contains(tag) -> {
                actions.addLast(UncheckIncluded(tag))
                uncheckTag(tag)
            }
            else -> {
                actions.addLast(Include(tag))
                includeTag(tag)
            }
        }
    }

    fun onTagLongClick(tag: Tag) {
        when {
            included.contains(tag) -> {
                actions.addLast(UncheckAndExclude(tag))
                excludeTag(tag)
            }
            !excluded.contains(tag) -> {
                actions.addLast(Exclude(tag))
                excludeTag(tag)
            }
            else -> {
                actions.addLast(UncheckExcluded(tag))
                uncheckTag(tag)
            }
        }
    }

    fun onClearClick() {
        actions.addLast(Clear(included.toSet(), excluded.toSet()))
        included.clear()
        excluded.clear()
        calculateTagsAndSelection()
    }

    fun calculateTagsAndSelection() {
        val resources = indexCache.listIds(rootAndFav) ?: emptySet()
        val tagsByResources = tagsCache.groupTagsByResources(resources)
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

    fun onBackClick(): Boolean {
        if (actions.isEmpty())
            return false

        val action = findLastActualAction() ?: return false

        when(action) {
            is Include -> {
                included.remove(action.tag!!)
            }
            is Exclude -> {
                excluded.remove(action.tag!!)
            }
            is UncheckIncluded -> {
                included.add(action.tag!!)
            }
            is UncheckExcluded -> {
                excluded.add(action.tag!!)
            }
            is UncheckAndExclude -> {
                excluded.remove(action.tag!!)
                included.add(action.tag)
            }
            is Clear -> {
                included = action.included.toMutableSet()
                excluded = action.excluded.toMutableSet()
            }
        }

        actions.removeLast()

        calculateTagsAndSelection()

        return true
    }

    private fun findLastActualAction(): TagsSelectorAction? {
        val allTags = tagsCache.getTags(rootAndFav)

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