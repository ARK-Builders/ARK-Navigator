package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.RoomDatabase

@androidx.room.Database(
    entities = [
        Folder::class,
        Resource::class
    ],
    version = 12,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun resourceDao(): ResourceDao

    companion object {
        const val DB_NAME = "ArkBrowser.db"
    }
}
