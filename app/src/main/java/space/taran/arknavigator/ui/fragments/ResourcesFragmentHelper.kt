package space.taran.arknavigator.ui.fragments

import android.view.View
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.ArkFilePickerFragment
import space.taran.arkfilepicker.ArkFilePickerMode
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.PopupSelectedResourcesActionsBinding
import space.taran.arknavigator.ui.fragments.dialog.ConfirmationDialogFragment
import space.taran.arknavigator.ui.fragments.dialog.EditTagsDialogFragment
import space.taran.arknavigator.ui.fragments.utils.toast
import space.taran.arknavigator.ui.view.DefaultPopup

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
            val selected = presenter.gridPresenter.selectedResources.map { it.id }
            if (selected.isEmpty()) {
                toast(R.string.select_at_least_one_resource)
                popup.popupWindow.dismiss()
                return@setOnClickListener
            }
            EditTagsDialogFragment
                .newInstance(
                    presenter.rootAndFav,
                    selected,
                    presenter.index,
                    presenter.storage
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
    }
    popup.showBelow(menuBtn)
}

private fun moveFilePickerConfig() = ArkFilePickerConfig(
    titleStringId = R.string.move_to,
    mode = ArkFilePickerMode.FOLDER,
    pathPickedRequestKey = ResourcesFragment.MOVE_SELECTED_REQUEST_KEY
)

private fun copyFilePickerConfig() = ArkFilePickerConfig(
    titleStringId = R.string.copy_to,
    mode = ArkFilePickerMode.FOLDER,
    pathPickedRequestKey = ResourcesFragment.COPY_SELECTED_REQUEST_KEY
)
