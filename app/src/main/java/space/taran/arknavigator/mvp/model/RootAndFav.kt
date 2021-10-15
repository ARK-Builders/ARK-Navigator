package space.taran.arknavigator.mvp.model

import java.nio.file.Path

data class RootAndFav (
    var root: Path?,
    var fav: Path?
) {

    fun isAllRoots(): Boolean {
        return root == null && fav == null
    }
}