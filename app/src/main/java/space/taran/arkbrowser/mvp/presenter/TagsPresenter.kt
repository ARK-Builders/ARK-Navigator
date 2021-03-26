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
import space.taran.arkbrowser.utils.mapToTagList
import space.taran.arkbrowser.utils.tagsComparator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
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
    var tags = mutableListOf<TagState>()
    var allFiles = mutableListOf<File>()

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
            if (!file.isFolder) {
                if (file.isImage()) {
                    val images = allFiles.filter { it.isImage() }
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
                        filesRepo.documentProvider.getFileUri(file.path),
                        filesRepo.documentProvider.getMimeType(file.path)
                    )
            }
        }
    }

    fun tagChecked(tag: String, isChecked: Boolean) {
        val tagState = tags.find { tagState -> tagState.tag == tag }
        tagState!!.isChecked = isChecked
        applyTagsToFiles()
    }

    fun clearTagsClicked() {
        tags.forEach { tagState ->
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
        fileGridPresenter.files.clear()
        fileGridPresenter.files.addAll(allFiles)
        viewState.updateAdapter()
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
        fileGridPresenter.files.clear()
        fileGridPresenter.files.addAll(allFiles)
        viewState.updateAdapter()
    }

    private fun initFromAllRoots() {
        viewState.setToolbarTitle("All roots")
        allFiles.clear()
        syncRepo.roots.forEach { root ->
            allFiles.addAll(root.files)
            syncRepo.getSyncObservable(root)?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(getSyncObserver(root))
        }
        fileGridPresenter.files.clear()
        fileGridPresenter.files.addAll(allFiles)
        viewState.updateAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        syncDisposable?.dispose()
    }

    fun onViewResumed() {
        setupTags()
    }

    private fun applyTagsToFiles() {
        tags.forEach { tagState ->
            tagState.isActual = false
        }

        if (tags.none{ tagState -> tagState.isChecked}) {
            fileGridPresenter.files.clear()
            fileGridPresenter.files.addAll(allFiles)
            viewState.updateAdapter()
            viewState.clearTags()
            viewState.setTags(tags)
            return
        }

        val filteredFiles = mutableListOf<File>()
        allFiles.forEach allFiles@{ file ->
            var isFileFit = true
            tags.forEach { tagState ->
                if (tagState.isChecked) {
                    if (!file.tags.contains(tagState.tag))
                        isFileFit = false
                }
            }
            if (isFileFit) filteredFiles.add(file)
        }

        filteredFiles.forEach { file ->
            tags.forEach { tagState ->
                if (file.tags.contains(tagState.tag))
                    tagState.isActual = true
            }
        }
        fileGridPresenter.files.clear()
        fileGridPresenter.files.addAll(filteredFiles)
        viewState.updateAdapter()
        tags.sortWith(tagsComparator())
        viewState.clearTags()
        viewState.setTags(tags)
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

    private fun setupTags() {
        viewState.clearTags()
        allFiles.forEach { file ->
            file.tags.mapToTagList().forEach { tag ->
                val state = tags.find { state -> state.tag == tag }
                if (state == null) {
                    tags.add(TagState(tag, false, false))
                }
            }
        }
        viewState.setTags(tags)
    }

}