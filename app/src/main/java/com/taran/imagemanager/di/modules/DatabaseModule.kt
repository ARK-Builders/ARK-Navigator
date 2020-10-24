package com.taran.imagemanager.di.modules

import android.os.Environment
import androidx.room.Room
import com.taran.imagemanager.mvp.model.entity.room.Database
import com.taran.imagemanager.ui.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun database(app: App): Database {
        return Room.databaseBuilder(app, Database::class.java, Database.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

}