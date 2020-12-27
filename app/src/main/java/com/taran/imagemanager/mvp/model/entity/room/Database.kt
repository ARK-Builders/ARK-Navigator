package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.RoomDatabase
import space.taran.arkbrowser.mvp.model.entity.room.dao.CardUriDao
import space.taran.arkbrowser.mvp.model.entity.room.dao.FolderDao
import space.taran.arkbrowser.mvp.model.entity.room.dao.ImageDao

@androidx.room.Database(
    entities = [
        RoomFolder::class,
        RoomImage::class,
        CardUri::class
    ],
    version = 9,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun imageDao(): ImageDao
    abstract fun cardUriDao(): CardUriDao
}