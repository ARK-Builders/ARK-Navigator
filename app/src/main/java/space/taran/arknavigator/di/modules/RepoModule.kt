package space.taran.arknavigator.di.modules

import android.util.Log
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.domain.Message
import space.taran.arklib.domain.index.ResourceIndexRepo
import space.taran.arklib.domain.meta.MetadataStorageRepo
import space.taran.arklib.domain.preview.PreviewStorageRepo
import space.taran.arklib.utils.Constants
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.scores.ScoreStorageRepo
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.utils.LogTags.MAIN
import javax.inject.Inject
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
    fun resourceIndexRepo(
        foldersRepo: FoldersRepo
    ): ResourceIndexRepo {
        Log.d(MAIN, "creating ResourceIndexRepo")
        return ResourceIndexRepo(foldersRepo)
    }

    @Singleton
    @Provides
    fun tagsStorageRepo(
        foldersRepo: FoldersRepo,
        resourceIndexRepo: ResourceIndexRepo,
        preferences: Preferences
    ): TagsStorageRepo {
        return TagsStorageRepo(foldersRepo, resourceIndexRepo, preferences)
    }

    @Singleton
    @Provides
    fun metadataStorageRepo(
        @Named(Constants.DI.APP_SCOPE_NAME)
        appScope: CoroutineScope
    ) = MetadataStorageRepo(appScope)

    @Inject
    lateinit var metadataStorageRepo: MetadataStorageRepo
    // todo: is it ok?

    @Singleton
    @Provides
    fun previewStorageRepo(
        @Named(Constants.DI.APP_SCOPE_NAME)
        appScope: CoroutineScope,
    ) = PreviewStorageRepo(appScope, metadataStorageRepo)

    @Singleton
    @Provides
    fun scoreStorageRepo(
        foldersRepo: FoldersRepo,
        indexRepo: ResourceIndexRepo
    ): ScoreStorageRepo {
        return ScoreStorageRepo(foldersRepo, indexRepo)
    }
}
