package space.taran.arknavigator.mvp.model.repo.meta

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.serialization.json.JsonElement
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.MetaExtraTag
import java.nio.file.Path
import java.util.*

interface MetadataStorage {

    fun locate(path: Path, resource: ResourceMeta): Map<MetaExtraTag, String?>?

    fun forget(id: ResourceId)

    fun generate(path: Path, extras: Map<MetaExtraTag, String?>, meta: ResourceMeta)
}
