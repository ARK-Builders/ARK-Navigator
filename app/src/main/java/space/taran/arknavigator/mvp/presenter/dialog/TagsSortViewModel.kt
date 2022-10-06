package space.taran.arknavigator.mvp.presenter.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import javax.inject.Inject

sealed class TagsSortState {
    object Loading : TagsSortState()
    class Sort(
        val sorting: TagsSorting,
        val ascending: Boolean
    ) : TagsSortState()
}

enum class TagsSorting {
    POPULARITY, LAST_USED
}

sealed class TagsSortSideEffect {
    object CloseDialog : TagsSortSideEffect()
}

class TagsSortViewModel(
    private val preferences: Preferences
) : ViewModel(), ContainerHost<TagsSortState, TagsSortSideEffect> {
    override val container: Container<TagsSortState, TagsSortSideEffect> =
        container(TagsSortState.Loading)

    init {
        intent {
            val sortingDef = viewModelScope.async {
                preferences.get(PreferenceKey.TagsSorting)
            }
            val ascendingDef = viewModelScope.async {
                preferences.get(PreferenceKey.TagsSortingAscending)
            }
            val sorting = TagsSorting.values()[sortingDef.await()]
            val ascending = ascendingDef.await()

            reduce {
                TagsSortState.Sort(sorting, ascending)
            }
        }
    }

    fun onSortingChanged(sorting: TagsSorting) = intent {
        viewModelScope.launch {
            preferences.set(
                PreferenceKey.TagsSorting,
                sorting.ordinal
            )
        }
        postSideEffect(TagsSortSideEffect.CloseDialog)
    }

    fun onAscendingChanged(ascending: Boolean) = intent {
        viewModelScope.launch {
            preferences.set(
                PreferenceKey.TagsSortingAscending,
                ascending
            )
        }
        postSideEffect(TagsSortSideEffect.CloseDialog)
    }
}

class TagsSortViewModelFactory @Inject constructor(
    private val preferences: Preferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TagsSortViewModel(preferences) as T
    }
}
