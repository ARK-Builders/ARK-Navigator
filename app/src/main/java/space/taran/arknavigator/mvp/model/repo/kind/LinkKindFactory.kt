package space.taran.arknavigator.mvp.model.repo.kind

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import java.nio.file.Path
import java.util.zip.ZipFile
import space.taran.arklib.index.ResourceKind
object LinkKindFactory : ResourceKindFactory<ResourceKind.Link> {
    private const val JSON_FILE = "link.json"

    override val acceptedExtensions = setOf("link")
    override val acceptedKindCode = KindCode.LINK
    override val acceptedMimeTypes: Set<String>
        get() = setOf()

    override fun fromPath(path: Path): ResourceKind.Link {
        val zip = ZipFile(path.toFile())
        val jsonEntry = zip
            .entries()
            .asSequence()
            .find { entry -> entry.name == JSON_FILE }
            ?: return ResourceKind.Link()

        val link = Json.decodeFromStream<JsonLink>(zip.getInputStream(jsonEntry))

        return ResourceKind.Link(link.title, link.desc, link.url)
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
