@file:OptIn(ExperimentalSerializationApi::class)

package space.taran.arknavigator.mvp.model.repo.stats.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.taran.arknavigator.mvp.model.repo.stats.StatsEvent
import space.taran.arknavigator.utils.Tag
import timber.log.Timber
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class TagQueriedNStorage(
    root: Path,
    scope: CoroutineScope
) : StatsCategoryStorage<Map<Tag, Int>>(root, scope) {
    override val fileName: String = "tag-queried-n"

    private val tagQueriedN = mutableMapOf<Tag, Int>()

    override suspend fun init() {
        val storage = locateStorage()?.also { if (it.notExists()) return }
        storage!!.inputStream().use {
            val json = Json.decodeFromStream<JsonTagQueriedN>(it)
            tagQueriedN.putAll(json.data)
            Timber.i("initialized with $tagQueriedN")
        }
    }

    override fun handleEvent(event: StatsEvent) {
        when (event) {
            is StatsEvent.PlainTagUsed ->
                tagQueriedN.merge(event.tag, 1, Int::plus)
            is StatsEvent.KindTagUsed ->
                tagQueriedN.merge(event.kind.name, 1, Int::plus)
            else -> return
        }
        requestFlush()
    }

    override fun provideData() = tagQueriedN

    override fun flush() {
        val data = Json.encodeToString(JsonTagQueriedN(tagQueriedN))
        locateStorage().writeText(data)
        Timber.i("flushed with $tagQueriedN")
    }
}

@Serializable
private class JsonTagQueriedN(val data: Map<Tag, Int>)
