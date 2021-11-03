package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.computeId
import space.taran.arknavigator.mvp.model.repo.extra.GifMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.PdfMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.VideoMetaExtra
import space.taran.arknavigator.utils.isFormat
import space.taran.arknavigator.utils.isImage
import space.taran.arknavigator.utils.isVideo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime

data class ResourceMeta(
    val id: ResourceId,
    val modified: FileTime,
    val size: Long,
    val extra: ResourceMetaExtra?) {
    companion object {
        fun fromPath(path: Path): ResourceMeta {
            val id = computeId(path)

            return ResourceMeta(
                id = id,
                modified = Files.getLastModifiedTime(path),
                size = Files.size(path),
                extra = ResourceMetaExtra.provide(path)
            )
        }

        fun fromRoom(resource: Resource): ResourceMeta =
            ResourceMeta(
                id = resource.id,
                modified = FileTime.fromMillis(resource.modified),
                size = resource.size,
                extra = ResourceMetaExtra.provide(Paths.get(resource.path))
            )
    }
}

enum class ResourceType {
    IMAGE, VIDEO, GIF, PDF
}

abstract class ResourceMetaExtra(val type: ResourceType) {

    companion object {
        fun provide(filePath: Path): ResourceMetaExtra? {
            return when {
                isImage(filePath) -> ImageMetaExtra()
                isVideo(filePath) -> VideoMetaExtra()
                isFormat(filePath, "pdf") -> PdfMetaExtra()
                isFormat(filePath, "gif") -> GifMetaExtra()
                else -> null
            }
        }

        fun fromRoom(room: ResourceExtra): ResourceMetaExtra {
            throw NotImplementedError()
        }
    }

    abstract val data: Map<ExtraInfoTag, String>

    abstract fun toRoom(): ResourceExtra
}

enum class ExtraInfoTag {
    MEDIA_RESOLUTION, MEDIA_DURATION
}