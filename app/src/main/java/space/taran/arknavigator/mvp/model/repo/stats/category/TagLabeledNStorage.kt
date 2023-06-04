package space.taran.arknavigator.mvp.model.repo.stats.category

import kotlinx.serialization.Serializable
import space.taran.arknavigator.utils.Tag

//class TagLabeledNStorage(
//    val index: ResourceIndex,
//    val tagsStorage: TagsStorage,
//    root: Path,
//    scope: CoroutineScope
//) : StatsCategoryStorage<Map<Tag, Int>>(root, scope) {
//    override val fileName = "tag-labeled-n"
//    private val tagLabeledAmount = mutableMapOf<Tag, Int>()
//
//    override suspend fun init() {
//        val storage = locateStorage()
//        if (storage.exists()) {
//            val json = Json.decodeFromStream<JsonTagLabeledN>(storage.inputStream())
//            tagLabeledAmount.putAll(json.data)
//        } else {
//            index.allIds()
//                .associateWith { tagsStorage.getTags(it) }
//                .forEach { (_, tags) ->
//                    tags.forEach { tag ->
//                        tagLabeledAmount.merge(tag, 1, Int::plus)
//                    }
//                }
//            requestFlush()
//        }
//        Timber.i("initialized with $tagLabeledAmount")
//    }
//
//    override fun handleEvent(event: StatsEvent) {
//        when (event) {
//            is StatsEvent.TagsChanged -> with(event) {
//                val same = oldTags.intersect(newTags)
//                val removed = oldTags.minus(same)
//                val new = newTags.minus(same)
//                new.forEach {
//                    tagLabeledAmount.merge(it, 1, Int::plus)
//                }
//                removed.forEach {
//                    tagLabeledAmount.merge(it, 1, Int::minus)
//                }
//            }
//            else -> return
//        }
//        requestFlush()
//    }
//
//    override fun provideData() = tagLabeledAmount
//
//    override fun flush() {
//        val data = Json.encodeToString(JsonTagLabeledN(tagLabeledAmount))
//        locateStorage().writeText(data)
//        Timber.i("flushed with $tagLabeledAmount")
//    }
//}

@Serializable
private class JsonTagLabeledN(val data: Map<Tag, Int>)
