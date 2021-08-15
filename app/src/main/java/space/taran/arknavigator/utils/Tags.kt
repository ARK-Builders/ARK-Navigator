package space.taran.arknavigator.utils

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
        fun stringFromTags(tags: Tags): String = tags
            .joinToString(",")

        fun tagsFromString(string: String): Tags = string
            .split(',')
            .map { it.trim() }
//            .filter { it.isNotEmpty() }
            .filter {
                if (it.isEmpty()) throw AssertionError("No tag can be empty")
                true
            }
            .toSet()
    }
}

//todo