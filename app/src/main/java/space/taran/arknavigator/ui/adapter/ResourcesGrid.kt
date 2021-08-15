package space.taran.arknavigator.ui.adapter

import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.presenter.adapter.ResourcesList
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