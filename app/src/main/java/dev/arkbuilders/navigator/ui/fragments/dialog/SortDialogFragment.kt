package dev.arkbuilders.navigator.ui.fragments.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moxy.MvpAppCompatDialogFragment
import moxy.ktx.moxyPresenter
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.DialogSortBinding
import dev.arkbuilders.navigator.mvp.presenter.dialog.SortDialogPresenter
import dev.arkbuilders.navigator.mvp.view.dialog.SortDialogView
import dev.arkbuilders.navigator.ui.App
import dev.arkbuilders.navigator.ui.fragments.utils.toast
import dev.arkbuilders.navigator.utils.LogTags.RESOURCES_SCREEN
import dev.arkbuilders.navigator.utils.Sorting

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
        val selectedSortingBtn = when (sorting) {
            Sorting.DEFAULT -> rbDefault
            Sorting.NAME -> rbName
            Sorting.SIZE -> rbSize
            Sorting.LAST_MODIFIED -> rbLastModified
            Sorting.TYPE -> rbType
        }
        selectedSortingBtn.isChecked = true
        selectedSortingBtn.jumpDrawablesToCurrentState()

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

    companion object {
        fun newInstance() = SortDialogFragment()
    }
}
