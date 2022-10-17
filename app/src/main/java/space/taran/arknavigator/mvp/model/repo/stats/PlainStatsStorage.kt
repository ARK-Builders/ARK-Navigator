package space.taran.arknavigator.mvp.model.repo.stats

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.arkFolder
import space.taran.arknavigator.mvp.model.arkStats
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.stats.category.StatsCategoryStorage
import space.taran.arknavigator.mvp.model.repo.stats.category.TagLabeledNStorage
import space.taran.arknavigator.mvp.model.repo.stats.category.TagUsedTSStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import timber.log.Timber
import java.nio.file.Path
import kotlin.io.path.createDirectories

class PlainStatsStorage(
    private val root: Path,
    private val index: ResourcesIndex,
    private val tagsStorage: TagsStorage,
    private val statsFlow: SharedFlow<StatsEvent>
) : StatsStorage {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val categoryStorages = mutableListOf<StatsCategoryStorage<Any>>()
    private val tagLabeledN =
        TagLabeledNStorage(index, tagsStorage, root, scope)
            .also { categoryStorages.add(it) }
    private val tagUsedTs =
        TagUsedTSStorage(root, scope).also { categoryStorages.add(it) }

    init {
        statsFlow.onEach(::handleEvent).launchIn(scope)
    }

    override suspend fun init() = withContext(Dispatchers.IO) {
        root.arkFolder().arkStats().createDirectories()
        categoryStorages.map { storage ->
            launch { storage.init() }
        }.joinAll()
    }

    override fun handleEvent(event: StatsEvent) {
        scope.launch {
            if (!event.belongToRoot()) return@launch
            Timber.i("Event [$event] received in root [$root]")
            categoryStorages
                .forEach { storage -> storage.handleEvent(event) }
        }
    }

    override fun statsTagLabeledAmount() = tagLabeledN.provideData()

    override fun statsTagUsedTS() = tagUsedTs.provideData()

    private suspend fun StatsEvent.belongToRoot(): Boolean {
        val ids = index.listAllIds()

        return when (this) {
            is StatsEvent.TagsChanged -> ids.contains(resource)
            is StatsEvent.PlainTagUsed -> tagsStorage.getTags(ids).contains(tag)
            is StatsEvent.KindTagUsed -> true
        }
    }
}
