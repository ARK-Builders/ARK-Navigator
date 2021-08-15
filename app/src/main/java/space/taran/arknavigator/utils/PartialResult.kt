package space.taran.arknavigator.utils

data class PartialResult<S,F>(val succeeded: S, val failed: F)