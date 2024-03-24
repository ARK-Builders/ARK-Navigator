package dev.arkbuilders.navigator.presentation.screen.gallery

import androidx.recyclerview.widget.DiffUtil
import dev.arkbuilders.arklib.data.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.Message
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.data.meta.MetadataProcessor
import dev.arkbuilders.arklib.data.meta.MetadataProcessorRepo
import dev.arkbuilders.arklib.data.preview.PreviewLocator
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
import dev.arkbuilders.navigator.data.utils.LogTags.GALLERY_SCREEN
import dev.arkbuilders.navigator.domain.HandleGalleryExternalChangesUseCase
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.screen.gallery.previewpager.PreviewImageViewHolder
import dev.arkbuilders.navigator.presentation.screen.gallery.previewpager.PreviewPlainTextViewHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import timber.log.Timber
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists

class GalleryPresenter(
    private val rootAndFav: RootAndFav,
    private val resourcesIds: List<ResourceId>,
    startAt: Int,
    private var selectingEnabled: Boolean,
    private val selectedResources: MutableList<ResourceId>,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MvpPresenter<GalleryView>() {

    val scoreWidgetController = ScoreWidgetController(
        presenterScope,
        getCurrentId = { currentItem.id() },
        onScoreChanged = { viewState.notifyResourceScoresChanged() }
    )

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

    data class GalleryItem(
        val resource: Resource,
        val preview: PreviewLocator,
        val metadata: Metadata
    ) {
        fun id() = resource.id
    }

    var galleryItems: MutableList<GalleryItem> = mutableListOf()

    var diffResult: DiffUtil.DiffResult? = null

    private var currentPos = startAt

    private val currentItem: GalleryItem
        get() = galleryItems[currentPos]

    private var sortByScores = false
    private var isControlsVisible = false

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var router: AppRouter

    @Inject
    lateinit var indexRepo: ResourceIndexRepo

    @Inject
    lateinit var previewStorageRepo: PreviewProcessorRepo

    @Inject
    lateinit var metadataStorageRepo: MetadataProcessorRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

    @Inject
    lateinit var statsStorageRepo: StatsStorageRepo

    @Inject
    lateinit var scoreStorageRepo: ScoreStorageRepo

    private val messageFlow: MutableSharedFlow<Message> = MutableSharedFlow()

    @Inject
    lateinit var handleGalleryExternalChangesUseCase:
        HandleGalleryExternalChangesUseCase

    @Inject
    lateinit var analytics: GalleryAnalytics

    override fun onFirstViewAttach() {
        analytics.trackScreen()
        Timber.d(GALLERY_SCREEN, "first view attached in GalleryPresenter")
        super.onFirstViewAttach()
        presenterScope.launch {
            viewState.init()
            viewState.setProgressVisibility(true, "Providing root index")

            index = indexRepo.provide(rootAndFav)
            messageFlow.onEach { message ->
                when (message) {
                    is Message.KindDetectFailed -> viewState.toastIndexFailedPath(
                        message.path
                    )
                }
            }.launchIn(presenterScope)

            viewState.setProgressVisibility(true, "Providing metadata storage")
            metadataStorage = metadataStorageRepo.provide(index)

            viewState.setProgressVisibility(true, "Providing previews storage")
            previewStorage = previewStorageRepo.provide(index)

            viewState.setProgressVisibility(true, "Proviging data storages")

            try {
                tagsStorage = tagsStorageRepo.provide(index)
                scoreStorage = scoreStorageRepo.provide(index)
            } catch (e: StorageException) {
                viewState.displayStorageException(
                    e.label,
                    e.msg
                )
            }

            statsStorage = statsStorageRepo.provide(index)
            scoreWidgetController.init(scoreStorage)

            galleryItems = provideGalleryItems().toMutableList()

            sortByScores = preferences.get(PreferenceKey.SortByScores)

            viewState.updatePagerAdapter()
            viewState.setProgressVisibility(false)
            scoreWidgetController.setVisible(sortByScores)
        }
    }

    fun onPageChanged(newPos: Int) = presenterScope.launch {
        if (galleryItems.isEmpty())
            return@launch

        checkResourceChanges(newPos)

        currentPos = newPos

        val id = currentItem.id()
        val tags = tagsStorage.getTags(id)
        displayPreview(id, currentItem.metadata, tags)
    }

    fun onResume() {
        checkResourceChanges(currentPos)
    }

    fun getKind(pos: Int): Int =
        galleryItems[pos].metadata.kind.ordinal

    fun bindView(view: PreviewImageViewHolder) = presenterScope.launch {
        view.reset()
        val item = galleryItems[view.pos]

        val path = index.getPath(item.id())!!
        val placeholder = ImageUtils.iconForExtension(extension(path))
        view.setSource(placeholder, item.id(), item.metadata, item.preview)
    }

    fun bindPlainTextView(view: PreviewPlainTextViewHolder) = presenterScope.launch {
        view.reset()
        val item = galleryItems[view.pos]

        val path = index.getPath(item.id())!!
        val content = readText(path)

        content.onSuccess {
            view.setContent(it)
        }
    }

    fun onTagsChanged() {
        val tags = tagsStorage.getTags(currentItem.id())
        viewState.displayPreviewTags(currentItem.id(), tags)
    }

    fun onOpenFabClick() = presenterScope.launch {
        analytics.trackResOpen()
        Timber.d(GALLERY_SCREEN, "[open_resource] clicked at position $currentPos")

        val id = currentItem.id()
        val path = index.getPath(id)!!

        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            viewState.openLink(url)

            return@launch
        }

        viewState.viewInExternalApp(path)
    }

    fun onInfoFabClick() = presenterScope.launch {
        analytics.trackResInfo()
        Timber.d(GALLERY_SCREEN, "[info_resource] clicked at position $currentPos")

        val path = index.getPath(currentItem.id())!!
        viewState.showInfoAlert(path, currentItem.resource, currentItem.metadata)
    }

    fun onShareFabClick() = presenterScope.launch {
        analytics.trackResShare()
        Timber.d(GALLERY_SCREEN, "[share_resource] clicked at position $currentPos")
        val path = index.getPath(currentItem.id())!!

        if (currentItem.metadata is Metadata.Link) {
            val url = readText(path).getOrThrow()
            viewState.shareLink(url)
            return@launch
        }

        viewState.shareResource(path)
    }

    fun onEditFabClick() = presenterScope.launch {
        analytics.trackResEdit()
        Timber.d(GALLERY_SCREEN, "[edit_resource] clicked at position $currentPos")
        val path = index.getPath(currentItem.id())!!
        viewState.editResource(path)
    }

    fun onRemoveFabClick() = presenterScope.launch(NonCancellable) {
        analytics.trackResRemove()
        Timber.d(GALLERY_SCREEN, "[remove_resource] clicked at position $currentPos")
        deleteResource(currentItem.id())
        galleryItems.removeAt(currentPos)

        if (galleryItems.isEmpty()) {
            onBackClick()
            return@launch
        }

        onTagsChanged()
        viewState.deleteResource(currentPos)
    }

    fun onTagSelected(tag: Tag) {
        analytics.trackTagSelect()
        router.navigateTo(
            Screens.ResourcesScreenWithSelectedTag(
                rootAndFav, tag
            )
        )
    }

    fun onTagRemove(tag: Tag) = presenterScope.launch(NonCancellable) {
        analytics.trackTagRemove()
        val id = currentItem.id()

        val tags = tagsStorage.getTags(id)
        val newTags = tags - tag

        viewState.displayPreviewTags(id, newTags)
        statsStorage.handleEvent(
            StatsEvent.TagsChanged(
                id, tags, newTags
            )
        )

        Timber.d(GALLERY_SCREEN, "setting new tags $newTags to $currentItem")

        tagsStorage.setTags(id, newTags)
        tagsStorage.persist()
        viewState.notifyTagsChanged()
    }

    fun onSelectBtnClick() {
        val id = currentItem.id()
        val wasSelected = id in selectedResources

        if (wasSelected) {
            selectedResources.remove(id)
        } else {
            selectedResources.add(id)
        }

        viewState.displaySelected(
            !wasSelected,
            showAnim = true,
            selectedResources.size,
            galleryItems.size
        )
    }

    fun onEditTagsDialogBtnClick() {
        analytics.trackTagsEdit()
        viewState.showEditTagsDialog(currentItem.id())
    }

    private fun checkResourceChanges(pos: Int) =
        presenterScope.launch(defaultDispatcher) {
            if (galleryItems.isEmpty()) {
                return@launch
            }

            val item = galleryItems[pos]

            val path = index.getPath(item.id())
                ?: let {
                    Timber.d("Resource ${item.id()} can't be found in the index")
                    handleGalleryExternalChangesUseCase(this@GalleryPresenter)
                    return@launch
                }

            if (path.notExists()) {
                Timber.d("Resource ${item.id()} isn't stored by path $path")
                handleGalleryExternalChangesUseCase(this@GalleryPresenter)
                return@launch
            }

            if (path.getLastModifiedTime() != item.resource.modified) {
                Timber.d("Index is not up-to-date regarding path $path")
                handleGalleryExternalChangesUseCase(this@GalleryPresenter)
                return@launch
            }
        }

    private suspend fun deleteResource(resource: ResourceId) {
        Timber.d(GALLERY_SCREEN, "deleting resource $resource")

        val path = index.getPath(resource)

        withContext(defaultDispatcher) {
            Files.delete(path)
        }

        index.updateAll()
        viewState.notifyResourcesChanged()
    }

    private fun displayPreview(
        id: ResourceId,
        meta: Metadata,
        tags: Tags
    ) {
        viewState.setupPreview(currentPos, meta)
        viewState.displayPreviewTags(id, tags)
        scoreWidgetController.displayScore()
        viewState.displaySelected(
            id in selectedResources,
            showAnim = false,
            selectedResources.size,
            galleryItems.size
        )
    }

    fun onPreviewsItemClick() {
        isControlsVisible = !isControlsVisible
        viewState.setControlsVisibility(isControlsVisible)
    }

    fun onSelectingChanged() {
        selectingEnabled = !selectingEnabled
        viewState.toggleSelecting(selectingEnabled)
        selectedResources.clear()
        if (selectingEnabled) {
            selectedResources.add(currentItem.resource.id)
        }
    }

    fun onPlayButtonClick() = presenterScope.launch {
        viewState.viewInExternalApp(index.getPath(currentItem.id())!!)
    }

    fun onBackClick() {
        Timber.d(GALLERY_SCREEN, "quitting from GalleryPresenter")
        viewState.notifySelectedChanged(selectedResources)
        viewState.exitFullscreen()
        router.exit()
    }

    fun provideGalleryItems(): List<GalleryItem> =
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
                    GalleryItem(resource, preview, metadata)
                }.toMutableList()
        } catch (e: Exception) {
            Timber.d("Can't provide gallery items")
            emptyList()
        }

    private suspend fun readText(source: Path): Result<String> =
        withContext(defaultDispatcher) {
            try {
                val content = FileReader(source.toFile()).readText()
                Result.success(content)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
