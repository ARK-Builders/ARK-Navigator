package space.taran.arknavigator.mvp.model.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourceMetaExtra

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("resource"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ResourceExtra(
    @ColumnInfo(index = true)
    val resource: ResourceId,

    @PrimaryKey(autoGenerate = false)
    val key: Int,

    val value: Long
) {
    companion object {
        fun fromMetaExtra(id: ResourceId, extra: ResourceMetaExtra?):
            List<ResourceExtra> =
            extra?.data?.entries?.map { (tag, value) ->
                ResourceExtra(
                    resource = id,
                    key = tag.ordinal,
                    value = value
                )
            } ?: emptyList()
    }
}
