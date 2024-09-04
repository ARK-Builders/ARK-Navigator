package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain

import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.Resource
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.meta.Metadata
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.arklib.user.tags.Tags
import dev.arkbuilders.navigator.data.stats.StatsStorage
import java.nio.file.Path

data class GalleryState(
    val rootAndFav: RootAndFav,
    val currentPos: Int = 0,
    val galleryItems: List<GalleryItem> = emptyList(),
    val selectingEnabled: Boolean = false,
    val selectedResources: List<ResourceId> = emptyList(),
    val controlsVisible: Boolean = true,
    val progressState: ProgressState = ProgressState.HideProgress,
    val tags: Tags = emptySet()
) {
    val currentItem: GalleryItem
        get() = galleryItems[currentPos]

    val currentItemSelected: Boolean
        get() = currentItem.id() in selectedResources
}

sealed interface ProgressState {
    data object ProvidingRootIndex : ProgressState
    data object ProvidingMetaDataStorage : ProgressState
    data object ProvidingPreviewStorage : ProgressState
    data object ProvidingDataStorage : ProgressState
    data object Indexing : ProgressState
    data object HideProgress : ProgressState
}

sealed class GallerySideEffect {
    data class ScrollToPage(val pos: Int) : GallerySideEffect()
    data object NotifyResourceScoresChanged : GallerySideEffect()
    data object NavigateBack : GallerySideEffect()
    data class DeleteResource(val pos: Int) : GallerySideEffect()
    data class ToastIndexFailedPath(val path: Path) : GallerySideEffect()
    data class ShowInfoAlert(
        val path: Path,
        val resource: Resource,
        val metadata: Metadata
    ) : GallerySideEffect()

    data class DisplayStorageException(
        val label: String,
        val messenger: String
    ) : GallerySideEffect()

    data class ShareLink(val url: String) : GallerySideEffect()
    data class ShareResource(val path: Path) : GallerySideEffect()
    data class EditResource(val path: Path) : GallerySideEffect()
    data class OpenLink(val url: String) : GallerySideEffect()
    data class ViewInExternalApp(val path: Path) : GallerySideEffect()

    data object NotifyTagsChanged : GallerySideEffect()
    data class ShowEditTagsDialog(
        val resource: ResourceId,
        val rootAndFav: RootAndFav,
        val resources: List<ResourceId>,
        val index: ResourceIndex,
        val storage: TagStorage,
        val statsStorage: StatsStorage
    ) : GallerySideEffect()

    // workaround to not show checkbox select animation when we change page
    data object AbortSelectAnimation: GallerySideEffect()

    data object NotifyResourceChange : GallerySideEffect()
    data object NotifyCurrentItemChange : GallerySideEffect()
}
