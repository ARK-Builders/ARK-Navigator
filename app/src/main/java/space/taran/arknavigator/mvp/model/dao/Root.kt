package space.taran.arknavigator.mvp.model.dao

import androidx.room.Entity
import androidx.room.PrimaryKey
import space.taran.arknavigator.utils.StringPath

@Entity
data class Root(
    @PrimaryKey(autoGenerate = false)
    val path: StringPath
)
