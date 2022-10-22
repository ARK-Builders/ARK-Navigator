package space.taran.arknavigator.mvp.model.repo.kind

import kotlinx.serialization.Serializable
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.meta.MetadataStorage
import java.nio.file.Path

object LinkKindFactory : ResourceKindFactory<ResourceKind.Link> {
    override val acceptedExtensions = setOf("link")
    override val acceptedKindCode = KindCode.LINK
    override val acceptedMimeTypes: Set<String>
        get() = setOf()

    override fun fromPath(
        path: Path,
        meta: ResourceMeta,
        metadataStorage: MetadataStorage
    ): ResourceKind.Link {
        // TODO: we don't need these lines anymore, but what about the native bindings?
        // val linkJson = loadLinkFile(path.pathString)
        // val link = Json.decodeFromString(JsonLink.serializer(), linkJson)

        return metadataStorage.locate(path, meta).kind as ResourceKind.Link
    }

    override fun fromRoom(extras: Map<MetaExtraTag, String>): ResourceKind.Link =
        ResourceKind.Link(
            extras[MetaExtraTag.TITLE],
            extras[MetaExtraTag.DESCRIPTION],
            extras[MetaExtraTag.URL]
        )

    override fun toRoom(
        id: ResourceId,
        kind: ResourceKind.Link
    ): Map<MetaExtraTag, String?> = mapOf(
        MetaExtraTag.URL to kind.url,
        MetaExtraTag.TITLE to kind.title,
        MetaExtraTag.DURATION to kind.description
    )
}

@Serializable
private data class JsonLink(val url: String, val title: String, val desc: String)
