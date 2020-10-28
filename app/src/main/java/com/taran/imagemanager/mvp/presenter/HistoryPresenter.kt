package com.taran.imagemanager.mvp.presenter

import android.util.Log
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Icons
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IFileGridPresenter
import com.taran.imagemanager.mvp.view.item.FileItemView
import com.taran.imagemanager.mvp.view.HistoryView
import com.taran.imagemanager.navigation.Screens
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject

class HistoryPresenter: MvpPresenter<HistoryView>() {

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var router: Router

    val fileGridPresenter = FileGridPresenter()

    inner class FileGridPresenter :
        IFileGridPresenter {

        var folders = mutableListOf<Folder>()

        override fun getCount() = folders.size

        override fun bindView(view: FileItemView) {
            val folder = folders[view.pos]
            if (folder.id != -1L) {
                view.setText(folder.name)
                view.setIcon(Icons.FOLDER, null)
            } else {
                view.setText("Новая папка")
                view.setIcon(Icons.PLUS, null)
            }
        }

        override fun onCardClicked(pos: Int) {
            val folder = folders[pos]
            if (folder.id != -1L)
                router.navigateTo(Screens.ExplorerScreen(folder.path))
            else
                router.navigateTo(Screens.ExplorerScreen("/"))
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        viewState.init()

        roomRepo.getAllFolders().subscribe(
            {
                val folders = it.toMutableList()
                folders.add(Folder(-1L, "", ""))
                fileGridPresenter.folders = folders
            }, {
                fileGridPresenter.folders = mutableListOf(Folder(-1L, "", ""))
            }
        )
    }

}