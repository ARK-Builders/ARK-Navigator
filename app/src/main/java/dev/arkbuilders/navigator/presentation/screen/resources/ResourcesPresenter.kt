package dev.arkbuilders.navigator.presentation.screen.resources

import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.Message
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.arklib.data.meta.MetadataProcessor
import dev.arkbuilders.arklib.data.meta.MetadataProcessorRepo
import dev.arkbuilders.arklib.data.preview.PreviewProcessor
import dev.arkbuilders.arklib.data.preview.PreviewProcessorRepo
import dev.arkbuilders.arklib.data.storage.StorageException
import dev.arkbuilders.arklib.user.score.ScoreStorage
import dev.arkbuilders.arklib.user.score.ScoreStorageRepo
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.arklib.user.tags.TagsStorageRepo
import dev.arkbuilders.components.tagselector.QueryMode
import dev.arkbuilders.components.tagselector.TagSelectorController
import dev.arkbuilders.components.tagselector.TagsSorting
import dev.arkbuilders.navigator.analytics.resources.ResourcesAnalytics
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.stats.StatsStorage
import dev.arkbuilders.navigator.data.stats.StatsStorageRepo
import dev.arkbuilders.navigator.data.utils.LogTags.RESOURCES_SCREEN
import dev.arkbuilders.navigator.data.utils.findNotExistCopyName
import dev.arkbuilders.navigator.di.modules.DefaultDispatcher
import dev.arkbuilders.navigator.di.modules.IoDispatcher
import dev.arkbuilders.navigator.di.modules.MainDispatcher
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.screen.resources.adapter.ResourcesGridPresenter
import dev.arkbuilders.navigator.presentation.utils.StringProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import timber.log.Timber
import java.nio.file.Path
import javax.inject.Inject
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
    lateinit var metadataProcessorRepo: MetadataProcessorRepo

    @Inject
    lateinit var previewProcessorRepo: PreviewProcessorRepo

    @Inject
    lateinit var statsStorageRepo: StatsStorageRepo

    @Inject
    lateinit var scoreStorageRepo: ScoreStorageRepo

    @Inject
    lateinit var stringProvider: StringProvider

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject
    lateinit var analytics: ResourcesAnalytics

    private val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow()

    lateinit var index: ResourceIndex
        private set
    lateinit var tagStorage: TagStorage
        private set
    private lateinit var metadataProcessor: MetadataProcessor
    private lateinit var previewProcessor: PreviewProcessor
    lateinit var statsStorage: StatsStorage
        private set
    private lateinit var scoreStorage: ScoreStorage

    val gridPresenter =
        ResourcesGridPresenter(folders, viewState, presenterScope, this)
            .apply {
                App.instance.appComponent.inject(this)
            }
    val tagsSelectorController =
        TagSelectorController(
            presenterScope,
            kindToString = { stringProvider.kindToString(it) },
            tagSortCriteria = { tagsSorting ->
                analytics.trackTagSortCriteria(tagsSorting)
                when (tagsSorting) {
                    TagsSorting.POPULARITY ->
                        error("TagsSorting.POPULARITY must be handled before")
                    TagsSorting.QUERIED_TS -> statsStorage.statsTagQueriedTS()
                    TagsSorting.QUERIED_N -> statsStorage.statsTagQueriedAmount()
                    TagsSorting.LABELED_TS -> statsStorage.statsTagLabeledTS()
                    TagsSorting.LABELED_N -> statsStorage.statsTagLabeledAmount()
                } as Map<Tag, Comparable<Any>>
            },
            onSelectionChangeListener = { queryMode, normal, focus ->
                presenterScope.launch(mainDispatcher) {
                    when (queryMode) {
                        QueryMode.NORMAL -> {
                            onSelectionChange(normal)
                            viewState.toastResourcesSelected(normal.size)
                        }

                        QueryMode.FOCUS -> {
                            onSelectionChange(focus!!)
                            viewState.toastResourcesSelectedFocusMode(
                                focus.size,
                                normal.size - focus.size
                            )
                        }
                    }
                }
            },
            onStatsEvent = {
                statsStorage.handleEvent(it)
            },
            onKindTagsChanged = {
                preferences.set(PreferenceKey.ShowKinds, it)
            },
            onQueryModeChangedCB = {
                analytics.trackQueryModeChanged(it)
                presenterScope.launch(mainDispatcher) {
                    viewState.updateMenu(it)
                }
            }
        )

    override fun onFirstViewAttach() {
        analytics.trackScreen()
        Timber.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
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

            viewState.setProgressVisibility(true, "Providing root index")

            index = resourcesIndexRepo.provide(folders)

            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed -> viewState.toastIndexFailedPath(
                        message.path
                    )
                }
            }.launchIn(presenterScope)

            viewState.setProgressVisibility(true, "Extracting metadata")
            metadataProcessor = metadataProcessorRepo.provide(index)

            viewState.setProgressVisibility(true, "Generating previews")
            previewProcessor = previewProcessorRepo.provide(index)

            initIndexingListeners()

            try {
                tagStorage = tagsStorageRepo.provide(index)
                scoreStorage = scoreStorageRepo.provide(index)
            } catch (e: StorageException) {
                analytics.trackStorageProvideException(e)
                viewState.displayStorageException(
                    e.label,
                    e.msg
                )
                return@launch
            }

            statsStorage = statsStorageRepo.provide(index)

            gridPresenter.init(
                index,
                tagStorage,
                router,
                metadataProcessor,
                previewProcessor,
                scoreStorage
            )

            viewState.setProgressVisibility(true, "Sorting resources")

            val resources = index.allResources()
            gridPresenter.resetResources(resources.values.toSet())
            tagsSelectorController.init(
                index,
                tagStorage,
                metadataProcessor,
                preferences.get(PreferenceKey.ShowKinds),
                TagsSorting.values()[
                    preferences.get(PreferenceKey.TagsSortingSelector)
                ],
                preferences.get(PreferenceKey.TagsSortingSelectorAsc),
                preferences.get(PreferenceKey.CollectTagUsageStats),
                preferences.flow(PreferenceKey.TagsSortingSelector).map {
                    TagsSorting.values()[it]
                },
                preferences.flow(PreferenceKey.TagsSortingSelectorAsc)
            )

            externallySelectedTag?.let {
                tagsSelectorController.onTagExternallySelect(it)
            }
            tagsSelectorController.calculateTagsAndSelection()

            viewState.setProgressVisibility(false)

            launch {
                delay(DELAY_CLEAR_TOASTS)
                viewState.clearStackedToasts()
            }
        }
    }

    fun onMoveSelectedResourcesClicked(
        directoryToMove: Path
    ) = presenterScope.launch(ioDispatcher) {
        analytics.trackMoveSelectedRes()
        withContext(mainDispatcher) {
            viewState.setProgressVisibility(true, "Moving selected resources")
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
        withContext(mainDispatcher) {
            onResourcesOrTagsChanged()
            gridPresenter.onSelectingChanged(false)
            viewState.setProgressVisibility(false)
        }
    }

    fun onCopySelectedResourcesClicked(
        directoryToCopy: Path
    ) = presenterScope.launch(mainDispatcher) {
        analytics.trackCopySelectedRes()
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
        withContext(mainDispatcher) {
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

    fun onShuffleSwitchedOn() = presenterScope.launch(defaultDispatcher) {
        analytics.trackResShuffle()
        gridPresenter.shuffleResources()
    }

    fun onShuffleSwitchedOff() = presenterScope.launch {
        gridPresenter.unShuffleResources()
    }

    fun onIncreaseScoreClicked() = presenterScope.launch {
        gridPresenter.increaseScore()
    }

    fun onDecreaseScoreClicked() = presenterScope.launch {
        gridPresenter.decreaseScore()
    }

    fun onResetScoresClicked() = presenterScope.launch(ioDispatcher) {
        gridPresenter.resetScores()
    }

    fun allowScoring() = gridPresenter.allowScoring()

    fun allowResettingScores() = gridPresenter.allowResettingScores()

    fun onShareSelectedResourcesClicked() = presenterScope.launch {
        analytics.trackShareSelectedRes()
        val selected = gridPresenter
            .resources
            .filter { it.isSelected }
            .map { index.getPath(it.id())!! }
        viewState.shareResources(selected)
        gridPresenter.onSelectingChanged(false)
    }

    fun onRemoveSelectedResourcesClicked() = presenterScope.launch(ioDispatcher) {
        analytics.trackRemoveSelectedRes()
        withContext(mainDispatcher) {
            viewState.setProgressVisibility(true, "Removing selected resources")
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
        withContext(mainDispatcher) {
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
        gridPresenter.resetResources(index.allResources().values.toSet())
        onTagsChanged()
    }

    suspend fun onTagsChanged() {
        tagsSelectorController.calculateTagsAndSelection()
    }

    fun onBackClick() = presenterScope.launch {
        if (!tagsSelectorController.onBackClick())
            router.exit()
    }

    suspend fun onRemovedResourceDetected() {
        viewState.setProgressVisibility(true, "Updating root index")

        index.updateAll()
        // update current tags storage
        tagsStorageRepo.provide(index)

        viewState.setProgressVisibility(true, "Sorting resources")
        onResourcesOrTagsChanged()
        viewState.setProgressVisibility(false)
    }

    private suspend fun onSelectionChange(selection: Set<ResourceId>) {
        gridPresenter.updateSelection(selection)
    }

    private fun initIndexingListeners() {
        metadataProcessor.busy.onEach {
            Timber.d("metadata extraction progress = $it")
            viewState.setPreviewGenerationProgress(
                it || previewProcessor.busy.value
            )
        }.launchIn(presenterScope)

        previewProcessor.busy.onEach {
            Timber.d("preview generation progress = $it")
            viewState.setPreviewGenerationProgress(
                it || metadataProcessor.busy.value
            )
        }.launchIn(presenterScope)
    }

    companion object {
        private const val DELAY_CLEAR_TOASTS = 1_500L
        private const val COPY_POSTFIX = "_1"
    }
}
