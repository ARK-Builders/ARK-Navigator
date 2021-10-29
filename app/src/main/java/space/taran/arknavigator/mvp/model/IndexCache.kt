package space.taran.arknavigator.mvp.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.AggregatedResourcesIndex
import space.taran.arknavigator.mvp.model.repo.Difference
import space.taran.arknavigator.mvp.model.repo.PlainResourcesIndex
import java.nio.file.Path

class IndexCache {
    private var indexByRoot = mutableMapOf<Path, PlainResourcesIndex>()
    private var aggregatedIndex = AggregatedResourcesIndex(listOf())
    private var flowByRootAndFav = mutableMapOf<RootAndFav, MutableStateFlow<Set<ResourceId>?>>()
    private var allRootsFlow = MutableStateFlow<Set<ResourceId>?>(null)

    suspend fun onIndexChange(root: Path, index: PlainResourcesIndex) {
        indexByRoot[root] = index
        emitChangesToAffectedRootAndFav(root, index)
    }

    suspend fun onResourceCreated(root: Path, resourcePath: Path) {
        val index = indexByRoot[root]!!
        index.reindexRoot(Difference(emptyList(), emptyList(), listOf(resourcePath)))
        emitChangesToAffectedRootAndFav(root, index)
    }

    suspend fun onResourceDeleted(root: Path, resourcePath: Path): ResourceId  {
        val index = indexByRoot[root]!!
        val id = index.metaByPath[resourcePath]!!.id
        index.remove(id)
        emitChangesToAffectedRootAndFav(root, indexByRoot[root]!!)
        return id
    }

    suspend fun onResourceModified(root: Path, resourcePath: Path): PlainResourcesIndex {
        val index = indexByRoot[root]!!
        index.reindexRoot(Difference(emptyList(), listOf(resourcePath), emptyList()))
        emitChangesToAffectedRootAndFav(root, index)
        return index
    }

    suspend fun onReindexFinish() {
        allRootsFlow.emit(aggregatedIndex.listAllIds())
    }

    fun getIndexByRoot(root: Path) = indexByRoot[root]!!

    fun listenResourcesChanges(rootAndFav: RootAndFav): StateFlow<Set<ResourceId>?> {
        return if (rootAndFav.isAllRoots()) {
            allRootsFlow
        } else {
            if (flowByRootAndFav[rootAndFav] == null) {
                indexByRoot[rootAndFav.root]?.let { index ->
                    flowByRootAndFav[rootAndFav] = MutableStateFlow(index.listIds(rootAndFav.fav))
                } ?: run {
                    flowByRootAndFav[rootAndFav] = MutableStateFlow(null)
                }
            }
            flowByRootAndFav[rootAndFav]!!
        }
    }

    fun listIds(rootAndFav: RootAndFav): Set<ResourceId> {
        return if (rootAndFav.isAllRoots())
            aggregatedIndex.listIds(rootAndFav.fav)
        else
            indexByRoot[rootAndFav.root]?.listIds(rootAndFav.fav) ?: emptySet()
    }

    fun getPath(resourceId: ResourceId): Path? {
        return aggregatedIndex.getPath(resourceId)
    }

    private suspend fun emitChangesToAffectedRootAndFav(root: Path, index: PlainResourcesIndex) {
        aggregatedIndex = AggregatedResourcesIndex(indexByRoot.values)
        if (allRootsFlow.value != null)
            allRootsFlow.emit(aggregatedIndex.listAllIds())

        val affectedRootAndFavs = flowByRootAndFav.keys.filter { it.root == root }
        affectedRootAndFavs.forEach {
            if (flowByRootAndFav[it]!!.value != index.listIds(it.fav))
                flowByRootAndFav[it]!!.emit(index.listIds(it.fav))
        }
    }
}