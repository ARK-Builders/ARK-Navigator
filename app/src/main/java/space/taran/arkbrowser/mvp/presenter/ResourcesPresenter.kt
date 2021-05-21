package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.common.TagState
import space.taran.arkbrowser.mvp.view.ResourcesView
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import android.util.Log
import space.taran.arkbrowser.mvp.model.repo.*
import space.taran.arkbrowser.ui.fragments.utils.Notifications
import space.taran.arkbrowser.utils.RESOURCES_SCREEN
import space.taran.arkbrowser.utils.tagsComparator
import java.nio.file.Path
import javax.inject.Inject

//todo: @InjectViewState
class ResourcesPresenter(val root: Path?, val prefix: Path?) :
    MvpPresenter<ResourcesView>() {

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var foldersRepo: FoldersRepo

    @Inject
    lateinit var resourcesIndexFactory: ResourcesIndexFactory

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage

    private lateinit var grid: ResourcesGrid

    private var tagStates = mutableListOf<TagState>()

    private var isTagsOff = false

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
            val tagged = storage.listIds()

            //todo: when async indexing will be ready, tagged ids must be boosted
            //in the indexing queue and be removed only if they fail to be indexed
            storage.removeIds(tagged - indexed.toSet())
        }

        index = AggregatedResourcesIndex(rootToIndex.values)
        storage = AggregatedTagsStorage(rootToStorage.values)

        //todo: with async indexing we must display non-indexed-yet resources too
        grid = ResourcesGrid(index, index.listIds(prefix))
        viewState.init(grid)

        applyTagsToFiles()

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

    fun onViewResumed() {
        Log.d(RESOURCES_SCREEN, "view resumed in ResourcesPresenter")
        if (isTagsOff) {
            findUntaggedFiles()
        } else {
            setupTags()
        }
    }


    fun tagChecked(tag: String, isChecked: Boolean) {
        Log.d(RESOURCES_SCREEN, "tag checked clicked in ResourcesPresenter")
        val tagState = tagStates.find { tagState -> tagState.tag == tag }
        tagState!!.isChecked = isChecked
        applyTagsToFiles()
    }

    fun clearTagsChecked() {
        Log.d(RESOURCES_SCREEN, "clearing checked tags in ResourcesPresenter")
        tagStates.forEach { tagState ->
            tagState.isChecked = false
        }
        applyTagsToFiles()
    }

    fun tagsOffChanged() {
        Log.d(RESOURCES_SCREEN, "tags on/off changed in ResourcesPresenter")
        isTagsOff = !isTagsOff
        if (isTagsOff) {
            findUntaggedFiles()
            viewState.setTagsLayoutVisibility(false)
        } else {
            setupTags()
            applyTagsToFiles()
            viewState.setTagsLayoutVisibility(true)
        }
    }

    private fun applyTagsToFiles() {
        Log.d(RESOURCES_SCREEN, "[mock] applying tags to resources in ResourcesPresenter")

        tagStates.forEach { tagState ->
            tagState.isActual = false
        }

        if (tagStates.none { tagState -> tagState.isChecked }) {
//            displayedResources.clear()
//            displayedResources.addAll(allResources)

//            sortAndUpdateFiles()
            sortAndUpdateTags()
            return
        }

//        val filteredFiles = mutableListOf<Resource>()
//        allResources.forEach allFiles@{ file ->
//            var isFileFit = true
//            tagStates.forEach { tagState ->
//                if (tagState.isChecked) {
//                    if (!file.tags.contains(tagState.tag))
//                        isFileFit = false
//                }
//            }
//            if (isFileFit) filteredFiles.add(file)
//        }
//
//        filteredFiles.forEach { file ->
//            tagStates.forEach { tagState ->
//                if (file.tags.contains(tagState.tag))
//                    tagState.isActual = true
//            }
//        }
//
//        displayedResources.clear()
//        displayedResources.addAll(filteredFiles)
//        sortAndUpdateFiles()
//        sortAndUpdateTags()
    }

    private fun findUntaggedFiles() {
        Log.d(RESOURCES_SCREEN, "[mock] looking for untagged resources in ResourcesPresenter")

//        val filteredFiles = allResources.filter { file ->
//            file.tags.isEmpty()
//        }
//
//        displayedResources.clear()
//        displayedResources.addAll(filteredFiles)
//        sortAndUpdateFiles()
//        tagStates.clear()
//        sortAndUpdateTags()
    }

    private fun sortAndUpdateTags() {
        Log.d(RESOURCES_SCREEN, "[mock] sorting and updating tags in ResourcesPresenter")
        tagStates.sortWith(tagsComparator())
        viewState.clearTags()
        viewState.setTags(tagStates)
    }

    private fun setupTags() {
        Log.d(RESOURCES_SCREEN, "[mock] setting up tags in ResourcesPresenter")

//        val filesTags = HashSet<Tag>()
//        allResources.forEach { file ->
//            file.tags.forEach { tag ->
//                filesTags.add(tag)
//            }
//        }
//        val currentTags = tagStates.map { it.tag }.toSet()
//        val newTags = filesTags.subtract(currentTags)
//        val deletedTags = currentTags.subtract(filesTags)
//
//        newTags.forEach { tag ->
//            tagStates.add(TagState(tag, false, false))
//        }
//
//        deletedTags.forEach { tag ->
//            var index: Int? = null
//            tagStates.forEachIndexed { i, tagState ->
//                if (tagState.tag == tag) {
//                    index = i
//                    return@forEachIndexed
//                }
//            }
//            index?.let { tagStates.removeAt(it) }
//        }
//
//        sortAndUpdateTags()
    }
}