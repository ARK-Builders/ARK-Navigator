package dev.arkbuilders.navigator.presentation.dialog.sort

import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.utils.Sorting
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
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
