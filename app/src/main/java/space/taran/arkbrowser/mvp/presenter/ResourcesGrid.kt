package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.mvp.model.repo.ResourcesIndex
import space.taran.arkbrowser.mvp.presenter.adapter.ItemGridPresenter
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.utils.*
import java.lang.AssertionError
import java.nio.file.Files
import java.nio.file.Path

//todo: inherit ReversibleItemGridPresenter to get "undo"
class ResourcesGrid(
    private val index: ResourcesIndex,
    private var resources: List<ResourceId>)
    : ItemGridPresenter<Unit, ResourceId>({
        Log.d(RESOURCES_SCREEN, "[mock] item $it clicked in ResourcesPresenter/ItemGridPresenter")

    //todo: one handler for single-click (preview & tags)
    // and one handler for long-press (execute)

    //todo: single-click handler replaces grid with "gallery" style adapter (it shouldn't be separate screen)

//        val path = index.getPath(it)!!
//        if (isPreviewable(path)) {
//            val previews = resources.filter {
//                isPreviewable(index.getPath(it)!!)
//            }
//
//            val newPos = images.indexOf(resource)
//            router.navigateTo(
//                Screens.DetailScreen(
//                    syncRepo.getRootById(resource.rootId!!)!!,
//                    images,
//                    newPos
//                )
//            )
//        } else {
//            viewState.openFile(
//                filesRepo.fileDataSource.getUriForFileByProvider(resource.file),
//                DocumentFile.fromFile(resource.file).type!!
//            )
//        }
    }) {

    fun <T: Comparable<T>>sortedBy(selector: (Path) -> T) =
        resources.sortedBy { selector(index.getPath(it)!!) }

    override fun label() = Unit

    override fun items() = resources

    override fun updateItems(label: Unit, items: List<ResourceId>) {
        resources = items
    }

    override fun bindView(view: FileItemView) {
        val resource = resources[view.position()]
        Log.d(RESOURCES_SCREEN, "binding view with $resource in ResourcesGridPresenter")

        val path = index.getPath(resource)
            ?: throw AssertionError("Resource to display must be indexed")

        view.setText(path.fileName.toString())

        if (Files.isDirectory(path)) {
            throw AssertionError("Resource can't be a directory")
        }

        view.setIcon(Icon.provide(path))
    }

    override fun backClicked(): Unit? = null
}