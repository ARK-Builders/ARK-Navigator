package space.taran.arkbrowser.utils

import java.util.zip.CRC32

fun getHash(bytes: ByteArray): String {
    val crc32 = CRC32()
    crc32.update(bytes)
    return java.lang.Long.toHexString(crc32.value)
}

