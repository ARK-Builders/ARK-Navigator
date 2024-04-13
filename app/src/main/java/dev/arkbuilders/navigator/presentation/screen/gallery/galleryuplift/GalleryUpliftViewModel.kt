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
import dev.arkbuilders.navigator.presentation.screen.resources.adapter.ResourceDiffUtilCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
) : ViewModel() {
    private lateinit var index: ResourceIndex
    private lateinit var tagsStorage: TagStorage
    private lateinit var previewStorage: PreviewProcessor
    private lateinit var metadataStorage: MetadataProcessor
    private lateinit var statsStorage: StatsStorage
    private lateinit var scoreStorage: ScoreStorage
    private lateinit var rootAndFav: RootAndFav
    private lateinit var resourcesIds: List<ResourceId>

    var galleryItems: MutableList<GalleryPresenter.GalleryItem> = mutableListOf()
    var diffResult: DiffUtil.DiffResult? = null

    val selectedResources: MutableList<ResourceId> = mutableListOf()
    val scoreWidgetController = ScoreWidgetController(
        scope = viewModelScope,
        getCurrentId = { currentItem.id() },
        onScoreChanged = {
            _notifyResourceScoresChanged.value = true
        }
    )

    private var currentPos = 0
    private var sortByScores = false
    private var selectingEnabled: Boolean = false

    private val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow()
    private val currentItem: GalleryPresenter.GalleryItem
        get() = galleryItems[currentPos]

    private val _notifyResourceScoresChanged: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyResourceScoresChanged: StateFlow<Boolean> =
        _notifyResourceScoresChanged

    private val _setControlsVisibility: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val setControlsVisibility: StateFlow<Boolean> =
        _setControlsVisibility

    private val _onNavigateBack: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val onNavigateBack: StateFlow<Boolean> = _onNavigateBack

    private val _deleteResource: MutableStateFlow<Int?> = MutableStateFlow(null)
    val deleteResource: StateFlow<Int?> = _deleteResource

    private val _toastIndexFailedPath: MutableStateFlow<Path?> =
        MutableStateFlow(null)
    val toastIndexFailedPath: StateFlow<Path?> = _toastIndexFailedPath

    private val _showInfoAlert: MutableStateFlow<ShowInfoData?> =
        MutableStateFlow(null)
    val showInfoAlert: StateFlow<ShowInfoData?> = _showInfoAlert

    private val _displayStorageException: MutableStateFlow<StorageExceptionGallery?> =
        MutableStateFlow(null)
    val displayStorageException: StateFlow<StorageExceptionGallery?> =
        _displayStorageException

    private val _updatePagerAdapter: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val updatePagerAdapter: StateFlow<Boolean> = _updatePagerAdapter

    private val _shareLink: MutableStateFlow<String> = MutableStateFlow("")
    val shareLink: StateFlow<String> = _shareLink

    private val _shareResource: MutableStateFlow<Path?> = MutableStateFlow(null)
    val shareResource: StateFlow<Path?> = _shareResource

    private val _editResource: MutableStateFlow<Path?> = MutableStateFlow(null)
    val editResource: StateFlow<Path?> = _editResource

    private val _openLink: MutableStateFlow<String> = MutableStateFlow("")
    val openLink: StateFlow<String> = _openLink

    private val _viewInExternalApp: MutableStateFlow<Path?> =
        MutableStateFlow(null)
    val viewInExternalApp: StateFlow<Path?> = _viewInExternalApp

    private val _displayPreviewTags: MutableStateFlow<ResourceIdTagsPreview?> =
        MutableStateFlow(null)
    val displayPreviewTags: StateFlow<ResourceIdTagsPreview?> = _displayPreviewTags

    private val _notifyTagsChanged: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyTagsChanged: StateFlow<Boolean> = _notifyTagsChanged

    private val _showEditTagsDialog: MutableStateFlow<ShowEditTagsData?> =
        MutableStateFlow(null)
    val showEditTagsDialog: StateFlow<ShowEditTagsData?> = _showEditTagsDialog

    private val _setUpPreview: MutableStateFlow<SetupPreview?> =
        MutableStateFlow(null)
    val setUpPreview: StateFlow<SetupPreview?> = _setUpPreview

    private val _displaySelected: MutableStateFlow<DisplaySelected?> =
        MutableStateFlow(null)
    val displaySelected: StateFlow<DisplaySelected?> = _displaySelected

    private val _notifyResourceChange: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyResourceChange: StateFlow<Boolean> = _notifyResourceChange

    private val _showProgressWithText: MutableStateFlow<ProgressWithText?> =
        MutableStateFlow(null)
    val showProgressWithText: StateFlow<ProgressWithText?> = _showProgressWithText

    private val _notifyCurrentItemChange: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyCurrentItemChange: StateFlow<Boolean> = _notifyCurrentItemChange

    private val _updatePagerAdapterWithDiff: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val updatePagerAdapterWithDiff: StateFlow<Boolean> = _updatePagerAdapterWithDiff

    fun initialize(
        rootAndFav: RootAndFav,
        resourcesIds: List<ResourceId>,
    ) {
        this.rootAndFav = rootAndFav
        this.resourcesIds = resourcesIds
        onFirstViewAttach()
    }

    fun onPreviewsItemClick() {
        _setControlsVisibility.value = !_setControlsVisibility.value
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
            "[remove_resource] clicked at position $currentPos"
        )
        deleteResource(currentItem.id())
        galleryItems.removeAt(currentPos)

        if (galleryItems.isEmpty()) {
            _onNavigateBack.emit(true)
            return@launch
        }

        onTagsChanged()
        _deleteResource.value = currentPos

    }

    private suspend fun deleteResource(resource: ResourceId) {
        Timber.d(LogTags.GALLERY_SCREEN, "deleting resource $resource")

        val path = index.getPath(resource)

        withContext(Dispatchers.IO) {
            Files.delete(path)
        }

        index.updateAll()
        _notifyResourceChange.value = true
    }

    private fun onFirstViewAttach() {
        analytics.trackScreen()
        Timber.d(LogTags.GALLERY_SCREEN, "first view attached in GalleryPresenter")
        viewModelScope.launch {
            _showProgressWithText.value = ProgressWithText(
                isVisible = true,
                text = "Providing root index",
            )
            index = indexRepo.provide(rootAndFav)
            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed ->
                        _toastIndexFailedPath.value = message.path
                }
            }.launchIn(viewModelScope)

            _showProgressWithText.value = ProgressWithText(
                isVisible = true,
                text = "Providing metadata storage",
            )
            metadataStorage = metadataStorageRepo.provide(index)
            _showProgressWithText.value = ProgressWithText(
                isVisible = true,
                text = "Providing previews storage",
            )
            previewStorage = previewStorageRepo.provide(index)
            _showProgressWithText.value = ProgressWithText(
                isVisible = true,
                text = "Providing data storage",
            )
            try {
                tagsStorage = tagsStorageRepo.provide(index)
                scoreStorage = scoreStorageRepo.provide(index)
            } catch (e: StorageException) {
                _displayStorageException.value = StorageExceptionGallery(
                    label = e.label,
                    messenger = e.msg
                )
            }

            statsStorage = statsStorageRepo.provide(index)
            scoreWidgetController.init(scoreStorage)

            galleryItems = provideGalleryItems().toMutableList()

            sortByScores = preferences.get(PreferenceKey.SortByScores)
            _updatePagerAdapter.value = true

            _showProgressWithText.value = ProgressWithText(
                isVisible = false,
                text = ""
            )
            scoreWidgetController.setVisible(sortByScores)
        }
    }

    fun onPlayButtonClick() = viewModelScope.launch {
        _viewInExternalApp.value = index.getPath(currentItem.id())!!
    }

    fun onInfoFabClick() = viewModelScope.launch {
        analytics.trackResInfo()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[info_resource] clicked at position $currentPos"
        )

        val path = index.getPath(currentItem.id())!!
        val data = ShowInfoData(
            path = path,
            resource = currentItem.resource,
            metadata = currentItem.metadata
        )
        _showInfoAlert.emit(data)
    }


    fun onShareFabClick() = viewModelScope.launch {
        analytics.trackResShare()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[share_resource] clicked at position $currentPos"
        )
        val path = index.getPath(currentItem.id())!!

        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            _shareLink.emit(url)
            return@launch
        }
        _shareResource.emit(path)
    }

    private val _toggleSelect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val toggleSelect: StateFlow<Boolean> = _toggleSelect
    fun onSelectingChanged() {
        viewModelScope.launch {
            selectingEnabled = !selectingEnabled
            _toggleSelect.emit(selectingEnabled)
            selectedResources.clear()
            if (selectingEnabled) {
                selectedResources.add(currentItem.resource.id)
            }
        }
    }

    fun onOpenFabClick() = viewModelScope.launch {
        analytics.trackResOpen()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[open_resource] clicked at position $currentPos"
        )

        val id = currentItem.id()
        val path = index.getPath(id)!!

        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            _openLink.emit(url)
            return@launch
        }
        _viewInExternalApp.emit(path)
    }

    fun onEditFabClick() = viewModelScope.launch {
        analytics.trackResEdit()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[edit_resource] clicked at position $currentPos"
        )
        val path = index.getPath(currentItem.id())!!
        _editResource.emit(path)
    }

    fun onSelectBtnClick() {
        val id = currentItem.id()
        val wasSelected = id in selectedResources

        if (wasSelected) {
            selectedResources.remove(id)
        } else {
            selectedResources.add(id)
        }

        _displaySelected.value = DisplaySelected(
            selected = !wasSelected,
            showAnim = true,
            selectedCount = selectedResources.size,
            itemCount = galleryItems.size
        )
    }

    fun onResume() {
        checkResourceChanges(currentPos)
    }

    fun onTagsChanged() {
        val tags = tagsStorage.getTags(currentItem.id())
        _displayPreviewTags.value = ResourceIdTagsPreview(
            resourceId = currentItem.id(),
            tags = tags,
        )
    }

    fun onPageChanged(newPos: Int) = viewModelScope.launch {
        if (galleryItems.isEmpty())
            return@launch

        checkResourceChanges(newPos)

        currentPos = newPos

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

        _displayPreviewTags.value = ResourceIdTagsPreview(
            resourceId = id,
            tags = newTags,
        )
        statsStorage.handleEvent(
            StatsEvent.TagsChanged(
                id, tags, newTags
            )
        )

        Timber.d(LogTags.GALLERY_SCREEN, "setting new tags $newTags to $currentItem")

        tagsStorage.setTags(id, newTags)
        tagsStorage.persist()
        _notifyTagsChanged.value = true
    }

    fun onEditTagsDialogBtnClick() {
        analytics.trackTagsEdit()
        _showEditTagsDialog.value = ShowEditTagsData(
            resource = currentItem.id(),
            resources = listOf(currentItem.id()),
            statsStorage = statsStorage,
            rootAndFav = rootAndFav,
            index = index,
            storage = tagsStorage,
        )
    }

    private fun displayPreview(
        id: ResourceId,
        meta: Metadata,
        tags: Tags
    ) {
        _setUpPreview.value = SetupPreview(
            position = currentPos,
            meta = meta,
        )

        _displayPreviewTags.value = ResourceIdTagsPreview(
            resourceId = id,
            tags = tags,
        )
        scoreWidgetController.displayScore()

        _displaySelected.value = DisplaySelected(
            selected = id in selectedResources,
            showAnim = false,
            selectedCount = selectedResources.size,
            itemCount = galleryItems.size,
        )
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


    private fun invokeHandleGalleryExternalChangesUseCase(
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _showProgressWithText.value = ProgressWithText(
                    isVisible = true,
                    text = "Changes detected, indexing"
                )
            }

            index.updateAll()

            withContext(Dispatchers.Main) {
                _notifyResourceChange.value = true
            }

            viewModelScope.launch {
                metadataStorage.busy.collect { busy ->
                    if (!busy) cancel()
                }
            }.join()

            val newItems = provideGalleryItems()
            if (newItems.isEmpty()) {
                _onNavigateBack.value = true
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
                _updatePagerAdapterWithDiff.value = true
                _notifyCurrentItemChange.value = true
                _showProgressWithText.value = ProgressWithText(
                    isVisible = true,
                    text = "Changes detected, indexing"
                )
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


