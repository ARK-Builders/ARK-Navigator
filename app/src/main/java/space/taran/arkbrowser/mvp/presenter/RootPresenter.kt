package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.*
import space.taran.arkbrowser.mvp.model.entity.common.Icons
import space.taran.arkbrowser.mvp.model.entity.room.SDCardUri
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.model.repo.SynchronizeRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IFileGridPresenter
import space.taran.arkbrowser.mvp.view.RootView
import space.taran.arkbrowser.mvp.view.item.FileItemView
import space.taran.arkbrowser.navigation.Screens
import space.taran.arkbrowser.utils.filesComparator
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import moxy.InjectViewState
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import javax.inject.Inject

@InjectViewState
class RootPresenter: MvpPresenter<RootView>() {
    @Inject
    lateinit var syncRepo: SynchronizeRepo

    @Inject
    lateinit var router: Router

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var filesRepo: FilesRepo

    val rootGridPresenter = FileGridPresenter()
    val dialogGridPresenter = DialogFileGridPresenter()
    var pickedDir: File? = null
    var dialogIsOpen: Boolean = false

    inner class FileGridPresenter :
        IFileGridPresenter {

        var roots = mutableListOf<Root>()

        override fun getCount() = roots.size

        override fun bindView(view: FileItemView) {
            val root = roots[view.pos]
            view.setText(root.name)
            view.setIcon(Icons.ROOT, null)
        }

        override fun onCardClicked(pos: Int) {
            val root = roots[pos]
            router.replaceScreen(Screens.TagsScreen(root = root, state = TagsPresenter.State.SINGLE_ROOT))
        }
    }

    inner class DialogFileGridPresenter: IFileGridPresenter {
        var files = mutableListOf<File>()

        override fun getCount() = files.size

        override fun bindView(view: FileItemView) {
            val file = files[view.pos]
            view.setText(file.name)
            if (file.isFolder)
                view.setIcon(Icons.FOLDER, null)
            else {
                if (file.isImage())
                    view.setIcon(Icons.IMAGE, file.path)
                else
                    view.setIcon(Icons.FILE, file.path)
            }
        }

        override fun onCardClicked(pos: Int) {
            val file = files[pos]
            if (file.isFolder) {
                pickedDir = file
                viewState.setDialogPath(pickedDir!!.path)
                files.clear()
                files.addAll(filesRepo.fileDataSource.list(file.path).sortedWith(filesComparator()))
                viewState.updateDialogAdapter()
            }
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
    }

    fun onViewResumed() {
        rootGridPresenter.roots.clear()
        val sortedRoots = syncRepo.roots.toMutableList()
        sortedRoots.sortBy { it.name }
        rootGridPresenter.roots.addAll(sortedRoots)
        viewState.updateRootAdapter()
    }

    fun rootPicked() {
        if (pickedDir == null)
            return
        val storagePath = filesRepo.createStorage(pickedDir!!.path)
        if (storagePath == null) {
            requestSdCardUri()
            return
        }

        val root = Root(name = pickedDir!!.name, parentPath = pickedDir!!.path, storagePath = storagePath)
        roomRepo.insertRoot(root).observeOn(AndroidSchedulers.mainThread()).subscribe(
            { id ->
                root.id = id
                syncRepo.synchronizeRoot(root)
                rootGridPresenter.roots.clear()
                val sortedRoots = syncRepo.roots.toMutableList()
                sortedRoots.sortBy { it.name }
                rootGridPresenter.roots.addAll(sortedRoots)
                viewState.updateRootAdapter()
                dismissDialog()
            },
            {
               it.printStackTrace()
            }
        )
    }

    fun dismissDialog() {
        viewState.closeChooserDialog()
        dialogIsOpen = false
    }

    fun fabClicked() {
        viewState.openChooserDialog()
        dialogIsOpen = true
        pickedDir = null
        dialogGridPresenter.files.clear()
        dialogGridPresenter.files.addAll(filesRepo.fileDataSource.getExtSdCards())
        viewState.updateDialogAdapter()
    }

    private fun requestSdCardUri() {
        val basePath = filesRepo.fileDataSource.getExtSdCardBaseFolder(pickedDir!!.path)
        roomRepo.getSdCardUriByPath(basePath!!).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                it.uri = null
                roomRepo.insertSdCardUri(it).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ viewState.requestSdCardUri() }, {})
            }, {
                roomRepo.insertSdCardUri(SDCardUri(path = basePath))
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
                        { viewState.requestSdCardUri() }, {})
            })
    }

    fun backClicked(): Boolean {
        if (dialogIsOpen) {
            if (pickedDir != null) {
                val extPaths = filesRepo.fileDataSource.getExtSdCards()
                extPaths.forEach {
                    if (pickedDir!!.path ==  it.path) {
                        pickedDir = null
                        dialogGridPresenter.files.clear()
                        dialogGridPresenter.files.addAll(extPaths)
                        viewState.updateDialogAdapter()
                        viewState.setDialogPath("/")
                        return true
                    }
                }

                val parent = filesRepo.fileDataSource.getParent(pickedDir!!.path)
                pickedDir = parent
                val files = filesRepo.fileDataSource.list(pickedDir!!.path)
                viewState.setDialogPath(pickedDir!!.path)
                dialogGridPresenter.files.clear()
                dialogGridPresenter.files.addAll(files.sortedWith(filesComparator()))
                viewState.updateDialogAdapter()
            } else
                viewState.closeChooserDialog()
        } else
            router.exit()

        return true
    }

}