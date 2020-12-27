package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.Folder
import space.taran.arkbrowser.mvp.model.entity.Image
import space.taran.arkbrowser.mvp.model.entity.room.CardUri
import space.taran.arkbrowser.mvp.model.entity.room.Database
import space.taran.arkbrowser.mvp.model.entity.room.RoomFolder
import space.taran.arkbrowser.mvp.model.entity.room.RoomImage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.RuntimeException

class RoomRepo(val database: Database) {

    fun insertFolder(folder: Folder) = Single.create<Long> {
        val roomFolder = RoomFolder(folder.id, folder.name, folder.path, folder.favorite, folder.tags, folder.lastModified!!)
        it.onSuccess(database.folderDao().insert(roomFolder))
    }.subscribeOn(Schedulers.io())

    fun insertImage(image: Image) = Single.create<Long> {
        val roomImage = RoomImage(image.id, image.name, image.path, image.tags, image.hash)
        it.onSuccess(database.imageDao().insert(roomImage))
    }.subscribeOn(Schedulers.io())

    fun insertCardUri(cardUri: CardUri) = Completable.create {
        database.cardUriDao().insert(cardUri)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun updateFavorite(id: Long, favorite: Boolean) = Completable.create {
        database.folderDao().updateFavorite(id, favorite)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun updateFolderTags(id: Long, tags: String) = Completable.create {
        database.folderDao().updateTags(id, tags)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun updateImageTags(id: Long, tags: String) = Completable.create {
        database.imageDao().updateTags(id, tags)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun getCardUris() = Single.create<List<CardUri>> {
        val cardUris = database.cardUriDao().getAll()
        it.onSuccess(cardUris)
    }.subscribeOn(Schedulers.io())

    fun getAllFolders() = Single.create<List<Folder>> {
        val folders = database.folderDao().getAll().map { roomFolder ->
            Folder(roomFolder.id, roomFolder.name, roomFolder.path, roomFolder.favorite, roomFolder.tags, lastModified = roomFolder.lastModified)
        }
        it.onSuccess(folders)
    }.subscribeOn(Schedulers.io())

    fun getFavoriteFolders() = Single.create<List<Folder>> {
        val folders = database.folderDao().getFavorite().map { roomFolder ->
            Folder(roomFolder.id, roomFolder.name, roomFolder.path, roomFolder.favorite, roomFolder.tags, lastModified = roomFolder.lastModified)
        }
        it.onSuccess(folders)
    }.subscribeOn(Schedulers.io())

    fun getFolderByPath(path: String) = Single.create<Folder> { emitter ->
        database.folderDao().findByPath(path)?.let {  roomFolder ->
            val folder = Folder(roomFolder.id, roomFolder.name, roomFolder.path, roomFolder.favorite, roomFolder.tags, lastModified = roomFolder.lastModified)
            emitter.onSuccess(folder) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

    fun getImageByPath(path: String) = Single.create<Image> { emitter ->
        database.imageDao().findByPath(path)?.let { roomImage ->
            val image = Image(roomImage.id, roomImage.name, roomImage.path, roomImage.tags, roomImage.hash)
            emitter.onSuccess(image) }
            ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

    fun getCardUriByPath(path: String) = Single.create<CardUri> { emitter ->
        database.cardUriDao().findByPath(path)?.let {
            emitter.onSuccess(it)
        } ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

}