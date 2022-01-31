package space.taran.arknavigator.mvp.presenter.dialog

import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.UserPreferences
import space.taran.arknavigator.mvp.view.dialog.SortDialogView
import space.taran.arknavigator.utils.Sorting
import javax.inject.Inject

class SortDialogPresenter : MvpPresenter<SortDialogView>() {
    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onFirstViewAttach() {
        presenterScope.launch {
            val sorting = userPreferences.getSorting()
            val ascending = userPreferences.isSortingAscending()
            viewState.init(sorting, ascending)
        }
    }

    fun onSortingSelected(sorting: Sorting) = presenterScope.launch {
        userPreferences.setSorting(sorting)
        viewState.closeDialog()
    }

    fun onAscendingSelected(ascending: Boolean) = presenterScope.launch {
        userPreferences.setSortingAscending(ascending)
        viewState.closeDialog()
    }
}
