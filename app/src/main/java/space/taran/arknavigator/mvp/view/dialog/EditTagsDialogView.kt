package space.taran.arknavigator.mvp.view.dialog

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags

@StateStrategyType(AddToEndSingleStrategy::class)
interface EditTagsDialogView: MvpView {
    fun init()
    fun setQuickTags(tags: List<Tag>)
    fun setResourceTags(tags: Tags)
    fun clearInput()
    @StateStrategyType(SkipStrategy::class)
    fun dismissDialog()
}