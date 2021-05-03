package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.common.TagState
import java.nio.file.Files
import java.nio.file.Path

enum class SortBy{
    NAME, SIZE, LAST_MODIFIED, TYPE
}

val fileComparator = Comparator<Path> { f1, f2 -> compareFiles(f1, f2) }

val markableFileComparator = Comparator<MarkableFile> { (_, f1), (_, f2) ->
    compareFiles(f1, f2)
}

fun resourceComparator(sortBy: SortBy, inverse: Boolean = false) = Comparator<Path> { r1, r2 ->
    val result = when (sortBy) {
        SortBy.NAME -> r1.fileName.compareTo(r2.fileName)
        SortBy.TYPE -> extension(r1).compareTo(extension(r2))
        SortBy.LAST_MODIFIED -> Files.getLastModifiedTime(r1).compareTo(Files.getLastModifiedTime(r2))
        SortBy.SIZE -> Files.size(r1).compareTo(Files.size(r2))
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

private fun compareFiles(f1: Path, f2: Path) = when {
    Files.isDirectory(f1) && Files.isDirectory(f2) -> f1.fileName.compareTo(f2.fileName)
    !Files.isDirectory(f1) && !Files.isDirectory(f2) -> f1.fileName.compareTo(f2.fileName)
    Files.isDirectory(f1) && !Files.isDirectory(f2) -> -1
    !Files.isDirectory(f1) && Files.isDirectory(f2) -> 1
    else -> 0
}

//todo