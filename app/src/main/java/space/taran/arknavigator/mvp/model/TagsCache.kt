package space.taran.arknavigator.mvp.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.AggregatedTagsStorage
import space.taran.arknavigator.mvp.model.repo.PlainResourcesIndex
import space.taran.arknavigator.mvp.model.repo.PlainTagsStorage
import space.taran.arknavigator.utils.Tags
import java.nio.file.Path

class TagsCache(val indexCache: IndexCache) {
    private var storageByRoot = mutableMapOf<Path, PlainTagsStorage>()
    private var aggregatedTagsStorage = AggregatedTagsStorage(listOf())
    private var flowByRootAndFav = mutableMapOf<RootAndFav, MutableStateFlow<Tags?>>()
    private var allRootsFlow = MutableStateFlow<Tags?>(null)

    suspend fun onIndexChanged(root: Path, index: PlainResourcesIndex) {
        val storage = PlainTagsStorage.provide(root, index)
        storageByRoot[root] = storage
        val allIds = index.listAllIds()
        storage.cleanup(allIds)
        aggregatedTagsStorage = AggregatedTagsStorage(storageByRoot.values)
        val affectedRootAndFavs = flowByRootAndFav.keys.filter { it.root == root }
        affectedRootAndFavs.forEach {
            flowByRootAndFav[it]!!.emit(storage.getTags(index.listIds(it.fav)))
        }
    }

    suspend fun onReindexFinish() {
        allRootsFlow.emit(aggregatedTagsStorage.getTags(indexCache.listIds(RootAndFav(null, null))))
    }

    fun listenTagsChanges(rootAndFav: RootAndFav): StateFlow<Tags?> {
        return if (rootAndFav.isAllRoots()) {
            allRootsFlow
        } else {
            if (flowByRootAndFav[rootAndFav] == null) {
                storageByRoot[rootAndFav.root]?.let { storage ->
                    val ids = indexCache.listIds(rootAndFav)
                    flowByRootAndFav[rootAndFav] =
                        MutableStateFlow(storage.getTags(ids))
                } ?: run {
                    flowByRootAndFav[rootAndFav] = MutableStateFlow(null)
                }
            }

            flowByRootAndFav[rootAndFav]!!
        }
    }

    fun listUntagged(rootAndFav: RootAndFav): Set<ResourceId>? {
        val underPrefix = indexCache.listIds(rootAndFav)
        return if (rootAndFav.isAllRoots())
            aggregatedTagsStorage.listUntaggedResources().intersect(underPrefix)
        else
            storageByRoot[rootAndFav.root]?.listUntaggedResources()?.intersect(underPrefix)
    }

    fun groupTagsByResources(ids: Iterable<ResourceId>): Map<ResourceId, Tags> =
        ids.map {it to getTags(it)}.toMap()

    fun getTags(id: ResourceId): Tags {
        return aggregatedTagsStorage.getTags(id)
    }

    fun getTags(ids: Iterable<ResourceId>): Tags {
        return  aggregatedTagsStorage.getTags(ids)
    }

    fun getTags(rootAndFav: RootAndFav): Tags {
        return if (rootAndFav.isAllRoots()) {
            aggregatedTagsStorage.getTags(indexCache.listIds(rootAndFav))
        } else {
            storageByRoot[rootAndFav.root]?.getTags(indexCache.listIds(rootAndFav)) ?: emptySet()
        }
    }

    suspend fun remove(resourceId: ResourceId) {
        aggregatedTagsStorage.remove(resourceId)
    }

    suspend fun setTags(id: ResourceId, tags: Tags) {
        aggregatedTagsStorage.setTags(id, tags)
    }
}