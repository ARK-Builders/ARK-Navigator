package com.taran.imagemanager.mvp.model.repo

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.entity.room.Database
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.RuntimeException

class RoomRepo(val database: Database) {

    fun insertFolder(folder: Folder) = Completable.create {
        database.folderDao().insert(folder)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun insertImage(image: Image) = Completable.create {
        database.imageDao().insert(image)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun getAllFolders() = Single.create<List<Folder>> {
        val folders = database.folderDao().getAll()
        it.onSuccess(folders)
    }.subscribeOn(Schedulers.io())

    fun getAllFavoriteFolders() = Single.create<List<Folder>> {
        val folders = database.folderDao().getAllFavorite()
        it.onSuccess(folders)
    }.subscribeOn(Schedulers.io())

    fun getFolderByPath(path: String) = Single.create<Folder> { emitter ->
        database.folderDao().findByPath(path)?.let { emitter.onSuccess(it) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

    fun getImageByPath(path: String) = Single.create<Image> { emitter ->
        database.imageDao().findByPath(path)?.let { emitter.onSuccess(it) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

    fun insertImageNonRx(image: Image) {
        database.imageDao().insert(image)
    }

    fun getImageById(id: Long) = Single.create<Image> { emitter ->
        database.imageDao().findById(id)?.let { emitter.onSuccess(it) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

}