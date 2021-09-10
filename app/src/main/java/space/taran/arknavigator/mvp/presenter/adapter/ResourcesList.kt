package space.taran.arknavigator.mvp.presenter.adapter


import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.view.item.FileItemView
import java.lang.AssertionError
import java.nio.file.Files
import java.nio.file.Path

//todo: move handler to presenter maybe ?

//todo: inherit ReversibleItemGridPresenter to get "undo"
class ResourcesList(
    private val index: ResourcesIndex,
    private var resources: List<ResourceId>,
    handler: ItemClickHandler<ResourceId>)
    : ItemsClickablePresenter<ResourceId, FileItemView>(handler) {

    fun <T: Comparable<T>>sortedBy(selector: (Path) -> T) =
        resources.sortedBy { selector(index.getPath(it)!!) }

    fun removeAt(position: Int): ResourceId {
        val items = resources.toMutableList()
        val resource = items.removeAt(position)
        updateItems(items.toList())

        return resource
    }

    override fun items() = resources

    override fun updateItems(items: List<ResourceId>) {
        resources = items
    }

    override fun bindView(view: FileItemView) {
        val resource = resources[view.position()]

        val path = index.getPath(resource)
            ?: throw AssertionError("Resource to display must be indexed")

        view.setText(path.fileName.toString())

        if (Files.isDirectory(path)) {
            throw AssertionError("Resource can't be a directory")
        }

        view.setIcon(Preview.provide(path))
    }
}