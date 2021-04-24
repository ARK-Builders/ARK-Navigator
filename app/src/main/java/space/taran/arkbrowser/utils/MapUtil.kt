package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.Favorite
import space.taran.arkbrowser.mvp.model.entity.Resource
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.room.RoomFavorite
import space.taran.arkbrowser.mvp.model.entity.room.RoomRoot

fun mapFavoriteToRoom(favorite: Favorite) = RoomFavorite(
    favorite.id,
    favorite.name,
    favorite.file.path
)

fun mapRootToRoom(root: Root) = RoomRoot(
    root.id,
    root.folder.path
)