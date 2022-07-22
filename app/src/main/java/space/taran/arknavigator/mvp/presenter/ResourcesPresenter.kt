package space.taran.arknavigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.AggregatedResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.tags.AggregatedTagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesGridPresenter
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.LogTags.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Tag
import java.nio.file.Path
import javax.inject.Inject

class ResourcesPresenter(
    private val rootAndFav: RootAndFav,
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

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage

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

            val rootToIndex = roots
                .map { it to resourcesIndexRepo.loadFromDatabase(it) }
                .toMap()

            val rootToStorage = roots
                .map { it to tagsStorageRepo.provide(it) }
                .toMap()

            index = AggregatedResourcesIndex(rootToIndex.values)
            storage = AggregatedTagsStorage(rootToStorage.values)

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
        }
    }

    override fun onDestroy() {
        Log.d(RESOURCES_SCREEN, "destroying ResourcesPresenter")
        super.onDestroy()
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
}
