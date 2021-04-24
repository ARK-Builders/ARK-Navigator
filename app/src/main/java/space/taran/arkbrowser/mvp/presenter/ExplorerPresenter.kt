package space.taran.arkbrowser.mvp.presenter

import androidx.core.net.toUri
import space.taran.arkbrowser.mvp.model.entity.*
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.repo.ResourcesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.model.repo.RootsRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IItemGridPresenter
import space.taran.arkbrowser.mvp.view.ExplorerView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import space.taran.arkbrowser.utils.*
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.mvp.model.repo.FavoritesRepo
import java.io.File
import javax.inject.Inject

class ExplorerPresenter(var currentFolder: File? = null) : MvpPresenter<ExplorerView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var rootsRepo: RootsRepo

    @Inject
    lateinit var resourcesRepo: ResourcesRepo

    @Inject
    lateinit var favoritesRepo: FavoritesRepo

    var fileGridPresenter: ItemGridPresenter? = null
    var currentRoot: Root? = null

    inner class ItemGridPresenter(var files: List<MarkableFile>) :
        IItemGridPresenter {

        init {
            this.files = files.sortedWith(markableFileComparator)
        }

        override fun getCount() = files.size

        override fun bindView(view: FileItemView) {
            val (favorite, file) = files[view.pos]
            view.setText(file.name)
            if (file.isDirectory) {
                view.setIcon(IconOrImage(icon = Icon.FOLDER))
            } else {
                //todo: improve image type recognition and remove copy-paste
                if (file.path.endsWith(".png") || file.path.endsWith(".jpg") || file.path.endsWith(".jpeg")) {
                    view.setIcon(IconOrImage(image = file))
                } else {
                    view.setIcon(IconOrImage(icon = Icon.FILE))
                }
            }
            view.setFav(favorite)
        }

        override fun itemClicked(pos: Int) {
            val (_, file) = files[pos]
            if (file.isDirectory) {
                router.navigateTo(Screens.ExplorerScreen(file.toUri()))
            }
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
        viewState.setFavoriteFabVisibility(false)
        viewState.setTagsFabVisibility(false)
        val favorites = favoritesRepo.getAll()
        val extFolders = resourcesRepo.fileDataSource.getExtSdCards()

        fileGridPresenter = ItemGridPresenter(
            extFolders.map {_ -> false}.zip(extFolders) +
                favorites.map {_ -> true}.zip(favorites.map { it.file }))

        viewState.updateAdapter()
        viewState.setTitle("/")
    }

    private fun initExplorerFiles() {
        val extSdCard = resourcesRepo.fileDataSource.getExtSdCards().find { extSd ->
            extSd.path == currentFolder!!.path
        }
        extSdCard?.let {
            viewState.setFavoriteFabVisibility(false)
            viewState.setTagsFabVisibility(false)
        } ?: let {
            viewState.setFavoriteFabVisibility(true)
            currentRoot = rootsRepo.getRootByFile(currentFolder!!)
            currentRoot?.let {
                viewState.setTagsFabVisibility(true)
            } ?: viewState.setTagsFabVisibility(false)
        }

        viewState.setTitle(currentFolder!!.path)

        val favorites = roomRepo.getFavorites()
            .map { it.file }
            .toSet()

        val filesHere = listChildren(currentFolder!!)
            .map { Pair(favorites.contains(it), it) }
            .sortedWith(markableFileComparator)

        fileGridPresenter = ItemGridPresenter(filesHere)
        viewState.updateAdapter()
    }

    fun dismissDialog() {
        viewState.closeDialog()
    }

    fun favFabClicked() {
        viewState.showDialog()
    }

    fun tagsFabClicked() {
        currentRoot?.let {
            router.newRootScreen(
                Screens.TagsScreen(
                    currentFolder!!.name,
                    resourcesRepo.retrieveResources(currentFolder!!)))
        }
    }

    fun favoriteChanged() {
        val folder = currentFolder!!
        val favorite = Favorite(name = folder.name, file = folder)

        roomRepo.insertFavorite(favorite)
    }

    fun backClicked(): Boolean {
        router.exit()
        return true
    }
}