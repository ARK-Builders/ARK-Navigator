package com.taran.imagemanager.utils

import java.io.File
import java.util.zip.CRC32

fun getHash(imagePath: String): String {
    val imageFile = File(imagePath)
    val bytes = imageFile.readBytes()
    val crc32 = CRC32()
    crc32.update(bytes)
    return java.lang.Long.toHexString(crc32.value)
}