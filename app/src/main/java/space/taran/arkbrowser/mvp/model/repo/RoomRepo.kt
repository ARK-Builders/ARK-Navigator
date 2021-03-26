package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.room.CardUri
import space.taran.arkbrowser.mvp.model.entity.room.db.Database
import space.taran.arkbrowser.utils.mapFileFromRoom
import space.taran.arkbrowser.utils.mapFileToRoom
import space.taran.arkbrowser.utils.mapRootFromRoom
import space.taran.arkbrowser.utils.mapRootToRoom
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class RoomRepo(val database: Database) {

    fun insertFile(file: File) = Single.create<Long> {
        val roomFile = mapFileToRoom(file)
        it.onSuccess(database.fileDao().insert(roomFile))
    }.subscribeOn(Schedulers.io())

    fun insertCardUri(cardUri: CardUri) = Completable.create {
        database.cardUriDao().insert(cardUri)
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    fun insertRoot(root: Root) = Single.create<Long> { emitter ->
        val id = database.rootDao().insert(mapRootToRoom(root))
        emitter.onSuccess(id)
    }.subscribeOn(Schedulers.io())


    fun getCardUris() = Single.create<List<CardUri>> {
        val cardUris = database.cardUriDao().getAll()
        it.onSuccess(cardUris)
    }.subscribeOn(Schedulers.io())

    fun getCardUriByPath(path: String) = Single.create<CardUri> { emitter ->
        database.cardUriDao().findByPath(path)?.let {
            emitter.onSuccess(it)
        } ?: emitter.onError(RuntimeException())
    }.subscribeOn(Schedulers.io())

    fun getFavFiles() = Single.create<List<File>> { emitter ->
        val files = database.fileDao().getAllFav().map { roomFile -> mapFileFromRoom(roomFile) }
        emitter.onSuccess(files)
    }.subscribeOn(Schedulers.io())

    fun getAllRoots() = Single.create<List<Root>> { emitter ->
        val roots = database.rootDao().getAll().map { roomRoot ->
            mapRootFromRoom(roomRoot)
        }
        emitter.onSuccess(roots)
    }.subscribeOn(Schedulers.io())

}