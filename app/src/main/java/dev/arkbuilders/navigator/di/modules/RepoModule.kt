package dev.arkbuilders.navigator.di.modules

import android.util.Log
import dagger.Module
import dagger.Provides
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arklib.data.Message
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.arklib.data.meta.MetadataProcessorRepo
import dev.arkbuilders.arklib.data.preview.PreviewProcessorRepo
import dev.arkbuilders.arklib.data.stats.StatsEvent
import dev.arkbuilders.arklib.user.score.ScoreStorageRepo
import dev.arkbuilders.arklib.user.tags.TagsStorageRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.stats.StatsStorageRepo
import dev.arkbuilders.navigator.data.utils.LogTags.MAIN
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
    @Named(STATS_FLOW_NAME)
    fun statsFlow(): MutableSharedFlow<StatsEvent> = MutableSharedFlow()

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
        @Named(STATS_FLOW_NAME)
        statsFlow: MutableSharedFlow<StatsEvent>,
    ): TagsStorageRepo {
        return TagsStorageRepo(appScope, statsFlow)
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

    @Singleton
    @Provides
    fun statsStorageRepo(
        tagsStorageRepo: TagsStorageRepo,
        prefs: Preferences,
        @Named(STATS_FLOW_NAME)
        statsFlow: MutableSharedFlow<StatsEvent>
    ) = StatsStorageRepo(tagsStorageRepo, prefs, statsFlow)

    companion object {
        const val MESSAGE_FLOW_NAME = "messageFlow"
        const val STATS_FLOW_NAME = "statsFLow"

        const val APP_SCOPE_NAME = "appScope"
    }
}
