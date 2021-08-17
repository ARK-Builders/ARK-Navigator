package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.model.repo.Folders

@StateStrategyType(AddToEndSingleStrategy::class)
interface FoldersView: MvpView, NotifiableView {
    fun loadFolders(folders: Folders)
}