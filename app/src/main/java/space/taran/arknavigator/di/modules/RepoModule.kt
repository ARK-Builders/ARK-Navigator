package space.taran.arknavigator.di.modules

import android.util.Log
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.index.ResourcesIndexRepo
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arklib.utils.Constants
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.scores.ScoreStorageRepo
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.utils.LogTags.MAIN
import javax.inject.Named
import javax.inject.Singleton

@Module
class RepoModule {

    @Singleton
    @Provides
    @Named(Constants.DI.MESSAGE_FLOW_NAME)
    fun mutableMessageFlow(): MutableSharedFlow<Message> = MutableSharedFlow()

    @Singleton
    @Provides
    @Named(Constants.DI.APP_SCOPE_NAME)
    fun appScope() = CoroutineScope(Dispatchers.IO)

    @Singleton
    @Provides
    fun resourcesIndexesRepo(
        foldersRepo: FoldersRepo,
        metadataStorageRepo: MetadataStorageRepo,
        @Named(Constants.DI.MESSAGE_FLOW_NAME)
        messageFlow: MutableSharedFlow<Message>,
    ): ResourcesIndexRepo {
        Log.d(MAIN, "creating ResourcesIndexesRepo")
        return ResourcesIndexRepo(
            foldersRepo,
            metadataStorageRepo,
            messageFlow
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
        foldersRepo: FoldersRepo,
        indexRepo: ResourcesIndexRepo,
        @Named(Constants.DI.APP_SCOPE_NAME)
        appScope: CoroutineScope
    ) = PreviewStorageRepo(
        foldersRepo,
        indexRepo,
        appScope
    )

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
