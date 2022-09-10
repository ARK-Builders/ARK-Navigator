package space.taran.arknavigator.stub

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arklib.index.ResourceKind
import java.nio.file.attribute.FileTime
import java.util.Date

object TestData {
    fun metasById() = mapOf(
        R1 to ResourceMeta(
            R1,
            "Resource1",
            ".jpg",
            fileTime(),
            100,
            ResourceKind.Image()
        ),
        R2 to ResourceMeta(
            R2,
            "Resource2",
            ".jpg",
            fileTime(),
            100,
            ResourceKind.Image()
        ),
        R3 to ResourceMeta(
            R3,
            "Resource3",
            ".jpg",
            fileTime(),
            120,
            ResourceKind.Image()
        ),
        R4 to ResourceMeta(
            R4,
            "Resource4",
            ".odt",
            fileTime(),
            140,
            ResourceKind.Document()
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

const val R1 = 1L
const val R2 = 2L
const val R3 = 3L
const val R4 = 4L

const val TAG1 = "tag1"
const val TAG2 = "tag2"
const val TAG3 = "tag3"
const val TAG4 = "tag4"
