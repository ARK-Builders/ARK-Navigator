package dev.arkbuilders.navigator.domain

import androidx.recyclerview.widget.DiffUtil
import dev.arkbuilders.navigator.presentation.screen.gallery.GalleryPresenter
import dev.arkbuilders.navigator.presentation.screen.resources.adapter.ResourceDiffUtilCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.presenterScope
import javax.inject.Inject

class HandleGalleryExternalChangesUseCase @Inject constructor() {
    operator fun invoke(
        presenter: GalleryPresenter
    ) = with(presenter) {
        presenterScope.launch {
            withContext(Dispatchers.Main) {
                viewState.setProgressVisibility(true, "Changes detected, indexing")
            }

            index.updateAll()

            withContext(Dispatchers.Main) {
                viewState.notifyResourcesChanged()
            }

            presenterScope.launch {
                metadataStorage.busy.collect { busy ->
                    if (!busy)
                        cancel()
                }
            }.join()

            val newItems = provideGalleryItems()
            if (newItems.isEmpty()) {
                onBackClick()
                return@launch
            }

            diffResult = DiffUtil.calculateDiff(
                ResourceDiffUtilCallback(
                    galleryItems.map { it.resource.id },
                    newItems.map { it.resource.id }
                )
            )

            galleryItems = newItems.toMutableList()

            withContext(Dispatchers.Main) {
                viewState.updatePagerAdapterWithDiff()
                viewState.notifyCurrentItemChanged()
                viewState.setProgressVisibility(false)
            }
        }
    }
}
