package space.taran.arknavigator.di.modules

import android.util.Log
import space.taran.arknavigator.mvp.model.dao.Database
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.ResourcesIndexFactory
import javax.inject.Singleton

@Module
class RepoModule {
    @Singleton
    @Provides
    fun foldersRepo(database: Database): FoldersRepo {
        Log.d("modules", "creating FoldersRepo")
        return FoldersRepo(database.folderDao())
    }

    @Singleton
    @Provides
    fun resourcesIndexesRepo(database: Database): ResourcesIndexFactory {
        Log.d("modules", "creating ResourcesIndexesRepo")
        return ResourcesIndexFactory(database.resourceDao())
    }
}