package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import ru.terrakok.cicerone.Router
import space.taran.arknavigator.mvp.model.*
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.*
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.Notifications
import space.taran.arknavigator.utils.RESOURCES_SCREEN
import java.nio.file.Path
import javax.inject.Inject

class ResourcesPresenter(
    val rootAndFav: RootAndFav
) : MvpPresenter<ResourcesView>() {

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var indexCache: IndexCache

    @Inject
    lateinit var tagsCache: TagsCache

    var tagsEnabled: Boolean = true

    val gridPresenter = ResourcesGridPresenter(rootAndFav, viewState, presenterScope).apply {
        App.instance.appComponent.inject(this)
    }
    val tagsSelectorPresenter = TagsSelectorPresenter(viewState, rootAndFav, ::onSelectionChange).apply {
        App.instance.appComponent.inject(this)
    }

    private var resources = setOf<ResourceId>()
    private var untaggedResources = setOf<ResourceId>()

    override fun onFirstViewAttach() {
        Log.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            viewState.setProgressVisibility(true)
            val folders = foldersRepo.query()
            Log.d(RESOURCES_SCREEN, "folders retrieved: $folders")
            listenRootAndFav()

            Notifications.notifyIfFailedPaths(viewState, folders.failed)

            val root = rootAndFav.root
            val prefix = rootAndFav.fav

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

            gridPresenter.init()
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

    fun onMenuTagsToggle(enabled: Boolean) {
        tagsEnabled = enabled
        viewState.setTagsEnabled(tagsEnabled)
        if (tagsEnabled) {
            gridPresenter.resetResources(resources)
            gridPresenter.updateSelection(tagsSelectorPresenter.selection)
        } else
            gridPresenter.resetResources(untaggedResources)
        if (tagsEnabled && tagsCache.getTags(rootAndFav).isEmpty()) {
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
        viewState.notifyUser("${selection.size} resources selected")
        if (tagsEnabled)
            gridPresenter.updateSelection(selection)
    }

    private suspend fun listenRootAndFav() {
        presenterScope.launch {
            indexCache.listenResourcesChanges(rootAndFav).collect { resources ->
                resources?.let {
                    viewState.setProgressVisibility(false)
                    this@ResourcesPresenter.resources = resources
                    this@ResourcesPresenter.untaggedResources = tagsCache.listUntagged(rootAndFav)!!
                    if (tagsEnabled) {
                        gridPresenter.resetResources(resources)
                        tagsSelectorPresenter.calculateTagsAndSelection()
                    } else {
                        gridPresenter.resetResources(untaggedResources)
                    }
                }
            }
        }
        presenterScope.launch {
            tagsCache.listenTagsChanges(rootAndFav).collect { tags ->
                tags?.let { tagsSelectorPresenter.calculateTagsAndSelection() }
            }
        }
    }
}