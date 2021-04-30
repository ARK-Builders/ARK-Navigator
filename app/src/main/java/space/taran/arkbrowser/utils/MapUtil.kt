package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.remove_Favorite
import space.taran.arkbrowser.mvp.model.entity.Resource
import space.taran.arkbrowser.mvp.model.entity.remove_Root
import space.taran.arkbrowser.mvp.model.entity.room.Favorite
import space.taran.arkbrowser.mvp.model.entity.room.Root

fun mapFavoriteToRoom(favorite: remove_Favorite) = Favorite(
    favorite.id,
    favorite.name,
    favorite.file.path
)

fun mapRootToRoom(root: remove_Root) = Root(
    root.id,
    root.folder.path
)