package com.taran.imagemanager.di.modules

import com.taran.imagemanager.mvp.model.entity.room.Database
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepoModule {
    @Singleton
    @Provides
    fun roomRepo(database: Database): RoomRepo {
        return RoomRepo(database)
    }

    @Singleton
    @Provides
    fun filesRepo(): FilesRepo {
        return FilesRepo()
    }


}