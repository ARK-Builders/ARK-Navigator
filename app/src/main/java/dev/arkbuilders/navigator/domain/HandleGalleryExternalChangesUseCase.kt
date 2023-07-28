package dev.arkbuilders.navigator.domain

import dev.arkbuilders.navigator.mvp.presenter.GalleryPresenter
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

            presenterScope.launch {
                metadataStorage.busy.collect { busy ->
                    if (!busy)
                        cancel()
                }
            }.join()

            fillGalleryItems()
            withContext(Dispatchers.Main) {
                viewState.notifyResourcesChanged()
                viewState.updatePagerAdapter()
                viewState.setProgressVisibility(false)
            }
        }
    }
}
