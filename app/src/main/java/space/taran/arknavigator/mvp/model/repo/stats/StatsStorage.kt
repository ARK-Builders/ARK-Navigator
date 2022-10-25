package space.taran.arknavigator.mvp.model.repo.stats

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.kind.KindCode
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags

sealed class StatsEvent {
    data class TagsChanged(
        val resource: ResourceId,
        val oldTags: Tags,
        val newTags: Tags
    ) : StatsEvent()

    data class PlainTagUsed(val tag: Tag) : StatsEvent()

    data class KindTagUsed(
        val kindCode: KindCode
    ) : StatsEvent()
}

enum class StatsCategory {
    TAG_LABELED_N, TAG_USED_TS
}

interface StatsStorage {
    suspend fun init()
    fun handleEvent(event: StatsEvent)
    fun statsTagLabeledAmount(): Map<Tag, Int>
    fun statsTagUsedTS(): Map<Tag, Long>
    fun statsTagUsedTSList() =
        statsTagUsedTS().toList().sortedBy { it.second }.map { it.first }

    companion object {
        val TAGS_USAGE_EVENTS = listOf<Class<out StatsEvent>>(
            StatsEvent.TagsChanged::class.java,
            StatsEvent.PlainTagUsed::class.java,
            StatsEvent.KindTagUsed::class.java
        )
    }
}
