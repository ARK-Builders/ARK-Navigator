package space.taran.arknavigator.utils

typealias Tag = String

typealias Tags = Set<Tag>

object TagUtils {
    fun validateTag(tag: Tag): Tag? {
        val validated = tag.trim()
        if (validated.isEmpty()) return null
        return validated
    }
}

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
