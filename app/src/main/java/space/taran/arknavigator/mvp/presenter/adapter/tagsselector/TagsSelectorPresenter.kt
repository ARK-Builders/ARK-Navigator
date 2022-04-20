package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.kind.KindCode
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.resource.StringProvider
import space.taran.arknavigator.utils.LogTags.TAGS_SELECTOR
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.Tag
import java.nio.file.Path
import javax.inject.Inject
import kotlin.reflect.KSuspendFunction1

sealed class TagItem {
    data class PlainTagItem(val tag: Tag) : TagItem()
    data class KindTagItem(val kind: KindCode) : TagItem()
}

enum class QueryMode {
    NORMAL, FOCUS
}

class TagsSelectorPresenter(
    private val viewState: ResourcesView,
    private val prefix: Path?,
    private val scope: CoroutineScope,
    private val onSelectionChangeListener: KSuspendFunction1<Set<ResourceId>, Unit>
) {
    @Inject
    lateinit var stringProvider: StringProvider

    @Inject
    lateinit var userPreferences: UserPreferences

    private var index: ResourcesIndex? = null
    private var storage: TagsStorage? = null
    private val actions = ArrayDeque<TagsSelectorAction>()
    private var filter = ""

    var queryMode = QueryMode.NORMAL
        private set
    var filterEnabled = false
        private set
    var showKindTags = false
        private set
    var includedTagItems = mutableSetOf<TagItem>()
        private set
    var excludedTagItems = mutableSetOf<TagItem>()
        private set
    private var availableTagItems = listOf<TagItem>()
    private var unavailableTagItems = listOf<TagItem>()

    var selection = setOf<ResourceId>()
        private set

    // this data is used by TagsSelectorAdapter
    var includedAndExcludedTagsForDisplay = listOf<TagItem>()
        private set
    var availableTagsForDisplay = listOf<TagItem>()
        private set
    var unavailableTagsForDisplay = listOf<TagItem>()
        private set
    var isClearBtnEnabled = false
        private set

    fun init(index: ResourcesIndex, storage: TagsStorage, kindTagsEnabled: Boolean) {
        this.index = index
        this.storage = storage
        showKindTags = kindTagsEnabled
    }

    fun onTagItemClick(item: TagItem) = scope.launch {
        when {
            excludedTagItems.contains(item) -> {
                actions.addLast(UncheckExcluded(item))
                uncheckTag(item)
            }
            includedTagItems.contains(item) -> {
                actions.addLast(UncheckIncluded(item))
                uncheckTag(item)
            }
            else -> {
                actions.addLast(Include(item))
                if (filterEnabled) resetFilter()
                includeTag(item)
            }
        }
    }

    fun onTagItemLongClick(item: TagItem) = scope.launch {
        when {
            includedTagItems.contains(item) -> {
                actions.addLast(UncheckAndExclude(item))
                excludeTag(item)
            }
            !excludedTagItems.contains(item) -> {
                actions.addLast(Exclude(item))
                if (filterEnabled) resetFilter()
                excludeTag(item)
            }
            else -> {
                actions.addLast(UncheckExcluded(item))
                uncheckTag(item)
            }
        }
    }

    fun onTagExternallySelect(tag: Tag) = scope.launch {
        includeTag(TagItem.PlainTagItem(tag))
    }

    fun onClearClick() = scope.launch {
        actions.addLast(Clear(includedTagItems.toSet(), excludedTagItems.toSet()))
        includedTagItems.clear()
        excludedTagItems.clear()
        calculateTagsAndSelection()
    }

    fun onFilterChanged(filter: String) {
        this.filter = filter
        filterTags()
        viewState.drawTags()
    }

    fun onQueryModeChanged(mode: QueryMode) = scope.launch {
        this@TagsSelectorPresenter.queryMode = mode
        viewState.updateMenu()
        calculateTagsAndSelection()
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

    suspend fun calculateTagsAndSelection() {
        Log.d(TAGS_SELECTOR, "calculating tags and selection")
        if (storage == null || index == null)
            return

        val tagItemsByResources = provideTagItemsByResources()
        val allItemsTags = tagItemsByResources.values.flatten().toSet()

        // some tags could have been removed from storage
        excludedTagItems = excludedTagItems.intersect(allItemsTags).toMutableSet()
        includedTagItems = includedTagItems.intersect(allItemsTags).toMutableSet()

        val selectionAndComplementWithTags = tagItemsByResources
            .toList()
            .groupBy { (_, tags) ->
                tags.containsAll(includedTagItems) &&
                    !excludedTagItems.any { tags.contains(it) }
            }

        val selectionWithTags = (
            selectionAndComplementWithTags[true] ?: emptyList()
            ).toMap()
        val complementWithTags = (
            selectionAndComplementWithTags[false] ?: emptyList()
            ).toMap()

        selection = selectionWithTags.keys
        val tagsOfSelectedResources = selectionWithTags.values.flatten()
        val tagsOfUnselectedResources = complementWithTags.values.flatten()

        availableTagItems = (
            tagsOfSelectedResources.toSet() -
                includedTagItems -
                excludedTagItems
            ).toList()
        unavailableTagItems = (
            allItemsTags -
                availableTagItems.toSet() -
                includedTagItems -
                excludedTagItems
            ).toList()

        val tagsOfSelectedResPopularity = Popularity
            .calculate(tagsOfSelectedResources)

        val tagsOfUnselectedResPopularity = Popularity
            .calculate(tagsOfUnselectedResources)

        val tagsPopularity = Popularity
            .calculate(tagItemsByResources.values.flatten())
        availableTagItems = availableTagItems.sortedByDescending {
            tagsOfSelectedResPopularity[it]
        }
        unavailableTagItems = unavailableTagItems.sortedByDescending {
            tagsOfUnselectedResPopularity[it]
        }

        includedAndExcludedTagsForDisplay = (includedTagItems + excludedTagItems)
            .sortedByDescending { tagsPopularity[it] }

        if (filterEnabled) filterTags()
        else resetTags()

        Log.d(TAGS_SELECTOR, "tags included: $includedTagItems")
        Log.d(TAGS_SELECTOR, "tags excluded: $excludedTagItems")
        Log.d(TAGS_SELECTOR, "tags available: $availableTagsForDisplay")
        Log.d(TAGS_SELECTOR, "tags unavailable: $unavailableTagsForDisplay")

        isClearBtnEnabled = includedTagItems.isNotEmpty() ||
            excludedTagItems.isNotEmpty()

        if (allItemsTags.isEmpty())
            viewState.setTagsSelectorHintEnabled(true)
        else
            viewState.setTagsSelectorHintEnabled(false)

        viewState.drawTags()

        calculateFocusModeSelectionIfNeeded(tagItemsByResources)
        onSelectionChangeListener(selection)
    }

    suspend fun onBackClick(): Boolean {
        if (actions.isEmpty())
            return false

        val action = findLastActualAction() ?: return false

        when (action) {
            is Include -> {
                includedTagItems.remove(action.item)
            }
            is Exclude -> {
                excludedTagItems.remove(action.item)
            }
            is UncheckIncluded -> {
                includedTagItems.add(action.item!!)
            }
            is UncheckExcluded -> {
                excludedTagItems.add(action.item!!)
            }
            is UncheckAndExclude -> {
                excludedTagItems.remove(action.item)
                includedTagItems.add(action.item!!)
            }
            is Clear -> {
                includedTagItems = action.included.toMutableSet()
                excludedTagItems = action.excluded.toMutableSet()
            }
        }

        actions.removeLast()

        calculateTagsAndSelection()

        return true
    }

    private fun calculateFocusModeSelectionIfNeeded(
        tagItemsByResources: Map<ResourceId, Set<TagItem>>
    ) {
        if (queryMode != QueryMode.FOCUS)
            return

        selection = selection.filter { id ->
            tagItemsByResources[id] == includedTagItems
        }.toSet()
    }

    private fun findLastActualAction(): TagsSelectorAction? {
        val tagItems = provideTagItemsByResources().values.flatten().toSet()

        while (actions.lastOrNull() != null) {
            val lastAction = actions.last()
            if (isActionActual(lastAction, tagItems)) {
                return lastAction
            } else
                actions.removeLast()
        }

        return null
    }

    private fun isActionActual(
        action: TagsSelectorAction,
        allTagItems: Set<TagItem>
    ): Boolean = when (action) {
        is Clear -> {
            action.excluded.intersect(allTagItems).isNotEmpty() ||
                action.included.intersect(allTagItems).isNotEmpty()
        }
        else -> {
            allTagItems.contains(action.item)
        }
    }

    private fun resetFilter() {
        filter = ""
        viewState.setTagsFilterText(filter)
    }

    private fun filterTags() {
        availableTagsForDisplay = availableTagItems
            .filter(::filterTagItem)

        unavailableTagsForDisplay = unavailableTagItems
            .filter(::filterTagItem)
    }

    private fun filterTagItem(item: TagItem) = when (item) {
        is TagItem.PlainTagItem -> {
            item.tag.startsWith(filter, false)
        }
        is TagItem.KindTagItem -> {
            stringProvider.kindToString(item.kind).startsWith(filter, false)
        }
    }

    private fun resetTags() {
        availableTagsForDisplay = availableTagItems
        unavailableTagsForDisplay = unavailableTagItems
    }

    private suspend fun includeTag(item: TagItem) {
        Log.d(TAGS_SELECTOR, "including tag $item")

        includedTagItems.add(item)
        excludedTagItems.remove(item)

        calculateTagsAndSelection()
    }

    private suspend fun excludeTag(item: TagItem) {
        Log.d(TAGS_SELECTOR, "excluding tag $item")

        excludedTagItems.add(item)
        includedTagItems.remove(item)

        calculateTagsAndSelection()
    }

    private suspend fun uncheckTag(item: TagItem, needToCalculate: Boolean = true) {
        Log.d(TAGS_SELECTOR, "un-checking tag $item")

        if (includedTagItems.contains(item) && excludedTagItems.contains(item)) {
            throw AssertionError("The tag is both included and excluded")
        }
        if (!includedTagItems.contains(item) && !excludedTagItems.contains(item)) {
            throw AssertionError("The tag is neither included nor excluded")
        }

        if (!includedTagItems.remove(item)) {
            excludedTagItems.remove(item)
        }

        if (needToCalculate)
            calculateTagsAndSelection()
    }

    private fun provideTagItemsByResources(): Map<ResourceId, Set<TagItem>> {
        val resources = index!!.listIds(prefix)
        val tagItemsByResources: Map<ResourceId, Set<TagItem>> =
            storage!!.groupTagsByResources(resources).map {
                it.key to it.value.map { tag -> TagItem.PlainTagItem(tag) }.toSet()
            }.toMap()

        if (!showKindTags) return tagItemsByResources

        return tagItemsByResources.map { (id, tags) ->
            var listOfTags = tags
            val kind = index!!.getMeta(id).kind
            if (kind != null) {
                listOfTags =
                    listOfTags + TagItem.KindTagItem(kind.code)
            }
            id to listOfTags
        }.toMap()
    }

    fun onKindTagsToggle(kindTagsEnabled: Boolean) = scope.launch {
        showKindTags = kindTagsEnabled
        viewState.setKindTagsEnabled(showKindTags)
        userPreferences.setKindTagsEnabled(kindTagsEnabled)
        calculateTagsAndSelection()
    }
}
