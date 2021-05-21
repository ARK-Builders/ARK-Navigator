package space.taran.arkbrowser.ui.adapter

import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.mvp.presenter.ResourcesGrid
import java.nio.file.Path

class ResourcesGridAdapter(private val grid: ResourcesGrid)
    : ItemGridRVAdapter<Unit, ResourceId>(grid) {

    fun <T: Comparable<T>>sortBy(selector: (Path) -> T) {
        super.updateItems(grid.label(), grid.sortedBy(selector))
    }

    fun reverse() {
        super.updateItems(grid.label(), grid.items().reversed())
    }
}