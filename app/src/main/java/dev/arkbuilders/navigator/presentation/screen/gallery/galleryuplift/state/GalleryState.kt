package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.state

import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.DisplaySelected
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ProgressWithText
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ResourceIdTagsPreview
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.SetupPreview
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ShowEditTagsData
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.ShowInfoData
import dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain.StorageExceptionGallery
import java.nio.file.Path

data class GalleryState(
    val currentPos: Int = 0,
    val sortByScores: Boolean = false,
    val selectingEnabled: Boolean = false,
)

sealed class GallerySideEffect {
    data object NotifyResourceScoresChanged : GallerySideEffect()
    data class ControlVisible(val isVisible: Boolean) : GallerySideEffect()
    data object NavigateBack : GallerySideEffect()
    data class DeleteResource(val pos: Int) : GallerySideEffect()
    data class ToastIndexFailedPath(val path: Path) : GallerySideEffect()
    data class ShowInfoAlert(val infoData: ShowInfoData) : GallerySideEffect()
    data class DisplayStorageException(val storageException: StorageExceptionGallery) :
        GallerySideEffect()

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
    data object NotifyResourceChange: GallerySideEffect()
    data class ShowProgressWithText(val text: ProgressWithText): GallerySideEffect()
    data object NotifyCurrentItemChange: GallerySideEffect()
    data object UpdatePagerAdapterWithDiff: GallerySideEffect()
}
