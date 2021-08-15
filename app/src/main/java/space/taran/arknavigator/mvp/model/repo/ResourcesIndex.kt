package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.ResourceId
import java.nio.file.Path

interface ResourcesIndex {

    //todo: with async indexing we must emit ids of not-indexed-yet resources too

    fun listIds(prefix: Path?): List<ResourceId>

    // we pass all known resource ids to a storage because
    // 1) any storage exists globally
    // 2) we maintain only 1 storage per root
    // 3) every storage is initialized with resource ids
    fun listAllIds(): List<ResourceId> = listIds(null)

    fun getPath(id: ResourceId): Path?

    fun remove(id: ResourceId): Path?

}