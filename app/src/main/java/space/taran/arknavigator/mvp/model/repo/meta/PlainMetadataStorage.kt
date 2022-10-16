package space.taran.arknavigator.mvp.model.repo.meta

import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import space.taran.arknavigator.mvp.model.arkFolder
import space.taran.arknavigator.mvp.model.arkMetadata
import space.taran.arknavigator.mvp.model.arkPreviews
import space.taran.arknavigator.mvp.model.arkThumbnails
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.kind.ImageKindFactory
import space.taran.arknavigator.mvp.model.repo.kind.MetaExtraTag
import space.taran.arknavigator.utils.LogTags.METADATA
import space.taran.arknavigator.utils.LogTags.PREVIEWS
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.collections.HashMap
import kotlin.io.path.*

class PlainMetadataStorage(val root: Path) : MetadataStorage {
    private val metaDir = root.arkFolder().arkMetadata()

    private fun metaPath(id: ResourceId): Path =
        metaDir.resolve(id.toString())

    init {
        metaDir.createDirectories()
    }

    override fun locate(path: Path, resource: ResourceMeta): Map<MetaExtraTag, String?>? {
        val metadata = metaPath(resource.id)
        if (!Files.exists(metadata)) {
            Log.w(METADATA, "metadata was not found for resource $resource")
            // means that we couldn't generate anything for this kind of resource
            return null
        }
        val json = Json.parseToJsonElement(metadata.bufferedReader().use { it.readText() }).jsonObject
        return mutableMapOf<MetaExtraTag, String>().apply {
            json.keys.forEach { put(MetaExtraTag.valueOf(it), json[it].toString()) }
        }.toMap()
    }

    override fun forget(id: ResourceId) {
        metaPath(id).deleteIfExists()
    }

    override fun generate(path: Path, extras: Map<MetaExtraTag, String?>, meta: ResourceMeta) {
        require(!path.isDirectory()) { "Metadata for folders are constant" }
        val metaPath = metaPath(meta.id)

        if (!Files.exists(metaPath)) {
            Log.d(
                METADATA,
                "Generating metadata for ${meta.id} ($path)"
            )
            metaPath.writeText(Json.encodeToString(extras))
        }
    }
}
