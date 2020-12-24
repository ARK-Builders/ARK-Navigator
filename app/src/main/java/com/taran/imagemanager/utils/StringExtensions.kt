package com.taran.imagemanager.utils

fun String.removeDuplicateTags(): String {
    val list = this.split(",")
    return list.toHashSet().joinToString(",")
}

fun String.addTag(tags: String): String {
    if (tags == "")
        return this

    return if (this == "")
        tags
    else
        "$this,$tags"
}

fun String.mapToTagList(): List<String> {
    return if (this.isNotEmpty())
        this.split(",")
    else
        listOf()
}

fun String.findNotDuplicateTags(tags: String): String {
    var list1 = this.split(",")
    var list2 = tags.split(",")

    if (list2.size > list1.size) {
        val temp = list1
        list1 = list2
        list2 = temp
    }

    var notDuplicateTags = ""

    list1.forEach { tag ->
        val foundTag = list2.find { it == tag }
        if (foundTag != null)
            notDuplicateTags = notDuplicateTags.addTag(tag)
    }

    return notDuplicateTags
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