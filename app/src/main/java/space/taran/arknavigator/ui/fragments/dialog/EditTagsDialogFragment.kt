package space.taran.arknavigator.ui.fragments.dialog

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import moxy.MvpBottomSheetDialogFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogEditTagsBinding
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.presenter.dialog.EditTagsDialogPresenter
import space.taran.arknavigator.mvp.view.dialog.EditTagsDialogView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.Tags
import space.taran.arknavigator.utils.extensions.closeKeyboard
import space.taran.arknavigator.utils.extensions.placeCursorToEnd

class EditTagsDialogFragment(
    private val index: ResourcesIndex? = null,
    private val storage: TagsStorage? = null
) : MvpBottomSheetDialogFragment(), EditTagsDialogView {
    private lateinit var binding: DialogEditTagsBinding
    private val presenter by moxyPresenter {
        EditTagsDialogPresenter(
            requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            requireArguments().getLong(RESOURCE_KEY),
            index,
            storage
        ).apply {
            App.instance.appComponent.inject(this)
        }
    }

    private var isDialogDismissed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogEditTagsBinding.inflate(inflater)
        return binding.root
    }

    override fun init(): Unit = with(binding) {
        setupFullHeight()
        etNewTags.doAfterTextChanged { editable ->
            presenter.onInputChanged(editable.toString())
        }
        etNewTags.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.onInputDone()
            }
            true
        }

        etNewTags.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL)
                presenter.onBackspacePressed()

            return@setOnKeyListener false
        }
        btnAdd.setOnClickListener {
            presenter.onAddBtnClick()
        }
        layoutOutside.setOnClickListener {
            presenter.onInputDone()
        }
        // Workaround to wait until the keyboard closes
        // otherwise there will be a weird dialog closing animation
        root.viewTreeObserver.addOnGlobalLayoutListener {
            if (isDialogDismissed)
                dismiss()
        }
    }

    override fun setResourceTags(tags: Tags) {
        binding.layoutInput
            .removeViews(1, binding.layoutInput.childCount - 4)

        tags.forEach { tag ->
            val chip = Chip(requireContext())
            chip.text = tag

            chip.setOnClickListener {
                presenter.onResourceTagClick(tag)
            }
            binding.layoutInput.addView(
                chip,
                binding.layoutInput.childCount - 3
            )
        }
    }

    override fun setQuickTags(tags: Tags) {
        binding.cgQuick.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(requireContext())
            chip.text = tag

            chip.setOnClickListener {
                presenter.onQuickTagClick(tag)
            }
            binding.cgQuick.addView(chip)
        }
    }

    override fun setInput(input: String) {
        if (binding.etNewTags.text.toString() != input)
            binding.etNewTags.setText(input)
    }

    override fun dismissDialog() {
        isDialogDismissed = true
        binding.etNewTags.closeKeyboard()
    }

    override fun onResume() {
        super.onResume()
        binding.etNewTags.placeCursorToEnd()
        binding.etNewTags.onBackPressedListener = {
            if (!presenter.onBackClick())
                dismissDialog()

            true
        }
    }

    override fun getTheme() = R.style.EditTagsDialogTheme

    override fun notifyTagsChanged() {
        setFragmentResult(REQUEST_TAGS_CHANGED_KEY, bundleOf())
    }

    // workaround to customize behavior when clicking outside
    private fun setupFullHeight() {
        val bottomSheetDialog = dialog as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )

        val behavior = BottomSheetBehavior.from(bottomSheet!!)
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false
    }

    companion object {
        const val REQUEST_TAGS_CHANGED_KEY = "tagsChangedEditTags"
        const val FRAGMENT_TAG = "editTagsDialogTag"
        private const val ROOT_AND_FAV_KEY = "rootAndFav"
        private const val RESOURCE_KEY = "resource"

        fun newInstance(
            rootAndFav: RootAndFav,
            resource: ResourceId,
            index: ResourcesIndex,
            storage: TagsStorage
        ) =
            EditTagsDialogFragment(index, storage).apply {
                arguments = Bundle().apply {
                    putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
                    putLong(RESOURCE_KEY, resource)
                }
            }
    }
}
