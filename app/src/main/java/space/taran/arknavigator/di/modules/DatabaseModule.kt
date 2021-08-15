package space.taran.arknavigator.di.modules

import android.util.Log
import androidx.room.Room
import space.taran.arknavigator.mvp.model.dao.Database
import space.taran.arknavigator.mvp.model.dao.Database.Companion.DB_NAME
import space.taran.arknavigator.ui.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun database(app: App): Database {
        Log.d("modules", "creating Database")
        return Room.databaseBuilder(app, Database::class.java, DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
}
