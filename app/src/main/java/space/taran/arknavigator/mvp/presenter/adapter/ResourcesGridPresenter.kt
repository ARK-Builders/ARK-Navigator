package space.taran.arknavigator.mvp.presenter.adapter

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.utils.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.reifySorting
import space.taran.arknavigator.utils.unequalCompareBy
import java.lang.AssertionError
import java.nio.file.Files
import javax.inject.Inject

class ResourcesGridPresenter(
    val viewState: ResourcesView,
    val scope: CoroutineScope
) {
    @Inject
    lateinit var userPreferences: UserPreferences

    private var resources = listOf<ResourceMeta>()
    private var selection = listOf<ResourceMeta>()

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage
    private lateinit var router: Router

    var sorting = Sorting.DEFAULT
        private set(value) {
            field = value
            scope.launch { userPreferences.setSorting(value) }
        }

    var ascending: Boolean = true
        private set(value) {
            field = value
            scope.launch { userPreferences.setSortingAscending(value) }
        }

    fun getCount() = selection.size

    fun bindView(view: FileItemView) {
        val resource = selection[view.position()]
        Log.d(RESOURCES_SCREEN, "binding view for resource ${resource.id}")

        val path = index.getPath(resource.id)

        view.setText(path.fileName.toString())

        if (Files.isDirectory(path)) {
            throw AssertionError("Resource can't be a directory")
        }

        view.setIconOrPreview(path, resource)
    }

    fun onItemClick(pos: Int) {
        router.navigateTo(Screens.GalleryScreen(index, storage, selection, pos))
    }

    suspend fun init(index: ResourcesIndex, storage: TagsStorage, router: Router) {
        this.index = index
        this.storage = storage
        this.router = router
        sorting = userPreferences.getSorting()
        ascending = userPreferences.isSortingAscending()
    }

    suspend fun updateSelection(selection: Set<ResourceId>) = withContext(Dispatchers.Default) {
        this@ResourcesGridPresenter.selection = resources.filter { selection.contains(it.id) }
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    suspend fun resetResources(resources: Set<ResourceMeta>) = withContext(Dispatchers.Default) {
        this@ResourcesGridPresenter.resources = resources.toList()
        sortAllResources()
        selection = this@ResourcesGridPresenter.resources
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    suspend fun resetSelection() = withContext(Dispatchers.Default) {
        selection = resources.toList()
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.notifyUser("${selection.size} resources selected")
            viewState.updateAdapter()
        }
    }

    suspend fun applyTaggedResources(taggedResources: Set<ResourceMeta>) = withContext(Dispatchers.Default) {
        selection = resources.filter { taggedResources.contains(it) }
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    fun removeResources(idToRemoveList: List<ResourceId>) {
        if (idToRemoveList.isEmpty()) return
        resources = resources.filter { !idToRemoveList.contains(it.id) }
        selection = selection.filter { !idToRemoveList.contains(it.id) }
    }

    fun updateSorting(sorting: Sorting) {
        scope.launch(Dispatchers.Default) {
            setProgressVisibility(true, "Sorting")
            this@ResourcesGridPresenter.sorting = sorting
            sortAllResources()
            sortSelectionAndUpdateAdapter()
        }
    }

    fun updateAscending(ascending: Boolean) {
        scope.launch(Dispatchers.Default) {
            setProgressVisibility(true, "Sorting")
            this@ResourcesGridPresenter.ascending = ascending
            sortAllResources()
            sortSelectionAndUpdateAdapter()
        }
    }

    private fun sortAllResources() {
        val comparator = reifySorting(sorting)
        if (comparator != null) {
            resources = resources.map { index.getPath(it.id) to it }
                .toMap()
                .toSortedMap(unequalCompareBy(comparator))
                .values
                .toList()
        }

        if (sorting != Sorting.DEFAULT && !ascending) {
            resources = resources.reversed()
        }
    }

    private fun sortSelectionAndUpdateAdapter() {
        val selection = this.selection.toSet()
        this.selection = resources.filter { selection.contains(it) }
        scope.launch(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    private suspend fun setProgressVisibility(isVisible: Boolean, withText: String = "") =
        withContext(Dispatchers.Main) {
            viewState.setProgressVisibility(isVisible, withText)
        }
}