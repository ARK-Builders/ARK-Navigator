package space.taran.arknavigator.mvp.model.repo.meta

import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import space.taran.arknavigator.mvp.model.arkFolder
import space.taran.arknavigator.mvp.model.arkMetadata
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.utils.LogTags.METADATA
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

class PlainMetadataStorage(val root: Path) : MetadataStorage {
    private val metaDir = root.arkFolder().arkMetadata()

    private fun metaPath(id: ResourceId): Path =
        metaDir.resolve(id.toString())

    init {
        metaDir.createDirectories()
    }

    override fun locate(path: Path, resource: ResourceMeta): ResourceMeta {
        val metadata = metaPath(resource.id)
        if (!Files.exists(metadata)) {
            Log.w(METADATA, "metadata was not found for resource $resource")
            // means that we couldn't generate anything for this kind of resource
            return resource
        }
        val result = resource.copy()
        result.kind = Json.decodeFromString(
            ResourceKind.serializer(),
            metadata.bufferedReader().use { it.readText() })
        return result
    }

    override fun forget(id: ResourceId) {
        metaPath(id).deleteIfExists()
    }

    override fun generate(path: Path, meta: ResourceMeta) {
        require(!path.isDirectory()) { "Metadata for folders are constant" }
        val metaPath = metaPath(meta.id)

        if (!Files.exists(metaPath)) {
            Log.d(
                METADATA,
                "Generating metadata for ${meta.id} ($path)"
            )
            metaPath.writeText(Json.encodeToString(meta.kind))
        }
    }
}
