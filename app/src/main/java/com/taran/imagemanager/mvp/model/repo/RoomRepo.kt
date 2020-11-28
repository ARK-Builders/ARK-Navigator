package com.taran.imagemanager.mvp.model.repo

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.entity.room.Database
import com.taran.imagemanager.mvp.model.entity.room.RoomFolder
import com.taran.imagemanager.mvp.model.entity.room.RoomImage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.RuntimeException

class RoomRepo(val database: Database) {

    fun insertFolder(folder: Folder) = Completable.create {
        val roomFolder = RoomFolder(folder.id, folder.name, folder.path, folder.favorite, folder.processed, folder.tags)
        database.folderDao().insert(roomFolder)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun insertImage(image: Image) = Completable.create {
        insertImageNonRx(image)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun getAllFolders() = Single.create<List<Folder>> {
        val folders = database.folderDao().getAll().map { roomFolder ->
            Folder(roomFolder.id, roomFolder.name, roomFolder.path, roomFolder.favorite, roomFolder.processed, roomFolder.tags)
        }
        it.onSuccess(folders)
    }.subscribeOn(Schedulers.io())

    fun getAllFavoriteFolders() = Single.create<List<Folder>> {
        val folders = database.folderDao().getAllFavorite().map { roomFolder ->
            Folder(roomFolder.id, roomFolder.name, roomFolder.path, roomFolder.favorite, roomFolder.processed, roomFolder.tags)
        }
        it.onSuccess(folders)
    }.subscribeOn(Schedulers.io())

    fun getFolderByPath(path: String) = Single.create<Folder> { emitter ->
        database.folderDao().findByPath(path)?.let {  roomFolder ->
            val folder = Folder(roomFolder.id, roomFolder.name, roomFolder.path, roomFolder.favorite, roomFolder.processed, roomFolder.tags)
            emitter.onSuccess(folder) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

    fun getImageByPath(path: String) = Single.create<Image> { emitter ->
        database.imageDao().findByPath(path)?.let { roomImage ->
            val image = Image(roomImage.id, roomImage.name, roomImage.path, roomImage.tags, roomImage.hash)
            emitter.onSuccess(image) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

    fun insertImageNonRx(image: Image) {
        val roomImage = RoomImage(image.id, image.name, image.path, image.tags, image.hash)
        database.imageDao().insert(roomImage)
    }

    fun getImageById(id: Long) = Single.create<Image> { emitter ->
        database.imageDao().findById(id)?.let { roomImage ->
            val image = Image(roomImage.id, roomImage.name, roomImage.path, roomImage.tags, roomImage.hash)
            emitter.onSuccess(image) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

}