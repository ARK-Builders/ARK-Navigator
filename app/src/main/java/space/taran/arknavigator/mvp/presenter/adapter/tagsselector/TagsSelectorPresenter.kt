package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.kind.KindCode
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.stats.StatsEvent
import space.taran.arknavigator.mvp.model.repo.stats.StatsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.presenter.dialog.TagsSorting
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.resource.StringProvider
import space.taran.arknavigator.utils.LogTags.TAGS_SELECTOR
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.Tag
import java.nio.file.Path
import javax.inject.Inject

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
    private val onSelectionChangeListener: suspend (Set<ResourceId>) -> Unit
) {
    @Inject
    lateinit var stringProvider: StringProvider

    @Inject
    lateinit var preferences: Preferences

    val actionsHistory = ArrayDeque<TagsSelectorAction>()

    private var index: ResourcesIndex? = null
    private var storage: TagsStorage? = null
    private lateinit var statsStorage: StatsStorage
    private var filter = ""

    var sorting = TagsSorting.POPULARITY
        private set
    var sortingAscending = true
        private set

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

    suspend fun init(
        index: ResourcesIndex,
        storage: TagsStorage,
        statsStorage: StatsStorage,
        kindTagsEnabled: Boolean,
    ) {
        this.index = index
        this.storage = storage
        this.statsStorage = statsStorage
        showKindTags = kindTagsEnabled
        sorting = TagsSorting.values()[preferences.get(PreferenceKey.TagsSorting)]
        sortingAscending = preferences.get(PreferenceKey.TagsSortingAscending)
        preferences.flow(PreferenceKey.TagsSorting).onEach {
            val newSorting = TagsSorting.values()[it]
            if (sorting == newSorting) return@onEach
            sorting = newSorting
            calculateTagsAndSelection()
        }.launchIn(scope)

        preferences.flow(PreferenceKey.TagsSortingAscending).onEach { ascending ->
            if (sortingAscending == ascending) return@onEach
            sortingAscending = ascending
            calculateTagsAndSelection()
        }.launchIn(scope)

        val collecting = preferences.get(PreferenceKey.CollectTagUsageStats)
        viewState.setTagsSortingVisibility(collecting)

        preferences.flow(PreferenceKey.CollectTagUsageStats).onEach {
            viewState.setTagsSortingVisibility(it)
        }.launchIn(scope)
    }

    fun onTagItemClick(item: TagItem) = scope.launch {
        when {
            excludedTagItems.contains(item) -> {
                actionsHistory.addLast(TagsSelectorAction.UncheckExcluded(item))
                uncheckTag(item)
            }
            includedTagItems.contains(item) -> {
                actionsHistory.addLast(TagsSelectorAction.UncheckIncluded(item))
                uncheckTag(item)
            }
            else -> {
                actionsHistory.addLast(TagsSelectorAction.Include(item))
                if (filterEnabled) resetFilter()
                includeTag(item)
            }
        }
    }

    fun onTagItemLongClick(item: TagItem) = scope.launch {
        when {
            includedTagItems.contains(item) -> {
                actionsHistory.addLast(TagsSelectorAction.UncheckAndExclude(item))
                excludeTag(item)
            }
            !excludedTagItems.contains(item) -> {
                actionsHistory.addLast(TagsSelectorAction.Exclude(item))
                if (filterEnabled) resetFilter()
                excludeTag(item)
            }
            else -> {
                actionsHistory.addLast(TagsSelectorAction.UncheckExcluded(item))
                uncheckTag(item)
            }
        }
    }

    fun onTagExternallySelect(tag: Tag) = scope.launch {
        includeTag(TagItem.PlainTagItem(tag))
    }

    fun onClearClick() = scope.launch {
        actionsHistory.addLast(
            TagsSelectorAction.Clear(
                includedTagItems.toSet(),
                excludedTagItems.toSet()
            )
        )
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

        sort(
            tagsOfSelectedResources,
            tagsOfUnselectedResources,
            tagItemsByResources
        )

        if (filterEnabled) filterTags()
        else resetTags()

        Log.d(TAGS_SELECTOR, "tags included: $includedTagItems")
        Log.d(TAGS_SELECTOR, "tags excluded: $excludedTagItems")
        Log.d(TAGS_SELECTOR, "tags available: $availableTagsForDisplay")
        Log.d(TAGS_SELECTOR, "tags unavailable: $unavailableTagsForDisplay")

        isClearBtnEnabled = includedTagItems.isNotEmpty() ||
            excludedTagItems.isNotEmpty()

        viewState.drawTags()

        when (queryMode) {
            QueryMode.NORMAL -> {
                viewState.toastResourcesSelected(selection.size)
                onSelectionChangeListener(selection)
            }
            QueryMode.FOCUS -> calculateFocusModeSelectionIfNeeded(
                tagItemsByResources
            )
        }
    }

    private fun sort(
        tagsOfSelectedResources: List<TagItem>,
        tagsOfUnselectedResources: List<TagItem>,
        tagItemsByResources: Map<ResourceId, Set<TagItem>>
    ) {
        when (sorting) {
            TagsSorting.POPULARITY -> sortByPopularity(
                tagsOfSelectedResources,
                tagsOfUnselectedResources,
                tagItemsByResources
            )
            TagsSorting.LAST_USED -> sortByLastUsed()
        }
        if (!sortingAscending) {
            availableTagItems = availableTagItems.reversed()
            unavailableTagItems = unavailableTagItems.reversed()
            includedAndExcludedTagsForDisplay =
                includedAndExcludedTagsForDisplay.reversed()
        }
    }

    private fun sortByPopularity(
        tagsOfSelectedResources: List<TagItem>,
        tagsOfUnselectedResources: List<TagItem>,
        tagItemsByResources: Map<ResourceId, Set<TagItem>>
    ) {
        val tagsOfSelectedResPopularity = Popularity
            .calculate(tagsOfSelectedResources)

        val tagsOfUnselectedResPopularity = Popularity
            .calculate(tagsOfUnselectedResources)

        val tagsPopularity = Popularity
            .calculate(tagItemsByResources.values.flatten())
        availableTagItems = availableTagItems.sortedBy {
            tagsOfSelectedResPopularity[it]
        }
        unavailableTagItems = unavailableTagItems.sortedBy {
            tagsOfUnselectedResPopularity[it]
        }

        includedAndExcludedTagsForDisplay = (includedTagItems + excludedTagItems)
            .sortedBy { tagsPopularity[it] }
    }

    private fun sortByLastUsed() {
        val lastUsed = statsStorage.statsTagUsedTSList()
        val kindCodes = KindCode.values().associateBy { it.name }
        val lastUsedTagItems = lastUsed.map { tag ->
            if (kindCodes.containsKey(tag))
                TagItem.KindTagItem(kindCodes[tag]!!)
            else
                TagItem.PlainTagItem(tag)
        }.withIndex().associate { it.value to it.index }
        availableTagItems = availableTagItems.sortedBy {
            lastUsedTagItems[it]
        }

        unavailableTagItems = unavailableTagItems.sortedBy {
            lastUsedTagItems[it]
        }

        val includedAnExcluded = includedTagItems + excludedTagItems
        includedAndExcludedTagsForDisplay = includedAnExcluded.sortedBy {
            lastUsedTagItems[it]
        }
    }

    suspend fun onBackClick(): Boolean {
        if (actionsHistory.isEmpty())
            return false

        val action = findLastActualAction() ?: return false

        when (action) {
            is TagsSelectorAction.Include -> {
                includedTagItems.remove(action.item)
            }
            is TagsSelectorAction.Exclude -> {
                excludedTagItems.remove(action.item)
            }
            is TagsSelectorAction.UncheckIncluded -> {
                includedTagItems.add(action.item)
            }
            is TagsSelectorAction.UncheckExcluded -> {
                excludedTagItems.add(action.item)
            }
            is TagsSelectorAction.UncheckAndExclude -> {
                excludedTagItems.remove(action.item)
                includedTagItems.add(action.item)
            }
            is TagsSelectorAction.Clear -> {
                includedTagItems = action.included.toMutableSet()
                excludedTagItems = action.excluded.toMutableSet()
            }
        }

        actionsHistory.removeLast()

        calculateTagsAndSelection()

        return true
    }

    private suspend fun calculateFocusModeSelectionIfNeeded(
        tagItemsByResources: Map<ResourceId, Set<TagItem>>
    ) {
        val normalModeSelectionSize = selection.size
        selection = selection.filter { id ->
            tagItemsByResources[id] == includedTagItems
        }.toSet()
        val hidden = normalModeSelectionSize - selection.size

        viewState.toastResourcesSelectedFocusMode(
            selection.size,
            hidden
        )

        onSelectionChangeListener(selection)
    }

    private suspend fun findLastActualAction(): TagsSelectorAction? {
        val tagItems = provideTagItemsByResources().values.flatten().toSet()

        while (actionsHistory.lastOrNull() != null) {
            val lastAction = actionsHistory.last()
            if (isActionActual(lastAction, tagItems)) {
                return lastAction
            } else
                actionsHistory.removeLast()
        }

        return null
    }

    private fun isActionActual(
        actions: TagsSelectorAction,
        allTagItems: Set<TagItem>
    ): Boolean = when (actions) {
        is TagsSelectorAction.Exclude -> allTagItems.contains(actions.item)
        is TagsSelectorAction.Include -> allTagItems.contains(actions.item)
        is TagsSelectorAction.UncheckAndExclude -> allTagItems.contains(actions.item)
        is TagsSelectorAction.UncheckExcluded -> allTagItems.contains(actions.item)
        is TagsSelectorAction.UncheckIncluded -> allTagItems.contains(actions.item)
        is TagsSelectorAction.Clear -> {
            actions.excluded.intersect(allTagItems).isNotEmpty() ||
                actions.included.intersect(allTagItems).isNotEmpty()
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
        val event = when (item) {
            is TagItem.KindTagItem -> StatsEvent.KindTagUsed(item.kind)
            is TagItem.PlainTagItem -> StatsEvent.PlainTagUsed(item.tag)
        }

        statsStorage.handleEvent(event)

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

    private suspend fun provideTagItemsByResources(): Map<ResourceId, Set<TagItem>> {
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
        preferences.set(PreferenceKey.ShowKinds, kindTagsEnabled)
        calculateTagsAndSelection()
    }
}
