package space.taran.arkbrowser.mvp.model.entity.room

import androidx.room.Entity
import space.taran.arkbrowser.utils.StringPath
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.io.File

@Entity(primaryKeys = ["root", "path"])
data class Folder(
    val root: StringPath,
    val relative: StringPath?
) {
    fun path(): File {
        val file = File("$root/$relative")
        if (!folderExists(file)) {
            throw IllegalStateException()
        }

        return file
    }

    fun isRoot(): Boolean =
        relative == null

    companion object {
        fun root(path: File): Folder =
            if (folderExists(path)) {
                Folder(path.canonicalPath, null)
            } else {
                throw IllegalArgumentException()
            }

        fun favorite(root: File, path: File): Folder =
            if (folderExists(root) && folderExists(path)) {
                val canonicalRoot = root.canonicalFile
                val canonicalPath = path.canonicalFile
                val relativePath = canonicalPath.relativeTo(canonicalRoot)
                Folder(canonicalRoot.toString(), relativePath.toString())
            } else {
                throw IllegalArgumentException()
            }

        private fun folderExists(path: File): Boolean =
            path.exists() && path.isDirectory
    }
}