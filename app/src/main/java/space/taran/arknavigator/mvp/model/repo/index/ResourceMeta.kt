package space.taran.arknavigator.mvp.model.repo.index

import java.io.IOException
import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.mvp.model.repo.kind.GeneralKindFactory
import space.taran.arknavigator.mvp.model.repo.kind.ResourceKind
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

data class ResourceMeta(
    val id: ResourceId,
    val name: String,
    val extension: String,
    val modified: FileTime,
    val size: Long,
    val kind: ResourceKind?
) {

    companion object {

        fun fromPath(path: Path): Result<ResourceMeta?> {
            val size = Files.size(path)
            if (size < 1) {
                return Result.failure(IOException("Invalid file size"))
            }

            val id = computeId(size, path)
            return try {
                val kind = GeneralKindFactory.fromPath(path)
                Result.success(
                    ResourceMeta(
                        id = id,
                        name = path.fileName.toString(),
                        extension = extension(path),
                        modified = Files.getLastModifiedTime(path),
                        size = size,
                        kind = kind,
                    )
                )
            } catch (e: IOException) {
                Result.failure<ResourceMeta>(e)
            }
        }

        fun fromRoom(room: ResourceWithExtra): ResourceMeta =
            ResourceMeta(
                id = room.resource.id,
                name = room.resource.name,
                extension = room.resource.extension,
                modified = FileTime.fromMillis(room.resource.modified),
                size = room.resource.size,
                kind = GeneralKindFactory.fromRoom(room.resource.kind, room.extras)
            )
    }
}
