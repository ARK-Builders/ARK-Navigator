package space.taran.arknavigator.mvp.model.repo.stats.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.stats.StatsCategory
import space.taran.arknavigator.mvp.model.repo.stats.StatsEvent
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.utils.Tag
import timber.log.Timber
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

class TagLabeledNStorage(
    val index: ResourcesIndex,
    val tagsStorage: TagsStorage,
    root: Path,
    scope: CoroutineScope
) : StatsCategoryStorage<Map<Tag, Int>>(root, scope) {
    override val fileName = Path("tag-labeled-n")
    override val category = StatsCategory.TAG_LABELED_N
    private val tagLabeledAmount = mutableMapOf<Tag, Int>()

    override suspend fun init() {
        val storage = locateStorage()
        if (storage.exists()) {
            val json = Json.decodeFromStream<JsonTagLabeledN>(storage.inputStream())
            tagLabeledAmount.putAll(json.data)
        } else {
            index.listAllIds()
                .associateWith { tagsStorage.getTags(it) }
                .forEach { (id, tags) ->
                    tags.forEach { tag ->
                        tagLabeledAmount.merge(tag, 1, Int::plus)
                    }
                }
            requestFlush()
        }
        Timber.i("initialized with $tagLabeledAmount")
    }

    override fun handleEvent(event: StatsEvent) {
        when (event) {
            is StatsEvent.TagsChanged -> with(event) {
                val same = oldTags.intersect(newTags)
                val removed = oldTags.minus(same)
                val new = newTags.minus(same)
                new.forEach {
                    tagLabeledAmount.merge(it, 1, Int::plus)
                }
                removed.forEach {
                    tagLabeledAmount.merge(it, 1, Int::minus)
                }
            }
            else -> return
        }
        requestFlush()
    }

    override fun provideData() = tagLabeledAmount

    override fun flush() {
        val data = Json.encodeToString(JsonTagLabeledN(tagLabeledAmount))
        locateStorage().writeText(data)
        Timber.i("flushed with $tagLabeledAmount")
    }
}

@Serializable
private class JsonTagLabeledN(val data: Map<Tag, Int>)
