package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.*
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.Tags
import java.nio.file.Path
import javax.inject.Inject

class ResourcesPresenter(
    val root: Path?,
    private val prefix: Path?
) : MvpPresenter<ResourcesView>() {

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexFactory: ResourcesIndexFactory

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage

    var sorting: Sorting = Sorting.DEFAULT
        set(value) {
            field = value
            presenterScope.launch { userPreferences.setSorting(value) }
        }

    var sortOrderAscending: Boolean = true
        set(value) {
            field = value
            presenterScope.launch { userPreferences.setSortingAscending(value) }
        }

    fun listTagsForAllResources(): Tags = resources()
        .flatMap { storage.getTags(it) }
        .toSet()

    fun createTagsSelector(): TagsSelector? {
        val tags = listTagsForAllResources()
        Log.d(RESOURCES_SCREEN, "tags loaded: $tags")

        if (tags.isEmpty()) {
            return null
        }

        return TagsSelector(tags, resources().toSet(), storage)
    }

    private fun getSorting() = presenterScope.launch {
        sorting = userPreferences.getSorting()
        sortOrderAscending = userPreferences.isSortingAscending()

        viewState.sortingValuesReceived()
    }

    override fun onFirstViewAttach() {
        Log.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
        super.onFirstViewAttach()

        presenterScope.launch {
            viewState.setProgressVisibility(true)
            val folders = foldersRepo.query()
            Log.d(RESOURCES_SCREEN, "folders retrieved: $folders")

            Notifications.notifyIfFailedPaths(viewState, folders.failed)

            val roots: List<Path> = {
                val all = folders.succeeded.keys
                if (root != null) {
                    if (!all.contains(root)) {
                        throw AssertionError("Requested root wasn't found in DB")
                    }

                    listOf(root)
                } else {
                    all.toList()
                }
            }()
            Log.d(RESOURCES_SCREEN, "using roots $roots")

            val rootToIndex = roots
                .map { it to resourcesIndexFactory.loadFromDatabase(it) }
                .toMap()

            val rootToStorage = roots
                .map { it to PlainTagsStorage.provide(it, rootToIndex[it]!!.listAllIds()) }
                .toMap()

            //todo: when async indexing will be ready, tagged ids must be boosted
            //in the indexing queue and be removed if they fail to be indexed

            roots.forEach { root ->
                val storage = rootToStorage[root]!!
                val indexed = rootToIndex[root]!!.listAllIds()

                storage.cleanup(indexed)
            }

            index = AggregatedResourcesIndex(rootToIndex.values)
            storage = AggregatedTagsStorage(rootToStorage.values)

            viewState.init(provideResourcesList())
            getSorting()

            val title = {
                val path = (prefix ?: root)
                if (path != null) "$path, " else ""
            }()

            viewState.setToolbarTitle("$title${roots.size} of roots chosen")
            viewState.setProgressVisibility(false)
        }
    }

    override fun onDestroy() {
        Log.d(RESOURCES_SCREEN, "destroying ResourcesPresenter")
        super.onDestroy()
    }

    fun provideResourcesList(untagged: Boolean = false): ResourcesList {
        var resourcesList: ResourcesList? = null
        resourcesList = ResourcesList(index, resources(untagged)) { position, resource ->
            Log.d(RESOURCES_SCREEN, "resource $resource at $position clicked ItemGridPresenter")

            router.navigateTo(Screens.GalleryScreen(index, storage, resourcesList!!, position))
        }

        return resourcesList
    }

    fun resources(untagged: Boolean = false): List<ResourceId> {
        val underPrefix = index.listIds(prefix)

        val result = if (untagged) {
            storage
                .listUntaggedResources()
                .intersect(underPrefix)
                .toList()
        } else {
            underPrefix
        }

        viewState.notifyUser("${result.size} resources selected")
        return result
    }
}