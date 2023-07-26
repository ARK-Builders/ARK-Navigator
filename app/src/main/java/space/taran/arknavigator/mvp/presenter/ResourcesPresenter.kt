package dev.arkbuilders.navigator.mvp.presenter

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.index.ResourceIndexRepo
import space.taran.arklib.domain.meta.MetadataStorage
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.PreviewStorage
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arklib.utils.Constants
import dev.arkbuilders.navigator.mvp.model.repo.preferences.PreferenceKey
import dev.arkbuilders.navigator.mvp.model.repo.preferences.Preferences
import dev.arkbuilders.navigator.mvp.model.repo.scores.ScoreStorage
import dev.arkbuilders.navigator.mvp.model.repo.scores.ScoreStorageRepo
import dev.arkbuilders.navigator.mvp.model.repo.stats.StatsStorage
import dev.arkbuilders.navigator.mvp.model.repo.stats.StatsStorageRepo
import dev.arkbuilders.navigator.mvp.model.repo.tags.PlainTagsStorage
import dev.arkbuilders.navigator.mvp.model.repo.tags.TagsStorage
import dev.arkbuilders.navigator.mvp.model.repo.tags.TagsStorageRepo
import dev.arkbuilders.navigator.mvp.presenter.adapter.ResourcesGridPresenter
import dev.arkbuilders.navigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import dev.arkbuilders.navigator.mvp.view.ResourcesView
import dev.arkbuilders.navigator.navigation.AppRouter
import dev.arkbuilders.navigator.ui.App
import dev.arkbuilders.navigator.utils.LogTags.RESOURCES_SCREEN
import dev.arkbuilders.navigator.utils.Tag
import dev.arkbuilders.navigator.utils.findNotExistCopyName
import timber.log.Timber
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Named
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists

class ResourcesPresenter(
    val folders: RootAndFav,
    private val externallySelectedTag: Tag? = null
) : MvpPresenter<ResourcesView>() {

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexRepo: ResourceIndexRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var metadataStorageRepo: MetadataStorageRepo

    @Inject
    lateinit var previewStorageRepo: PreviewStorageRepo

    @Inject
    lateinit var statsStorageRepo: StatsStorageRepo

    @Inject
    lateinit var scoreStorageRepo: ScoreStorageRepo

    @Inject
    @Named(Constants.DI.MESSAGE_FLOW_NAME)
    lateinit var messageFlow: MutableSharedFlow<Message>

    lateinit var index: ResourceIndex
        private set
    lateinit var tagStorage: TagsStorage
        private set
    lateinit var metadataStorage: MetadataStorage
        private set
    lateinit var previewStorage: PreviewStorage
        private set
    lateinit var statsStorage: StatsStorage
        private set
    lateinit var scoreStorage: ScoreStorage
        private set

    val gridPresenter =
        ResourcesGridPresenter(folders, viewState, presenterScope, this)
            .apply {
                App.instance.appComponent.inject(this)
            }
    val tagsSelectorPresenter =
        TagsSelectorPresenter(
            viewState,
            presenterScope,
            ::onSelectionChange
        ).apply {
            App.instance.appComponent.inject(this)
        }

    override fun onFirstViewAttach() {
        Log.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
        super.onFirstViewAttach()

        presenterScope.launch {
            val ascending = preferences.get(PreferenceKey.IsSortingAscending)
            val sortByScores = preferences.get(PreferenceKey.SortByScores)
            viewState.init(ascending, sortByScores)
            viewState.initResourcesAdapter()

            val root = folders.root
            val title = if (root != null) {
                val favorite = folders.fav
                if (favorite != null) {
                    "Favorite folder \"${favorite.last()}\" selected"
                } else {
                    "Root folder \"${root.last()}\" selected"
                }
            } else {
                "All roots are selected"
            }

            viewState.setToolbarTitle(title)

            viewState.setProgressVisibility(true, "Indexing resources")

            index = resourcesIndexRepo.provide(folders)

            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed -> viewState.toastIndexFailedPath(
                        message.path
                    )
                }
            }.launchIn(presenterScope)

            viewState.setProgressVisibility(true, "Extracting metadata")
            metadataStorage = metadataStorageRepo.provide(index)

            viewState.setProgressVisibility(true, "Generating previews")
            previewStorage = previewStorageRepo.provide(index)

            initIndexingListeners()

            tagStorage = tagsStorageRepo.provide(index)

            if (tagStorage.isCorrupted()) {
                viewState.showCorruptNotificationDialog(
                    PlainTagsStorage.TYPE
                )
                return@launch
            }

            scoreStorage = scoreStorageRepo.provide(index)
            statsStorage = statsStorageRepo.provide(index)

            gridPresenter.init(
                index,
                tagStorage,
                router,
                metadataStorage,
                previewStorage,
                scoreStorage
            )

            viewState.setProgressVisibility(true, "Sorting resources")

            val resources = index.allResources()
            gridPresenter.resetResources(resources)
            val kindTagsEnabled = preferences.get(PreferenceKey.ShowKinds)
            tagsSelectorPresenter.init(
                index,
                tagStorage,
                statsStorage,
                metadataStorage,
                kindTagsEnabled
            )

            viewState.setKindTagsEnabled(kindTagsEnabled)
            externallySelectedTag?.let {
                tagsSelectorPresenter.onTagExternallySelect(it)
            }
            tagsSelectorPresenter.calculateTagsAndSelection()

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
            .map { it.id() }
        val jobs = resourcesToMove.map { id ->
            launch {
                val path = index.getPath(id)!!
                val newPath = directoryToMove.findNotExistCopyName(path)
                path.copyTo(newPath)
                if (path != newPath)
                    path.deleteIfExists()
            }
        }
        jobs.forEach { it.join() }
        migrateTags(resourcesToMove, directoryToMove)

        index.updateAll()

        tagsStorageRepo.provide(index)
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
            .map { it.id() }
        resourcesToCopy.map { id ->
            launch {
                val path = index.getPath(id)!!
                val newPath = directoryToCopy.findNotExistCopyName(path)
                path.copyTo(newPath)
            }
        }
        withContext(Dispatchers.Main) {
            gridPresenter.onSelectingChanged(false)
        }
        migrateTags(resourcesToCopy, directoryToCopy)
    }

    fun onAscendingChanged(isAscending: Boolean) = presenterScope.launch {
        viewState.updateOrderBtn(isAscending)
        preferences.set(
            PreferenceKey.IsSortingAscending,
            isAscending
        )
    }

    fun onScoresSwitched(enabled: Boolean) = presenterScope.launch {
        preferences.set(PreferenceKey.SortByScores, enabled)
    }

    fun onShuffleSwitchedOn() = presenterScope.launch(Dispatchers.Default) {
        gridPresenter.shuffleResources()
    }

    fun onShuffleSwitchedOff() = presenterScope.launch {
        gridPresenter.unShuffleResources()
    }

    fun onIncreaseScoreClicked() = presenterScope.launch(Dispatchers.Default) {
        gridPresenter.increaseScore()
    }

    fun onDecreaseScoreClicked() = presenterScope.launch(Dispatchers.Default) {
        gridPresenter.decreaseScore()
    }

    fun onResetScoresClicked() = presenterScope.launch(Dispatchers.IO) {
        gridPresenter.resetScores()
    }

    fun allowScoring() = gridPresenter.allowScoring()

    fun allowResettingScores() = gridPresenter.allowResettingScores()

    fun onShareSelectedResourcesClicked() = presenterScope.launch {
        val selected = gridPresenter
            .resources
            .filter { it.isSelected }
            .map { index.getPath(it.id())!! }
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
                val path = index.getPath(item.id())!!
                path.deleteIfExists()
            }
        }
        results.forEach { it.await() }

        index.updateAll()

        tagsStorageRepo.provide(index)
        withContext(Dispatchers.Main) {
            onResourcesOrTagsChanged()
            gridPresenter.onSelectingChanged(false)
            viewState.setProgressVisibility(false)
        }
    }

    private suspend fun migrateTags(resources: List<ResourceId>, to: Path) {
        val newStorage = tagsStorageRepo.provide(index)
        resources
            .associateWith { tagStorage.getTags(it) }
            .forEach { (id, tags) ->
                newStorage.setTags(id, tags)
            }
        newStorage.persist()
    }

    suspend fun onResourcesOrTagsChanged() {
        gridPresenter.resetResources(index.allResources())
        tagsSelectorPresenter.calculateTagsAndSelection()
    }

    fun onBackClick() = presenterScope.launch {
        if (!tagsSelectorPresenter.onBackClick())
            router.exit()
    }

    suspend fun onRemovedResourceDetected() {
        viewState.setProgressVisibility(true, "Indexing")

        index.updateAll()
        // update current tags storage
        tagsStorageRepo.provide(index)

        viewState.setProgressVisibility(true, "Sorting")
        onResourcesOrTagsChanged()
        viewState.setProgressVisibility(false)
    }

    private suspend fun onSelectionChange(selection: Set<ResourceId>) {
        gridPresenter.updateSelection(selection)
    }

    private fun initIndexingListeners() {
        metadataStorage.inProgress.onEach {
            Timber.d("metadata extraction progress = $it")
            viewState.setPreviewGenerationProgress(it)
        }.launchIn(presenterScope)

        previewStorage.inProgress.onEach {
            Timber.d("preview generation progress = $it")
            viewState.setPreviewGenerationProgress(it)
        }.launchIn(presenterScope)
    }

    companion object {
        private const val DELAY_CLEAR_TOASTS = 1_500L
        private const val COPY_POSTFIX = "_1"
    }
}
