package space.taran.arkbrowser.di.modules

import androidx.room.Room
import space.taran.arkbrowser.mvp.model.entity.room.Database
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.utils.DB_NAME
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
