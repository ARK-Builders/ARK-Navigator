package space.taran.arkbrowser.mvp.model.entity.room.db

import androidx.room.RoomDatabase
import space.taran.arkbrowser.mvp.model.entity.room.SDCardUri
import space.taran.arkbrowser.mvp.model.entity.room.RoomRoot
import space.taran.arkbrowser.mvp.model.entity.room.dao.*

@androidx.room.Database(
    entities = [
        SDCardUri::class,
        FavoriteDao::class,
        RoomRoot::class
    ],
    version = 12,
    exportSchema = false
)

abstract class Database : RoomDatabase() {
    abstract fun sdCardUriDao(): SDCardUriDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun rootDao(): RootDao

    companion object {
        const val DB_NAME = "ArkBrowser.db"
    }
}
