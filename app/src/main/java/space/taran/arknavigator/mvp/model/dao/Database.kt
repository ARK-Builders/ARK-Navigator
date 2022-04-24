package space.taran.arknavigator.mvp.model.dao

import androidx.room.RoomDatabase

@androidx.room.Database(
    entities = [
        Root::class,
        Resource::class,
        ResourceExtra::class
    ],
    version = 17,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun rootDao(): RootDao
    abstract fun resourceDao(): ResourceDao

    companion object {
        const val DB_NAME = "ArkBrowser.db"
    }
}
