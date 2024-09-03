package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.state

import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.DisplaySelected
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ResourceIdTagsPreview
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.SetupPreview
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ShowEditTagsData
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ShowInfoData
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.StorageExceptionGallery
import java.nio.file.Path

data class GalleryState(
    val rootAndFav: RootAndFav,
    val currentPos: Int = 0,
    val galleryItems: List<GalleryPresenter.GalleryItem> = emptyList(),
    val selectingEnabled: Boolean = false,
    val controlsVisible: Boolean = true,
)

sealed interface ProgressState {
    data object ProvidingRootIndex : ProgressState
    data object ProvidingMetaDataStorage : ProgressState
    data object ProvidingPreviewStorage : ProgressState
    data object ProvidingDataStorage : ProgressState
    data object Indexing : ProgressState
    data object HideProgress : ProgressState
}

sealed class GallerySideEffect {
    data class ScrollToPage(val pos: Int): GallerySideEffect()
    data object NotifyResourceScoresChanged : GallerySideEffect()
    data object NavigateBack : GallerySideEffect()
    data class DeleteResource(val pos: Int) : GallerySideEffect()
    data class ToastIndexFailedPath(val path: Path) : GallerySideEffect()
    data class ShowInfoAlert(val infoData: ShowInfoData) : GallerySideEffect()
    data class DisplayStorageException(
        val storageException: StorageExceptionGallery
    ) : GallerySideEffect()

    data object UpdatePagerAdapter : GallerySideEffect()
    data class ShareLink(val url: String) : GallerySideEffect()
    data class ShareResource(val path: Path) : GallerySideEffect()
    data class EditResource(val path: Path) : GallerySideEffect()
    data class OpenLink(val url: String) : GallerySideEffect()
    data class ViewInExternalApp(val path: Path) : GallerySideEffect()
    data class DisplayPreviewTags(val data: ResourceIdTagsPreview) :
        GallerySideEffect()

    data object NotifyTagsChanged : GallerySideEffect()
    data class ShowEditTagsDialog(val data: ShowEditTagsData) : GallerySideEffect()
    data class SetUpPreview(val data: SetupPreview) : GallerySideEffect()
    data class DisplaySelectedFile(val data: DisplaySelected) : GallerySideEffect()
    data object NotifyResourceChange : GallerySideEffect()
    data class ShowProgressWithText(val state: ProgressState) : GallerySideEffect()
    data object NotifyCurrentItemChange : GallerySideEffect()
    data object UpdatePagerAdapterWithDiff : GallerySideEffect()
    data class ToggleSelect(val isEnabled: Boolean) : GallerySideEffect()
}
