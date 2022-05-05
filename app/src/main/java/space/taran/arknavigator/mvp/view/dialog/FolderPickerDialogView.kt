package space.taran.arknavigator.mvp.view.dialog

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface FolderPickerDialogView : MvpView {
    fun init()
    fun updateFolders()
    fun setFolderName(folderName: String)
    fun setPickBtnState(isEnabled: Boolean, isRootNotFavorite: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun toastDeviceChosenAsRoot()
    @StateStrategyType(SkipStrategy::class)
    fun notifyPathPicked(path: Path, rootNotFavorite: Boolean)
    @StateStrategyType(SkipStrategy::class)
    fun toastFileChosenAsRoot()
}
