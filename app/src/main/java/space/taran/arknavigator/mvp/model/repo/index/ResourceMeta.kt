package space.taran.arknavigator.mvp.model.repo.index

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

        fun fromPath(path: Path): space.taran.arklib.index.ResourceMeta {
            val size = Files.size(path)
            if (size < 1) {
                return null
            }

            val id = computeId(size, path)
            val kind = GeneralKindFactory.fromPath(path)

            return space.taran.arklib.index.ResourceMeta(
                id = id,
                name = path.fileName.toString(),
                extension = extension(path),
                modified = Files.getLastModifiedTime(path),
                size = size,
                kind = kind,
            )
        }

        fun fromRoom(room: ResourceWithExtra): space.taran.arklib.index.ResourceMeta =
            space.taran.arklib.index.ResourceMeta(
                id = room.resource.id,
                name = room.resource.name,
                extension = room.resource.extension,
                modified = FileTime.fromMillis(room.resource.modified),
                size = room.resource.size,
                kind = GeneralKindFactory.fromRoom(room.resource.kind, room.extras)
            )
    }
}
