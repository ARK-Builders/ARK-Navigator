package space.taran.arknavigator.mvp.model.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import space.taran.arknavigator.ui.App

@androidx.room.Database(
    entities = [
        Resource::class,
        ResourceExtra::class
    ],
    version = 18,
    exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun resourceDao(): ResourceDao

    companion object {
        private const val DB_NAME = "ArkBrowser.db"

        fun build(app: App) =
            Room.databaseBuilder(app, Database::class.java, DB_NAME)
                .addMigrations(RoomMigration.MIGRATION_17_18)
                .fallbackToDestructiveMigration()
                .build()
    }
}

private object RoomMigration {
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE Root")
        }
    }
}
