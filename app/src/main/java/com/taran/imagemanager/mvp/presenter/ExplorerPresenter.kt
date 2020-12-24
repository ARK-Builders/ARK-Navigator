package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.entity.*
import com.taran.imagemanager.mvp.model.entity.room.CardUri
import com.taran.imagemanager.mvp.model.entity.room.RoomFolder
import com.taran.imagemanager.mvp.model.entity.room.RoomImage
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IFileGridPresenter
import com.taran.imagemanager.mvp.view.ExplorerView
import com.taran.imagemanager.mvp.view.item.FileItemView
import com.taran.imagemanager.navigation.Screens
import com.taran.imagemanager.utils.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject


class ExplorerPresenter(var currentFolder: Folder) : MvpPresenter<ExplorerView>() {
    @Inject
    lateinit var indexingSubjects: IndexingSubjects

    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var router: Router

    val fileGridPresenter = FileGridPresenter()
    var images = listOf<Image>()

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
            viewState.setTitle(currentFolder.path, true)
            if (filesRepo.fileProvider.isBaseFolder(currentFolder.path))
                viewState.setFabVisibility(false)
            images = files.filterIsInstance<Image>().toMutableList()
            viewState.updateAdapter()
            processFolder()
        } else {
            viewState.setFabVisibility(false)
            viewState.setTitle(currentFolder.path, false)
            images = filesRepo.getImagesFromGallery()
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

    private fun processFolder() {
        filesRepo.mkFile(currentFolder.storagePath)
            .observeOn(AndroidSchedulers.mainThread()).subscribe(
                { exist ->
                    if (exist)
                        checkFolder()
                    else
                        requestSdCardUri()
                }, {}
            )
    }

    private fun checkFolder() {
        roomRepo.getFolderByPath(currentFolder.path).subscribe(
            {
                currentFolder = it
                if (currentFolder.lastModified == filesRepo.fileProvider.getLastModified(currentFolder.storagePath))
                    loadTags().subscribe()
                else
                    synchronizeWithFile().subscribe()
            },
            {
                synchronizeWithFile().subscribe()
            }
        )
    }

    private fun loadTags() = Single.create<Boolean> { emitter ->
        val indexingSubject = ReplaySubject.create<Image>()
        indexingSubjects.map[currentFolder.path] = indexingSubject
        images.forEach { image ->
            val roomImage = roomRepo.database.imageDao().findByPath(image.path)
            if (roomImage == null) {
                image.hash = getHash(image.path)
            } else {
                image.id = roomImage.id
                image.hash = roomImage.hash
                image.tags = roomImage.tags
            }
            image.synchronized = true
        }

        currentFolder.synchronized = true
        indexingSubject.onComplete()
        emitter.onSuccess(true)
    }.subscribeOn(Schedulers.io())

    private fun synchronizeWithFile() = Single.create<Boolean> { emitter ->
        val indexingSubject = ReplaySubject.create<Image>()
        indexingSubjects.map[currentFolder.path] = indexingSubject
        val fileMap = filesRepo.readFromFile(currentFolder.storagePath)
        currentFolder.tags = ""
        images.forEach { image ->
            val roomImage = roomRepo.database.imageDao().findByPath(image.path)
            if (roomImage == null) {
                image.hash = getHash(image.path)
            } else {
                image.id = roomImage.id
                image.hash = roomImage.hash
            }

            if (!fileMap[image.hash].isNullOrEmpty()) {
                val newTag = image.tags.findNewTags(fileMap[image.hash]!!)
                image.tags = image.tags.addTag(newTag)
                currentFolder.tags = currentFolder.tags.addTag(newTag)
            }

            image.synchronized = true
            roomRepo.database.imageDao()
                .insert(RoomImage(image.id, image.name, image.path, image.tags, image.hash))
        }

        filesRepo.writeToFile(currentFolder.storagePath, images)
        currentFolder.lastModified =
            filesRepo.fileProvider.getLastModified(currentFolder.storagePath)
        roomRepo.database.folderDao().insert(
            RoomFolder(
                currentFolder.id,
                currentFolder.name,
                currentFolder.path,
                currentFolder.favorite,
                currentFolder.tags,
                currentFolder.lastModified!!
            )
        )
        currentFolder.synchronized = true
        indexingSubject.onComplete()
        emitter.onSuccess(true)
    }.subscribeOn(Schedulers.io())

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
}