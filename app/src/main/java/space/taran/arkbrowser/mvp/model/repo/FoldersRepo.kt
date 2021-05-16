package space.taran.arkbrowser.mvp.model.repo

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.room.Favorite
import space.taran.arkbrowser.mvp.model.entity.room.FolderDao
import space.taran.arkbrowser.mvp.model.entity.room.Root
import space.taran.arkbrowser.utils.*
import java.nio.file.Path
import java.nio.file.Paths

import java.lang.AssertionError

typealias Folders = Map<Path, List<Path>>

class FoldersRepo(private val dao: FolderDao) {

    fun query(): PartialResult<Folders, List<Path>> {
        val missingPaths = mutableListOf<Path>()

        val rootsWithFavorites = CoroutineRunner.runAndBlock {
            dao.query()
        }

        val validPaths = rootsWithFavorites
            .flatMap {
                Log.d(DATABASE, "retrieved $it")

                val root = Paths.get(it.root.path)

                if (!folderExists(root)) {
                    missingPaths.add(root)
                    fail()
                } else {
                    val favorites = it.favorites.flatMap { favorite ->
                        if (favorite.root != it.root.path) {
                            throw AssertionError("foreign key violation")
                        }

                        val folder = root.resolve(favorite.relative)

                        if (!folderExists(folder)) {
                            missingPaths.add(folder)
                            fail()
                        } else {
                            ok(Paths.get(favorite.relative))
                        }
                    }

                    ok(root to favorites)
                }
            }

        return PartialResult(
            validPaths.toMap(),
            missingPaths.toList())
    }

    fun insertRoot(path: Path) {
        val entity = Root(path.toString())
        Log.d(DATABASE, "storing $entity")

        CoroutineRunner.runAndBlock {
            dao.insert(entity)
        }
    }

    fun insertFavorite(root: Path, favorite: Path) {
        val entity = Favorite(root.toString(), favorite.toString())

        Log.d(DATABASE, "storing $entity")
        CoroutineRunner.runAndBlock {
            dao.insert(entity)
        }
    }
}