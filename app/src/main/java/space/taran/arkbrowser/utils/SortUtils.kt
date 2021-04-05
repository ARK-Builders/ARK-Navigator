package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.common.TagState

enum class SortBy{
    NAME, SIZE, LAST_MODIFIED, TYPE
}

fun filesComparator(sortBy: SortBy = SortBy.NAME) = Comparator<File> { f1, f2 ->
    when {
        f1.isFolder && f2.isFolder -> fileCompare(sortBy, f1, f2)
        f1.isFolder && !f2.isFolder -> -1
        !f1.isFolder && f2.isFolder -> 1
        !f1.isFolder && !f2.isFolder -> fileCompare(sortBy, f1, f2)
        else -> 0
    }
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

private fun fileCompare(sortBy: SortBy, f1: File, f2: File): Int {
    return when (sortBy) {
        SortBy.NAME -> f1.name.compareTo(f2.name)
        SortBy.TYPE -> f1.type.compareTo(f2.type)
        SortBy.LAST_MODIFIED -> f1.lastModified.compareTo(f2.lastModified)
        SortBy.SIZE -> f1.size.compareTo(f2.size)
    }
}