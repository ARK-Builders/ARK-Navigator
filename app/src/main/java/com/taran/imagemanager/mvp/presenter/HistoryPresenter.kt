package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Icons
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IFileGridPresenter
import com.taran.imagemanager.mvp.view.HistoryView
import com.taran.imagemanager.mvp.view.item.FileItemView
import com.taran.imagemanager.navigation.Screens
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
            if (folder.id != -1L) {
                view.setIcon(Icons.FOLDER, null)
            } else {
                view.setIcon(Icons.PLUS, null)
            }
        }

        override fun onCardClicked(pos: Int) {
            val folder = folders[pos]
            if (folder.id == -2L)
                router.navigateTo(Screens.ExplorerScreen("gallery"))
            if (folder.id == -1L)
                router.navigateTo(Screens.ExplorerScreen(fileRepo.getExternalStorage()))
            if (folder.id != -2L && folder.id != -1L)
                router.navigateTo(Screens.ExplorerScreen(folder.path))
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
        roomRepo.getAllFavoriteFolders().observeOn(AndroidSchedulers.mainThread()).subscribe(
            {
                val folders = it.toMutableList()
                folders.add(Folder(-2L, "Gallery", ""))
                folders.add(Folder(-1L, "New Folder", ""))
                fileGridPresenter.folders = folders
                viewState.updateAdapter()
            }, {}
        )
    }
}