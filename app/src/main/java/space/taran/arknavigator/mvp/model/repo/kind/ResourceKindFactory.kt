package space.taran.arknavigator.mvp.model.repo.kind

import space.taran.arknavigator.mvp.model.dao.ResourceExtra
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.utils.extension
import java.nio.file.Path

interface ResourceKindFactory<T : ResourceKind> {
    val acceptedExtensions: Set<String>
    val acceptedKindCode: KindCode
    fun isBelong(path: Path) = acceptedExtensions.contains(extension(path))
    fun isBelong(kindCode: Int) = acceptedKindCode.ordinal == kindCode

    fun fromPath(path: Path): T
    fun fromRoom(extras: Map<MetaExtraTag, String>): T
    fun toRoom(id: ResourceId, kind: T): Map<MetaExtraTag, String?>
}

object GeneralKindFactory {
    private val factories =
        listOf(
            ImageKindFactory,
            VideoKindFactory,
            DocumentKindFactory,
            LinkKindFactory,
        )

    fun fromPath(path: Path): ResourceKind? =
        factories.find { factory ->
            factory.isBelong(path)
        }?.fromPath(path)

    fun fromRoom(
        kindCode: Int?,
        extras: List<ResourceExtra>
    ): ResourceKind? {
        kindCode ?: return null

        val data = extras.map {
            MetaExtraTag.values()[it.ordinal] to it.value
        }.toMap()

        return factories.find { factory ->
            factory.isBelong(kindCode)
        }?.fromRoom(data) ?: error("Factory not found")
    }

    fun toRoom(id: ResourceId, kind: ResourceKind?): List<ResourceExtra> {
        kind ?: return emptyList()

        val factory = factories.find { factory ->
            factory.isBelong(kind.code.ordinal)
        } ?: error("Factory not found")

        return (factory as ResourceKindFactory<ResourceKind>)
            .toRoom(id, kind)
            .filter { entry ->
                entry.value != null
            }.map { entry ->
                ResourceExtra(id, entry.key.ordinal, entry.value!!)
            }
    }
}
