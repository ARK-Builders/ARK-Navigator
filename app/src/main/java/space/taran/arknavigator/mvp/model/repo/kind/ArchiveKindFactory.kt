package space.taran.arknavigator.mvp.model.repo.kind

import java.nio.file.Path
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arklib.index.ResourceKind

object ArchiveKindFactory : ResourceKindFactory<ResourceKind.Archive> {
    override val acceptedExtensions: Set<String> =
        setOf("zip", "7z", "rar", "tar.gz", "tar.xz")

    override val acceptedMimeTypes: Set<String>
        get() = setOf("application/zip")
    override val acceptedKindCode = KindCode.ARCHIVE

    override fun fromPath(path: Path) = ResourceKind.Archive()

    override fun fromRoom(extras: Map<MetaExtraTag, String>) =
        ResourceKind.Archive()

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Archive
    ): Map<MetaExtraTag, String?> =
        emptyMap()
}
