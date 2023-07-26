package dev.arkbuilders.navigator.mvp.presenter.dialog

import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import dev.arkbuilders.navigator.mvp.model.repo.preferences.PreferenceKey
import dev.arkbuilders.navigator.mvp.model.repo.preferences.Preferences
import dev.arkbuilders.navigator.mvp.view.dialog.SortDialogView
import dev.arkbuilders.navigator.utils.Sorting
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
