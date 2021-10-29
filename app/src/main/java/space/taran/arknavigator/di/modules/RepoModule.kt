package space.taran.arknavigator.di.modules

import android.util.Log
import space.taran.arknavigator.mvp.model.dao.Database
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import space.taran.arknavigator.mvp.model.IndexCache
import space.taran.arknavigator.mvp.model.IndexingEngine
import space.taran.arknavigator.mvp.model.TagsCache
import space.taran.arknavigator.mvp.model.fsmonitoring.FSMonitoring
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

    @Singleton
    @Provides
    fun indexCache(): IndexCache {
        return IndexCache()
    }

    @Singleton
    @Provides
    fun tagsCache(indexCache: IndexCache): TagsCache {
        return TagsCache(indexCache)
    }

    @Singleton
    @Provides
    fun appCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Singleton
    @Provides
    fun fsMonitoring(indexCache: IndexCache, tagsCache: TagsCache, appScope: CoroutineScope): FSMonitoring {
        return FSMonitoring(indexCache, tagsCache, appScope)
    }

    @Singleton
    @Provides
    fun indexingEngine(
        foldersRepo: FoldersRepo,
        indexCache: IndexCache,
        tagsCache: TagsCache,
        resourcesIndexFactory: ResourcesIndexFactory,
        fsMonitoring: FSMonitoring
    ): IndexingEngine {
        return IndexingEngine(indexCache, tagsCache, foldersRepo, resourcesIndexFactory, fsMonitoring)
    }
}