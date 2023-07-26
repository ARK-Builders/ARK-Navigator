package dev.arkbuilders.navigator.stub

import space.taran.arklib.domain.stats.StatsEvent
import dev.arkbuilders.navigator.mvp.model.repo.stats.StatsStorage
import dev.arkbuilders.navigator.utils.Tag

class StatsStorageStub : StatsStorage {
    override suspend fun init() {}

    override fun handleEvent(event: StatsEvent) {}

    override fun statsTagLabeledAmount(): Map<Tag, Int> = emptyMap()
    override fun statsTagQueriedAmount(): Map<Tag, Int> = emptyMap()

    override fun statsTagQueriedTS(): Map<Tag, Long> = emptyMap()
    override fun statsTagLabeledTS(): Map<Tag, Long> = emptyMap()
}
