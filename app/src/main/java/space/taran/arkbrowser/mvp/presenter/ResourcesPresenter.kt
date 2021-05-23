package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.view.ResourcesView
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.mvp.model.repo.*
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.RESOURCES_SCREEN
import space.taran.arkbrowser.utils.Tags
import java.nio.file.Path
import javax.inject.Inject

//todo: @InjectViewState
class ResourcesPresenter(
    val root: Path?,
    private val prefix: Path?)
    : MvpPresenter<ResourcesView>() {

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexFactory: ResourcesIndexFactory

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage

    private lateinit var grid: ResourcesGrid


    fun listTagsForAllResources(): Tags = grid.items()
        .flatMap { storage.listTags(it) }
        .toSet()

    fun listUntaggedResources(): Set<ResourceId> =
        index.listIds(prefix).toSet()
            .minus(storage.listResources())


    override fun onFirstViewAttach() {
        Log.d(RESOURCES_SCREEN, "first view attached in ResourcesPresenter")
        super.onFirstViewAttach()

        val folders = foldersRepo.query()
        Log.d(RESOURCES_SCREEN, "folders retrieved: $folders")

        Notifications.notifyIfFailedPaths(viewState, folders.failed)

        val roots: List<Path> = {
            val all = folders.succeeded.keys
            if (root != null) {
                if (!all.contains(root)) {
                    throw AssertionError("Requested root wasn't found in DB")
                }

                listOf(root)
            } else {
                all.toList()
            }
        }()
        Log.d(RESOURCES_SCREEN, "using roots $roots")

        val rootToIndex = roots
            .map { it to resourcesIndexFactory.loadFromDatabase(it) }
            .toMap()

        val rootToStorage = roots
            .map { it to PlainTagsStorage.provide(it) }
            .toMap()

        roots.forEach { root ->
            val storage = rootToStorage[root]!!

            val indexed = rootToIndex[root]!!.listIds(null)
            val tagged = storage.listResources()

            //todo: when async indexing will be ready, tagged ids must be boosted
            //in the indexing queue and be removed only if they fail to be indexed
            storage.forgetResources(tagged - indexed.toSet())
        }

        index = AggregatedResourcesIndex(rootToIndex.values)
        storage = AggregatedTagsStorage(rootToStorage.values)

        //todo: with async indexing we must display non-indexed-yet resources too
        grid = ResourcesGrid(index, index.listIds(prefix))
        viewState.init(grid)

        val title = {
            val path = (prefix ?: root)
            if (path != null) "$path, " else ""
        }()

        viewState.setToolbarTitle("$title${roots.size} of roots chosen")
    }

    override fun onDestroy() {
        Log.d(RESOURCES_SCREEN, "destroying ResourcesPresenter")
        super.onDestroy()
    }
}