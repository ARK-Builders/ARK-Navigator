package space.taran.arknavigator.mvp.model.repo.stats

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags

sealed class StatsEvent(val categories: List<StatsCategory>) {
    class TagsChanged(
        val resource: ResourceId,
        val oldTags: Tags,
        val newTags: Tags
    ) : StatsEvent(listOf(StatsCategory.TAG_LABELED_N))

    class TagUsed(val tag: Tag) : StatsEvent(listOf(StatsCategory.TAG_USED_TS))
}

enum class StatsCategory {
    TAG_LABELED_N, TAG_USED_TS
}

interface StatsStorage {
    suspend fun init()
    fun handleEvent(event: StatsEvent)
    fun statsTagLabeledAmount(): Map<Tag, Int>
    fun statsTagUsedTS(): Map<Tag, Long>
    fun statsTagUsedTSDescending() =
        statsTagUsedTS().toList().sortedByDescending { it.second }.map { it.first }
}
