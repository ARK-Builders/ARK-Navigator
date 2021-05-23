package space.taran.arkbrowser.ui.adapter

import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.mvp.presenter.adapter.ResourcesList
import java.nio.file.Path

class ResourcesGrid(private val grid: ResourcesList)
    : FilesRVAdapter<ResourceId>(grid) {

    fun <T: Comparable<T>>sortBy(selector: (Path) -> T) {
        super.updateItems(grid.sortedBy(selector))
    }

    fun reverse() {
        super.updateItems(grid.items().reversed())
    }
}