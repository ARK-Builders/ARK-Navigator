package space.taran.arknavigator.mvp.model.repo.stats.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.taran.arknavigator.mvp.model.repo.stats.StatsCategory
import space.taran.arknavigator.mvp.model.repo.stats.StatsEvent
import space.taran.arknavigator.utils.Tag
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class TagUsedTSStorage(
    root: Path,
    scope: CoroutineScope
) : StatsCategoryStorage<Map<Tag, Long>>(root, scope) {
    override val fileName = Path("tag-used-ts")
    override val category = StatsCategory.TAG_USED_TS
    private val tagUsedTS = mutableMapOf<Tag, Long>()

    override suspend fun init() {
        val storage = locateStorage()
        if (storage.notExists()) return
        val json = Json.decodeFromStream<JsonTagUsedTS>(storage.inputStream())
        tagUsedTS.putAll(json.data)
    }

    override fun handleEvent(event: StatsEvent) {
        when (event) {
            is StatsEvent.PlainTagUsed ->
                tagUsedTS[event.tag] = System.currentTimeMillis()
            is StatsEvent.KindTagUsed ->
                tagUsedTS[event.kindCode.name] = System.currentTimeMillis()
            else -> {}
        }
        requestFlush()
    }

    override fun provideData() = tagUsedTS

    override fun flush() {
        val data = Json.encodeToString(JsonTagUsedTS(tagUsedTS))
        locateStorage().writeText(data)
    }
}

@Serializable
private class JsonTagUsedTS(val data: Map<Tag, Long>)
