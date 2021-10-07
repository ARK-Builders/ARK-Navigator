package space.taran.arknavigator.mvp.model.repo

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.dao.Favorite
import space.taran.arknavigator.mvp.model.dao.FolderDao
import space.taran.arknavigator.mvp.model.dao.Root
import space.taran.arknavigator.utils.*
import java.nio.file.Path
import java.nio.file.Paths

import java.lang.AssertionError

typealias Folders = Map<Path, List<Path>>

class FoldersRepo(private val dao: FolderDao) {

    suspend fun query(): PartialResult<Folders, List<Path>> = withContext(Dispatchers.IO) {
        val missingPaths = mutableListOf<Path>()

        val rootsWithFavorites = dao.query()

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
                            throw AssertionError("Foreign key violation")
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

        return@withContext PartialResult(
            validPaths.toMap(),
            missingPaths.toList())
    }

    suspend fun insertRoot(path: Path) = withContext(Dispatchers.IO) {
        val entity = Root(path.toString())
        Log.d(DATABASE, "storing $entity")
        dao.insert(entity)
    }

    suspend fun insertFavorite(root: Path, favorite: Path) = withContext(Dispatchers.IO) {
        val entity = Favorite(root.toString(), favorite.toString())
        Log.d(DATABASE, "storing $entity")
        dao.insert(entity)
    }
}