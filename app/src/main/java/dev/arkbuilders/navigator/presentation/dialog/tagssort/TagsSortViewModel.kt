package dev.arkbuilders.navigator.presentation.dialog.tagssort

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences

sealed class TagsSortState {
    object Loading : TagsSortState()
    class Sort(
        val sorting: TagsSorting,
        val ascending: Boolean
    ) : TagsSortState()
}

enum class TagsSorting {
    POPULARITY, QUERIED_TS, QUERIED_N, LABELED_TS, LABELED_N
}

sealed class TagsSortSideEffect {
    object CloseDialog : TagsSortSideEffect()
}

class TagsSortViewModel(
    selectorNotEdit: Boolean,
    private val preferences: Preferences
) : ViewModel(), ContainerHost<TagsSortState, TagsSortSideEffect> {
    override val container: Container<TagsSortState, TagsSortSideEffect> =
        container(TagsSortState.Loading)

    private val sortingPrefKey = if (selectorNotEdit) {
        PreferenceKey.TagsSortingSelector
    } else {
        PreferenceKey.TagsSortingEdit
    }

    private val ascPrefKey = if (selectorNotEdit) {
        PreferenceKey.TagsSortingSelectorAsc
    } else {
        PreferenceKey.TagsSortingEditAsc
    }

    init {
        intent {
            val sortingDef = viewModelScope.async {
                preferences.get(sortingPrefKey)
            }
            val ascendingDef = viewModelScope.async {
                preferences.get(ascPrefKey)
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
                sortingPrefKey,
                sorting.ordinal
            )
        }
        postSideEffect(TagsSortSideEffect.CloseDialog)
    }

    fun onAscendingChanged(ascending: Boolean) = intent {
        viewModelScope.launch {
            preferences.set(
                ascPrefKey,
                ascending
            )
        }
        postSideEffect(TagsSortSideEffect.CloseDialog)
    }
}

class TagsSortViewModelFactory @AssistedInject constructor(
    @Assisted private val selectorNotEdit: Boolean,
    private val preferences: Preferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TagsSortViewModel(selectorNotEdit, preferences) as T
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted selectorNotEdit: Boolean
        ): TagsSortViewModelFactory
    }
}
