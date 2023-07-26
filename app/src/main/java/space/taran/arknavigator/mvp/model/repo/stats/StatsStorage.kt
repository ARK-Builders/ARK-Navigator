package dev.arkbuilders.navigator.mvp.model.repo.stats

import space.taran.arklib.ResourceId
import space.taran.arklib.domain.meta.Kind
import dev.arkbuilders.navigator.utils.Tag
import dev.arkbuilders.navigator.utils.Tags

sealed class StatsEvent {
    data class TagsChanged(
        val resource: ResourceId,
        val oldTags: Tags,
        val newTags: Tags
    ) : StatsEvent()

    data class PlainTagUsed(val tag: Tag) : StatsEvent()

    data class KindTagUsed(
        val kind: Kind
    ) : StatsEvent()
}

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
