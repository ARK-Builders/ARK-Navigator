package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.common.Icons
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.common.TagState
import space.taran.arkbrowser.mvp.model.repo.SynchronizeRepo
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IFileGridPresenter
import space.taran.arkbrowser.mvp.view.TagsView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.utils.*
import javax.inject.Inject

class TagsPresenter(val root: Root?, val files: List<File>?, val state: State) :
    MvpPresenter<TagsView>() {

    enum class State {
        SINGLE_ROOT, FILES, ALL_ROOTS
    }

    @Inject
    lateinit var syncRepo: SynchronizeRepo

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var filesRepo: FilesRepo

    val fileGridPresenter = FileGridPresenter()
    var syncDisposable: Disposable? = null
    var tagStates = mutableListOf<TagState>()
    var allFiles = mutableListOf<File>()
    var sortBy = SortBy.NAME
    var isReversedSort = false
    var displayFiles = mutableListOf<File>()

    var isTagsOff = false

    inner class FileGridPresenter :
        IFileGridPresenter {

        var files = mutableListOf<File>()

        override fun getCount() = files.size

        override fun bindView(view: FileItemView) {
            val file = files[view.pos]
            view.setText(file.name)
            if (file.isImage())
                view.setIcon(Icons.IMAGE, file.path)
            else
                view.setIcon(Icons.FILE, file.path)
        }

        override fun onCardClicked(pos: Int) {
            val file = files[pos]
            if (file.isImage()) {
                val sortedFiles = if (isReversedSort) displayFiles.reversed() else displayFiles
                val images = sortedFiles.filter { it.isImage() }
                val newPos = images.indexOf(file)
                router.navigateTo(
                    Screens.DetailScreen(
                        syncRepo.getRootForId(file.rootId!!)!!,
                        images,
                        newPos
                    )
                )
            } else
                viewState.openFile(
                    filesRepo.fileDataSource.getUriForFileByProvider(file.path),
                    filesRepo.documentDataSource.getMimeType(file.path)
                )
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
        when (state) {
            State.SINGLE_ROOT -> initFromRoot()
            State.FILES -> initFromFiles()
            State.ALL_ROOTS -> initFromAllRoots()
        }
    }

    private fun initFromRoot() {
        allFiles.clear()
        allFiles.addAll(root!!.files)
        displayFiles.clear()
        displayFiles.addAll(allFiles)
        applyTagsToFiles()
        viewState.setToolbarTitle(root.name)
        syncRepo.getSyncObservable(root)?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(getSyncObserver(root))
    }

    private fun initFromFiles() {
        viewState.setToolbarTitle(root!!.name)
        allFiles.clear()
        files!!.forEach { file ->
            root.files.forEach { rootFile ->
                if (file.path == rootFile.path)
                    allFiles.add(rootFile)
            }
        }
        displayFiles.clear()
        displayFiles.addAll(allFiles)
        applyTagsToFiles()
    }

    private fun initFromAllRoots() {
        viewState.setToolbarTitle("All roots")
        allFiles.clear()
        syncRepo.roots.forEach { root ->
            allFiles.addAll(root.files)
            syncRepo.getSyncObservable(root)?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(getSyncObserver(root))
        }
        displayFiles.clear()
        displayFiles.addAll(allFiles)
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
            displayFiles.clear()
            displayFiles.addAll(allFiles)

            sortAndUpdateFiles()
            sortAndUpdateTags()
            return
        }

        val filteredFiles = mutableListOf<File>()
        allFiles.forEach allFiles@{ file ->
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

        displayFiles.clear()
        displayFiles.addAll(filteredFiles)
        sortAndUpdateFiles()
        sortAndUpdateTags()
    }

    private fun findUntaggedFiles() {
        val filteredFiles = mutableListOf<File>()
        allFiles.forEach { file ->
            if (file.tags.isEmpty())
                filteredFiles.add(file)
        }

        displayFiles.clear()
        displayFiles.addAll(filteredFiles)
        sortAndUpdateFiles()
        tagStates.clear()
        sortAndUpdateTags()
    }

    private fun getSyncObserver(root: Root) = object : Observer<File> {
        override fun onSubscribe(d: Disposable?) {
            syncDisposable = d
        }

        override fun onNext(syncFile: File) {

        }

        override fun onError(e: Throwable?) {}

        override fun onComplete() {}
    }

    private fun sortAndUpdateFiles() {
        displayFiles.sortWith(filesComparator(sortBy))
        fileGridPresenter.files.clear()
        if (isReversedSort)
            fileGridPresenter.files.addAll(displayFiles.reversed())
        else
            fileGridPresenter.files.addAll(displayFiles)
        viewState.updateAdapter()
    }

    private fun sortAndUpdateTags() {
        tagStates.sortWith(tagsComparator())
        viewState.clearTags()
        viewState.setTags(tagStates)
    }

    private fun setupTags() {
        val filesTags = HashSet<Tag>()
        allFiles.forEach { file ->
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