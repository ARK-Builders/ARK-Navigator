package space.taran.arknavigator.mvp.model.repo.index

import android.widget.TextView
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceWithExtra
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.DocumentMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.VideoMetaExtra
import space.taran.arknavigator.utils.extension
import space.taran.arknavigator.utils.extensions.makeGone
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

data class ResourceMeta(
    val id: ResourceId,
    val modified: FileTime,
    val size: Long,
    val kind: ResourceKind?,
    val extra: ResourceMetaExtra?) {

    companion object {
        fun fromPath(path: Path): ResourceMeta {
            val id = computeId(path)
            val kind = kindByExt(extension(path))

            return ResourceMeta(
                id = id,
                modified = Files.getLastModifiedTime(path),
                size = Files.size(path),
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
                modified = FileTime.fromMillis(room.resource.modified),
                size = room.resource.size,
                kind = kindByCode(room.resource.kind),
                extra = extra)
        }

        private fun kindByExt(extension: String): ResourceKind? {
            return when (extension) {
                in ImageMetaExtra.ACCEPTED_EXTENSIONS -> ResourceKind.IMAGE
                in VideoMetaExtra.ACCEPTED_EXTENSIONS -> ResourceKind.VIDEO
                in DocumentMetaExtra.ACCEPTED_EXTENSIONS -> ResourceKind.DOCUMENT
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
    IMAGE, VIDEO, DOCUMENT
}

enum class MetaExtraTag {
    DURATION, WIDTH, HEIGHT, PAGES
}

data class ResourceMetaExtra(val data: Map<MetaExtraTag, Long>) {
    companion object {
        fun fromRoom(room: List<ResourceExtra>): ResourceMetaExtra {
            val data = room.map { MetaExtraTag.values()[it.key] to it.value }.toMap()
            return ResourceMetaExtra(data)
        }
        fun provide(kind: ResourceKind?, path: Path): ResourceMetaExtra? {
            return when (kind) {
                ResourceKind.IMAGE -> ImageMetaExtra.extract(path)
                ResourceKind.VIDEO -> VideoMetaExtra.extract(path)
                ResourceKind.DOCUMENT -> DocumentMetaExtra.extract(path)
                null -> null
            }
        }

        fun draw(kind: ResourceKind?, extra: ResourceMetaExtra?, extraTVs: Array<TextView>, verbose: Boolean) {
            extraTVs.forEach { it.makeGone() }

            if (extra != null) {
                when(kind) {
                    ResourceKind.VIDEO -> VideoMetaExtra.draw(extra,
                        extraTVs[0], extraTVs[1])
                    ResourceKind.DOCUMENT -> DocumentMetaExtra.draw(extra,
                        extraTVs[0], verbose)
                    else -> {}
                }
            }
        }
    }
}