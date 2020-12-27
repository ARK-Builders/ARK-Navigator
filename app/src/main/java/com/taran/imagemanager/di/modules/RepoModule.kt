package space.taran.arkbrowser.di.modules

import space.taran.arkbrowser.mvp.model.entity.IndexingSubjects
import space.taran.arkbrowser.mvp.model.entity.room.Database
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.file.FileProvider
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
    fun filesRepo(fileProvider: FileProvider): FilesRepo {
        return FilesRepo(fileProvider)
    }

    @Singleton
    @Provides
    fun fileProvider(app: App): FileProvider {
        return FileProvider(app)
    }

    @Singleton
    @Provides
    fun indexingStorage(): IndexingSubjects {
        return IndexingSubjects()
    }
}