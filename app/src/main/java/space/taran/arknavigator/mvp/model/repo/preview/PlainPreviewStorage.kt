package space.taran.arknavigator.mvp.model.repo.preview

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.preview.generator.PreviewGenerator
import space.taran.arknavigator.utils.PREVIEW_STORAGE
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists

class PlainPreviewStorage(root: Path) : PreviewStorage {
    private val previewById = mutableMapOf<ResourceId, Preview>()

    private val previewsStorage: Path = root.resolve(".previews")
    private val thumbnailsStorage: Path = root.resolve(".thumbnails")

    override fun getPreview(id: ResourceId): Preview = previewById[id]!!

    override fun contains(id: ResourceId): Boolean = previewById.containsKey(id)

    override suspend fun forget(id: ResourceId): Unit = withContext(Dispatchers.IO) {
        previewById.remove(id)

        Files.deleteIfExists(previewPath(id))
        Files.deleteIfExists(thumbnailPath(id))
    }

    suspend fun cleanup(existing: Collection<ResourceId>) {
        val disappeared = previewById.keys.minus(existing)

        disappeared.forEach { forget(it) }
    }

    suspend fun init(index: ResourcesIndex) = withContext(Dispatchers.IO) {
        if (previewsStorage.notExists()) Files.createDirectories(previewsStorage)
        if (thumbnailsStorage.notExists()) Files.createDirectories(thumbnailsStorage)

        val generatedIds = findGeneratedPreviews()
        val indexedIds = index.listAllIds()

        val lostIds = generatedIds.subtract(indexedIds)
        if (lostIds.isNotEmpty()) {
            Log.d(PREVIEW_STORAGE, "lostResources: $lostIds")
            throw AssertionError("Index lost resources")
        }

        val newIds = indexedIds.subtract(generatedIds)
        newIds.forEach {
            generatePreview(index.getPath(it), index.getMeta(it))
        }
    }

    private suspend fun generatePreview(path: Path, meta: ResourceMeta) = withContext(Dispatchers.IO) {
        if (Files.isDirectory(path)) {
            throw AssertionError("Previews for folders are constant")
        }

        previewById[meta.id] =
                PreviewGenerator.generate(path, meta, previewPath(meta.id), thumbnailPath(meta.id))
    }

    private fun previewPath(id: ResourceId): Path =
        previewsStorage.resolve(id.toString())

    private fun thumbnailPath(id: ResourceId): Path =
        thumbnailsStorage.resolve(id.toString())

    private fun findGeneratedPreviews(): Set<ResourceId> =
        previewsStorage.listDirectoryEntries()
            .intersect(thumbnailsStorage.listDirectoryEntries())
            .mapNotNull {
                it.fileName.toString().toLongOrNull()
            }
            .toSet()
}