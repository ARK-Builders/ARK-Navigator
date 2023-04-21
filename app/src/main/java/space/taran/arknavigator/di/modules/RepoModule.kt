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
        preferences: Preferences
    ): TagsStorageRepo {
        return TagsStorageRepo(preferences)
    }

    @Singleton
    @Provides
    fun metadataStorageRepo(
        @Named(Constants.DI.APP_SCOPE_NAME)
        appScope: CoroutineScope
    ) = MetadataStorageRepo(appScope)

    @Singleton
    @Provides
    fun previewStorageRepo(
        @Named(Constants.DI.APP_SCOPE_NAME)
        appScope: CoroutineScope,
        metadataStorageRepo: MetadataStorageRepo
    ) = PreviewStorageRepo(appScope, metadataStorageRepo)

    @Singleton
    @Provides
    fun scoreStorageRepo(): ScoreStorageRepo {
        return ScoreStorageRepo()
    }
}
