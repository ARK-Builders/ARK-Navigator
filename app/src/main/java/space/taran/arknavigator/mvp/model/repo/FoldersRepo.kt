package space.taran.arknavigator.mvp.model.repo

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.taran.arknavigator.mvp.model.arkFavorites
import space.taran.arknavigator.mvp.model.arkFolder
import space.taran.arknavigator.mvp.model.dao.Root
import space.taran.arknavigator.mvp.model.dao.RootDao
import space.taran.arknavigator.utils.LogTags.DATABASE
import space.taran.arknavigator.utils.LogTags.FILES
import space.taran.arknavigator.utils.PartialResult
import space.taran.arknavigator.utils.folderExists
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.writeText

typealias Folders = Map<Path, List<Path>>

class FoldersRepo(
    private val dao: RootDao,
) {
    private val provideMutex = Mutex()
    private lateinit var folders: Folders

    suspend fun provideFoldersWithMissing(): PartialResult<Folders, List<Path>> =
        withContext(Dispatchers.IO) {
            provideMutex.withLock {
                val result = if (!::folders.isInitialized) {
                    val foldersResult = query()
                    if (foldersResult.failed.isNotEmpty())
                        Log.w(
                            FILES,
                            "Failed to verify the following paths: \n ${
                            foldersResult.failed.joinToString("\n")
                            }"
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

    suspend fun provideFolders(): Folders =
        provideFoldersWithMissing().succeeded

    suspend fun resolveRoots(rootAndFav: RootAndFav): List<Path> {
        return if (!rootAndFav.isAllRoots())
            listOf(rootAndFav.root!!)
        else
            provideFolders().keys.toList()
    }

    private suspend fun query(): PartialResult<Folders, List<Path>> =
        withContext(Dispatchers.IO) {
            val missingPaths = mutableListOf<Path>()

            val roots = dao.query().mapNotNull {
                val root = Path(it.path)
                if (!folderExists(root)) {
                    missingPaths.add(root)
                    return@mapNotNull null
                }
                val arkFolder = root.arkFolder()
                if (!folderExists(arkFolder)) {
                    missingPaths.add(arkFolder)
                    return@mapNotNull null
                }
                root
            }

            val favoritesByRoot = roots.map { root ->
                val favoritesRelatives = readFavorites(root)
                val (valid, missing) = checkFavorites(root, favoritesRelatives)
                missingPaths.addAll(missing)
                root to valid
            }

            return@withContext PartialResult(
                favoritesByRoot.toMap(),
                missingPaths.toList()
            )
        }

    suspend fun insertRoot(root: Path) =
        withContext(Dispatchers.IO) {
            root.arkFolder().createDirectories()

            val entity = Root(root.toString())
            Log.d(DATABASE, "storing $entity")

            folders = folders + mapOf(root to readFavorites(root))

            dao.insert(entity)
        }

    suspend fun insertFavorite(root: Path, favoriteRelative: Path) =
        withContext(Dispatchers.IO) {
            val mutableFolders = folders.toMutableMap()
            val favsByRoot = mutableFolders[root]?.toMutableList()
            favsByRoot?.add(favoriteRelative)
            mutableFolders[root] = favsByRoot ?: listOf(favoriteRelative)
            folders = mutableFolders

            val favoritesRelatives = readFavorites(root).toMutableSet()
            favoritesRelatives.add(favoriteRelative)
            writeFavorites(root, favoritesRelatives)
        }

    private fun checkFavorites(
        root: Path,
        favoritesRelatives: List<Path>
    ): PartialResult<List<Path>, List<Path>> {
        val missingPaths = mutableListOf<Path>()

        val validFavoritesRelatives = favoritesRelatives.filter {
            val favorite = root.resolve(it)
            val valid = folderExists(favorite)
            if (!valid) missingPaths.add(it)
            valid
        }

        return PartialResult(validFavoritesRelatives, missingPaths)
    }

    private fun readFavorites(root: Path): List<Path> {
        val arkFolder = root.arkFolder()
        if (arkFolder.notExists()) error("Ark folder must exist")
        val favoritesFile = arkFolder.arkFavorites()

        return try {
            val jsonFavorites =
                Json.decodeFromStream<JsonFavorites>(favoritesFile.inputStream())
            jsonFavorites.favorites.map { Path(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeFavorites(root: Path, favoritesRelatives: Set<Path>) {
        val arkFolder = root.arkFolder()
        if (arkFolder.notExists()) error("Ark folder must exist")
        val favoritesFile = arkFolder.arkFavorites()

        val jsonFavorites = JsonFavorites(
            favoritesRelatives
                .map { it.toString() }
                .toSet()
        )

        val content = Json.encodeToString(jsonFavorites)
        favoritesFile.writeText(content)
    }
}

@Serializable
private data class JsonFavorites(val favorites: Set<String>)
