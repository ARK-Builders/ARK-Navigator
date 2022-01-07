package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.*
import space.taran.arknavigator.mvp.model.repo.tags.AggregatedTagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.RESOURCES_SCREEN
import java.nio.file.Path
import javax.inject.Inject

class ResourcesPresenter(
    private val rootAndFav: RootAndFav
) : MvpPresenter<ResourcesView>() {

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexRepo: ResourcesIndexRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage
    var tagsEnabled: Boolean = true

    val gridPresenter = ResourcesGridPresenter(rootAndFav, viewState, presenterScope).apply {
        App.instance.appComponent.inject(this)
    }
    val tagsSelectorPresenter =
        TagsSelectorPresenter(viewState, rootAndFav.fav, ::onSelectionChange)

    override fun onFirstViewAttach() {
        Log.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            viewState.setProgressVisibility(true, "Indexing")
            val folders = foldersRepo.provideFolders()
            Log.d(RESOURCES_SCREEN, "folders retrieved: $folders")

            Notifications.notifyIfFailedPaths(viewState, folders.failed)

            val all = folders.succeeded.keys
            val roots: List<Path> = if (rootAndFav.root != null) {
                if (!all.contains(rootAndFav.root)) {
                    throw AssertionError("Requested root wasn't found in DB")
                }

                listOf(rootAndFav.root!!)
            } else {
                all.toList()
            }
            Log.d(RESOURCES_SCREEN, "using roots $roots")

            val rootToIndex = roots
                .map { it to resourcesIndexRepo.loadFromDatabase(it) }
                .toMap()

            val rootToStorage = roots
                .map { it to tagsStorageRepo.provide(it) }
                .toMap()

            index = AggregatedResourcesIndex(rootToIndex.values)
            storage = AggregatedTagsStorage(rootToStorage.values)

            gridPresenter.init(index, storage, router)

            val resources = listResources()
            viewState.setProgressVisibility(true, "Sorting")

            resetResources(resources, false)
            tagsSelectorPresenter.init(index, storage)
            tagsSelectorPresenter.calculateTagsAndSelection()

            val path = (rootAndFav.fav ?: rootAndFav.root)
            val title = if (path != null) "$path, " else ""

            viewState.setToolbarTitle("$title${roots.size} of roots chosen")
            viewState.setProgressVisibility(false)
        }
    }

    override fun onDestroy() {
        Log.d(RESOURCES_SCREEN, "destroying ResourcesPresenter")
        super.onDestroy()
    }

    fun onResourcesOrTagsChanged() = presenterScope.launch {
        if (tagsEnabled)
            resetResources(listResources(), needToUpdateAdapter = false)
        else
            resetResources(listResources(untaggedOnly = true))

        tagsSelectorPresenter.calculateTagsAndSelection()
    }

    fun onMenuTagsToggle(enabled: Boolean) {
        tagsEnabled = enabled
        viewState.setTagsEnabled(tagsEnabled)

        presenterScope.launch {
            if (tagsEnabled) {
                gridPresenter.resetResources(listResources(), false)
                updateSelection(tagsSelectorPresenter.selection)
            } else {
                resetResources(listResources(untaggedOnly = true))
            }
        }

        val ids = listResources().map { it.id }
        if (tagsEnabled && storage.getTags(ids).isEmpty()) {
            viewState.notifyUser("Tag something first")
        }
    }

    fun onMenuSortDialogClick() {
        viewState.showSortDialog(gridPresenter.sorting, gridPresenter.ascending)
    }

    fun onSortDialogClose() {
        viewState.closeSortDialog()
    }

    fun onBackClick() {
        if (!tagsSelectorPresenter.onBackClick())
            router.exit()
    }

    private fun onSelectionChange(selection: Set<ResourceId>) {
        if (tagsEnabled)
            presenterScope.launch { updateSelection(selection) }
    }

    private fun listResources(untaggedOnly: Boolean = false): Set<ResourceMeta> {
        val underPrefix = index.listResources(rootAndFav.fav)

        val result = if (untaggedOnly) {
            val untagged = storage.listUntaggedResources()
            underPrefix.filter { untagged.contains(it.id) }
        } else {
            underPrefix
        }

        return result.toSet()
    }

    private suspend fun updateSelection(
        selection: Set<ResourceId>,
        needToUpdateAdapter: Boolean = true
    ) {
        if (this.tagsEnabled) {
            viewState.notifyUser("${selection.size} resources selected")
            gridPresenter.updateSelection(selection, needToUpdateAdapter)
        }
    }

    private suspend fun resetResources(
        resources: Set<ResourceMeta>,
        needToUpdateAdapter: Boolean = true
    ) {
        if (!this.tagsEnabled) {
            viewState.notifyUser("${resources.size} resources selected")
        }
        gridPresenter.resetResources(resources, needToUpdateAdapter)
    }
}