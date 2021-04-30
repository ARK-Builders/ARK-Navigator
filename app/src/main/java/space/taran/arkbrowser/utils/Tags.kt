package space.taran.arkbrowser.utils

import androidx.room.TypeConverter

typealias Tag = String

typealias Tags = Set<Tag>

class Constants {
    companion object {
        const val EMPTY_TAG: Tag = ""
        val NO_TAGS: Tags = HashSet()
    }
}

class Converters {
    companion object {
        @JvmStatic
        @TypeConverter
        fun stringFromTags(tags: Tags): String = tags
            .joinToString(",")

        @JvmStatic
        @TypeConverter
        fun tagsFromString(string: String): Tags = string
            .split(',')
            .filter { tag -> tag.isNotEmpty() }
            .toSet()
    }
}