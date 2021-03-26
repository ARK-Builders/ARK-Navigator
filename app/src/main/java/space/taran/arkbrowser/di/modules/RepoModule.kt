package space.taran.arkbrowser.di.modules

import space.taran.arkbrowser.mvp.model.entity.room.db.Database
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.model.repo.SynchronizeRepo
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.file.DocumentProvider
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
    fun fileProvider3(app: App): FileProvider {
        return FileProvider(app)
    }

    @Singleton
    @Provides
    fun documentProvider(app: App, fileProvider: FileProvider): DocumentProvider {
        return DocumentProvider(app, fileProvider)
    }

    @Singleton
    @Provides
    fun filesRepo3(fileProvider: FileProvider, documentProvider: DocumentProvider): FilesRepo {
        return FilesRepo(fileProvider, documentProvider)
    }

    @Singleton
    @Provides
    fun syncManager(roomRepo: RoomRepo, filesRepo: FilesRepo) = SynchronizeRepo(roomRepo, filesRepo)
}