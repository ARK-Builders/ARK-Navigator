package dev.arkbuilders.navigator.mvp.view

import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arkfilepicker.presentation.folderstree.FolderNode
import java.nio.file.Path

@StateStrategyType(AddToEndSingleStrategy::class)
interface FoldersView : CommonMvpView {
    fun init()
    fun setProgressVisibility(isVisible: Boolean, withText: String = "")
    fun updateFoldersTree(devices: List<Path>, rootsWithFavs: Map<Path, List<Path>>)

    @StateStrategyType(SkipStrategy::class)
    fun openRootPickerDialog(path: Path?)

    @StateStrategyType(SkipStrategy::class)
    fun openConfirmForgetFolderDialog(node: FolderNode)

    @StateStrategyType(SkipStrategy::class)
    fun openRootsScanDialog()
    @StateStrategyType(SkipStrategy::class)
    fun toastFailedPath(failedPaths: List<Path>)
    @StateStrategyType(SkipStrategy::class)
    fun toastRootIsAlreadyPicked()
    @StateStrategyType(SkipStrategy::class)
    fun toastFavoriteIsAlreadyPicked()
    @StateStrategyType(SkipStrategy::class)
    fun toastIndexingCanTakeMinutes()
}
