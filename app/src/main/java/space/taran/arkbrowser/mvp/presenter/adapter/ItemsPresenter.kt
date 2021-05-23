package space.taran.arkbrowser.mvp.presenter.adapter

abstract class ItemsPresenter<Item, View> {

    abstract fun items(): List<Item>
    abstract fun updateItems(items: List<Item>)
    abstract fun bindView(view: View)

    fun getCount() = items().size

}