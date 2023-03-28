package space.taran.arknavigator.mvp.presenter.dialog

import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arknavigator.mvp.view.dialog.SortDialogView
import space.taran.arknavigator.utils.Sorting
import javax.inject.Inject

class SortDialogPresenter : MvpPresenter<SortDialogView>() {
    @Inject
    lateinit var preferences: Preferences

    override fun onFirstViewAttach() {
        presenterScope.launch {
            val sorting = Sorting.values()[preferences.get(PreferenceKey.Sorting)]
            val ascending = preferences.get(PreferenceKey.IsSortingAscending)
            val sortByScores = preferences.get(PreferenceKey.SortByScores)
            viewState.init(sorting, ascending, sortByScores)
        }
    }

    fun onSortingSelected(sorting: Sorting) = presenterScope.launch {
        preferences.set(PreferenceKey.Sorting, sorting.ordinal)
        viewState.closeDialog()
    }
}
