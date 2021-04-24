package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.Favorite
import space.taran.arkbrowser.mvp.model.entity.room.dao.FavoriteDao

class FavoritesRepo(private val dao: FavoriteDao) {

    fun getFavorites(): List<Favorite> = dao.getAll()
        .map { Favorite.fromRoom(it) }


}