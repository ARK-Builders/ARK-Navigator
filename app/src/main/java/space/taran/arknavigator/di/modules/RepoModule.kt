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
import space.taran.arklib.domain.meta.MetadataProcessorRepo
import space.taran.arklib.domain.preview.PreviewProcessorRepo
import space.taran.arklib.domain.score.ScoreStorageRepo
import space.taran.arklib.domain.tags.TagsStorageRepo
import space.taran.arknavigator.utils.LogTags.MAIN
import javax.inject.Named
import javax.inject.Singleton

@Module
class RepoModule {

    @Singleton
    @Provides
    @Named(MESSAGE_FLOW_NAME)
    fun mutableMessageFlow(): MutableSharedFlow<Message> = MutableSharedFlow()

    @Singleton
    @Provides
    @Named(APP_SCOPE_NAME)
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
        @Named(APP_SCOPE_NAME)
        appScope: CoroutineScope,
    ): TagsStorageRepo {
        return TagsStorageRepo(appScope)
    }

    @Singleton
    @Provides
    fun metadataStorageRepo(
        @Named(APP_SCOPE_NAME)
        appScope: CoroutineScope
    ) = MetadataProcessorRepo(appScope)

    @Singleton
    @Provides
    fun previewStorageRepo(
        @Named(APP_SCOPE_NAME)
        appScope: CoroutineScope,
        metadataStorageRepo: MetadataProcessorRepo
    ) = PreviewProcessorRepo(appScope, metadataStorageRepo)

    @Singleton
    @Provides
    fun scoreStorageRepo(
        @Named(APP_SCOPE_NAME)
        appScope: CoroutineScope,
    ) = ScoreStorageRepo(appScope)

    companion object {
        const val MESSAGE_FLOW_NAME = "messageFlow"

        const val APP_SCOPE_NAME = "appScope"
    }
}
