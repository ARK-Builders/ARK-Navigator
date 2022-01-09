package space.taran.arknavigator.mvp.model.repo.index

import android.util.Log
import space.taran.arknavigator.utils.KILOBYTE
import space.taran.arknavigator.utils.MEGABYTE
import space.taran.arknavigator.utils.RESOURCES_INDEX
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32

typealias ResourceId = Long

// Reading from SD card is up to 3 times slower!
// Calculating CRC-32 hash of a file takes about the
// same time as reading the file from internal storage.

private external fun computeIdNative(size: Long, file: String): Long;

fun computeId(size: Long, file: Path): ResourceId {
    System.loadLibrary("arkutils")
    System.loadLibrary("arkutils")
    return computeIdNative(size, file.toString());
}

private const val BUFFER_CAPACITY = 512 * KILOBYTE