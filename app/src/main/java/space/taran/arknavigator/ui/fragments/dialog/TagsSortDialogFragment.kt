package space.taran.arknavigator.ui.fragments.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import org.orbitmvi.orbit.viewmodel.observe
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogTagsSortBinding
import space.taran.arknavigator.mvp.presenter.dialog.TagsSortSideEffect
import space.taran.arknavigator.mvp.presenter.dialog.TagsSortState
import space.taran.arknavigator.mvp.presenter.dialog.TagsSortViewModel
import space.taran.arknavigator.mvp.presenter.dialog.TagsSortViewModelFactory
import space.taran.arknavigator.mvp.presenter.dialog.TagsSorting
import space.taran.arknavigator.ui.App
import javax.inject.Inject

class TagsSortDialogFragment : DialogFragment(R.layout.dialog_tags_sort) {
    private val binding by viewBinding(DialogTagsSortBinding::bind)

    @Inject
    lateinit var factory: TagsSortViewModelFactory.Factory
    private val viewModel: TagsSortViewModel by viewModels {
        factory.create(requireArguments().getBoolean(SELECTOR_NOT_EDIT_KEY))
    }

    override fun onAttach(context: Context) {
        App.instance.appComponent.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        viewModel.observe(this, ::render, ::handleSideEffect)
    }

    private fun initUI() = with(binding) {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        rbPopularity.setOnClickListener {
            viewModel.onSortingChanged(TagsSorting.POPULARITY)
        }
        rbQueriedTs.setOnClickListener {
            viewModel.onSortingChanged(TagsSorting.QUERIED_TS)
        }
        rbQueriedN.setOnClickListener {
            viewModel.onSortingChanged(TagsSorting.QUERIED_N)
        }
        rbLabeledTs.setOnClickListener {
            viewModel.onSortingChanged(TagsSorting.LABELED_TS)
        }
        rbLabeledAmount.setOnClickListener {
            viewModel.onSortingChanged(TagsSorting.LABELED_N)
        }
        rbAscending.setOnClickListener {
            viewModel.onAscendingChanged(true)
        }
        rbDescending.setOnClickListener {
            viewModel.onAscendingChanged(false)
        }
    }

    private fun render(state: TagsSortState) = with(binding) {
        if (state is TagsSortState.Loading) return@with

        state as TagsSortState.Sort

        rgSorting.children.forEach {
            (it as RadioButton).isChecked = false
        }

        when (state.sorting) {
            TagsSorting.POPULARITY -> rbPopularity
            TagsSorting.QUERIED_TS -> rbQueriedTs
            TagsSorting.QUERIED_N -> rbQueriedN
            TagsSorting.LABELED_TS -> rbLabeledTs
            TagsSorting.LABELED_N -> rbLabeledAmount
        }.isChecked = true

        rbAscending.isChecked = state.ascending
        rbDescending.isChecked = !state.ascending
    }

    private fun handleSideEffect(effect: TagsSortSideEffect) = when (effect) {
        TagsSortSideEffect.CloseDialog -> dismiss()
    }

    companion object {
        private const val SELECTOR_NOT_EDIT_KEY = "selectorNotEdit"

        fun newInstance(
            selectorNotEdit: Boolean
        ) = TagsSortDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(SELECTOR_NOT_EDIT_KEY, selectorNotEdit)
            }
        }
    }
}
