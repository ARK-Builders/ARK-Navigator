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

        fun tagsFromString(string: String, strict: Boolean = true): Tags =
            tagsListFromString(string, strict)
                .toSet()

        fun tagsListFromString(string: String, strict: Boolean = true): List<Tag> = string
            .split(',')
            .map { it.trim() }
            .filter {
                if (it.isEmpty()) {
                    if (strict) {
                        throw AssertionError("No tag can be empty")
                    }
                    false
                }
                true
            }
    }
}