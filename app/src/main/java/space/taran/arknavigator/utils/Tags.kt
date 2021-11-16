package space.taran.arknavigator.utils

typealias Tag = String

typealias Tags = Set<Tag>

class Constants {
    companion object {
        val NO_TAGS: Tags = emptySet()
    }
}

class Converters {
    companion object {
        fun stringFromTags(tags: Tags): String = tags
            .joinToString(",")

        fun tagsFromString(string: String): Tags = string
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}