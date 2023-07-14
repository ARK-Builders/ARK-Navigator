package space.taran.arknavigator.stub

import space.taran.arklib.domain.stats.StatsEvent
import space.taran.arknavigator.mvp.model.repo.stats.StatsStorage
import space.taran.arknavigator.utils.Tag

class StatsStorageStub : StatsStorage {
    override suspend fun init() {}

    override fun handleEvent(event: StatsEvent) {}

    override fun statsTagLabeledAmount(): Map<Tag, Int> = emptyMap()
    override fun statsTagQueriedAmount(): Map<Tag, Int> = emptyMap()

    override fun statsTagQueriedTS(): Map<Tag, Long> = emptyMap()
    override fun statsTagLabeledTS(): Map<Tag, Long> = emptyMap()
}
