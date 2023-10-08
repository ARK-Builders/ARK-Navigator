package dev.arkbuilders.navigator.presentation.screen.resources

import android.view.View
import androidx.core.view.isVisible
import dev.arkbuilders.arkfilepicker.ArkFilePickerConfig
import dev.arkbuilders.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
import dev.arkbuilders.arkfilepicker.presentation.filepicker.ArkFilePickerMode
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.PopupSelectedResourcesActionsBinding
import dev.arkbuilders.navigator.presentation.dialog.ConfirmationDialogFragment
import dev.arkbuilders.navigator.presentation.dialog.edittags.EditTagsDialogFragment
import dev.arkbuilders.navigator.presentation.utils.toast
import dev.arkbuilders.navigator.presentation.view.DefaultPopup

fun ResourcesFragment.setupAndShowSelectedResourcesMenu(menuBtn: View) {
    val menuBinding = PopupSelectedResourcesActionsBinding
        .inflate(requireActivity().layoutInflater)
    val popup = DefaultPopup(
        menuBinding,
        R.style.FadeAnimation,
        R.drawable.bg_rounded_8,
        24f
    )
    menuBinding.apply {
        btnMove.setOnClickListener {
            val selected = presenter.gridPresenter.selectedResources
            if (selected.isEmpty()) {
                toast(R.string.select_at_least_one_resource)
                popup.popupWindow.dismiss()
                return@setOnClickListener
            }
            ArkFilePickerFragment
                .newInstance(moveFilePickerConfig())
                .show(childFragmentManager, null)
            popup.popupWindow.dismiss()
        }
        btnCopy.setOnClickListener {
            val selected = presenter.gridPresenter.selectedResources
            if (selected.isEmpty()) {
                toast(R.string.select_at_least_one_resource)
                popup.popupWindow.dismiss()
                return@setOnClickListener
            }
            ArkFilePickerFragment
                .newInstance(copyFilePickerConfig())
                .show(childFragmentManager, null)
            popup.popupWindow.dismiss()
        }
        btnEditTags.setOnClickListener {
            val selected = presenter.gridPresenter.selectedResources.map { it }
            if (selected.isEmpty()) {
                toast(R.string.select_at_least_one_resource)
                popup.popupWindow.dismiss()
                return@setOnClickListener
            }
            EditTagsDialogFragment
                .newInstance(
                    presenter.folders,
                    selected,
                    presenter.index,
                    presenter.tagStorage,
                    presenter.statsStorage
                )
                .show(childFragmentManager, null)
            popup.popupWindow.dismiss()
        }
        btnShare.setOnClickListener {
            val selected = presenter.gridPresenter.selectedResources
            if (selected.isEmpty()) {
                toast(R.string.select_at_least_one_resource)
                popup.popupWindow.dismiss()
                return@setOnClickListener
            }
            presenter.onShareSelectedResourcesClicked()
            popup.popupWindow.dismiss()
        }
        btnRemove.setOnClickListener {
            val selectedSize = presenter.gridPresenter.selectedResources.size
            if (selectedSize == 0) {
                toast(R.string.select_at_least_one_resource)
                popup.popupWindow.dismiss()
                return@setOnClickListener
            }
            val description = "$selectedSize " +
                getString(R.string.resources_will_be_removed)
            ConfirmationDialogFragment
                .newInstance(
                    getString(R.string.are_you_sure),
                    description,
                    getString(R.string.yes),
                    getString(R.string.no),
                    ResourcesFragment.DELETE_CONFIRMATION_REQUEST_KEY
                )
                .show(parentFragmentManager, null)
            popup.popupWindow.dismiss()
        }
        with(btnIncreaseScore) {
            isVisible = presenter.allowScoring()
            setOnClickListener {
                presenter.onIncreaseScoreClicked()
            }
        }
        with(btnDecreaseScore) {
            isVisible = presenter.allowScoring()
            setOnClickListener {
                presenter.onDecreaseScoreClicked()
            }
        }
        with(btnResetScores) {
            val selectedSize = presenter.gridPresenter.selectedResources.size
            isVisible = presenter.allowResettingScores()
            text = getString(
                R.string.reset_scores,
                if (selectedSize == 1) "" else "s"
            )
            setOnClickListener {
                popup.popupWindow.dismiss()
                if (selectedSize == 0) {
                    toast(R.string.select_at_least_one_resource)
                    popup.popupWindow.dismiss()
                    return@setOnClickListener
                }
                ConfirmationDialogFragment.newInstance(
                    getString(R.string.are_you_sure),
                    getString(
                        R.string.resource_scores_erased,
                        selectedSize,
                        if (selectedSize == 1) "" else "s"
                    ),
                    getString(R.string.yes),
                    getString(R.string.no),
                    ResourcesFragment.RESET_SCORES_FOR_SELECTED
                ).show(parentFragmentManager, null)
            }
        }
    }
    popup.showBelow(menuBtn)
}

private fun moveFilePickerConfig() = ArkFilePickerConfig(
    titleStringId = R.string.move_to,
    mode = ArkFilePickerMode.FOLDER,
    pathPickedRequestKey = ResourcesFragment.MOVE_SELECTED_REQUEST_KEY,
    showRoots = true
)

private fun copyFilePickerConfig() = ArkFilePickerConfig(
    titleStringId = R.string.copy_to,
    mode = ArkFilePickerMode.FOLDER,
    pathPickedRequestKey = ResourcesFragment.COPY_SELECTED_REQUEST_KEY,
    showRoots = true
)
