package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.IFile
import com.taran.imagemanager.mvp.model.entity.Icons
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IFileGridPresenter
import com.taran.imagemanager.mvp.view.ExplorerView
import com.taran.imagemanager.mvp.view.item.FileItemView
import com.taran.imagemanager.navigation.Screens
import com.taran.imagemanager.utils.getHash
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import java.io.File
import javax.inject.Inject


class ExplorerPresenter(val currentFolder: String) : MvpPresenter<ExplorerView>() {

    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var router: Router

    val fileGridPresenter = FileGridPresenter()

    inner class FileGridPresenter :
        IFileGridPresenter {

        var files = mutableListOf<IFile>()

        override fun getCount() = files.size

        override fun bindView(view: FileItemView) {
            val file = files[view.pos]
            if (file is Folder) {
                view.setText(file.name)
                view.setIcon(Icons.FOLDER, null)
            }
            if (file is Image) {
                view.setText(file.name)
                view.setIcon(Icons.IMAGE, file.path)
            }
        }

        override fun onCardClicked(pos: Int) {
            val file = files[pos]
            if (file is Folder)
                router.navigateTo(Screens.ExplorerScreen(file.path))
            else
                router.navigateTo(Screens.DetailScreen(currentFolder, pos))
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()

        val files = filesRepo.getFilesInFolder(currentFolder)
        fileGridPresenter.files = files
        viewState.updateAdapter()
        calculateHash(files)
    }

    private fun calculateHash(files: MutableList<IFile>) {
        roomRepo.getFolderByPath(currentFolder).subscribe(
            { folder ->
                //The folder is in the database, we processed it
            },
            {
                //The folder is not in the database
                //consistent file processing
                processImages(files.filterIsInstance<Image>()).subscribe(
                    {
                        //At the end, write the folder to the database
                        val folderFile = File(currentFolder)
                        roomRepo.insertFolder(
                            Folder(
                                name = folderFile.name,
                                path = folderFile.absolutePath
                            )
                        ).subscribe()
                    },
                    {}
                )
            }
        )
    }

    private fun processImages(images: List<Image>) = Completable
        .create { emitter ->
            images.forEach {
                it.hash = getHash(it.path)
                roomRepo.insertImageNonRx(it)
            }
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())

}