package space.taran.arknavigator.mvp.model.repo

import space.taran.arknavigator.mvp.model.dao.Resource
import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.dao.computeId
import java.nio.file.Files
import java.nio.file.Path
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
                size = Files.size(path))
        }

        fun fromRoom(resource: Resource): ResourceMeta =
            ResourceMeta(
                id = resource.id,
                modified = FileTime.fromMillis(resource.modified),
                size = resource.size)
    }
}

enum class ResourceType {
    IMAGE, VIDEO, GIF, PDF
}

interface ResourceMetaExtra {
    fun type(): ResourceType

    fun appData(): Any //TODO: PR #33

    fun roomData(): ResourceExtra
}