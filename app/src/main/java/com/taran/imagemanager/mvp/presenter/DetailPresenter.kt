package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IDetailListPresenter
import com.taran.imagemanager.mvp.view.DetailView
import com.taran.imagemanager.mvp.view.item.DetailItemView
import com.taran.imagemanager.utils.TEXT_STORAGE_NAME
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import moxy.MvpPresenter
import javax.inject.Inject

class DetailPresenter(val images: List<Image>, val pos: Int, val currentFolder: Folder) :
    MvpPresenter<DetailView>() {

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

        detailListPresenter.images = images.toMutableList()
        viewState.setCurrentItem(pos)
    }

    fun imageChanged(newPos: Int) {
        currentImage = images[newPos]
        loadTags()
    }

    private fun loadTags() {
        roomRepo.getImageByPath(currentImage!!.path).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ imageRoom ->
                currentImage = imageRoom
                val tags = mapToTagsList(currentImage!!.tags)
                viewState.setImageTags(tags)
            }, {
                viewState.setImageTags(listOf())
                filesRepo.getHashSingle(currentImage!!.path).subscribe(
                    { hash ->
                        currentImage!!.hash = hash
                        roomRepo.insertImage(currentImage!!).subscribe(
                            { id ->
                                currentImage!!.id = id
                            },
                            {}
                        )
                    }, {}
                )
            })
    }

    fun fabClicked() {
        val folderTags = mapToTagsList(currentFolder.tags)
        val imageTags = mapToTagsList(currentImage!!.tags)
        viewState.showTagsDialog(imageTags, deleteDuplicates(folderTags))
    }

    fun tagRemoved(tag: String) {
        val folderTags = currentFolder.tags.split(",").toMutableList()
        folderTags.remove(tag)
        currentFolder.tags = folderTags.joinToString(",")

        val imageTags = currentImage!!.tags.split(",").toMutableList()
        imageTags.remove(tag)
        currentImage!!.tags = imageTags.joinToString(",")
        roomRepo.updateImageTags(currentImage!!.id, currentImage!!.tags).subscribe()


        if (currentFolder.path != "gallery") {
            roomRepo.updateFolderTags(currentFolder.id, currentFolder.tags).subscribe()
            filesRepo.writeToFile(
                "${currentFolder.path}/$TEXT_STORAGE_NAME",
                listOf(currentImage!!),
                true
            ).subscribe()
        }

        viewState.setImageTags(imageTags)
        viewState.setDialogTags(imageTags, deleteDuplicates(folderTags))
    }

    fun tagAdded(tag: String) {
        if (currentFolder.tags.isNotEmpty()) {
            currentFolder.tags += ",$tag"
        } else
            currentFolder.tags = tag

        if (currentImage!!.tags.isNotEmpty()) {
            if (!currentImage!!.tags.contains(tag))
                currentImage!!.tags += ",$tag"
        } else
            currentImage!!.tags = tag
        roomRepo.updateImageTags(currentImage!!.id, currentImage!!.tags).subscribe()

        if (currentFolder.path != "gallery") {
            roomRepo.updateFolderTags(currentFolder.id, currentFolder.tags).subscribe()
            filesRepo.writeToFile(
                "${currentFolder.path}/$TEXT_STORAGE_NAME",
                listOf(currentImage!!),
                true
            ).subscribe()
        }

        val folderTags = mapToTagsList(currentFolder.tags)
        val imageTags = mapToTagsList(currentImage!!.tags)
        viewState.setImageTags(imageTags)
        viewState.setDialogTags(imageTags, deleteDuplicates(folderTags))
        viewState.closeDialog()
    }

    fun dismissDialog() {
        viewState.closeDialog()
    }

    private fun mapToTagsList(str: String): List<String> {
        return if (str.isNotEmpty())
            str.split(",")
        else
            listOf()
    }

    private fun deleteDuplicates(list: List<String>): List<String> = list.toHashSet().toList()


}