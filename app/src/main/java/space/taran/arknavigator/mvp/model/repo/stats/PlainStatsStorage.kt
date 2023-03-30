package space.taran.arknavigator.mvp.model.repo.stats

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arklib.arkFolder
import space.taran.arklib.arkStats
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.model.repo.stats.category.StatsCategoryStorage
import space.taran.arknavigator.mvp.model.repo.stats.category.TagLabeledNStorage
import space.taran.arknavigator.mvp.model.repo.stats.category.TagLabeledTSStorage
import space.taran.arknavigator.mvp.model.repo.stats.category.TagQueriedNStorage
import space.taran.arknavigator.mvp.model.repo.stats.category.TagQueriedTSStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import timber.log.Timber
import java.nio.file.Path
import kotlin.io.path.createDirectories

class PlainStatsStorage(
    private val root: Path,
    private val index: ResourceIndex,
    private val tagsStorage: TagsStorage,
    private val preferences: Preferences,
    private val statsFlow: SharedFlow<StatsEvent>
) : StatsStorage {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val categoryStorages = mutableListOf<StatsCategoryStorage<Any>>()
    private val tagLabeledTS =
        TagLabeledTSStorage(root, scope).also { categoryStorages.add(it) }
    private val tagLabeledN =
        TagLabeledNStorage(index, tagsStorage, root, scope)
            .also { categoryStorages.add(it) }
    private val tagQueriedTS =
        TagQueriedTSStorage(root, scope).also { categoryStorages.add(it) }
    private val tagQueriedN =
        TagQueriedNStorage(root, scope).also { categoryStorages.add(it) }
    private var collectingTagEvents = true

    init {
        statsFlow.onEach(::handleEvent).launchIn(scope)
        scope.launch {
            collectingTagEvents = preferences.get(PreferenceKey.CollectTagUsageStats)
            preferences.flow(PreferenceKey.CollectTagUsageStats).onEach { new ->
                collectingTagEvents = new
            }.launchIn(scope)
        }
    }

    override suspend fun init() = withContext(Dispatchers.IO) {
        root.arkFolder().arkStats().createDirectories()
        categoryStorages.map { storage ->
            launch { storage.init() }
        }.joinAll()
    }

    override fun handleEvent(event: StatsEvent) {
        scope.launch {
            if (!collectingTagEvents &&
                StatsStorage.TAGS_USAGE_EVENTS.contains(event.javaClass)
            ) {
                return@launch
            }
            if (!event.belongToRoot()) return@launch
            Timber.i("Event [$event] received in root [$root]")
            categoryStorages
                .forEach { storage -> storage.handleEvent(event) }
        }
    }

    override fun statsTagLabeledTS() = tagLabeledTS.provideData()

    override fun statsTagLabeledAmount() = tagLabeledN.provideData()

    override fun statsTagQueriedTS() = tagQueriedTS.provideData()

    override fun statsTagQueriedAmount() = tagQueriedN.provideData()

    private suspend fun StatsEvent.belongToRoot(): Boolean {
        val ids = index.allIds()

        return when (this) {
            is StatsEvent.TagsChanged -> ids.contains(resource)
            is StatsEvent.PlainTagUsed -> tagsStorage.getTags(ids).contains(tag)
            is StatsEvent.KindTagUsed -> true
        }
    }
}
