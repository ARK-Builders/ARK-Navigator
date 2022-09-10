package space.taran.arknavigator.mvp.model.repo.index

import space.taran.arklib.index.ResourceMeta
import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.mvp.model.repo.kind.GeneralKindFactory
import space.taran.arknavigator.utils.extension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
typealias ResourceMeta = ResourceMeta

class ResourceMetaExtra {
    companion object {

        fun fromPath(path: Path): ResourceMeta? {
            val size = Files.size(path)
            if (size < 1) {
                return null
            }

            val id = computeId(size, path)
            val kind = GeneralKindFactory.fromPath(path)

            return ResourceMeta(
                id = id,
                name = path.fileName.toString(),
                extension = extension(path),
                modified = Files.getLastModifiedTime(path),
                size = size,
                kind = kind,
            )
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

//    }
// }
