package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.entity.IndexingSubjects
import space.taran.arkbrowser.mvp.model.entity.Folder
import space.taran.arkbrowser.mvp.model.entity.Image
import space.taran.arkbrowser.mvp.model.repo.FilesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.presenter.adapter.IDetailListPresenter
import space.taran.arkbrowser.mvp.view.DetailView
import space.taran.arkbrowser.mvp.view.item.DetailItemView
import space.taran.arkbrowser.utils.*
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import moxy.MvpPresenter
import javax.inject.Inject

class DetailPresenter(val images: List<Image>, val pos: Int, val currentFolder: Folder) :
    MvpPresenter<DetailView>() {
    @Inject
    lateinit var indexingSubjects: IndexingSubjects

    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var roomRepo: RoomRepo

    var currentImage: Image? = null

    val detailListPresenter = DetailListPresenter()

    inner class DetailListPresenter :
        IDetailListPresenter {

        var images = mutableListOf<Image>()

        override fun getCount() = images.size

        override fun bindView(view: DetailItemView) {
            val image = images[view.pos]
            view.setImage(image.path)
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()

        if (!currentFolder.synchronized)
            indexingSubjects.map[currentFolder.path]?.subscribe(getIndexingObserver())

        detailListPresenter.images = images.toMutableList()
        viewState.setCurrentItem(pos)
    }

    fun imageChanged(newPos: Int) {
        currentImage = images[newPos]
        viewState.setTitle(currentImage!!.name)
        viewState.setImageTags(currentImage!!.tags.mapToTagList())
    }

    fun fabClicked() {
        viewState.showTagsDialog(
            currentImage!!.tags.mapToTagList(),
            currentFolder.tags.removeDuplicateTags().mapToTagList()
        )
    }

    fun tagRemoved(tag: String) {
        val folderTags = currentFolder.tags.split(",").toMutableList()
        folderTags.remove(tag)
        currentFolder.tags = folderTags.joinToString(",")

        val imageTags = currentImage!!.tags.split(",").toMutableList()
        imageTags.remove(tag)
        currentImage!!.tags = imageTags.joinToString(",")

        if (currentImage!!.synchronized)
            roomRepo.updateImageTags(currentImage!!.id, currentImage!!.tags).subscribe()

        if (currentFolder.synchronized) {
            if (currentFolder.path != "gallery") {
                filesRepo.writeToFileSingle(currentFolder.storagePath, images).subscribe(
                    {
                        currentFolder.lastModified = filesRepo.fileProvider.getLastModified(currentFolder.storagePath)
                        roomRepo.insertFolder(currentFolder).subscribe()
                    },
                    {}
                )
            }
        }
        viewState.setImageTags(currentImage!!.tags.mapToTagList())
        viewState.setDialogTags(
            currentImage!!.tags.mapToTagList(),
            currentFolder.tags.removeDuplicateTags().mapToTagList()
        )
    }

    fun tagAdded(tag: String) {
        val newTag = currentImage!!.tags.findNewTags(tag)
        currentImage!!.tags = currentImage!!.tags.addTag(newTag)
        currentFolder.tags = currentFolder.tags.addTag(newTag)

        if (currentImage!!.synchronized)
            roomRepo.updateImageTags(currentImage!!.id, currentImage!!.tags).subscribe()

        if (currentFolder.synchronized) {

            if (currentFolder.path != "gallery") {
                filesRepo.writeToFileSingle(currentFolder.storagePath, images).subscribe(
                    {
                        currentFolder.lastModified = filesRepo.fileProvider.getLastModified(currentFolder.storagePath)
                        roomRepo.insertFolder(currentFolder).subscribe()
                    },
                    {}
                )
            }
        }
        viewState.setImageTags(currentImage!!.tags.mapToTagList())
        viewState.setDialogTags(
            currentImage!!.tags.mapToTagList(),
            currentFolder.tags.removeDuplicateTags().mapToTagList()
        )
        viewState.closeDialog()
    }

    fun dismissDialog() {
        viewState.closeDialog()
    }

    private fun getIndexingObserver() = object : Observer<Image> {
        override fun onSubscribe(d: Disposable?) {}

        override fun onNext(synchronizedImage: Image?) {}

        override fun onError(e: Throwable?) {}

        override fun onComplete() {
            filesRepo.writeToFileSingle(currentFolder.storagePath, images).subscribe(
                {
                    currentFolder.lastModified = filesRepo.fileProvider.getLastModified(currentFolder.storagePath)
                    roomRepo.insertFolder(currentFolder).subscribe()
                },
                {}
            )
        }

    }

}