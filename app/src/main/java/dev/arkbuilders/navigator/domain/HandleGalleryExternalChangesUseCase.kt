package dev.arkbuilders.navigator.domain

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.presenterScope
import java.nio.file.Path
import javax.inject.Inject

class HandleGalleryExternalChangesUseCase @Inject constructor() {
    operator fun invoke(
        resourcePath: Path,
        oldId: ResourceId,
        presenter: GalleryPresenter
    ) = with(presenter) {
        presenterScope.launch {
            withContext(Dispatchers.Main) {
                viewState.setProgressVisibility(true, "Changes detected, indexing")
            }

            val update = index.updateOne(resourcePath, oldId)
            val newId = update.added.keys.firstOrNull()

            withContext(Dispatchers.Main) {
                viewState.notifyResourcesChanged()
            }

            newId?.let {
                presenterScope.launch {
                    metadataStorage.busy.collect { busy ->
                        if (!busy)
                            cancel()
                    }
                }.join()
            }

            val newResourceIds = presenter.resourcesIds.toMutableList()
            newResourceIds.apply {
                val oldIdIndex = indexOf(oldId)
                removeAt(oldIdIndex)
                newId?.let { add(oldIdIndex, newId) }
            }
            presenter.resourcesIds = newResourceIds

            galleryItems = provideGalleryItems().toMutableList()
            if (galleryItems.isEmpty()) {
                onBackClick()
                return@launch
            }

            withContext(Dispatchers.Main) {
                viewState.notifyCurrentItemChanged()
                viewState.setProgressVisibility(false)
            }
        }
    }
}
