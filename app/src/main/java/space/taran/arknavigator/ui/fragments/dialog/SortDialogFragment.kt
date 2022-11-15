package space.taran.arknavigator.ui.fragments.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moxy.MvpAppCompatDialogFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogSortBinding
import space.taran.arknavigator.mvp.presenter.dialog.SortDialogPresenter
import space.taran.arknavigator.mvp.view.dialog.SortDialogView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.fragments.utils.toast
import space.taran.arknavigator.utils.LogTags.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.extensions.changeEnabledStatus

class SortDialogFragment : MvpAppCompatDialogFragment(), SortDialogView {

    private lateinit var binding: DialogSortBinding
    private val presenter by moxyPresenter {
        SortDialogPresenter().apply {
            App.instance.appComponent.inject(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSortBinding.inflate(inflater)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun init(
        sorting: Sorting,
        ascending: Boolean,
        sortByScoresEnabled: Boolean
    ) = with(binding) {
        when (sorting) {
            Sorting.DEFAULT -> rbDefault.isChecked = true
            Sorting.NAME -> rbName.isChecked = true
            Sorting.SIZE -> rbSize.isChecked = true
            Sorting.LAST_MODIFIED -> rbLastModified.isChecked = true
            Sorting.TYPE -> rbType.isChecked = true
        }

        if (sorting == Sorting.DEFAULT) {
            changeSortOrderEnabledStatus(this, false)
        } else {
            if (ascending) {
                rbAscending.isChecked = true
            } else {
                rbDescending.isChecked = true
            }
        }
        if (sortByScoresEnabled) {
            swScores.isChecked = true
        }

        rgSorting.setOnCheckedChangeListener { _, checkedId ->

            val newSorting = sortingCategorySelected(checkedId)

            Log.d(
                RESOURCES_SCREEN,
                "sorting criteria changed, sorting = $newSorting"
            )

            if (newSorting == Sorting.DEFAULT) {
                toast(
                    R.string.as_is_sorting_selected
                )
            }

            presenter.onSortingSelected(newSorting)
        }

        rgSortingDirection.setOnCheckedChangeListener { _, checkedId ->
            var newAscending = false
            when (checkedId) {
                R.id.rb_ascending -> newAscending = true
                R.id.rb_descending -> newAscending = false
            }

            Log.d(
                RESOURCES_SCREEN,
                "sorting direction changed, ascending = $newAscending"
            )

            presenter.onAscendingSelected(newAscending)
        }

        swScores.setOnCheckedChangeListener { _, isChecked ->
            Log.d(
                RESOURCES_SCREEN,
                "sorting by scores ${if (isChecked) "enabled" else "disabled"}"
            )
            presenter.onScoresSwitched(isChecked)
        }
    }

    override fun closeDialog() {
        dismiss()
    }

    private fun sortingCategorySelected(itemID: Int): Sorting {
        return when (itemID) {
            R.id.rb_default -> { Sorting.DEFAULT }
            R.id.rb_name -> Sorting.NAME
            R.id.rb_size -> Sorting.SIZE
            R.id.rb_last_modified -> Sorting.LAST_MODIFIED
            R.id.rb_type -> Sorting.TYPE
            else -> Sorting.DEFAULT
        }
    }

    private fun changeSortOrderEnabledStatus(
        dialogBinding: DialogSortBinding,
        isEnabledStatus: Boolean
    ) {
        val childCount = dialogBinding.rgSortingDirection.childCount
        for (radioButton in 0 until childCount) {
            dialogBinding.rgSortingDirection.getChildAt(radioButton)
                .changeEnabledStatus(isEnabledStatus)
        }
    }

    companion object {
        fun newInstance() = SortDialogFragment()
    }
}
