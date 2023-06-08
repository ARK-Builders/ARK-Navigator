package space.taran.arknavigator.mvp.model.repo.stats

import space.taran.arklib.domain.stats.StatsEvent
import space.taran.arknavigator.utils.Tag

interface StatsStorage {
    suspend fun init()
    fun handleEvent(event: StatsEvent)
    fun statsTagLabeledAmount(): Map<Tag, Int>
    fun statsTagQueriedAmount(): Map<Tag, Int>
    fun statsTagQueriedTS(): Map<Tag, Long>
    fun statsTagLabeledTS(): Map<Tag, Long>

    companion object {
        val TAGS_USAGE_EVENTS = listOf(
            StatsEvent.TagsChanged::class.java,
            StatsEvent.PlainTagUsed::class.java,
            StatsEvent.KindTagUsed::class.java
        )
    }
}
