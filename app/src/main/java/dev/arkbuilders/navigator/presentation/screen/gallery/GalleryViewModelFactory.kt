package dev.arkbuilders.navigator.presentation.screen.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.arklib.data.meta.MetadataProcessorRepo
import dev.arkbuilders.arklib.data.preview.PreviewProcessorRepo
import dev.arkbuilders.arklib.user.score.ScoreStorageRepo
import dev.arkbuilders.arklib.user.tags.TagsStorageRepo
import dev.arkbuilders.navigator.analytics.gallery.GalleryAnalytics
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.stats.StatsStorageRepo
import dev.arkbuilders.navigator.presentation.navigation.AppRouter

class GalleryViewModelFactory @AssistedInject constructor(
    @Assisted val startPos: Int,
    @Assisted val selectingEnabled: Boolean,
    @Assisted private val rootAndFav: RootAndFav,
    @Assisted("all") private val resourcesIds: List<ResourceId>,
    @Assisted("selected") private val selectedResources: List<ResourceId>,
    val preferences: Preferences,
    val router: AppRouter,
    val indexRepo: ResourceIndexRepo,
    val previewStorageRepo: PreviewProcessorRepo,
    val metadataStorageRepo: MetadataProcessorRepo,
    val tagsStorageRepo: TagsStorageRepo,
    val statsStorageRepo: StatsStorageRepo,
    val scoreStorageRepo: ScoreStorageRepo,
    val analytics: GalleryAnalytics
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GalleryViewModel(
            startPos = startPos,
            selectingEnabled = selectingEnabled,
            selectedResources = selectedResources,
            rootAndFav = rootAndFav,
            resourcesIds = resourcesIds,
            preferences = preferences,
            router = router,
            indexRepo = indexRepo,
            previewStorageRepo = previewStorageRepo,
            metadataStorageRepo = metadataStorageRepo,
            tagsStorageRepo = tagsStorageRepo,
            statsStorageRepo = statsStorageRepo,
            scoreStorageRepo = scoreStorageRepo,
            analytics = analytics
        ) as T
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted startPos: Int,
            @Assisted selectingEnabled: Boolean,
            @Assisted rootAndFav: RootAndFav,
            @Assisted("all") resourcesIds: List<ResourceId>,
            @Assisted("selected") selectedResources: List<ResourceId>
        ): GalleryViewModelFactory
    }
}
