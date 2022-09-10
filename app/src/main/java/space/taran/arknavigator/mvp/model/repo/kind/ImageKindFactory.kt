package space.taran.arknavigator.mvp.model.repo.kind

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import java.nio.file.Path
import space.taran.arklib.index.ResourceKind
object ImageKindFactory : ResourceKindFactory<ResourceKind.Image> {
    override val acceptedExtensions: Set<String> =
        setOf("jpg", "jpeg", "png", "svg", "gif")
    override val acceptedMimeTypes: Set<String>
        get() = setOf("image/jpeg", "image/jpg", "image/png", "image/gif")
    override val acceptedKindCode = KindCode.IMAGE

    override fun fromPath(path: Path) = ResourceKind.Image()

    override fun fromRoom(extras: Map<MetaExtraTag, String>) = ResourceKind.Image()

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Image
    ): Map<MetaExtraTag, String?> =
        emptyMap()
}
