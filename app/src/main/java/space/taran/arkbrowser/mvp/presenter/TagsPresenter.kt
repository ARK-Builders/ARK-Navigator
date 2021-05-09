package space.taran.arkbrowser.mvp.presenter

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.common.TagState
import space.taran.arkbrowser.mvp.view.TagsView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.entity.room.ResourceId
import space.taran.arkbrowser.mvp.presenter.adapter.ItemGridPresenter
import space.taran.arkbrowser.utils.*
import javax.inject.Inject

class TagsPresenter(val allResources: Set<ResourceId>) :
    MvpPresenter<TagsView>() {

    enum class State {
        SINGLE_ROOT, FILES, ALL_ROOTS
    }

    @Inject
    lateinit var router: Router

    val fileGridPresenter = XItemGridPresenter()
//    var syncDisposable: Disposable? = null
    var tagStates = mutableListOf<TagState>()
    var sortBy = SortBy.NAME
    var isReversedSort = false

    var selectedResources = setOf<ResourceId>()

    var isTagsOff = false

    inner class XItemGridPresenter :
        ItemGridPresenter<Unit, ResourceId>({
            Log.d("flow", "[mock] item $it clicked in TagsPresenter/ItemGridPresenter")
//            if (resource.isImage()) {
//                val images = selectedResources.filter { it.isImage() }
//                val newPos = images.indexOf(resource)
//                router.navigateTo(
//                    Screens.DetailScreen(
//                        syncRepo.getRootById(resource.rootId!!)!!,
//                        images,
//                        newPos
//                    )
//                )
//            } else {
//                viewState.openFile(
//                    filesRepo.fileDataSource.getUriForFileByProvider(resource.file),
//                    DocumentFile.fromFile(resource.file).type!!
//                )
//            }
        }) {

        var resources = mutableListOf<ResourceId>()

        override fun items() = resources //todo

        override fun updateItems(label: Unit, items: List<ResourceId>) {
            //TODO
        }

        override fun bindView(view: FileItemView) {
            val resource = resources[view.pos]
            Log.d("flow", "[mock] binding view with $resource in TagsPresenter/ItemGridPresenter")
//            view.setText(resource.name)
//            if (resource.isImage()) {
//                view.setIcon(IconOrImage(image = resource.file))
//            } else {
//                view.setIcon(IconOrImage(icon = Icon.FILE))
//            }
        }

        override fun backClicked(): Unit {
            TODO("Not yet implemented")
        }
    }

    fun tagChecked(tag: String, isChecked: Boolean) {
        Log.d("flow", "tag checked clicked in TagsPresenter")
        val tagState = tagStates.find { tagState -> tagState.tag == tag }
        tagState!!.isChecked = isChecked
        applyTagsToFiles()
    }

    fun clearTagsChecked() {
        Log.d("flow", "clearing checked tags in TagsPresenter")
        tagStates.forEach { tagState ->
            tagState.isChecked = false
        }
        applyTagsToFiles()
    }

    override fun onFirstViewAttach() {
        Log.d("flow", "first view attached in TagsPresenter")
        super.onFirstViewAttach()
        viewState.init()
        selectedResources = allResources
        applyTagsToFiles()

//        viewState.setToolbarTitle(rootName ?: "All resources")
    }

//    private fun initFromRoot() {
//
//        viewState.setToolbarTitle(rootName)
//        syncRepo.getSyncObservable(root)?.observeOn(AndroidSchedulers.mainThread())
//            ?.subscribe(getSyncObserver(root))
//    }
//
//    private fun initFromFiles() {
//        viewState.setToolbarTitle(root!!.name)
//
//        val buffer = mutableListOf<Resource>()
//        resources!!.forEach { file ->
//            root.resources.forEach { rootFile ->
//                if (file.path == rootFile.path)
//                    buffer.add(rootFile)
//            }
//        }
//        allResources = buffer.toSet()
//
//        selectedResources = allResources
//        applyTagsToFiles()
//    }
//
//    private fun initFromAllRoots() {
//        viewState.setToolbarTitle("All roots")
//
//        val buffer = mutableListOf<Resource>()
//        syncRepo.roots.forEach { root ->
//            buffer.addAll(root.resources)
//            syncRepo.getSyncObservable(root)?.observeOn(AndroidSchedulers.mainThread())
//                ?.subscribe(getSyncObserver(root))
//        }
//        allResources = buffer.toSet()
//
//        selectedResources = allResources
//        applyTagsToFiles()
//    }

    override fun onDestroy() {
        Log.d("flow", "destroying TagsPresenter")
        super.onDestroy()
        //syncDisposable?.dispose()
    }

    fun onViewResumed() {
        Log.d("flow", "view resumed in TagsPresenter")
        if (isTagsOff) {
            findUntaggedFiles()
        } else {
            setupTags()
        }
    }

    fun sortByChanged(sortBy: SortBy) {
        Log.d("flow", "sorting by changed date in TagsPresenter")
        this.sortBy = sortBy
        sortAndUpdateFiles()
        dismissDialog()
    }

    fun reversedSortChanged(isReversedSort: Boolean) {
        Log.d("flow", "reversed sort changed in TagsPresenter")
        this.isReversedSort = isReversedSort
        sortAndUpdateFiles()
        dismissDialog()
    }

    fun tagsOffChanged() {
        Log.d("flow", "tags on/off changed in TagsPresenter")
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

    fun sortByMenuClicked() {
        Log.d("flow", "sort-by menu clicked in TagsPresenter")
        viewState.showSortByDialog(sortBy, isReversedSort)
    }

    fun dismissDialog() {
        Log.d("flow", "dialog dismissed in TagsPresenter")
        viewState.closeSortByDialog()
    }

    private fun applyTagsToFiles() {
        Log.d("flow", "[mock] applying tags to resources in TagsPresenter")

        tagStates.forEach { tagState ->
            tagState.isActual = false
        }

        if (tagStates.none { tagState -> tagState.isChecked }) {
//            selectedResources.clear()
//            selectedResources.addAll(allResources)

            sortAndUpdateFiles()
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
//        selectedResources.clear()
//        selectedResources.addAll(filteredFiles)
//        sortAndUpdateFiles()
//        sortAndUpdateTags()
    }

    private fun findUntaggedFiles() {
        Log.d("flow", "[mock] looking for untagged resources in TagsPresenter")

//        val filteredFiles = allResources.filter { file ->
//            file.tags.isEmpty()
//        }
//
//        selectedResources.clear()
//        selectedResources.addAll(filteredFiles)
//        sortAndUpdateFiles()
//        tagStates.clear()
//        sortAndUpdateTags()
    }

//    private fun getSyncObserver(root: remove_Root) = object : Observer<Resource> {
//        override fun onSubscribe(d: Disposable?) {
//            syncDisposable = d
//        }
//
//        override fun onNext(syncResource: Resource) {
//
//        }
//
//        override fun onError(e: Throwable?) {}
//
//        override fun onComplete() {}
//    }

    private fun sortAndUpdateFiles() {
        Log.d("flow", "[mock] sorting and updating resources in TagsPresenter")
//        selectedResources.sortWith(resourceComparator(sortBy, isReversedSort))
//        fileGridPresenter.resources.clear()
//        fileGridPresenter.resources.addAll(selectedResources)
//        viewState.updateAdapter()
    }

    private fun sortAndUpdateTags() {
        Log.d("flow", "[mock] sorting and updating tags in TagsPresenter")
        tagStates.sortWith(tagsComparator())
        viewState.clearTags()
        viewState.setTags(tagStates)
    }

    private fun setupTags() {
        Log.d("flow", "[mock] setting up tags in TagsPresenter")

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