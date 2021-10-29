package space.taran.arknavigator.mvp.model

import java.nio.file.Path

data class RootAndFav (
    val root: Path?,
    val fav: Path?
) {
    init {
        if (root == null && fav != null)
            throw AssertionError("Combination null root and not null fav isn't allowed")
    }

    fun isAllRoots(): Boolean {
        return root == null
    }
}