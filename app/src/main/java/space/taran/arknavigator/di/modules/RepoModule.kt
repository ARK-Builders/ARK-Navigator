package space.taran.arknavigator.di.modules

import android.util.Log
import dagger.Module
import dagger.Provides
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.domain.index.ResourcesIndexRepo
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arknavigator.mvp.model.repo.Database
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.scores.ScoreStorageRepo
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.utils.LogTags.MAIN
import javax.inject.Singleton

@Module
class RepoModule {
    @Singleton
    @Provides
    fun resourcesIndexesRepo(
        database: Database,
        foldersRepo: FoldersRepo,
        previewStorageRepo: PreviewStorageRepo,
        metadataStorageRepo: MetadataStorageRepo
    ): ResourcesIndexRepo {
        Log.d(MAIN, "creating ResourcesIndexesRepo")
        return ResourcesIndexRepo(
            database.resourceDao(),
            foldersRepo,
            previewStorageRepo,
            metadataStorageRepo
        )
    }

    @Singleton
    @Provides
    fun tagsStorageRepo(
        foldersRepo: FoldersRepo,
        resourcesIndexRepo: ResourcesIndexRepo,
        preferences: Preferences
    ): TagsStorageRepo {
        return TagsStorageRepo(foldersRepo, resourcesIndexRepo, preferences)
    }

    @Singleton
    @Provides
    fun previewStorageRepo(
        foldersRepo: FoldersRepo
    ) = PreviewStorageRepo(foldersRepo)

    @Singleton
    @Provides
    fun metadataStorageRepo(
        foldersRepo: FoldersRepo
    ) = MetadataStorageRepo(foldersRepo)

    @Singleton
    @Provides
    fun scoreStorageRepo(
        foldersRepo: FoldersRepo,
        indexRepo: ResourcesIndexRepo
    ): ScoreStorageRepo {
        return ScoreStorageRepo(foldersRepo, indexRepo)
    }
}
