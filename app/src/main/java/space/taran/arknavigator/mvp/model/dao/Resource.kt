package space.taran.arknavigator.mvp.model.dao

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import space.taran.arknavigator.mvp.model.repo.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.ResourceMetaExtra
import space.taran.arknavigator.utils.*
import java.util.zip.CRC32

import java.nio.file.Files
import java.nio.file.Path

typealias ResourceExtra = Boolean //TODO: PR #33

@Entity
data class Resource (
    @PrimaryKey(autoGenerate = false)
    val id: ResourceId,
    val root: StringPath,
    val path: StringPath,
    val modified: Milliseconds,
    val size: Long,
    val extra: ResourceExtra?
) {
    companion object {
        fun fromMeta(meta: ResourceMeta, root: Path, path: Path): Resource =
            Resource(
                id = meta.id,
                root = root.toString(),
                path = path.toString(),
                modified = meta.modified.toMillis(),
                size = meta.size,
                extra = meta.extra?.roomData())
    }
}

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

private const val BUFFER_CAPACITY = 512 * KILOBYTE