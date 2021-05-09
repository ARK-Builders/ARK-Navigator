package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.view.ExplorerView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import space.taran.arkbrowser.utils.*
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.model.repo.FoldersRepo
import space.taran.arkbrowser.mvp.presenter.adapter.ItemGridPresenter
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

class ExplorerPresenter(var currentFolder: Path? = null) : MvpPresenter<ExplorerView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    var fileGridPresenter: XItemGridPresenter? = null
    var currentRoot: Path? = null

    inner class XItemGridPresenter(var files: List<MarkableFile>) :
        ItemGridPresenter<Unit, MarkableFile>({
            val (_, file) = it
            if (Files.isDirectory(file)) {
                router.navigateTo(Screens.ExplorerScreen(file))
            }
        }) {

        init {
            this.files = files.sortedWith(markableFileComparator)
        }

        override fun label() = Unit

        override fun items() = files //todo

        override fun updateItems(label: Unit, items: List<MarkableFile>) {
            this.files = items.sortedWith(markableFileComparator)
        }

        override fun bindView(view: FileItemView) {
            val (favorite, path) = files[view.pos]
            view.setText(path.fileName.toString())
            if (Files.isDirectory(path)) {
                view.setIcon(IconOrImage(icon = Icon.FOLDER))
            } else {
                //todo: improve image type recognition and remove copy-paste
                if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                    view.setIcon(IconOrImage(image = path))
                } else {
                    view.setIcon(IconOrImage(icon = Icon.FILE))
                }
            }
            view.setFav(favorite)
        }

        override fun backClicked(): Unit {
            TODO("Not yet implemented")
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
    }

    fun onViewResumed() {
        init()
    }

    private fun init() {
        if (currentFolder == null) {
            initHomeFiles()
        } else {
            initExplorerFiles()
        }
    }

    private fun initHomeFiles() {
        Log.d("flow", "[mock] initializing home files in ExplorerPresenter")
        viewState.setFavoriteFabVisibility(false)
        viewState.setTagsFabVisibility(false)
//        val favorites = favoritesRepo.getAll()
//        val extFolders = resourcesRepo.fileDataSource.getExtSdCards()
//
//        fileGridPresenter = ItemGridPresenter(
//            extFolders.map {_ -> false}.zip(extFolders) +
//                favorites.map {_ -> true}.zip(favorites.map { it.file }))
//
//        viewState.updateAdapter()
//        viewState.setTitle("/")
    }

    private fun initExplorerFiles() {
        Log.d("flow", "[mock] initializing explorer files in ExplorerPresenter")
//        val extSdCard = resourcesRepo.fileDataSource.getExtSdCards().find { extSd ->
//            extSd.path == currentFolder!!.path
//        }
//        extSdCard?.let {
//            viewState.setFavoriteFabVisibility(false)
//            viewState.setTagsFabVisibility(false)
//        } ?: let {
//            viewState.setFavoriteFabVisibility(true)
//            currentRoot = rootsRepo.getRootByFile(currentFolder!!)
//            currentRoot?.let {
//                viewState.setTagsFabVisibility(true)
//            } ?: viewState.setTagsFabVisibility(false)
//        }
//
//        viewState.setTitle(currentFolder!!.path)
//
//        val favorites = roomRepo.getFavorites()
//            .map { it.file }
//            .toSet()
//
//        val filesHere = listChildren(currentFolder!!)
//            .map { Pair(favorites.contains(it), it) }
//            .sortedWith(markableFileComparator)
//
//        fileGridPresenter = ItemGridPresenter(filesHere)
//        viewState.updateAdapter()
    }

    fun dismissDialog() {
        Log.d("flow", "dialog dismissed in ExplorerPresenter")
        viewState.closeDialog()
    }

    fun favFabClicked() {
        Log.d("flow", "fab clicked in ExplorerPresenter")
        viewState.showDialog()
    }

    fun tagsFabClicked() {
        Log.d("flow", "[mock] tags fab clicked in ExplorerPresenter")
//        currentRoot?.let {
//            router.newRootScreen(
//                Screens.TagsScreen(
//                    resourcesRepo.retrieveResources(currentFolder!!)))
//        }
    }

    fun favoriteChanged() {
        Log.d("flow", "favorite changed in ExplorerPresenter")
//        val folder = currentFolder!!
//        val favorite = remove_Favorite(name = folder.name, file = folder)
//
//        roomRepo.insertFavorite(favorite)
    }

    fun backClicked(): Boolean {
        Log.d("flow", "back clicked in ExplorerPresenter")
        router.exit()
        return true
    }
}