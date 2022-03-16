package space.taran.arknavigator.mvp.model.repo.index

import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.mvp.model.repo.extra.DocumentMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.LinkMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.VideoMetaExtra
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
    val kind: ResourceKind?,
    val extra: ResourceMetaExtra?
) {

    companion object {

        fun fromPath(path: Path): ResourceMeta? {
            val size = Files.size(path)
            if (size < 1) {
                return null
            }

            val id = computeId(size, path)
            val kind = kindByExt(extension(path))

            return ResourceMeta(
                id = id,
                name = path.fileName.toString(),
                extension = extension(path),
                modified = Files.getLastModifiedTime(path),
                size = size,
                kind = kind,
                extra = ResourceMetaExtra.provide(kind, path)
            )
        }

        fun fromRoom(room: ResourceWithExtra): ResourceMeta {
            val extra = if (room.extras.isNotEmpty()) {
                ResourceMetaExtra.fromRoom(room.extras)
            } else {
                null
            }

            return ResourceMeta(
                id = room.resource.id,
                name = room.resource.name,
                extension = room.resource.extension,
                modified = FileTime.fromMillis(room.resource.modified),
                size = room.resource.size,
                kind = kindByCode(room.resource.kind),
                extra = extra
            )
        }

        private fun kindByExt(extension: String): ResourceKind? {
            return when (extension) {
                in ImageMetaExtra.ACCEPTED_EXTENSIONS -> ResourceKind.IMAGE
                in VideoMetaExtra.ACCEPTED_EXTENSIONS -> ResourceKind.VIDEO
                in DocumentMetaExtra.ACCEPTED_EXTENSIONS -> ResourceKind.DOCUMENT
                in LinkMetaExtra.ACCEPTED_EXTENSIONS -> ResourceKind.LINK
                else -> null
            }
        }

        private fun kindByCode(code: Int): ResourceKind? {
            if (code == -1) return null

            return ResourceKind.values().first {
                it.ordinal == code
            }
        }
    }
}

enum class ResourceKind {
    IMAGE, VIDEO, DOCUMENT, LINK
}

enum class MetaExtraTag {
    DURATION, WIDTH, HEIGHT, PAGES, TITLE, DESCRIPTION, URL
}

data class ResourceMetaExtra(val data: Map<MetaExtraTag, String>) {
    companion object {
        fun fromRoom(room: List<ResourceExtra>): ResourceMetaExtra {
            val data = room.map {
                MetaExtraTag.values()[it.ordinal] to it.value
            }.toMap()
            return ResourceMetaExtra(data)
        }
        fun provide(kind: ResourceKind?, path: Path): ResourceMetaExtra? {
            return when (kind) {
                ResourceKind.IMAGE -> ImageMetaExtra.extract(path)
                ResourceKind.VIDEO -> VideoMetaExtra.extract(path)
                ResourceKind.DOCUMENT -> DocumentMetaExtra.extract(path)
                ResourceKind.LINK -> LinkMetaExtra.extract(path)
                null -> null
            }
        }
    }
}
