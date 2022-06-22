package space.taran.arknavigator.mvp.presenter.adapter

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.presenter.ResourcesPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.utils.LogTags.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.reifySorting
import space.taran.arknavigator.utils.unequalCompareBy
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.notExists
import kotlin.system.measureTimeMillis

class ResourceItem(val meta: ResourceMeta, var isSelected: Boolean = false)

class ResourcesGridPresenter(
    val rootAndFav: RootAndFav,
    val viewState: ResourcesView,
    val scope: CoroutineScope,
    val resourcesPresenter: ResourcesPresenter
) {
    @Inject
    lateinit var preferences: Preferences

    private var resources = listOf<ResourceItem>()
    private var selection = listOf<ResourceItem>()

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage
    private lateinit var router: AppRouter

    var sorting = Sorting.DEFAULT
        private set
    var ascending: Boolean = true
        private set
    var selectingEnabled: Boolean = false

    fun getCount() = selection.size

    fun bindView(view: FileItemView) {
        val resource = selection[view.position()]
        Log.d(RESOURCES_SCREEN, "binding view for resource ${resource.meta.id}")

        val path = index.getPath(resource.meta.id)

        view.setText(path.fileName.toString())
        view.setSelectedOnBind(selectingEnabled, resource.isSelected)

        if (Files.isDirectory(path)) {
            throw AssertionError("Resource can't be a directory")
        }

        if (path.notExists())
            scope.launch { resourcesPresenter.onRemovedResourceDetected() }

        view.setIconOrPreview(path, resource.meta)
    }

    fun onItemClick(pos: Int) = scope.launch {
        val containsNotExistingResource = selection.any { item ->
            index.getPath(item.meta.id).notExists()
        }

        if (containsNotExistingResource) {
            val clickedResource = selection[pos]
            resourcesPresenter.onRemovedResourceDetected()
            // selection has been updated
            if (!selection.contains(clickedResource))
                return@launch
        }

        router.navigateToFragmentUsingAdd(
            Screens.GalleryScreen(
                rootAndFav,
                selection.map { it.meta.id },
                pos
            )
        )
    }

    fun onSelectingChanged(enabled: Boolean) {
        selectingEnabled = enabled
        if (!selectingEnabled)
            resources.forEach { it.isSelected = false }
        viewState.onSelectingChanged(enabled)
    }

    fun onItemSelectChanged(itemView: FileItemView) {
        val item = selection[itemView.position()]
        item.isSelected = !item.isSelected
        itemView.setSelected(item.isSelected)
    }

    suspend fun init(
        index: ResourcesIndex,
        storage: TagsStorage,
        router: AppRouter
    ) {
        this.index = index
        this.storage = storage
        this.router = router
        sorting = Sorting.values()[preferences.get(PreferenceKey.Sorting)]
        ascending = preferences.get(PreferenceKey.IsSortingAscending)

        scope.launch(Dispatchers.IO) {
            preferences.flow(PreferenceKey.Sorting).collect { intSorting ->
                val newSorting = Sorting.values()[intSorting]
                if (sorting != newSorting)
                    updateSorting(newSorting)
            }
        }
        scope.launch(Dispatchers.IO) {
            preferences.flow(PreferenceKey.IsSortingAscending).collect {
                if (ascending != it)
                    updateAscending(it)
            }
        }
    }

    suspend fun updateSelection(
        selection: Set<ResourceId>
    ) = withContext(Dispatchers.Default) {
        this@ResourcesGridPresenter.selection = resources
            .filter { selection.contains(it.meta.id) }

        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    suspend fun resetResources(
        resources: Set<ResourceMeta>
    ) = withContext(Dispatchers.Default) {
        this@ResourcesGridPresenter.resources = mapNewResources(resources)
        sortAllResources()
        selection = this@ResourcesGridPresenter.resources
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
        }
    }

    private fun updateSorting(sorting: Sorting) {
        scope.launch(Dispatchers.Default) {
            setProgressVisibility(true, "Sorting")
            this@ResourcesGridPresenter.sorting = sorting
            sortAllResources()
            sortSelectionAndUpdateAdapter()
        }
    }

    private fun updateAscending(ascending: Boolean) {
        scope.launch(Dispatchers.Default) {
            setProgressVisibility(true, "Sorting")
            this@ResourcesGridPresenter.ascending = ascending
            sortAllResources()
            sortSelectionAndUpdateAdapter()
        }
    }

    private fun mapNewResources(
        newResources: Set<ResourceMeta>
    ): List<ResourceItem> {
        if (!selectingEnabled)
            return newResources.map { ResourceItem(it) }

        return newResources.map { meta ->
            val selected = resources
                .find { item -> item.meta == meta }
                ?.isSelected ?: false

            ResourceItem(meta, selected)
        }
    }

    private fun sortAllResources() {
        val sortTime = measureTimeMillis {
            val comparator = reifySorting(sorting)
            if (comparator != null) {
                resources = resources.map { it to it }
                    .toMap()
                    .toSortedMap(unequalCompareBy(comparator))
                    .values
                    .toList()
            }

            if (sorting != Sorting.DEFAULT && !ascending) {
                resources = resources.reversed()
            }
        }
        Log.d(
            RESOURCES_SCREEN,
            "sorting by $sorting of ${
            resources.size
            } resources took $sortTime milliseconds"
        )
    }

    private fun sortSelectionAndUpdateAdapter() {
        val selection = this.selection.toSet()
        this.selection = resources.filter { selection.contains(it) }
        scope.launch(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    private suspend fun setProgressVisibility(
        isVisible: Boolean,
        withText: String = ""
    ) =
        withContext(Dispatchers.Main) {
            viewState.setProgressVisibility(isVisible, withText)
        }
}
