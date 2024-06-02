package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.Message
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.meta.MetadataProcessor
import dev.arkbuilders.arklib.data.meta.MetadataProcessorRepo
import dev.arkbuilders.arklib.data.preview.PreviewProcessor
import dev.arkbuilders.arklib.data.preview.PreviewProcessorRepo
import dev.arkbuilders.arklib.data.stats.StatsEvent
import dev.arkbuilders.arklib.data.storage.StorageException
import dev.arkbuilders.arklib.user.score.ScoreStorage
import dev.arkbuilders.arklib.user.score.ScoreStorageRepo
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.arklib.user.tags.Tags
import dev.arkbuilders.arklib.user.tags.TagsStorageRepo
import dev.arkbuilders.arklib.utils.ImageUtils
import dev.arkbuilders.arklib.utils.extension
import dev.arkbuilders.components.scorewidget.ScoreWidgetController
import dev.arkbuilders.navigator.analytics.gallery.GalleryAnalytics
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.stats.StatsStorage
import dev.arkbuilders.navigator.data.stats.StatsStorageRepo
import dev.arkbuilders.navigator.data.utils.LogTags
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.DisplaySelected
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ProgressWithText
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ResourceIdTagsPreview
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.SetupPreview
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ShowEditTagsData
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ShowInfoData
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.StorageExceptionGallery
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.state.GallerySideEffect
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.state.GalleryState
import dev.arkbuilders.navigator.presentation.screen.resources.adapter.ResourceDiffUtilCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

class GalleryUpliftViewModel(
    selectorNotEdit: Boolean,
    val preferences: Preferences,
    val router: AppRouter,
    val indexRepo: ResourceIndexRepo,
    val previewStorageRepo: PreviewProcessorRepo,
    val metadataStorageRepo: MetadataProcessorRepo,
    val tagsStorageRepo: TagsStorageRepo,
    val statsStorageRepo: StatsStorageRepo,
    val scoreStorageRepo: ScoreStorageRepo,
    val analytics: GalleryAnalytics,
) : ContainerHost<GalleryState, GallerySideEffect>, ViewModel() {
    private lateinit var index: ResourceIndex
    private lateinit var tagsStorage: TagStorage
    private lateinit var previewStorage: PreviewProcessor
    private lateinit var metadataStorage: MetadataProcessor
    private lateinit var statsStorage: StatsStorage
    private lateinit var scoreStorage: ScoreStorage
    private lateinit var rootAndFav: RootAndFav
    private lateinit var resourcesIds: List<ResourceId>

    override val container: Container<GalleryState, GallerySideEffect> =
        container(GalleryState())

    var galleryItems: MutableList<GalleryPresenter.GalleryItem> = mutableListOf()
    var diffResult: DiffUtil.DiffResult? = null

    private val _selectedResources: MutableList<ResourceId> = mutableListOf()
    val selectedResources: List<ResourceId> = _selectedResources

    val scoreWidgetController = ScoreWidgetController(
        scope = viewModelScope,
        getCurrentId = { currentItem.id() },
        onScoreChanged = {
            intent {
                postSideEffect(GallerySideEffect.NotifyResourceScoresChanged)
            }
        }
    )

    private val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow()
    private val currentItem: GalleryPresenter.GalleryItem
        get() = galleryItems[container.stateFlow.value.currentPos]

    fun initialize(
        rootAndFav: RootAndFav,
        resourcesIds: List<ResourceId>,
    ) {
        this.rootAndFav = rootAndFav
        this.resourcesIds = resourcesIds
        onFirstViewAttach()
    }

    fun onPreviewsItemClick() {
        intent {
            postSideEffect(GallerySideEffect.ControlVisible(isVisible = true))
        }
    }

    fun bindPlainTextView(view: PreviewPlainTextViewHolderUplift) {
        viewModelScope.launch {
            view.reset()
            val item = galleryItems[view.pos]
            val path = index.getPath(item.id())!!
            val content = readText(path)
            content.onSuccess {
                view.setContent(it)
            }
        }
    }

    fun bindView(view: PreviewImageViewHolderUplift) = viewModelScope.launch {
        view.reset()
        val item = galleryItems[view.pos]
        val path = index.getPath(item.id())!!
        val placeholder = ImageUtils.iconForExtension(extension(path))
        view.setSource(placeholder, item.id(), item.metadata, item.preview)
    }

    fun getKind(pos: Int): Int =
        galleryItems[pos].metadata.kind.ordinal

    fun onRemoveFabClick() = viewModelScope.launch(NonCancellable) {
        analytics.trackResRemove()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            buildString {
                append("[remove_resource] clicked at position ")
                append("${container.stateFlow.value.currentPos}")
            }
        )
        deleteResource(currentItem.id())
        galleryItems.removeAt(container.stateFlow.value.currentPos)
        if (galleryItems.isEmpty()) {
            intent {
                postSideEffect(GallerySideEffect.NavigateBack)
            }
            return@launch
        }
        onTagsChanged()
        intent {
            postSideEffect(
                GallerySideEffect.DeleteResource(
                    container.stateFlow.value.currentPos
                )
            )
        }
    }

    private suspend fun deleteResource(resource: ResourceId) {
        Timber.d(LogTags.GALLERY_SCREEN, "deleting resource $resource")
        withContext(Dispatchers.IO) {
            val path = index.getPath(resource)
            Files.delete(path)
        }
        index.updateAll()
        intent {
            postSideEffect(GallerySideEffect.NotifyResourceChange)
        }
    }

    private fun onFirstViewAttach() {
        analytics.trackScreen()
        Timber.d(LogTags.GALLERY_SCREEN, "first view attached in GalleryPresenter")
        viewModelScope.launch {
            intent {
                postSideEffect(
                    GallerySideEffect.ShowProgressWithText(
                        ProgressWithText(
                            isVisible = true,
                            text = "Providing root index",
                        )
                    )
                )
            }
            index = indexRepo.provide(rootAndFav)
            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed ->
                        intent {
                            postSideEffect(
                                GallerySideEffect.ToastIndexFailedPath(
                                    message.path
                                )
                            )
                        }
                }
            }.launchIn(viewModelScope)
            intent {
                postSideEffect(
                    GallerySideEffect.ShowProgressWithText(
                        ProgressWithText(
                            isVisible = true,
                            text = "Providing metadata storage",
                        )
                    )
                )
            }
            metadataStorage = metadataStorageRepo.provide(index)
            intent {
                postSideEffect(
                    GallerySideEffect.ShowProgressWithText(
                        ProgressWithText(
                            isVisible = true,
                            text = "Providing previews storage",
                        )
                    )
                )
            }
            previewStorage = previewStorageRepo.provide(index)
            intent {
                postSideEffect(
                    GallerySideEffect.ShowProgressWithText(
                        ProgressWithText(
                            isVisible = true,
                            text = "Providing data storage",
                        )
                    )
                )
            }
            try {
                tagsStorage = tagsStorageRepo.provide(index)
                scoreStorage = scoreStorageRepo.provide(index)
            } catch (e: StorageException) {
                intent {
                    postSideEffect(
                        GallerySideEffect.DisplayStorageException(
                            StorageExceptionGallery(
                                label = e.label,
                                messenger = e.msg
                            )
                        )
                    )
                }
            }
            statsStorage = statsStorageRepo.provide(index)
            scoreWidgetController.init(scoreStorage)
            galleryItems = provideGalleryItems().toMutableList()
            intent {
                reduce {
                    viewModelScope.launch {
                        state.copy(
                            sortByScores = preferences.get(
                                PreferenceKey.SortByScores
                            )
                        )
                    }
                    state
                }
                postSideEffect(
                    GallerySideEffect.UpdatePagerAdapter
                )
                postSideEffect(
                    GallerySideEffect.ShowProgressWithText(
                        ProgressWithText(
                            isVisible = false,
                            text = "",
                        )
                    )
                )
            }
            scoreWidgetController.setVisible(container.stateFlow.value.sortByScores)
        }
    }

    fun onPlayButtonClick() = intent {
        postSideEffect(
            GallerySideEffect.ViewInExternalApp(
                index.getPath(currentItem.id())!!
            )
        )
    }

    fun onInfoFabClick() = viewModelScope.launch {
        analytics.trackResInfo()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[info_resource] clicked at position" +
                " ${container.stateFlow.value.currentPos}"
        )
        val path = index.getPath(currentItem.id())!!
        val data = ShowInfoData(
            path = path,
            resource = currentItem.resource,
            metadata = currentItem.metadata
        )
        intent {
            postSideEffect(GallerySideEffect.ShowInfoAlert(data))
        }
    }

    fun onShareFabClick() = viewModelScope.launch {
        analytics.trackResShare()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[share_resource] clicked at position " +
                "${container.stateFlow.value.currentPos}"
        )
        val path = index.getPath(currentItem.id())!!
        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            intent {
                postSideEffect(GallerySideEffect.ShareLink(url))
            }
            return@launch
        }
        intent {
            postSideEffect(GallerySideEffect.ShareResource(path))
        }
    }

    fun onSelectingChanged() {
        intent {
            reduce {
                state.copy(selectingEnabled = !state.selectingEnabled)
            }
            postSideEffect(
                GallerySideEffect.ToggleSelect(
                    container.stateFlow.value.selectingEnabled
                )
            )
        }
        _selectedResources.clear()
        if (container.stateFlow.value.selectingEnabled) {
            _selectedResources.add(currentItem.resource.id)
        }
    }

    fun onOpenFabClick() = viewModelScope.launch {
        analytics.trackResOpen()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[open_resource] clicked at position " +
                "${container.stateFlow.value.currentPos}"
        )
        val id = currentItem.id()
        val path = index.getPath(id)!!
        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            intent {
                postSideEffect(GallerySideEffect.OpenLink(url))
            }
            return@launch
        }
        intent {
            postSideEffect(
                GallerySideEffect.ViewInExternalApp(
                    index.getPath(
                        currentItem.id()
                    )!!
                )
            )
        }
    }

    fun onEditFabClick() = viewModelScope.launch {
        analytics.trackResEdit()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[edit_resource] clicked at position " +
                "${container.stateFlow.value.currentPos}"
        )
        val path = index.getPath(currentItem.id())!!
        intent {
            postSideEffect(GallerySideEffect.EditResource(path))
        }
    }

    fun onSelectBtnClick() {
        val id = currentItem.id()
        val wasSelected = id in _selectedResources
        if (wasSelected) {
            _selectedResources.remove(id)
        } else {
            _selectedResources.add(id)
        }

        intent {
            postSideEffect(
                GallerySideEffect.DisplaySelectedFile(
                    DisplaySelected(
                        selected = !wasSelected,
                        showAnim = true,
                        selectedCount = _selectedResources.size,
                        itemCount = galleryItems.size
                    )
                )
            )
        }
    }

    fun onResume() {
        checkResourceChanges(container.stateFlow.value.currentPos)
    }

    fun onTagsChanged() {
        intent {
            val tags = tagsStorage.getTags(currentItem.id())
            postSideEffect(
                GallerySideEffect.DisplayPreviewTags(
                    ResourceIdTagsPreview(
                        resourceId = currentItem.id(),
                        tags = tags,
                    )
                )
            )
        }
    }

    fun onPageChanged(newPos: Int) = viewModelScope.launch {
        if (galleryItems.isEmpty())
            return@launch
        checkResourceChanges(newPos)
        intent {
            reduce {
                state.copy(currentPos = newPos)
            }
        }
        val id = currentItem.id()
        val tags = tagsStorage.getTags(id)
        displayPreview(id, currentItem.metadata, tags)
    }

    fun onTagSelected(tag: Tag) {
        analytics.trackTagSelect()
        router.navigateTo(
            Screens.ResourcesScreenWithSelectedTag(
                rootAndFav, tag
            )
        )
    }

    fun onTagRemove(tag: Tag) = viewModelScope.launch(NonCancellable) {
        analytics.trackTagRemove()
        val id = currentItem.id()
        val tags = tagsStorage.getTags(id)
        val newTags = tags - tag

        intent {
            postSideEffect(
                GallerySideEffect.DisplayPreviewTags(
                    ResourceIdTagsPreview(
                        resourceId = id,
                        tags = newTags,
                    )
                )
            )
        }
        statsStorage.handleEvent(
            StatsEvent.TagsChanged(
                id, tags, newTags
            )
        )

        Timber.d(LogTags.GALLERY_SCREEN, "setting new tags $newTags to $currentItem")
        tagsStorage.setTags(id, newTags)
        tagsStorage.persist()
        intent {
            postSideEffect(GallerySideEffect.NotifyTagsChanged)
        }
    }

    fun onEditTagsDialogBtnClick() {
        analytics.trackTagsEdit()
        intent {
            postSideEffect(
                GallerySideEffect.ShowEditTagsDialog(
                    ShowEditTagsData(
                        resource = currentItem.id(),
                        resources = listOf(currentItem.id()),
                        statsStorage = statsStorage,
                        rootAndFav = rootAndFav,
                        index = index,
                        storage = tagsStorage,
                    )
                )
            )
        }
    }

    private fun displayPreview(
        id: ResourceId,
        meta: Metadata,
        tags: Tags
    ) {
        intent {
            postSideEffect(
                GallerySideEffect.SetUpPreview(
                    SetupPreview(
                        position = container.stateFlow.value.currentPos,
                        meta = meta,
                    )
                )
            )
        }

        intent {
            postSideEffect(
                GallerySideEffect.DisplayPreviewTags(
                    ResourceIdTagsPreview(
                        resourceId = id,
                        tags = tags,
                    )
                )
            )
        }
        scoreWidgetController.displayScore()

        intent {
            postSideEffect(
                GallerySideEffect.DisplaySelectedFile(
                    DisplaySelected(
                        selected = id in _selectedResources,
                        showAnim = false,
                        selectedCount = _selectedResources.size,
                        itemCount = galleryItems.size,
                    )
                )
            )
        }
    }

    private fun checkResourceChanges(pos: Int) =
        viewModelScope.launch {
            if (galleryItems.isEmpty()) {
                return@launch
            }
            val item = galleryItems[pos]
            val path = index.getPath(item.id())
                ?: let {
                    Timber.d("Resource ${item.id()} can't be found in the index")
                    invokeHandleGalleryExternalChangesUseCase()
                    return@launch
                }
            if (path.notExists()) {
                Timber.d("Resource ${item.id()} isn't stored by path $path")
                invokeHandleGalleryExternalChangesUseCase()
                return@launch
            }
            if (path.getLastModifiedTime() != item.resource.modified) {
                Timber.d("Index is not up-to-date regarding path $path")
                invokeHandleGalleryExternalChangesUseCase()
                return@launch
            }
        }

    private fun provideGalleryItems(): List<GalleryPresenter.GalleryItem> =
        try {
            val allResources = index.allResources()
            resourcesIds
                .filter { allResources.keys.contains(it) }
                .map { id ->
                    val preview = previewStorage.retrieve(id).getOrThrow()
                    val metadata = metadataStorage.retrieve(id).getOrThrow()
                    val resource = allResources.getOrElse(id) {
                        throw NullPointerException("Resource not exist")
                    }
                    GalleryPresenter.GalleryItem(resource, preview, metadata)
                }.toMutableList()
        } catch (e: Exception) {
            Timber.d("Can't provide gallery items")
            emptyList()
        }

    private fun invokeHandleGalleryExternalChangesUseCase() {
        viewModelScope.launch {
            intent {
                postSideEffect(
                    GallerySideEffect.ShowProgressWithText(
                        ProgressWithText(
                            isVisible = true,
                            text = "Changes detected, indexing"
                        )
                    )
                )
            }

            index.updateAll()

            intent {
                postSideEffect(GallerySideEffect.NotifyResourceChange)
            }

            viewModelScope.launch {
                metadataStorage.busy.collect { busy ->
                    if (!busy) cancel()
                }
            }.join()

            val newItems = provideGalleryItems()
            if (newItems.isEmpty()) {
                intent {
                    postSideEffect(GallerySideEffect.NavigateBack)
                }
                return@launch
            }

            diffResult = DiffUtil.calculateDiff(
                ResourceDiffUtilCallback(
                    galleryItems.map { it.resource.id },
                    newItems.map { it.resource.id }
                )
            )

            galleryItems = newItems.toMutableList()

            viewModelScope.launch {
                intent {
                    postSideEffect(GallerySideEffect.UpdatePagerAdapterWithDiff)
                    postSideEffect(GallerySideEffect.NotifyCurrentItemChange)
                    postSideEffect(
                        GallerySideEffect.ShowProgressWithText(
                            ProgressWithText(
                                isVisible = true,
                                text = "Changes detected, indexing"
                            )
                        )
                    )
                }
            }
        }
    }

    private suspend fun readText(source: Path): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val content = FileReader(source.toFile()).readText()
                Result.success(content)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
