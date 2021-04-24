package space.taran.arkbrowser.mvp.model.repo

import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.room.dao.RootDao
import space.taran.arkbrowser.utils.*

import java.io.File
import java.lang.IllegalArgumentException

class RootsRepo(private val dao: RootDao) {
    //todo: Room cache

    fun getRoots(): List<Root> = dao.getAll()
        .map { mapRootFromRoom(it) }

    fun getRootById(id: Long): Root? {
        val root = dao.getById(id)
        return if (root != null) {
            mapRootFromRoom(root)
        } else {
            null
        }
    }

    fun getRootByFile(file: File): Root? {
        val roots = getRoots()
        return roots.find { file.startsWith(it.folder) }
        //todo fs.normalize `path` before check
    }

    fun insertRoot(root: Root): Root {
        if (root.id != 0L) {
            throw IllegalArgumentException("Root ID will be generated automatically")
        }

        val id = dao.insert(mapRootToRoom(root))
        root.id = id
        return root
    }
}