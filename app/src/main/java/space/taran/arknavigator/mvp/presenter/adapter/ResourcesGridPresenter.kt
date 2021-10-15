package space.taran.arknavigator.mvp.presenter.adapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.IndexCache
import space.taran.arknavigator.mvp.model.IndexingEngine
import space.taran.arknavigator.mvp.model.TagsCache
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.reifySorting
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
    lateinit var indexCache: IndexCache

    @Inject
    lateinit var router: Router

    private var resources = listOf<ResourceId>()
    private var selection = listOf<ResourceId>()

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

        val path = indexCache.getPath(resource)
            ?: throw AssertionError("Resource to display must be indexed")

        view.setText(path.fileName.toString())

        if (Files.isDirectory(path)) {
            throw AssertionError("Resource can't be a directory")
        }

        view.setIcon(Preview.provide(path))
    }

    fun onItemClick(pos: Int) {
        router.navigateTo(Screens.GalleryScreen(selection, pos))
    }

    suspend fun init() {
        sorting = userPreferences.getSorting()
        ascending = userPreferences.isSortingAscending()
    }

    fun updateSelection(selection: Set<ResourceId>) {
        this.selection = resources.filter { selection.contains(it) }
        viewState.updateAdapter()
    }

    fun resetResources(resources: Set<ResourceId>) {
        this.resources = resources.toList()
        sortAllResources()
        selection = this.resources
        viewState.updateAdapter()
    }

    fun updateSorting(sorting: Sorting) {
        this.sorting = sorting
        sortAllResources()
        sortSelectionAndUpdateAdapter()
    }

    fun updateAscending(ascending: Boolean) {
        this.ascending = ascending
        sortAllResources()
        sortSelectionAndUpdateAdapter()
    }

    private fun sortAllResources() {
        val comparator = reifySorting(sorting)
        if (comparator != null) {
            resources = resources.map { indexCache.getPath(it)!! to it }
                .toMap()
                .toSortedMap(comparator)
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
        viewState.updateAdapter()
    }
}