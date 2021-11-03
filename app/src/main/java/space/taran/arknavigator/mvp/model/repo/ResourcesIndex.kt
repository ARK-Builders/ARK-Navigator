package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceId
import java.nio.file.Path

interface ResourcesIndex {

    //todo: with async indexing we must emit ids of not-indexed-yet resources too

    // we pass all known resource ids to a storage because
    // 1) any storage exists globally
    // 2) we maintain only 1 storage per root
    // 3) every storage is initialized with resource ids
    fun listIds(prefix: Path?): Set<ResourceId>

    fun listAllIds(): Set<ResourceId> = listIds(null)

    // whenever we have an id, we assume that we have this id in the index
    // we must load/calculate all necessary ids before we load presenters
    fun getPath(id: ResourceId): Path

    fun getMeta(id: ResourceId): ResourceMeta

    fun remove(id: ResourceId): Path

}