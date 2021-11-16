package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.*
import space.taran.arknavigator.mvp.model.repo.index.AggregatedResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexFactory
import space.taran.arknavigator.mvp.model.repo.tags.AggregatedTagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.PlainTagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
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
            viewState.setProgressVisibility(true, "Indexing")
            val folders = foldersRepo.query()
            Log.d(RESOURCES_SCREEN, "folders retrieved: $folders")

            Notifications.notifyIfFailedPaths(viewState, folders.failed)

            val all = folders.succeeded.keys
            val roots: List<Path> = if (root != null) {
                if (!all.contains(root)) {
                    throw AssertionError("Requested root wasn't found in DB")
                }

                listOf(root)
            } else {
                all.toList()
            }
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

            val resources = listResources()
            viewState.setProgressVisibility(true, "Sorting")

            resetResources(resources)
            tagsSelectorPresenter.init(index, storage)
            tagsSelectorPresenter.calculateTagsAndSelection()

            val path = (prefix ?: root)
            val title = if (path != null) "$path, " else ""

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
        if (!tagsEnabled) {
            presenterScope.launch {
                resetResources(listResources(untaggedOnly = true))
            }
        }
    }

    fun onMenuTagsToggle(enabled: Boolean) {
        tagsEnabled = enabled
        viewState.setTagsEnabled(tagsEnabled)

        presenterScope.launch {
            if (tagsEnabled) {
                gridPresenter.resetResources(listResources())
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

    fun onBackClick(): Boolean {
        if (!tagsSelectorPresenter.onBackClick())
            router.exit()
        return true
    }

    private fun onSelectionChange(selection: Set<ResourceId>) {
        presenterScope.launch { updateSelection(selection) }
    }

    private fun listResources(untaggedOnly: Boolean = false): Set<ResourceMeta> {
        val underPrefix = index.listResources(prefix)

        val result = if (untaggedOnly) {
            val untagged = storage.listUntaggedResources()
            underPrefix.filter { untagged.contains(it.id) }
        } else {
            underPrefix
        }

        return result.toSet()
    }

    private suspend fun updateSelection(selection: Set<ResourceId>) {
        if (this.tagsEnabled) {
            viewState.notifyUser("${selection.size} resources selected")
            gridPresenter.updateSelection(selection)
        }
    }

    private suspend fun resetResources(resources: Set<ResourceMeta>) {
        if (!this.tagsEnabled) {
            viewState.notifyUser("${resources.size} resources selected")
        }
        gridPresenter.resetResources(resources)
    }
}