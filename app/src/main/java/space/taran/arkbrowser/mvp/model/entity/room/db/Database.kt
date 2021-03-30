package space.taran.arkbrowser.mvp.model.entity.room.db

import androidx.room.RoomDatabase
import space.taran.arkbrowser.mvp.model.entity.room.SDCardUri
import space.taran.arkbrowser.mvp.model.entity.room.RoomFile
import space.taran.arkbrowser.mvp.model.entity.room.RoomRoot
import space.taran.arkbrowser.mvp.model.entity.room.dao.*

@androidx.room.Database(
    entities = [
        SDCardUri::class,
        RoomRoot::class,
        RoomFile::class
    ],
    version = 12,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun sdCardUriDao(): SDCardUriDao
    abstract fun rootDao(): RootDao
    abstract fun fileDao(): FileDao

    companion object {
        const val DB_NAME = "ArkBrowser.db"
    }
}
