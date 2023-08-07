package dev.arkbuilders.navigator.data.stats.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.taran.arklib.domain.stats.StatsEvent
import space.taran.arklib.domain.tags.Tag
import timber.log.Timber
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class TagQueriedTSStorage(
    root: Path,
    scope: CoroutineScope
) : StatsCategoryStorage<Map<Tag, Long>>(root, scope) {
    override val fileName = "tag-queried-ts"
    private val tagQueriedTS = mutableMapOf<Tag, Long>()

    override suspend fun init() {
        val storage = locateStorage()
        if (storage.notExists()) return
        val json = Json.decodeFromStream<JsonTagQueriedTS>(storage.inputStream())
        tagQueriedTS.putAll(json.data)
        Timber.i("initialized with $tagQueriedTS")
    }

    override fun handleEvent(event: StatsEvent) {
        when (event) {
            is StatsEvent.PlainTagUsed ->
                tagQueriedTS[event.tag] = System.currentTimeMillis()
            is StatsEvent.KindTagUsed ->
                tagQueriedTS[event.kind.name] = System.currentTimeMillis()
            else -> return
        }
        requestFlush()
    }

    override fun provideData() = tagQueriedTS

    override fun flush() {
        val data = Json.encodeToString(JsonTagQueriedTS(tagQueriedTS))
        locateStorage().writeText(data)
        Timber.i("flushed with $tagQueriedTS")
    }
}

@Serializable
private class JsonTagQueriedTS(val data: Map<Tag, Long>)
