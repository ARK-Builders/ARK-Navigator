package space.taran.arkbrowser.mvp.presenter

import androidx.documentfile.provider.DocumentFile
import space.taran.arkbrowser.mvp.model.entity.Resource
import space.taran.arkbrowser.mvp.model.entity.common.Icon
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.common.TagState
import space.taran.arkbrowser.mvp.model.repo.RootsRepo
import space.taran.arkbrowser.mvp.model.repo.ResourcesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IItemGridPresenter
import space.taran.arkbrowser.mvp.view.TagsView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.entity.common.IconOrImage
import space.taran.arkbrowser.utils.*
import javax.inject.Inject

class TagsPresenter(val rootName: String?, val allResources: Set<Resource>) :
    MvpPresenter<TagsView>() {

    enum class State {
        SINGLE_ROOT, FILES, ALL_ROOTS
    }

    @Inject
    lateinit var syncRepo: RootsRepo

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var filesRepo: ResourcesRepo

    val fileGridPresenter = ItemGridPresenter()
    var syncDisposable: Disposable? = null
    var tagStates = mutableListOf<TagState>()
    var sortBy = SortBy.NAME
    var isReversedSort = false

    var selectedResources = setOf<Resource>()

    var isTagsOff = false

    inner class ItemGridPresenter :
        IItemGridPresenter {

        var resources = mutableListOf<Resource>()

        override fun getCount() = resources.size

        override fun bindView(view: FileItemView) {
            val resource = resources[view.pos]
            view.setText(resource.name)
            if (resource.isImage()) {
                view.setIcon(IconOrImage(image = resource.file))
            } else {
                view.setIcon(IconOrImage(icon = Icon.FILE))
            }
        }

        override fun itemClicked(pos: Int) {
            val resource = resources[pos]
            if (resource.isImage()) {
                val images = selectedResources.filter { it.isImage() }
                val newPos = images.indexOf(resource)
                router.navigateTo(
                    Screens.DetailScreen(
                        syncRepo.getRootById(resource.rootId!!)!!,
                        images,
                        newPos
                    )
                )
            } else {
                viewState.openFile(
                    filesRepo.fileDataSource.getUriForFileByProvider(resource.file),
                    DocumentFile.fromFile(resource.file).type!!
                )
            }
        }
    }

    fun tagChecked(tag: String, isChecked: Boolean) {
        val tagState = tagStates.find { tagState -> tagState.tag == tag }
        tagState!!.isChecked = isChecked
        applyTagsToFiles()
    }

    fun clearTagsChecked() {
        tagStates.forEach { tagState ->
            tagState.isChecked = false
        }
        applyTagsToFiles()
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
        selectedResources = allResources
        applyTagsToFiles()

        viewState.setToolbarTitle(rootName ?: "All resources")
    }

    private fun initFromRoot() {


        viewState.setToolbarTitle(rootName)
        syncRepo.getSyncObservable(root)?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(getSyncObserver(root))
    }

    private fun initFromFiles() {
        viewState.setToolbarTitle(root!!.name)

        val buffer = mutableListOf<Resource>()
        resources!!.forEach { file ->
            root.resources.forEach { rootFile ->
                if (file.path == rootFile.path)
                    buffer.add(rootFile)
            }
        }
        allResources = buffer.toSet()

        selectedResources = allResources
        applyTagsToFiles()
    }

    private fun initFromAllRoots() {
        viewState.setToolbarTitle("All roots")

        val buffer = mutableListOf<Resource>()
        syncRepo.roots.forEach { root ->
            buffer.addAll(root.resources)
            syncRepo.getSyncObservable(root)?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(getSyncObserver(root))
        }
        allResources = buffer.toSet()

        selectedResources = allResources
        applyTagsToFiles()
    }

    override fun onDestroy() {
        super.onDestroy()
        syncDisposable?.dispose()
    }

    fun onViewResumed() {
        if (isTagsOff) {
            findUntaggedFiles()
        } else {
            setupTags()
        }
    }

    fun sortByChanged(sortBy: SortBy) {
        this.sortBy = sortBy
        sortAndUpdateFiles()
        dismissDialog()
    }

    fun reversedSortChanged(isReversedSort: Boolean) {
        this.isReversedSort = isReversedSort
        sortAndUpdateFiles()
        dismissDialog()
    }

    fun tagsOffChanged() {
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
        viewState.showSortByDialog(sortBy, isReversedSort)
    }

    fun dismissDialog() {
        viewState.closeSortByDialog()
    }

    private fun applyTagsToFiles() {
        tagStates.forEach { tagState ->
            tagState.isActual = false
        }

        if (tagStates.none { tagState -> tagState.isChecked }) {
            selectedResources.clear()
            selectedResources.addAll(allResources)

            sortAndUpdateFiles()
            sortAndUpdateTags()
            return
        }

        val filteredFiles = mutableListOf<Resource>()
        allResources.forEach allFiles@{ file ->
            var isFileFit = true
            tagStates.forEach { tagState ->
                if (tagState.isChecked) {
                    if (!file.tags.contains(tagState.tag))
                        isFileFit = false
                }
            }
            if (isFileFit) filteredFiles.add(file)
        }

        filteredFiles.forEach { file ->
            tagStates.forEach { tagState ->
                if (file.tags.contains(tagState.tag))
                    tagState.isActual = true
            }
        }

        selectedResources.clear()
        selectedResources.addAll(filteredFiles)
        sortAndUpdateFiles()
        sortAndUpdateTags()
    }

    private fun findUntaggedFiles() {
        val filteredFiles = allResources.filter { file ->
            file.tags.isEmpty()
        }

        selectedResources.clear()
        selectedResources.addAll(filteredFiles)
        sortAndUpdateFiles()
        tagStates.clear()
        sortAndUpdateTags()
    }

    private fun getSyncObserver(root: Root) = object : Observer<Resource> {
        override fun onSubscribe(d: Disposable?) {
            syncDisposable = d
        }

        override fun onNext(syncResource: Resource) {

        }

        override fun onError(e: Throwable?) {}

        override fun onComplete() {}
    }

    private fun sortAndUpdateFiles() {
        selectedResources.sortWith(resourceComparator(sortBy, isReversedSort))
        fileGridPresenter.resources.clear()
        fileGridPresenter.resources.addAll(selectedResources)
        viewState.updateAdapter()
    }

    private fun sortAndUpdateTags() {
        tagStates.sortWith(tagsComparator())
        viewState.clearTags()
        viewState.setTags(tagStates)
    }

    private fun setupTags() {
        val filesTags = HashSet<Tag>()
        allResources.forEach { file ->
            file.tags.forEach { tag ->
                filesTags.add(tag)
            }
        }
        val currentTags = tagStates.map { it.tag }.toSet()
        val newTags = filesTags.subtract(currentTags)
        val deletedTags = currentTags.subtract(filesTags)

        newTags.forEach { tag ->
            tagStates.add(TagState(tag, false, false))
        }

        deletedTags.forEach { tag ->
            var index: Int? = null
            tagStates.forEachIndexed { i, tagState ->
                if (tagState.tag == tag) {
                    index = i
                    return@forEachIndexed
                }
            }
            index?.let { tagStates.removeAt(it) }
        }

        sortAndUpdateTags()
    }

}