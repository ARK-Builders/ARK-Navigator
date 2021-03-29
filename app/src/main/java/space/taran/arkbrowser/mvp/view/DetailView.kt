package space.taran.arkbrowser.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arkbrowser.utils.Tags

@StateStrategyType(AddToEndSingleStrategy::class)
interface DetailView: MvpView {
    fun init()
    fun setTitle(title: String)
    fun showTagsDialog(imageTags: Tags)
    fun closeDialog()
    fun setImageTags(imageTags: Tags)
    fun setDialogTags(imageTags: Tags)
    fun setCurrentItem(pos: Int)
    fun updateAdapter()
}