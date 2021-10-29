package space.taran.arknavigator.mvp.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        emitChangesToAffectedRootAndFav(root, storage, index)
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

    suspend fun setTags(rootAndFav: RootAndFav, id: ResourceId, tags: Tags) {
        if (rootAndFav.isAllRoots()) {
            aggregatedTagsStorage.setTags(id, tags)
            emitChangesToAllRootsFlow()
        } else {
            val root = rootAndFav.root!!
            val storage =  storageByRoot[root]!!
            storage.setTags(id, tags)
            emitChangesToAffectedRootAndFav(root, storage, indexCache.getIndexByRoot(root))
        }
    }

    private suspend fun emitChangesToAllRootsFlow() {
        aggregatedTagsStorage = AggregatedTagsStorage(storageByRoot.values)
        if (allRootsFlow.value != null)
            allRootsFlow.emit(aggregatedTagsStorage.getTags(indexCache.listIds(RootAndFav(null, null))))
    }

    private suspend fun emitChangesToAffectedRootAndFav(root: Path, storage: PlainTagsStorage, index: PlainResourcesIndex) {
        emitChangesToAllRootsFlow()

        val affectedRootAndFavs = flowByRootAndFav.keys.filter { it.root == root }
        affectedRootAndFavs.forEach {
            val tags = storage.getTags(index.listIds(it.fav))
            if (flowByRootAndFav[it]!!.value != tags)
                flowByRootAndFav[it]!!.emit(tags)
        }
    }
}