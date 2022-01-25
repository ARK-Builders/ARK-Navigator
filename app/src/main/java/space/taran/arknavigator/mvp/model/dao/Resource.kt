package space.taran.arknavigator.mvp.model.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.nio.file.Path
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.utils.Milliseconds
import space.taran.arknavigator.utils.StringPath

@Entity
data class Resource(
    @PrimaryKey(autoGenerate = false)
    val id: ResourceId,
    val root: StringPath,
    val path: StringPath,
    val name: String,
    val extension: String,
    val modified: Milliseconds,
    val size: Long,
    val kind: Int,
) {
    companion object {
        fun fromMeta(meta: ResourceMeta, root: Path, path: Path): Resource =
            Resource(
                id = meta.id,
                root = root.toString(),
                path = path.toString(),
                name = meta.name,
                extension = meta.extension,
                modified = meta.modified.toMillis(),
                size = meta.size,
                kind = meta.kind?.ordinal ?: -1
            )
    }
}
