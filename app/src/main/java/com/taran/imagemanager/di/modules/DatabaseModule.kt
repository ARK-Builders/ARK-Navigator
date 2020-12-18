package com.taran.imagemanager.di.modules

import androidx.room.Room
import com.taran.imagemanager.mvp.model.entity.room.Database
import com.taran.imagemanager.ui.App
import com.taran.imagemanager.utils.DB_NAME
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
