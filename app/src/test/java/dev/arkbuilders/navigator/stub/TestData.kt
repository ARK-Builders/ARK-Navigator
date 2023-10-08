package dev.arkbuilders.navigator.stub

import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.domain.index.Resource
import java.nio.file.attribute.FileTime
import java.util.Date

object TestData {
    fun resourceById() = mapOf(
        R1 to Resource(
            R1,
            "Resource1",
            ".jpg",
            fileTime(),
        ),
        R2 to Resource(
            R2,
            "Resource2",
            ".jpg",
            fileTime(),
        ),
        R3 to Resource(
            R3,
            "Resource3",
            ".jpg",
            fileTime(),
        ),
        R4 to Resource(
            R4,
            "Resource4",
            ".odt",
            fileTime(),
        )
    )

    fun tagsById() = mapOf(
        R1 to setOf(TAG1, TAG2),
        R2 to setOf(TAG2),
        R3 to setOf(),
        R4 to setOf(TAG3, TAG4)
    )

    private fun fileTime() = FileTime.from(Date().toInstant())
}

val R1 = ResourceId(1L, 1L)
val R2 = ResourceId(2L, 2L)
val R3 = ResourceId(3L, 3L)
val R4 = ResourceId(4L, 4L)

const val TAG1 = "tag1"
const val TAG2 = "tag2"
const val TAG3 = "tag3"
const val TAG4 = "tag4"
