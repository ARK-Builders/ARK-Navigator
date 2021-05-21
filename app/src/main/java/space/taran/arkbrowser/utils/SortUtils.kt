package space.taran.arkbrowser.utils

import space.taran.arkbrowser.mvp.model.entity.common.TagState
import java.nio.file.Files
import java.nio.file.Path

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