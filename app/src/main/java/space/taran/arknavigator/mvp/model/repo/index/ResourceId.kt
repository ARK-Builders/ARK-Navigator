package space.taran.arknavigator.mvp.model.repo.index

import java.nio.file.Path

typealias ResourceId = Long

// Reading from SD card is up to 3 times slower!
// Calculating CRC-32 hash of a file takes about the
// same time as reading the file from internal storage.

private external fun computeIdNative(size: Long, file: String): Long

fun computeId(size: Long, file: Path): ResourceId {
    return computeIdNative(size, file.toString())
}
