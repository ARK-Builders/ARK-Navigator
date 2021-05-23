package space.taran.arkbrowser.mvp.presenter.adapter

import space.taran.arkbrowser.mvp.model.dao.common.Preview
import space.taran.arkbrowser.mvp.view.item.PreviewItemView

class PreviewsList(
    private val previews: List<Preview>,
    private val position: Int)
    : ItemsPresenter<Preview, PreviewItemView>() {

    override fun items() = previews

    override fun updateItems(items: List<Preview>) {
        //we might delete some previews, but doubtfully add more
        TODO("Not yet implemented")
    }

    override fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]

        if (preview.predefined != null) {
            view.setPredefined(preview.predefined)
        } else {
            view.setImage(preview.image!!)
        }
    }

    fun position() = position
}