package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.zip.CRC32
import java.io.File

import space.taran.arkbrowser.utils.Converters
import space.taran.arkbrowser.utils.StringPath
import space.taran.arkbrowser.utils.Timestamp
import space.taran.arkbrowser.utils.readBytes

@Entity
@TypeConverters(Converters::class)
data class Resource (
    @PrimaryKey(autoGenerate = false)
    val id: ResourceId,
    val root: StringPath,
    val path: StringPath,
    val modified: Timestamp
)

typealias ResourceId = Long

fun computeId(file: File): ResourceId {
    val crc32 = CRC32()
    crc32.update(readBytes(file))
    return crc32.value
}