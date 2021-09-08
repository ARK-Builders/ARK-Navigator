package space.taran.arknavigator.utils.extensions

import space.taran.arknavigator.utils.Sorting

fun Int.convertToSorting(): Sorting =
    Sorting.values()[this]