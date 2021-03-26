package space.taran.arkbrowser.utils

fun String.addTag(tags: String): String {
    if (tags == "")
        return this

    return if (this == "")
        tags
    else
        "$this,$tags"
}

fun String.removeTag(tag: String): String {
    val tags = this.split(",").toMutableList()
    tags.remove(tag)
    return tags.joinToString(",")
}

fun String.mapToTagList(): List<String> {
    return if (this.isNotEmpty())
        this.split(",")
    else
        listOf()
}

fun String.findNewTags(tags: String): String {
    val list1 = this.split(",")
    val list2 = tags.split(",")

    if (list1[0] == "")
        return tags

    if (list2[0] == "")
        return this

    var newTags = ""

    list2.forEach { tag ->
        val foundTag = list1.find { it == tag }
        if (foundTag == null)
            newTags = newTags.addTag(tag)
    }

    return newTags
}
