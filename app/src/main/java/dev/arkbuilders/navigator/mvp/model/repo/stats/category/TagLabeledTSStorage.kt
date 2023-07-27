package dev.arkbuilders.navigator.mvp.model.repo.stats.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.taran.arklib.domain.stats.StatsEvent
import dev.arkbuilders.navigator.utils.Tag
import timber.log.Timber
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class TagLabeledTSStorage(
    root: Path,
    scope: CoroutineScope
) : StatsCategoryStorage<Map<Tag, Long>>(root, scope) {
    override val fileName: String = "tag-labeled-ts"
    private val tagLabeledTS = mutableMapOf<Tag, Long>()

    override suspend fun init() {
        val storage = locateStorage()?.also { if (it.notExists()) return }
        storage!!.inputStream().use {
            val json = Json.decodeFromStream<JsonTagLabeledTS>(it)
            tagLabeledTS.putAll(json.data)
            Timber.i("initialized with $tagLabeledTS")
        }
    }

    override fun handleEvent(event: StatsEvent) {
        when (event) {
            is StatsEvent.TagsChanged -> with(event) {
                val same = oldTags.intersect(newTags)
                val new = newTags.minus(same)
                new.forEach {
                    tagLabeledTS[it] = System.currentTimeMillis()
                }
            }
            else -> return
        }
        requestFlush()
    }

    override fun provideData() = tagLabeledTS

    override fun flush() {
        val data = Json.encodeToString(JsonTagLabeledTS(tagLabeledTS))
        locateStorage().writeText(data)
        Timber.i("flushed with $tagLabeledTS")
    }
}

@Serializable
private class JsonTagLabeledTS(val data: Map<Tag, Long>)
