package space.taran.arknavigator.mvp.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.AggregatedResourcesIndex
import space.taran.arknavigator.mvp.model.repo.PlainResourcesIndex
import java.nio.file.Path

class IndexCache {
    private var indexByRoot = mutableMapOf<Path, PlainResourcesIndex>()
    private var aggregatedIndex = AggregatedResourcesIndex(listOf())
    private var flowByRootAndFav = mutableMapOf<RootAndFav, MutableStateFlow<Set<ResourceId>?>>()
    private var allRootsFlow = MutableStateFlow<Set<ResourceId>?>(null)

    suspend fun onIndexChange(root: Path, index: PlainResourcesIndex) {
        indexByRoot[root] = index
        aggregatedIndex = AggregatedResourcesIndex(indexByRoot.values)
        val affectedRootAndFavs = flowByRootAndFav.keys.filter { it.root == root }
        affectedRootAndFavs.forEach {
            flowByRootAndFav[it]!!.emit(indexByRoot[root]!!.listIds(it.fav))
        }
    }

    suspend fun onReindexFinish() {
        allRootsFlow.emit(aggregatedIndex.listAllIds())
    }

    fun remove(resourceId: ResourceId): Path? {
        val path = aggregatedIndex.remove(resourceId)
        return path
    }

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
}