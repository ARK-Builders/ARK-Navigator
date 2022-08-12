package space.taran.arknavigator.ui.fragments.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moxy.MvpAppCompatDialogFragment
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
import space.taran.arknavigator.utils.extensions.placeCursorToEnd
import space.taran.arknavigator.utils.extensions.showKeyboard

class EditTagsDialogFragment(
    private val index: ResourcesIndex? = null,
    private val storage: TagsStorage? = null
) : MvpAppCompatDialogFragment(), EditTagsDialogView {
    private lateinit var binding: DialogEditTagsBinding
    private val presenter by moxyPresenter {
        EditTagsDialogPresenter(
            requireArguments()[ROOT_AND_FAV_KEY] as RootAndFav,
            requireArguments().getLongArray(RESOURCES_KEY)!!.toList(),
            index,
            storage
        ).apply {
            App.instance.appComponent.inject(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogEditTagsBinding.inflate(inflater)
        return binding.root
    }

    override fun init(): Unit = with(binding) {
        if (presenter.resources.size != 1)
            tvTags.text = getString(R.string.common_tags)
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
    }

    override fun showKeyboardAndView() = with(binding) {
        etNewTags.placeCursorToEnd()
        etNewTags.showKeyboard()
        etNewTags.onBackPressedListener = {
            if (!presenter.onBackClick())
                dismissDialog()

            true
        }
        lifecycleScope.launch {
            // Waiting for the keyboard to show up, otherwise there will be lags
            // Android doesn't have a listener for this
            delay(50)
            if (root.currentState == R.id.start) {
                root.setTransitionDuration(OPEN_DURATION)
                root.transitionToEnd()
            }
        }
        return@with
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
        binding.btnAdd.isVisible = input.isNotEmpty()
        if (binding.etNewTags.text.toString() != input)
            binding.etNewTags.setText(input)
    }

    override fun dismissDialog() {
        binding.root.setTransitionListener(object : RelaxedTransitionListener {
            override fun onTransitionCompleted(
                motionLayout: MotionLayout?,
                currentId: Int
            ) {
                notifyTagsChanged()
                dismiss()
            }
        })
        binding.root.setTransitionDuration(CLOSE_DURATION)
        binding.root.transitionToStart()
    }
    override fun getTheme() = R.style.EditTagsDialogTheme

    override fun onResume() = with(binding) {
        super.onResume()
        if (root.currentState == R.id.end) {
            etNewTags.placeCursorToEnd()
            etNewTags.onBackPressedListener = {
                if (!presenter.onBackClick())
                    dismissDialog()
                true
            }
        }
    }

    private fun notifyTagsChanged() {
        setFragmentResult(REQUEST_TAGS_CHANGED_KEY, bundleOf())
    }

    // workaround to customize behavior when clicking outside
    private fun setupFullHeight() {
        dialog!!.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    companion object {
        const val REQUEST_TAGS_CHANGED_KEY = "tagsChangedEditTags"
        const val FRAGMENT_TAG = "editTagsDialogTag"
        private const val ROOT_AND_FAV_KEY = "rootAndFav"
        private const val RESOURCES_KEY = "resources"
        private const val OPEN_DURATION = 300
        private const val CLOSE_DURATION = 200

        fun newInstance(
            rootAndFav: RootAndFav,
            resources: List<ResourceId>,
            index: ResourcesIndex,
            storage: TagsStorage
        ) =
            EditTagsDialogFragment(index, storage).apply {
                arguments = Bundle().apply {
                    putParcelable(ROOT_AND_FAV_KEY, rootAndFav)
                    putLongArray(RESOURCES_KEY, resources.toLongArray())
                }
            }
    }
}

interface RelaxedTransitionListener : MotionLayout.TransitionListener {
    override fun onTransitionChange(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int,
        progress: Float
    ) {
    }

    override fun onTransitionStarted(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int
    ) {
    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout?,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {
    }
}
