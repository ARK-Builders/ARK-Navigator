package space.taran.arknavigator.stub

import space.taran.arknavigator.mvp.model.repo.stats.StatsEvent
import space.taran.arknavigator.mvp.model.repo.stats.StatsStorage
import space.taran.arknavigator.utils.Tag

class StatsStorageStub : StatsStorage {
    override suspend fun init() {}

    override fun handleEvent(event: StatsEvent) {}

    override fun statsTagLabeledAmount(): Map<Tag, Int> = emptyMap()

    override fun statsTagUsedTS(): Map<Tag, Long> = emptyMap()
}
