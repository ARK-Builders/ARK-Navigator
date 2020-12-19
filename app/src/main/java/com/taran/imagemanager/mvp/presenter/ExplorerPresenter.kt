package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.IFile
import com.taran.imagemanager.mvp.model.entity.Icons
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.entity.room.CardUri
import com.taran.imagemanager.mvp.model.entity.room.RoomImage
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IFileGridPresenter
import com.taran.imagemanager.mvp.view.ExplorerView
import com.taran.imagemanager.mvp.view.item.FileItemView
import com.taran.imagemanager.navigation.Screens
import com.taran.imagemanager.utils.TEXT_STORAGE_NAME
import com.taran.imagemanager.utils.getHash
import com.taran.imagemanager.utils.isInternalStorage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject


class ExplorerPresenter(var currentFolder: Folder) : MvpPresenter<ExplorerView>() {

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
            if (file is Folder) {
                router.navigateTo(Screens.ExplorerScreen(file))
            } else {
                val images = files.filterIsInstance<Image>().toMutableList()
                val imagePos = images.indexOf(file as Image)
                router.navigateTo(Screens.DetailScreen(images, imagePos, currentFolder))
            }
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()


        if (currentFolder.path != "gallery") {
            val files = filesRepo.getFilesInFolder(currentFolder.path)
            files.sortBy { it.name }
            fileGridPresenter.files = files
            viewState.updateAdapter()
            filesRepo.mkFile("${currentFolder.path}/$TEXT_STORAGE_NAME")
                .observeOn(AndroidSchedulers.mainThread()).subscribe(
                    { exist ->
                        if (exist)
                            checkCurrentFolder()
                        else
                            requestSdCardUri()
                    }, {}
                )
        } else {
            viewState.setFabVisibility(false)
            val images = filesRepo.getImagesFromGallery()
            fileGridPresenter.files = images.toMutableList()
            viewState.updateAdapter()
        }
    }

    fun dismissDialog() {
        viewState.closeDialog()
    }

    fun fabClicked() {
        viewState.showDialog()
    }

    fun favoriteChanged() {
        currentFolder.favorite = true
        roomRepo.updateFavorite(currentFolder.id, true).subscribe()
    }

    private fun checkCurrentFolder() {
        roomRepo.getFolderByPath(currentFolder.path).subscribe(
            {
                currentFolder = it
            },
            {
                processFolder()
            }
        )
    }

    private fun processFolder() {
        roomRepo.insertFolder(currentFolder).subscribe(
            {
                currentFolder.id = it
                calculateHash()
            },
            {}
        )
    }

    private fun requestSdCardUri() {
        roomRepo.getCardUriByPath(currentFolder.path).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                it.uri = null
                roomRepo.insertCardUri(it).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ viewState.requestSdCardUri() }, {})
            }, {
                roomRepo.insertCardUri(CardUri(path = currentFolder.path))
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
                        { viewState.requestSdCardUri() }, {})
            })
    }


    private fun calculateHash() {
        val images = fileGridPresenter.files.filterIsInstance<Image>()
        processImages(images).subscribe(
            { processedImages ->
                currentFolder.processed = true
                roomRepo.updateFolderProcessed(currentFolder.id, currentFolder.processed)
                    .subscribe()
                filesRepo.writeToFile(
                    "${currentFolder.path}/$TEXT_STORAGE_NAME",
                    processedImages,
                    false
                ).subscribe()
            },
            {}
        )
    }

    private fun processImages(images: List<Image>) = Single.create<List<Image>> { emitter ->
        images.forEach { image ->
            val roomImage = roomRepo.database.imageDao().findByPath(image.path)
            if (roomImage == null) {
                image.hash = getHash(image.path)
                image.id = roomRepo.database.imageDao()
                    .insert(RoomImage(image.id, image.name, image.path, image.tags, image.hash))
            } else {
                image.hash = roomImage.hash
            }
        }
        emitter.onSuccess(images)
    }.subscribeOn(Schedulers.io())
}