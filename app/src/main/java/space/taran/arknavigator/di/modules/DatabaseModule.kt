package space.taran.arknavigator.di.modules

import android.util.Log
import androidx.room.Room
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.mvp.model.dao.Database
import space.taran.arknavigator.mvp.model.dao.Database.Companion.DB_NAME
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.MODULES
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun database(app: App): Database {
        Log.d(MODULES, "creating Database")
        return Room.databaseBuilder(app, Database::class.java, DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
}
