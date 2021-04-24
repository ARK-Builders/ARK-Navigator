package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.Resource
import space.taran.arkbrowser.mvp.model.entity.common.TagState
import java.io.File

enum class SortBy{
    NAME, SIZE, LAST_MODIFIED, TYPE
}

val fileComparator = Comparator<File> { f1, f2 -> compareFiles(f1, f2) }

val markableFileComparator = Comparator<MarkableFile> { (_, f1), (_, f2) ->
    compareFiles(f1, f2)
}

fun resourceComparator(sortBy: SortBy, inverse: Boolean = false) = Comparator<Resource> { r1, r2 ->
    val result = when (sortBy) {
        SortBy.NAME -> r1.name.compareTo(r2.name)
        SortBy.TYPE -> r1.type.compareTo(r2.type)
        SortBy.LAST_MODIFIED -> r1.lastModified.compareTo(r2.lastModified)
        SortBy.SIZE -> r1.size.compareTo(r2.size)
    }

    if (inverse) { result * -1 } else { result }
}

fun tagsComparator() = Comparator<TagState> { o1, o2 ->
    when {
        o1.isChecked && o2.isChecked -> o1.tag.compareTo(o2.tag)
        o1.isChecked && !o2.isChecked -> -1
        !o1.isChecked && o2.isChecked -> 1
        !o1.isChecked && !o2.isChecked -> {
            when {
                o1.isActual && o2.isActual -> o1.tag.compareTo(o2.tag)
                o1.isActual && !o2.isActual -> -1
                !o1.isActual && o2.isActual -> 1
                !o1.isActual && !o2.isActual -> o1.tag.compareTo(o2.tag)
                else -> 0
            }
        }
        else -> 0
    }
}

private fun compareFiles(f1: File, f2: File) = when {
    f1.isDirectory && f2.isDirectory   -> f1.name.compareTo(f2.name)
    !f1.isDirectory && !f2.isDirectory -> f1.name.compareTo(f2.name)
    f1.isDirectory && !f2.isDirectory  -> -1
    !f1.isDirectory && f2.isDirectory  -> 1
    else -> 0
}