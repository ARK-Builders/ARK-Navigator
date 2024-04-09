package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.Message
import dev.arkbuilders.arklib.data.index.Resource
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
import dev.arkbuilders.navigator.domain.HandleGalleryExternalChangesUseCase
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import dev.arkbuilders.navigator.presentation.screen.resources.adapter.ResourceDiffUtilCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
import javax.inject.Inject
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

class GalleryUpliftViewModel @Inject constructor(
    val preferences: Preferences,
    val router: AppRouter,
    val indexRepo: ResourceIndexRepo,
    val previewStorageRepo: PreviewProcessorRepo,
    val metadataStorageRepo: MetadataProcessorRepo,
    val tagsStorageRepo: TagsStorageRepo,
    val statsStorageRepo: StatsStorageRepo,
    val scoreStorageRepo: ScoreStorageRepo,
    private val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow(),
    val handleGalleryExternalChangesUseCase: HandleGalleryExternalChangesUseCase,
    private val resourcesIndexRepo: ResourceIndexRepo,
    private val metadataProcessorRepo: MetadataProcessorRepo,
    private val previewProcessorRepo: PreviewProcessorRepo,
    val analytics: GalleryAnalytics,
    private val rootAndFav: RootAndFav,
    val folders: RootAndFav,
) : ViewModel() {
    lateinit var index: ResourceIndex
        private set
    lateinit var tagsStorage: TagStorage
        private set
    private lateinit var previewStorage: PreviewProcessor
    lateinit var metadataStorage: MetadataProcessor
        private set
    lateinit var statsStorage: StatsStorage
        private set
    private lateinit var scoreStorage: ScoreStorage

    var galleryItems: MutableList<GalleryPresenter.GalleryItem> = mutableListOf()

    var diffResult: DiffUtil.DiffResult? = null

    private var currentPos = 0
    val selectedResources: MutableList<ResourceId> = mutableListOf()

    private lateinit var previewProcessor: PreviewProcessor


    private val currentItem: GalleryPresenter.GalleryItem
        get() = galleryItems[currentPos]

    private val _notifyResourceScoresChanged: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyResourceScoresChanged: StateFlow<Boolean> =
        _notifyResourceScoresChanged

    fun bindPlainTextView(view: PreviewPlainTextViewHolderUplift) =
        viewModelScope.launch {
            view.reset()
            val item = galleryItems[view.pos]

            val path = index.getPath(item.id())!!
            val content = readText(path)

            content.onSuccess {
                view.setContent(it)
            }
        }

    private var isControlsVisible = false

    private val _setControlsVisibility: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val setControlsVisibility: StateFlow<Boolean> =
        _setControlsVisibility

    fun onPreviewsItemClick() {
        isControlsVisible = !isControlsVisible
        // TODO Trigger setControlsVisibility
        _setControlsVisibility.value = isControlsVisible
//        viewState.setControlsVisibility(isControlsVisible)
    }

    fun bindView(view: PreviewImageViewHolderUplift) = viewModelScope.launch {
        view.reset()
        val item = galleryItems[view.pos]

        val path = index.getPath(item.id())!!
        val placeholder = ImageUtils.iconForExtension(extension(path))
        view.setSource(placeholder, item.id(), item.metadata, item.preview)
    }

    val scoreWidgetController = ScoreWidgetController(
        scope = viewModelScope,
        getCurrentId = { currentItem.id() },
        onScoreChanged = {
            _notifyResourceScoresChanged.value = true
            // TODO Trigger notifyResourceScoresChanged
//            viewState.notifyResourceScoresChanged()
        }
    )

    fun getKind(pos: Int): Int =
        galleryItems[pos].metadata.kind.ordinal

    private val _onNavigateBack: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val onNavigateBack: StateFlow<Boolean> = _onNavigateBack

    private val _deleteResource: MutableStateFlow<Int?> = MutableStateFlow(null)
    val deleteResource: StateFlow<Int?> = _deleteResource
    fun onRemoveFabClick() = viewModelScope.launch(NonCancellable) {
        analytics.trackResRemove()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[remove_resource] clicked at position $currentPos"
        )
        //TODO Trigger fragment.deleteResource
//        deleteResource(currentItem.id())
        deleteResource(currentItem.id())
        galleryItems.removeAt(currentPos)

        if (galleryItems.isEmpty()) {
            //TODO Trigger fragment.onBackClick()
            _onNavigateBack.emit(true)
//            onBackClick()
            return@launch
        }

        onTagsChanged()
        _deleteResource.value = currentPos
//        viewState.deleteResource(currentPos)

    }

    private suspend fun deleteResource(resource: ResourceId) {
        Timber.d(LogTags.GALLERY_SCREEN, "deleting resource $resource")

        val path = index.getPath(resource)

        withContext(Dispatchers.IO) {
            Files.delete(path)
        }

        index.updateAll()
        _notifyResourceChange.value = true
        // TODO Trigger notifyResourcesChanged
//        viewState.notifyResourcesChanged()
    }

    private var sortByScores = false

    private val _toastIndexFailedPath: MutableStateFlow<Path?> =
        MutableStateFlow(null)
    val toastIndexFailedPath: StateFlow<Path?> = _toastIndexFailedPath

    private val resourcesIds: List<ResourceId> = listOf()

    private val _showInfoAlert: MutableStateFlow<ShowInfoData?> =
        MutableStateFlow(null)
    val showInfoAlert: StateFlow<ShowInfoData?> = _showInfoAlert

    private val _init: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val init: StateFlow<Boolean> = _init

    private val _displayStorageException: MutableStateFlow<StorageExceptionGallery?> =
        MutableStateFlow(null)
    val displayStorageException: StateFlow<StorageExceptionGallery?> =
        _displayStorageException

    private val _updatePagerAdapter: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val updatePagerAdapter: StateFlow<Boolean> = _updatePagerAdapter
    fun onFirstViewAttach() {
        analytics.trackScreen()
        Timber.d(LogTags.GALLERY_SCREEN, "first view attached in GalleryPresenter")
        viewModelScope.launch {
            //TODO Trigger init
            _init.value = true
//            viewState.init()
            _showProgress.value = true
            //TODO Trigger setProgressVisibility
//            viewState.setProgressVisibility(true, "Providing root index")

            index = indexRepo.provide(rootAndFav)
            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed ->
                        _toastIndexFailedPath.value = message.path
                    //TODO Trigger toastIndexFailedPath
//                        viewState.toastIndexFailedPath(message.path)
                }
            }.launchIn(viewModelScope)

            //TODO Trigger setProgressVisibility
            _showProgress.value = true
//            viewState.setProgressVisibility(true, "Providing metadata storage")
            metadataStorage = metadataStorageRepo.provide(index)

            //TODO Trigger setProgressVisibility
            _showProgress.value = true
//            viewState.setProgressVisibility(true, "Providing previews storage")
            previewStorage = previewStorageRepo.provide(index)

            //TODO Trigger setProgressVisibility
            _showProgress.value = true
//            viewState.setProgressVisibility(true, "Proviging data storages")

            try {
                tagsStorage = tagsStorageRepo.provide(index)
                scoreStorage = scoreStorageRepo.provide(index)
            } catch (e: StorageException) {
                _displayStorageException.value = StorageExceptionGallery(
                    label = e.label,
                    messenger = e.msg
                )
                // TODO Trigger displayStorageException
//                viewState.displayStorageException(
//                    e.label,
//                    e.msg
//
            }

            statsStorage = statsStorageRepo.provide(index)
            scoreWidgetController.init(scoreStorage)

            galleryItems = provideGalleryItems().toMutableList()

            sortByScores = preferences.get(PreferenceKey.SortByScores)

            // TODO Trigger updatePagerAdapter
//            viewState.updatePagerAdapter()
            _updatePagerAdapter.value = true

            // TODO Trigger setProgressVisibility
            _showProgress.value = false
//            viewState.setProgressVisibility(false)
            scoreWidgetController.setVisible(sortByScores)
        }
    }

    fun onPlayButtonClick() = viewModelScope.launch {
        // TODO Trigger viewInExternalApp
        _viewInExternalApp.value = index.getPath(currentItem.id())!!
//        viewState.viewInExternalApp(index.getPath(currentItem.id())!!)
    }

    data class StorageExceptionGallery(val label: String, val messenger: String)

    data class ShowInfoData(
        val path: Path,
        val resource: Resource,
        val metadata: Metadata
    )

    fun onInfoFabClick() = viewModelScope.launch {
        analytics.trackResInfo()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[info_resource] clicked at position $currentPos"
        )

        val path = index.getPath(currentItem.id())!!
        //TODO Trigger showInfoAlert
        val data = ShowInfoData(
            path = path,
            resource = currentItem.resource,
            metadata = currentItem.metadata
        )
        _showInfoAlert.emit(data)
//        viewState.showInfoAlert(path, currentItem.resource, currentItem.metadata)
    }


    private val _shareLink: MutableStateFlow<String> = MutableStateFlow("")
    val shareLink: StateFlow<String> = _shareLink


    private val _shareResource: MutableStateFlow<Path?> = MutableStateFlow(null)
    val shareResource: StateFlow<Path?> = _shareResource
    fun onShareFabClick() = viewModelScope.launch {
        analytics.trackResShare()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[share_resource] clicked at position $currentPos"
        )
        val path = index.getPath(currentItem.id())!!

        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            //TODO Trigger sharelink
//            viewState.shareLink(url)
            _shareLink.emit(url)
            return@launch
        }
        //TODO Trigger shareResource
//        viewState.shareResource(path)
        _shareResource.emit(path)
    }

    private var selectingEnabled: Boolean = false

    private val _toggleSelect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val toggleSelect: StateFlow<Boolean> = _toggleSelect
    fun onSelectingChanged() {
        viewModelScope.launch {
            selectingEnabled = !selectingEnabled
            //TODO Trigger toggleSelecting
            _toggleSelect.emit(selectingEnabled)
//        viewState.toggleSelecting(selectingEnabled)
            selectedResources.clear()
            if (selectingEnabled) {
                selectedResources.add(currentItem.resource.id)
            }
        }
    }

    private val _openLink: MutableStateFlow<String> = MutableStateFlow("")
    val openLink: StateFlow<String> = _openLink


    private val _viewInExternalApp: MutableStateFlow<Path?> =
        MutableStateFlow(null)
    val viewInExternalApp: StateFlow<Path?> = _viewInExternalApp
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
            //TODO Trigger openLink
//            viewState.openLink(url)
            _openLink.emit(url)
            return@launch
        }

        //TODO Trigger viewInExternalApp
//        viewState.viewInExternalApp(path)
        _viewInExternalApp.emit(path)

    }


    private val _editResource: MutableStateFlow<Path?> = MutableStateFlow(null)
    val editResource: StateFlow<Path?> = _editResource
    fun onEditFabClick() = viewModelScope.launch {
        analytics.trackResEdit()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[edit_resource] clicked at position $currentPos"
        )
        val path = index.getPath(currentItem.id())!!
        //TODO Trigger editResource
//        viewState.editResource(path)
        _editResource.emit(path)
    }


    fun onSelectBtnClick() {}
    fun onResume() {}
    fun onTagsChanged() {}
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

    private val _displayPreviewTags: MutableStateFlow<ResourceIdTagsPreview?> =
        MutableStateFlow(null)
    val displayPreviewTags: StateFlow<ResourceIdTagsPreview?> = _displayPreviewTags

    private val _notifyTagsChanged: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyTagsChanged: StateFlow<Boolean> = _notifyTagsChanged

    fun onTagRemove(tag: Tag) = viewModelScope.launch(NonCancellable) {
        analytics.trackTagRemove()
        val id = currentItem.id()

        val tags = tagsStorage.getTags(id)
        val newTags = tags - tag

        // TODO Trigger displaypreviewtags
        _displayPreviewTags.value =
            ResourceIdTagsPreview(resourceId = id, tags = newTags)
//        viewState.displayPreviewTags(id, newTags)
        statsStorage.handleEvent(
            StatsEvent.TagsChanged(
                id, tags, newTags
            )
        )

        Timber.d(LogTags.GALLERY_SCREEN, "setting new tags $newTags to $currentItem")

        tagsStorage.setTags(id, newTags)
        tagsStorage.persist()
        _notifyTagsChanged.value = true
        // TODO Trigger notifyTagsChanged
//        viewState.notifyTagsChanged()
    }

    private val _showEditTagsDialog: MutableStateFlow<ResourceId?> =
        MutableStateFlow(null)
    val showEditTagsDialog: StateFlow<ResourceId?> = _showEditTagsDialog
    fun onEditTagsDialogBtnClick() {
        analytics.trackTagsEdit()
        // TODO _showEditTagsDialog
        _showEditTagsDialog.value = currentItem.id()
//        viewState.showEditTagsDialog(currentItem.id())
    }


    private val _setUpPreview: MutableStateFlow<SetupPreview?> =
        MutableStateFlow(null)
    val setUpPreview: StateFlow<SetupPreview?> = _setUpPreview

    private val _displaySelected: MutableStateFlow<DisplaySelected?> =
        MutableStateFlow(null)
    val displaySelected: StateFlow<DisplaySelected?> = _displaySelected
    private fun displayPreview(
        id: ResourceId,
        meta: Metadata,
        tags: Tags
    ) {
        _setUpPreview.value = SetupPreview(position = currentPos, meta = meta)
        // TODO Trigger setupPreview
//        viewState.setupPreview(currentPos, meta)

        _displayPreviewTags.value =
            ResourceIdTagsPreview(resourceId = id, tags = tags)
        // TODO Trigger displayPreviewTags
//        viewState.displayPreviewTags(id, tags)
        scoreWidgetController.displayScore()

        _displaySelected.value = DisplaySelected(
            selected = id in selectedResources,
            showAnim = false,
            selectedCount = selectedResources.size,
            itemCount = galleryItems.size
        )
        // TODO Trigger displaySelected
//        viewState.displaySelected(
//            id in selectedResources,
//            showAnim = false,
//            selectedResources.size,
//            galleryItems.size
//        )
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
//                    handleGalleryExternalChangesUseCase(this@GalleryPresenter)
                    return@launch
                }

            if (path.notExists()) {
                Timber.d("Resource ${item.id()} isn't stored by path $path")
                invokeHandleGalleryExternalChangesUseCase()
//                handleGalleryExternalChangesUseCase(this@GalleryPresenter)
                return@launch
            }

            if (path.getLastModifiedTime() != item.resource.modified) {
                Timber.d("Index is not up-to-date regarding path $path")
                invokeHandleGalleryExternalChangesUseCase()
//                handleGalleryExternalChangesUseCase(this@GalleryPresenter)
                return@launch
            }
        }

    fun provideGalleryItems(): List<GalleryPresenter.GalleryItem> =
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

    private val _notifyResourceChange: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyResourceChange: StateFlow<Boolean> = _notifyResourceChange

    private val _showProgress: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val showProgress: StateFlow<Boolean> = _showProgress

    private val _notifyCurrentItemChange: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val notifyCurrentItemChange: StateFlow<Boolean> = _notifyCurrentItemChange

    private val _updatePagerAdapterWithDiff: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val updatePagerAdapterWithDiff: StateFlow<Boolean> = _updatePagerAdapterWithDiff

    private fun invokeHandleGalleryExternalChangesUseCase(
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                // trigger show setProgressVisibility
                _showProgress.value = true
//                viewState.setProgressVisibility(true, "Changes detected, indexing")
            }

            index.updateAll()

            withContext(Dispatchers.Main) {
                _notifyResourceChange.value = true
                // TODO Trigger notifyResourcesChanged
//                viewState.notifyResourcesChanged()
            }

            // TODO: Investigate more
//            viewModelScope.launch {
//                metadataStorage.busy.collect { busy -> if (!busy) cancel()
//                }
//            }.join()

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
                // TODO trigger updatePagerAdapterWithDiff
//                viewState.updatePagerAdapterWithDiff()

                // TODO trigger updatePagerAdapterWithDiff
                _notifyCurrentItemChange.value = true
//                viewState.notifyCurrentItemChanged()

                // TODO trigger show setProgressVisibility
                _showProgress.value = true
//                viewState.setProgressVisibility(true, "Changes detected, indexing")            }
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

data class ResourceIdTagsPreview(val resourceId: ResourceId, val tags: Set<String>)
data class SetupPreview(val position: Int, val meta: Metadata)
data class DisplaySelected(
    val selected: Boolean,
    val showAnim: Boolean,
    val selectedCount: Int,
    val itemCount: Int,
)
