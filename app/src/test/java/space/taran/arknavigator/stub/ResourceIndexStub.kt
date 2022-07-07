package space.taran.arknavigator.stub

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import java.nio.file.Path
import kotlin.io.path.Path

class ResourceIndexStub : ResourcesIndex {
    private val metas = TestData.metasById().toMutableMap()

    override fun listResources(prefix: Path?): Set<ResourceMeta> =
        metas.values.toSet()

    override fun getPath(id: ResourceId): Path =
        Path("")

    override fun getMeta(id: ResourceId): ResourceMeta =
        metas[id]!!

    override suspend fun reindex() {}

    override fun remove(id: ResourceId): Path {
        metas.remove(id)
        return Path("")
    }

    override suspend fun updateResource(oldId: ResourceId, path: Path, newResource: ResourceMeta) {}
}
