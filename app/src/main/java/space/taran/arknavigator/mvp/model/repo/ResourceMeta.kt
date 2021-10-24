package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.computeId
import space.taran.arknavigator.ui.fragments.utils.Preview
import space.taran.arknavigator.utils.isFormat
import space.taran.arknavigator.utils.isImage
import space.taran.arknavigator.utils.isPDF
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
                extra = ResourceMetaExtra.provideMetaExtraInstance(path)
            )
        }

        fun fromRoom(resource: Resource): ResourceMeta =
            ResourceMeta(
                id = resource.id,
                modified = FileTime.fromMillis(resource.modified),
                size = resource.size,
                extra = ResourceMetaExtra.provideMetaExtraInstance(Paths.get(resource.path))
            )
    }
}

enum class ResourceType {
    IMAGE, VIDEO, GIF, PDF, UNDEFINED
}


abstract class ResourceMetaExtra {

    abstract var filePath: Path?

    companion object {
        fun provideMetaExtraInstance(filePath: Path?): ResourceMetaExtra {
            return when {
                isVideo(filePath) -> VideoMetaExtra(filePath)
                else -> BaseMetaExtra(filePath)
            }
        }
    }

    fun type(): ResourceType {
        return when {
            filePath == null -> ResourceType.UNDEFINED
            isImage(filePath) -> ResourceType.IMAGE
            isVideo(filePath) -> ResourceType.VIDEO
            isPDF(filePath) -> ResourceType.PDF
            isFormat(filePath, "gif") -> ResourceType.GIF
            else -> ResourceType.UNDEFINED
        }
    }

    abstract fun appData(): MutableMap<Preview.ExtraInfoTag, String>

    abstract fun roomData(): ResourceExtra //TODO: PR #33
}