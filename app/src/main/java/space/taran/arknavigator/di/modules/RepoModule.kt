package space.taran.arknavigator.di.modules

import android.util.Log
import dagger.Module
import dagger.Provides
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.model.dao.Database
import space.taran.arknavigator.mvp.model.repo.FoldersRepo
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.utils.MAIN
import javax.inject.Singleton

@Module
class RepoModule {
    @Singleton
    @Provides
    fun foldersRepo(database: Database): FoldersRepo {
        Log.d(MAIN, "creating FoldersRepo")
        return FoldersRepo(database.folderDao())
    }

    @Singleton
    @Provides
    fun resourcesIndexesRepo(
        database: Database,
        foldersRepo: FoldersRepo
    ): ResourcesIndexRepo {
        Log.d(MAIN, "creating ResourcesIndexesRepo")
        return ResourcesIndexRepo(database.resourceDao(), foldersRepo)
    }

    @Singleton
    @Provides
    fun tagsStorageRepo(
        foldersRepo: FoldersRepo,
        resourcesIndexRepo: ResourcesIndexRepo,
        userPreferences: UserPreferences
    ): TagsStorageRepo {
        return TagsStorageRepo(foldersRepo, resourcesIndexRepo, userPreferences)
    }
}
