package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.Folder
import space.taran.arkbrowser.mvp.model.entity.Icons
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IFileGridPresenter
import space.taran.arkbrowser.mvp.view.HistoryView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject

class HistoryPresenter: MvpPresenter<HistoryView>() {

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var fileRepo: FilesRepo

    val fileGridPresenter = FileGridPresenter()

    inner class FileGridPresenter :
        IFileGridPresenter {

        var folders = mutableListOf<Folder>()

        override fun getCount() = folders.size

        override fun bindView(view: FileItemView) {
            val folder = folders[view.pos]
            view.setText(folder.name)
            view.setIcon(Icons.FOLDER, null)
        }

        override fun onCardClicked(pos: Int) {
            val folder = folders[pos]
            router.navigateTo(Screens.ExplorerScreen(folder))
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
    }

    fun onViewResumed() {
        setupAdapterList()
    }

    private fun setupAdapterList() {
        roomRepo.getFavoriteFolders().observeOn(AndroidSchedulers.mainThread()).subscribe(
            { list ->
                val folders = list.toMutableList()
                folders.sortBy { it.name }
                addDefaultFolders(folders)
                fileGridPresenter.folders = folders
                viewState.updateAdapter()
            }, {}
        )
    }

    private fun addDefaultFolders(folders: MutableList<Folder>) {
        folders.addAll(fileRepo.getExtSdCards())
        folders.add(Folder( name = "Gallery", path =  "gallery"))
    }
}