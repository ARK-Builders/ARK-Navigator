package space.taran.arknavigator.mvp.model.dao

import androidx.room.RoomDatabase

@androidx.room.Database(
    entities = [
        Root::class,
        Favorite::class,
        Resource::class,
        ResourceExtra::class
    ],
    version = 16,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun resourceDao(): ResourceDao

    companion object {
        const val DB_NAME = "ArkBrowser.db"
    }
}
