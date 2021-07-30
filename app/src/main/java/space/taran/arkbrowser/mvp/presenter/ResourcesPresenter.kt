package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.view.ResourcesView
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import android.util.Log
import space.taran.arkbrowser.mvp.model.dao.ResourceId
import space.taran.arkbrowser.mvp.model.repo.*
import space.taran.arkbrowser.mvp.presenter.adapter.ResourcesList
import space.taran.arkbrowser.navigation.Screens
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.RESOURCES_SCREEN
import space.taran.arkbrowser.utils.Tags
import java.nio.file.Path
import javax.inject.Inject

//todo: @InjectViewState ?
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

    private lateinit var resources: List<ResourceId>


    fun listTagsForAllResources(): Tags = resources
        .flatMap { storage.getTags(it) }
        .toSet()

    fun listUntaggedResources(): Set<ResourceId> = resources
        .toSet()
        .minus(storage.listTaggedResources())

    fun createTagsSelector(): TagsSelector? {
        val tags = listTagsForAllResources()
        Log.d(RESOURCES_SCREEN, "tags loaded: $tags")

        return TagsSelector(tags, resources.toSet(), storage)
    }


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
            .map { it to PlainTagsStorage.provide(it, rootToIndex[it]!!.listAllIds()) }
            .toMap()

        //todo: when async indexing will be ready, tagged ids must be boosted
        //in the indexing queue and be removed if they fail to be indexed

        roots.forEach { root ->
            val storage = rootToStorage[root]!!
            val indexed = rootToIndex[root]!!.listAllIds()

            storage.cleanup(indexed)
        }

        index = AggregatedResourcesIndex(rootToIndex.values)
        storage = AggregatedTagsStorage(rootToStorage.values)

        //todo: with async indexing we must display non-indexed-yet resources too
        resources = index.listIds(prefix)

        viewState.init(provideResources())

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

    fun provideResources(): ResourcesList =
        ResourcesList(index, resources) { position, resource ->
            Log.d(RESOURCES_SCREEN, "resource $resource at $position clicked ItemGridPresenter")

            //todo not `this.resources` but `somewhere.resourcesSelection`
            router.navigateTo(Screens.GalleryScreen(index, storage, resources, position))

            //todo: long-press handler
            //        viewState.openFile(
            //            filesRepo.fileDataSource.getUriForFileByProvider(resource.file),
            //            DocumentFile.fromFile(resource.file).type!!)

            //in view:
            //    fun openFile(uri: Uri, mimeType: String) {
            //        Log.d(RESOURCES_SCREEN, "opening file $uri in ResourcesFragment")
            //        try {
            //            val intent = Intent(Intent.ACTION_VIEW)
            //            intent.setDataAndType(uri, mimeType)
            //            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            //            startActivity(intent)
            //        } catch (e: Exception) {
            //            Toast.makeText(context, "No app can handle this file", Toast.LENGTH_SHORT).show()
            //        }
            //    }
        }
}