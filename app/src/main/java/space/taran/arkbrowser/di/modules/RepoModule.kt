package space.taran.arkbrowser.di.modules

import space.taran.arkbrowser.mvp.model.entity.room.Database
import space.taran.arkbrowser.mvp.model.repo.ResourcesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.model.repo.RootsRepo
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.file.DocumentDataSource
import space.taran.arkbrowser.ui.file.FileDataSource
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
    fun fileProvider3(app: App): FileDataSource {
        return FileDataSource(app)
    }

    @Singleton
    @Provides
    fun documentProvider(app: App, fileDataSource: FileDataSource): DocumentDataSource {
        return DocumentDataSource(app, fileDataSource)
    }

    @Singleton
    @Provides
    fun filesRepo3(fileDataSource: FileDataSource, documentDataSource: DocumentDataSource): ResourcesRepo {
        return ResourcesRepo(fileDataSource, documentDataSource)
    }

    @Singleton
    @Provides
    fun syncManager(roomRepo: RoomRepo, resourcesRepo: ResourcesRepo) = RootsRepo(roomRepo, resourcesRepo)
}