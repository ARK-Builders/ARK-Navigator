package space.taran.arknavigator.utils

fun <T> ok(value: T): List<T> = listOf(value)

fun <T> fail(): List<T> = emptyList()
