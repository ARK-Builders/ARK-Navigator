package space.taran.arknavigator.mvp.model.repo

import android.util.Log
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.taran.arknavigator.mvp.model.dao.Favorite
import space.taran.arknavigator.mvp.model.dao.FolderDao
import space.taran.arknavigator.mvp.model.dao.Root
import space.taran.arknavigator.utils.DATABASE
import space.taran.arknavigator.utils.FILES
import space.taran.arknavigator.utils.PartialResult
import space.taran.arknavigator.utils.fail
import space.taran.arknavigator.utils.folderExists
import space.taran.arknavigator.utils.ok

typealias Folders = Map<Path, List<Path>>

class FoldersRepo(private val dao: FolderDao) {

    private val provideMutex = Mutex()
    private lateinit var folders: Folders

    suspend fun provideFolders(): PartialResult<Folders, List<Path>> =
        withContext(Dispatchers.IO) {
            provideMutex.withLock {
                val result = if (!::folders.isInitialized) {
                    val foldersResult = query()
                    if (foldersResult.failed.isNotEmpty())
                        Log.w(
                            FILES,
                            "Failed to verify the following paths: \n ${
                            foldersResult.failed.joinToString("\n")}"
                        )

                    folders = foldersResult.succeeded
                    Log.d(DATABASE, "folders loaded: $folders")

                    foldersResult
                } else {
                    PartialResult(succeeded = folders, failed = listOf())
                }

                return@withContext result
            }
        }

    suspend fun resolveRoots(rootAndFav: RootAndFav): List<Path> {
        return if (!rootAndFav.isAllRoots())
            listOf(rootAndFav.root!!)
        else
            provideFolders().succeeded.keys.toList()
    }

    private suspend fun query(): PartialResult<Folders, List<Path>> =
        withContext(Dispatchers.IO) {
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
                missingPaths.toList()
            )
        }

    suspend fun insertRoot(root: Path) =
        withContext(Dispatchers.IO) {
            val entity = Root(root.toString())
            Log.d(DATABASE, "storing $entity")

            val mutableFolders = folders.toMutableMap()
            mutableFolders[root] = listOf()
            folders = mutableFolders

            dao.insert(entity)
        }

    suspend fun insertFavorite(root: Path, favorite: Path) =
        withContext(Dispatchers.IO) {
            val entity = Favorite(root.toString(), favorite.toString())
            Log.d(DATABASE, "storing $entity")

            val mutableFolders = folders.toMutableMap()
            val favsByRoot = mutableFolders[root]?.toMutableList()
            favsByRoot?.add(favorite)
            mutableFolders[root] = favsByRoot ?: listOf(favorite)
            folders = mutableFolders

            dao.insert(entity)
        }
}
