package space.taran.arknavigator.mvp.presenter.adapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.PreviewRepo
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.extension
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

    @Inject
    lateinit var previewRepo: PreviewRepo

    private var resources = listOf<ResourceId>()
    private var selection = listOf<ResourceId>()

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

        val path = index.getPath(resource)
            ?: throw AssertionError("Resource to display must be indexed")

        view.setText(path.fileName.toString())

        if (Files.isDirectory(path)) {
            throw AssertionError("Resource can't be a directory")
        }

        view.setIcon(previewRepo.providePreview(path, resource))
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
        this@ResourcesGridPresenter.selection = resources.filter { selection.contains(it) }
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    suspend fun resetResources(resources: Set<ResourceId>) = withContext(Dispatchers.Default) {
        this@ResourcesGridPresenter.resources = resources.toList()
        sortAllResources()
        selection = this@ResourcesGridPresenter.resources
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
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
            resources = resources.map { index.getPath(it)!! to it }
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