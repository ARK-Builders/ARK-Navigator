package space.taran.arkbrowser.di.modules

import androidx.room.Room
import space.taran.arkbrowser.mvp.model.entity.room.db.Database
import space.taran.arkbrowser.mvp.model.entity.room.db.Database.Companion.DB_NAME
import space.taran.arkbrowser.ui.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun database(app: App): Database {
        return Room.databaseBuilder(app, Database::class.java, DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

}
