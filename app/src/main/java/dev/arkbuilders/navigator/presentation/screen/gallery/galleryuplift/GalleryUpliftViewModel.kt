package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import dev.arkbuilders.arklib.data.Message
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
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
import dev.arkbuilders.navigator.domain.HandleGalleryExternalChangesUseCase
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import kotlinx.coroutines.flow.MutableSharedFlow
import moxy.presenterScope
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

    private val currentItem: GalleryPresenter.GalleryItem
        get() = galleryItems[currentPos]

    val scoreWidgetController = ScoreWidgetController(
        scope = viewModelScope,
        getCurrentId = { currentItem.id() },
        onScoreChanged = {
//            viewState.notifyResourceScoresChanged()
        }
    )


    fun onSelectBtnClick() {}
    fun onResume() {}
    fun onTagsChanged() {}
    fun onPageChanged(postion: Int) {}
    fun onTagSelected(tag: Tag) {}
    fun onTagRemove(tag: Tag) {}
    fun onEditTagsDialogBtnClick() {}
}
