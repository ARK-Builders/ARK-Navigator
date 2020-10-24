package com.taran.imagemanager.mvp.presenter

import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.model.repo.FilesRepo
import com.taran.imagemanager.mvp.model.repo.RoomRepo
import com.taran.imagemanager.mvp.presenter.adapter.IDetailListPresenter
import com.taran.imagemanager.mvp.view.DetailView
import com.taran.imagemanager.mvp.view.item.DetailItemView
import com.taran.imagemanager.utils.getHash
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import moxy.MvpPresenter
import javax.inject.Inject

class DetailPresenter(val currentFolder: String, val pos: Int) : MvpPresenter<DetailView>() {

    @Inject
    lateinit var filesRepo: FilesRepo

    @Inject
    lateinit var roomRepo: RoomRepo

    val detailListPresenter = DetailListPresenter()

    inner class DetailListPresenter :
        IDetailListPresenter {

        var images = mutableListOf<Image>()

        override fun getCount() = images.size

        override fun bindView(view: DetailItemView) {
            view.setImage(images[view.pos].path)
        }
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()


        detailListPresenter.images = filesRepo.getImagesInFolder(currentFolder)
        viewState.setCurrentItem(pos)
        viewState.updateAdapter()
    }

    fun fabClicked(pos: Int) {
        val image = detailListPresenter.images[pos]
        roomRepo.getImageByPath(image.path)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { imageRoom ->

                    imageRoom.tags?.let { viewState.showTagsDialog(it) }
                        ?: viewState.showTagsDialog("")

                }, {
                    viewState.showTagsDialog("")
                }
            )
    }

    fun tagsEdit(pos: Int, tags: String) {
        val image = detailListPresenter.images[pos]
        roomRepo.getImageByPath(image.path)
            .subscribe({ imageRoom ->
                imageRoom.tags = tags
                roomRepo.insertImage(imageRoom).subscribe()

            }, {
                image.hash = getHash(image.path)
                image.tags = tags
                roomRepo.insertImage(image)
            })
    }


}