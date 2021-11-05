package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.computeId
import space.taran.arknavigator.mvp.model.repo.extra.AnimationMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.ImageMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.DocumentMetaExtra
import space.taran.arknavigator.mvp.model.repo.extra.VideoMetaExtra
import space.taran.arknavigator.utils.extension
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
    IMAGE, VIDEO, ANIMATION, DOCUMENT
}

abstract class ResourceMetaExtra(val type: ResourceType) {
    companion object {
        fun provide(filePath: Path): ResourceMetaExtra? {
            return when(extension(filePath)) {
                in ImageMetaExtra.ACCEPTED_EXTENSIONS -> ImageMetaExtra()
                in VideoMetaExtra.ACCEPTED_EXTENSIONS -> VideoMetaExtra()
                in DocumentMetaExtra.ACCEPTED_EXTENSIONS -> DocumentMetaExtra()
                in AnimationMetaExtra.ACCEPTED_EXTENSIONS -> AnimationMetaExtra()
                else -> null
            }
        }

        fun fromRoom(room: ResourceExtra): ResourceMetaExtra {
            TODO()
        }
    }

    abstract val data: Map<ExtraInfoTag, String>

    abstract fun toRoom(): ResourceExtra
}

enum class ExtraInfoTag {
    MEDIA_RESOLUTION, MEDIA_DURATION
}