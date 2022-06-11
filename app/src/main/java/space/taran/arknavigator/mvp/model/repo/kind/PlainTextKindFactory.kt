package space.taran.arknavigator.mvp.model.repo.kind

import java.nio.file.Path
import space.taran.arknavigator.mvp.model.repo.index.ResourceId

object PlainTextKindFactory : ResourceKindFactory<ResourceKind.PlainText> {
    override val acceptedExtensions: Set<String> =
        setOf("txt")
    override val acceptedKindCode = KindCode.PLAINTEXT

    override fun fromPath(path: Path) = ResourceKind.PlainText()

    override fun fromRoom(extras: Map<MetaExtraTag, String>) =
        ResourceKind.PlainText()

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.PlainText
    ): Map<MetaExtraTag, String?> =
        emptyMap()
}
