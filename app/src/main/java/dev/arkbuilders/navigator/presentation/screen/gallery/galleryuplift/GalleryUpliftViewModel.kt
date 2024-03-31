package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
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
import dev.arkbuilders.arklib.user.score.ScoreStorage
import dev.arkbuilders.arklib.user.score.ScoreStorageRepo
import dev.arkbuilders.arklib.user.tags.Tag
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.arklib.user.tags.TagsStorageRepo
import dev.arkbuilders.components.scorewidget.ScoreWidgetController
import dev.arkbuilders.navigator.analytics.gallery.GalleryAnalytics
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.stats.StatsStorage
import dev.arkbuilders.navigator.data.stats.StatsStorageRepo
import dev.arkbuilders.navigator.data.utils.LogTags
import dev.arkbuilders.navigator.domain.HandleGalleryExternalChangesUseCase
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.presenterScope
import timber.log.Timber
import java.io.FileReader
import java.nio.file.Path
import javax.inject.Inject

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
    val analytics: GalleryAnalytics
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

    private val currentItem: GalleryPresenter.GalleryItem
        get() = galleryItems[currentPos]

    val scoreWidgetController = ScoreWidgetController(
        scope = viewModelScope,
        getCurrentId = { currentItem.id() },
        onScoreChanged = {
//            viewState.notifyResourceScoresChanged()
        }
    )

    private val _onNavigateBack: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val onNavigateBack: StateFlow<Boolean> = _onNavigateBack

    private val _deleteResource: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val deleteResource: StateFlow<Boolean> = _deleteResource
    fun onRemoveFabClick() = viewModelScope.launch(NonCancellable) {
        analytics.trackResRemove()
        Timber.d(
            LogTags.GALLERY_SCREEN,
            "[remove_resource] clicked at position $currentPos"
        )
        //TODO Trigger fragment.deleteResource
//        deleteResource(currentItem.id())
        _deleteResource.emit(true)
        galleryItems.removeAt(currentPos)

        if (galleryItems.isEmpty()) {
            //TODO Trigger fragment.onBackClick()
            _onNavigateBack.emit(true)
//            onBackClick()
            return@launch
        }

        onTagsChanged()
        _deleteResource.emit(true)
//        viewState.deleteResource(currentPos)

    }


    private val _showInfoAlert: MutableStateFlow<ShowInfoData?> = MutableStateFlow(null)
    val showInfoAlert: StateFlow<ShowInfoData?> = _showInfoAlert

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
    fun onPageChanged(postion: Int) {}
    fun onTagSelected(tag: Tag) {}
    fun onTagRemove(tag: Tag) {}
    fun onEditTagsDialogBtnClick() {}


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
