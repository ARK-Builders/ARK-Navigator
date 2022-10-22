package space.taran.arknavigator.mvp.model.repo.kind

import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.meta.MetadataStorage
import java.nio.file.Path

object PlainTextKindFactory : ResourceKindFactory<ResourceKind.PlainText> {
    override val acceptedExtensions: Set<String> =
        setOf("txt")
    override val acceptedMimeTypes: Set<String>
        get() = setOf("text/plain")
    override val acceptedKindCode = KindCode.PLAINTEXT

    override fun fromPath(
        path: Path,
        meta: ResourceMeta,
        metadataStorage: MetadataStorage
    ) = ResourceKind.PlainText()

    override fun fromRoom(extras: Map<MetaExtraTag, String>) =
        ResourceKind.PlainText()

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.PlainText
    ): Map<MetaExtraTag, String?> =
        emptyMap()
}
