package space.taran.arknavigator.mvp.model.repo.index

import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.mvp.model.repo.kind.GeneralKindFactory
import space.taran.arknavigator.mvp.model.repo.kind.MetaExtraTag
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.mvp.model.repo.meta.MetadataStorage
import space.taran.arknavigator.utils.MetaResult
import space.taran.arknavigator.utils.extension
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime


data class ResourceMeta(
    val id: ResourceId,
    val name: String,
    val extension: String,
    val modified: FileTime,
    val size: Long,
    val kind: ResourceKind?,
    val extras: Map<MetaExtraTag, String>?
) {

    companion object {

        fun fromPath(path: Path): MetaResult {
            val size = Files.size(path)
            if (size < 1) {
                return MetaResult.failure(IOException("Invalid file size"))
            }

            val id = computeId(size, path)

            var kind: ResourceKind? = null
            var kindDetectException: Exception? = null

            try {
                kind = GeneralKindFactory.fromPath(path)
            } catch (e: Exception) {
                kindDetectException = e
            }

            val meta = ResourceMeta(
                id = id,
                name = path.fileName.toString(),
                extension = extension(path),
                modified = Files.getLastModifiedTime(path),
                size = size,
                kind = kind,
                extras = null,
            )

            val extras =

            return MetaResult(meta, kindDetectException)
        }

        fun fromRoom(room: ResourceWithExtra): ResourceMeta =
            ResourceMeta(
                id = room.resource.id,
                name = room.resource.name,
                extension = room.resource.extension,
                modified = FileTime.fromMillis(room.resource.modified),
                size = room.resource.size,
                kind = GeneralKindFactory.fromRoom(room.resource.kind, room.extras),
                extras = room.extras.associate { MetaExtraTag.values()[it.ordinal] to it.value }
            )
    }
}
