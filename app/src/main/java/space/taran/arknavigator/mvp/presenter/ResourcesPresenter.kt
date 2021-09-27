package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.*
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.TagsSelectorPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.RESOURCES_SCREEN
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
    var tagsEnabled: Boolean = true

    val gridPresenter = ResourcesGridPresenter(viewState, presenterScope).apply {
        App.instance.appComponent.inject(this)
    }
    val tagsSelectorPresenter = TagsSelectorPresenter(viewState, prefix, ::onSelectionChange)

    override fun onFirstViewAttach() {
        Log.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
        super.onFirstViewAttach()

        viewState.init()
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
                .map { it to PlainTagsStorage.provide(it, rootToIndex[it]!!) }
                .toMap()

            roots.forEach { root ->
                val storage = rootToStorage[root]!!
                val indexed = rootToIndex[root]!!.listAllIds()

                storage.cleanup(indexed)
            }

            index = AggregatedResourcesIndex(rootToIndex.values)
            storage = AggregatedTagsStorage(rootToStorage.values)

            gridPresenter.init(index, storage, router)
            gridPresenter.resetResources(resources())
            tagsSelectorPresenter.init(index, storage)
            tagsSelectorPresenter.calculateTagsAndSelection()

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

    fun onViewResume() {
        tagsSelectorPresenter.calculateTagsAndSelection()
        if (!tagsEnabled)
            gridPresenter.resetResources(resources(untagged = true))
    }

    fun onMenuTagsToggle(enabled: Boolean) {
        tagsEnabled = enabled
        viewState.setTagsEnabled(tagsEnabled)
        if (tagsEnabled) {
            gridPresenter.resetResources(resources())
            gridPresenter.updateSelection(tagsSelectorPresenter.selection)
        } else
            gridPresenter.resetResources(resources(untagged = true))
        if (tagsEnabled && storage.getTags(resources()).isEmpty()) {
            viewState.notifyUser("Tag something first")
        }
    }

    fun onMenuSortDialogClick() {
        viewState.showSortDialog(gridPresenter.sorting, gridPresenter.ascending)
    }

    fun onSortDialogClose() {
        viewState.closeSortDialog()
    }

    private fun onSelectionChange(selection: Set<ResourceId>) {
        viewState.notifyUser("${selection.size} resources selected")
        gridPresenter.updateSelection(selection)
    }

    private fun resources(untagged: Boolean = false): Set<ResourceId> {
        val underPrefix = index.listIds(prefix)

        val result = if (untagged) {
            storage
                .listUntaggedResources()
                .intersect(underPrefix)
        } else {
            underPrefix
        }

        viewState.notifyUser("${result.size} resources selected")
        return result
    }
}