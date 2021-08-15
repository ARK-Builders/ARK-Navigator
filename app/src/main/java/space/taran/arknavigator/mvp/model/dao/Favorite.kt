package space.taran.arknavigator.mvp.model.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import space.taran.arknavigator.utils.StringPath

@Entity(foreignKeys = [ForeignKey(
    entity = Root::class,
    parentColumns = arrayOf("path"),
    childColumns = arrayOf("root"),
    onDelete = ForeignKey.CASCADE)])
data class Favorite(
    @ColumnInfo(index = true)
    val root: StringPath,

    @PrimaryKey(autoGenerate = false)
    val relative: StringPath)