package space.taran.arkbrowser.mvp.model.dao

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.zip.CRC32

import space.taran.arkbrowser.utils.StringPath
import space.taran.arkbrowser.utils.Milliseconds
import space.taran.arkbrowser.utils.RESOURCES_INDEX
import java.nio.file.Files
import java.nio.file.Path

@Entity
data class Resource (
    @PrimaryKey(autoGenerate = false)
    val id: ResourceId,
    val root: StringPath,
    val path: StringPath,
    val modified: Milliseconds
)

typealias ResourceId = Long

// Reading from SD card is up to 3 times slower!
// Calculating CRC-32 hash of a file takes about the
// same time as reading the file from internal storage.

fun computeId(file: Path): ResourceId {
    val size = Files.size(file)
    Log.d(RESOURCES_INDEX, "calculating hash of $file " +
        "(size is ${size / MEGABYTE} megabytes)")

    val crc32 = CRC32()
    //todo: synchronize access

    val source = Files.newInputStream(file)
    val buffer = ByteArray(BUFFER_CAPACITY)

    var total = 0L
    var read = source.read(buffer)

    while (read > 0) {
        crc32.update(buffer, 0, read)

        total += read.toLong()
        read = source.read(buffer)
    }
    source.close()

    Log.d(RESOURCES_INDEX, "$total bytes has been read")
    if (total != size) throw AssertionError(
        "File wasn't read to the end")

    return crc32.value
}

private const val KILOBYTE = 1024
private const val MEGABYTE = 1024 * KILOBYTE

private const val BUFFER_CAPACITY = 512 * KILOBYTE