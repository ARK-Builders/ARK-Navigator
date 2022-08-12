package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.tags.PlainTagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.LogTags.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.findNotExistCopyName
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name

class ResourcesPresenter(
    val rootAndFav: RootAndFav,
    private val externallySelectedTag: Tag? = null
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
    lateinit var preferences: Preferences

    lateinit var index: ResourcesIndex
    lateinit var storage: TagsStorage

    val gridPresenter =
        ResourcesGridPresenter(rootAndFav, viewState, presenterScope, this)
            .apply {
                App.instance.appComponent.inject(this)
            }
    val tagsSelectorPresenter =
        TagsSelectorPresenter(
            viewState,
            rootAndFav.fav,
            presenterScope,
            ::onSelectionChange
        ).apply {
            App.instance.appComponent.inject(this)
        }

    override fun onFirstViewAttach() {
        Log.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
        super.onFirstViewAttach()

        viewState.init()
        presenterScope.launch {
            viewState.setProgressVisibility(true, "Indexing")
            val folders = foldersRepo.provideFoldersWithMissing()
            Log.d(RESOURCES_SCREEN, "folders retrieved: $folders")

            viewState.toastPathsFailed(folders.failed)

            val all = folders.succeeded.keys
            val roots: List<Path> = if (rootAndFav.root != null) {
                if (!all.contains(rootAndFav.root)) {
                    throw AssertionError("Requested root wasn't found in DB")
                }

                listOf(rootAndFav.root)
            } else {
                all.toList()
            }
            Log.d(RESOURCES_SCREEN, "using roots $roots")
            index = resourcesIndexRepo.provide(rootAndFav)
            index.kindDetectFailedFlow.onEach { path ->
                viewState.toastIndexFailedPath(path)
            }.launchIn(presenterScope)
            index.reindex()
            storage = tagsStorageRepo.provide(rootAndFav)

            gridPresenter.init(index, storage, router)

            val resources = index.listResources(rootAndFav.fav)
            viewState.setProgressVisibility(true, "Sorting")

            gridPresenter.resetResources(resources)
            val kindTagsEnabled = preferences.get(PreferenceKey.ShowKinds)
            tagsSelectorPresenter.init(index, storage, kindTagsEnabled)
            viewState.setKindTagsEnabled(kindTagsEnabled)
            externallySelectedTag?.let {
                tagsSelectorPresenter.onTagExternallySelect(it)
            }
            tagsSelectorPresenter.calculateTagsAndSelection()

            val path = (rootAndFav.fav ?: rootAndFav.root)
            val title = if (path != null) "${path.last()}, " else ""

            viewState.setToolbarTitle("$title${roots.size} of roots chosen")
            viewState.setProgressVisibility(false)
            launch {
                delay(DELAY_CLEAR_TOASTS)
                viewState.clearStackedToasts()
            }
        }
    }

    fun onMoveSelectedResourcesClicked(
        directoryToMove: Path
    ) = presenterScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            viewState.setProgressVisibility(true, "Moving")
        }
        val resourcesToMove = gridPresenter
            .resources
            .filter { it.isSelected }
            .map { it.meta.id }
        val jobs = resourcesToMove.map { id ->
            launch {
                val path = index.getPath(id)
                val newPath = directoryToMove.findNotExistCopyName(path.name)
                path.copyTo(newPath)
                if (path != newPath)
                    path.deleteIfExists()
            }
        }
        jobs.forEach { it.join() }
        migrateTags(resourcesToMove, directoryToMove)
        index.reindex()
        tagsStorageRepo.provide(rootAndFav)
        withContext(Dispatchers.Main) {
            onResourcesOrTagsChanged()
            gridPresenter.onSelectingChanged(false)
            viewState.setProgressVisibility(false)
        }
    }

    fun onCopySelectedResourcesClicked(
        directoryToCopy: Path
    ) = presenterScope.launch(Dispatchers.IO) {
        val resourcesToCopy = gridPresenter
            .resources
            .filter { it.isSelected }
            .map { it.meta.id }
        resourcesToCopy.map { id ->
            launch {
                val path = index.getPath(id)
                val newPath = directoryToCopy.findNotExistCopyName(path.name)
                path.copyTo(newPath)
            }
        }
        withContext(Dispatchers.Main) {
            gridPresenter.onSelectingChanged(false)
        }
        migrateTags(resourcesToCopy, directoryToCopy)
    }

    fun onShareSelectedResourcesClicked() = presenterScope.launch {
        val selected = gridPresenter
            .resources
            .filter { it.isSelected }
            .map { index.getPath(it.meta.id) }
        viewState.shareResources(selected)
        gridPresenter.onSelectingChanged(false)
    }

    fun onRemoveSelectedResourcesClicked() = presenterScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            viewState.setProgressVisibility(true, "Removing")
        }
        val resourcesToRemove = gridPresenter.resources.filter { it.isSelected }
        val results = resourcesToRemove.map { item ->
            async {
                val path = index.getPath(item.meta.id)
                path.deleteIfExists()
            }
        }
        results.forEach { it.await() }
        index.reindex()
        tagsStorageRepo.provide(rootAndFav)
        withContext(Dispatchers.Main) {
            onResourcesOrTagsChanged()
            gridPresenter.onSelectingChanged(false)
            viewState.setProgressVisibility(false)
        }
    }

    private suspend fun migrateTags(resources: List<ResourceId>, to: Path) {
        val newRoot = foldersRepo.findRootByPath(to)
        newRoot?.let {
            val newStorage = PlainTagsStorage(it, resources, preferences).apply {
                init()
            }
            resources
                .associateWith { storage.getTags(it) }
                .apply { newStorage.setTagsAndPersist(this) }
        }
    }

    suspend fun onResourcesOrTagsChanged() {
        gridPresenter.resetResources(index.listResources(rootAndFav.fav))
        tagsSelectorPresenter.calculateTagsAndSelection()
    }

    fun onBackClick() = presenterScope.launch {
        if (!tagsSelectorPresenter.onBackClick())
            router.exit()
    }

    suspend fun onRemovedResourceDetected() {
        viewState.setProgressVisibility(true, "Indexing")

        index.reindex()
        // update current tags storage
        tagsStorageRepo.provide(rootAndFav)

        viewState.setProgressVisibility(true, "Sorting")
        onResourcesOrTagsChanged()
        viewState.setProgressVisibility(false)
    }

    private suspend fun onSelectionChange(selection: Set<ResourceId>) {
        gridPresenter.updateSelection(selection)
    }

    companion object {
        private const val DELAY_CLEAR_TOASTS = 1_500L
        private const val COPY_POSTFIX = "_1"
    }
}
