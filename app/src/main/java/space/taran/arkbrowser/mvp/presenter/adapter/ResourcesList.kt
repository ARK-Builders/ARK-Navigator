package space.taran.arkbrowser.mvp.presenter.adapter


import space.taran.arkbrowser.mvp.model.dao.common.Preview
import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.mvp.model.repo.ResourcesIndex
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.*
import ru.terrakok.cicerone.Router
import android.util.Log
import space.taran.arkbrowser.navigation.Screens
import java.lang.AssertionError
import java.nio.file.Files
import java.nio.file.Path

//todo: pass Query from tags selector. every resource
// must be verified against the Query when its tags are changed.
// if the Query doesn't match the changed resource anymore,
// then it needs to be removed both from Gallery.
// when Gallery is closed, all removed resources must be removed from Grid too

//todo: move handler to presenter maybe ?

//todo: inherit ReversibleItemGridPresenter to get "undo"
class ResourcesList(
    private val index: ResourcesIndex,
    private var resources: List<ResourceId>,
    private var handler: ItemClickHandler<ResourceId>)
    : ItemsClickablePresenter<ResourceId, FileItemView>(handler) {

    fun <T: Comparable<T>>sortedBy(selector: (Path) -> T) =
        resources.sortedBy { selector(index.getPath(it)!!) }

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