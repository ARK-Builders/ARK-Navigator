package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.room.SDCardUri
import space.taran.arkbrowser.mvp.model.entity.room.db.Database
import space.taran.arkbrowser.mvp.model.entity.Favorite
import space.taran.arkbrowser.utils.*

class RoomRepo(val database: Database) {
    //todo return async stuff in case performance drops

    fun insertResource(resource: Resource) {
        val roomResource = mapResourceToRoom(resource)
        database.resourceDao().insert(roomResource)
    }

    fun insertFavorite(favorite: Favorite) {
        val roomFavorite = mapFavoriteToRoom(favorite)
        database.favoriteDao().insert(roomFavorite)
    }

    fun insertRoot(root: Root): Long =
        database.rootDao().insert(mapRootToRoom(root))

    fun insertSdCardUri(sdCardUri: SDCardUri) {
        database.sdCardUriDao().insert(sdCardUri)
    }

    fun getSdCardUris(): List<SDCardUri> =
        database.sdCardUriDao().getAll()

    fun getSdCardUriByPath(path: String): SDCardUri? =
        database.sdCardUriDao().findByPath(path)

    fun getFavorites(): List<Favorite> =
        database.favoriteDao().getAll().map { mapFavoriteFromRoom(it) }

    fun getAllRoots(): List<Root> =
        database.rootDao().getAll().map { mapRootFromRoom(it) }
}
