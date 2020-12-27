package space.taran.arkbrowser.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType

@StateStrategyType(AddToEndSingleStrategy::class)
interface DetailView: MvpView {
    fun init()
    fun setTitle(title: String)
    fun showTagsDialog(imageTags: List<String>, folderTags: List<String>)
    fun closeDialog()
    fun setImageTags(imageTags: List<String>)
    fun setDialogTags(imageTags: List<String>, folderTags: List<String>)
    fun setCurrentItem(pos: Int)
    fun updateAdapter()
}