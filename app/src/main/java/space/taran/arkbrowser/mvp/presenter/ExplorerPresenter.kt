package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.*
import space.taran.arkbrowser.mvp.model.entity.common.Icons
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.model.repo.SynchronizeRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IFileGridPresenter
import space.taran.arkbrowser.mvp.view.ExplorerView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import space.taran.arkbrowser.utils.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject


class ExplorerPresenter(var currentFolder: File? = null) : MvpPresenter<ExplorerView>() {

    @Inject
    lateinit var syncRepo: SynchronizeRepo

    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var router: Router

    val fileGridPresenter = FileGridPresenter()

    inner class FileGridPresenter :
        IFileGridPresenter {

        var files = mutableListOf<File>()

        override fun getCount() = files.size

        override fun bindView(view: FileItemView) {
            val file = files[view.pos]
            view.setText(file.name)
            if (file.isFolder)
                view.setIcon(Icons.FOLDER, null)
            else {
                if (file.isImage())
                    view.setIcon(Icons.IMAGE, file.path)
                else
                    view.setIcon(Icons.FILE, null)
            }
            view.setFav(file.fav)
        }

        override fun onCardClicked(pos: Int) {
            val file = files[pos]
            if (file.isFolder) {
                router.navigateTo(Screens.ExplorerScreen(file))
            }
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
    }

    fun onViewResumed() {
        loadFiles()
    }

    private fun loadFiles() {
        if (currentFolder == null) {
            viewState.setFabsVisibility(false)
            roomRepo.getFavFiles().observeOn(AndroidSchedulers.mainThread()).subscribe(
                { favFiles ->
                    val extFolders = filesRepo.fileProvider.getExtSdCards()
                    fileGridPresenter.files.clear()
                    fileGridPresenter.files.addAll(extFolders)
                    fileGridPresenter.files.addAll(favFiles)
                    fileGridPresenter.files =
                        fileGridPresenter.files.sortedWith(filesComparator()).toMutableList()
                    viewState.updateAdapter()
                    viewState.setTitle("/")
                },
                {
                    it.printStackTrace()
                }
            )
        } else {
            val ext = filesRepo.fileProvider.getExtSdCards().find { extSd ->
                extSd.path == currentFolder!!.path
            }
            ext?.let {
                viewState.setFabsVisibility(false)
            } ?: viewState.setFabsVisibility(true)

            viewState.setTitle(currentFolder!!.path)

            roomRepo.getFavFiles().observeOn(AndroidSchedulers.mainThread()).subscribe(
                { roomFiles ->
                    val files = filesRepo.fileProvider.list(currentFolder!!.path)
                    roomFiles.forEach { roomFile ->
                        files.forEach { file ->
                            if (roomFile.path == file.path)
                                file.fav = roomFile.fav
                        }
                    }
                    fileGridPresenter.files.clear()
                    fileGridPresenter.files.addAll(files.sortedWith(filesComparator()))
                    viewState.updateAdapter()
                },
                {
                    it.printStackTrace()
                }
            )
        }
    }

    fun dismissDialog() {
        viewState.closeDialog()
    }

    fun favFabClicked() {
        viewState.showDialog()
    }

    fun tagsFabClicked() {
        val root = syncRepo.getRootByPath(currentFolder!!.path)
        root?.let {
            viewState.setSelectedTab(1)
            router.newRootScreen(
                Screens.TagsScreen(
                    it,
                    fileGridPresenter.files,
                    TagsPresenter.State.FILES
                )
            )
        } ?: viewState.showToast("Root not found")

    }

    fun favoriteChanged() {
        currentFolder!!.fav = true
        roomRepo.insertFile(currentFolder!!).subscribe()
    }

    fun backClicked(): Boolean {
        router.exit()
        return true
    }


}