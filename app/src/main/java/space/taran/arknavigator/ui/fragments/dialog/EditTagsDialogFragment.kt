package space.taran.arknavigator.ui.fragments.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import moxy.MvpAppCompatDialogFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogEditTagsBinding
import space.taran.arknavigator.mvp.model.dao.ResourceId
import space.taran.arknavigator.mvp.model.repo.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.TagsStorage
import space.taran.arknavigator.mvp.presenter.dialog.EditTagsDialogPresenter
import space.taran.arknavigator.mvp.view.dialog.EditTagsDialogView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.utils.Tag
import space.taran.arknavigator.utils.Tags
import space.taran.arknavigator.utils.extensions.placeCursorToEnd

class EditTagsDialogFragment(resourceId: ResourceId, storage: TagsStorage, index: ResourcesIndex, onTagsChangedListener: (resource: ResourceId) -> Unit) :
    MvpAppCompatDialogFragment(), EditTagsDialogView {
    private lateinit var binding: DialogEditTagsBinding

    private val presenter by moxyPresenter {
        EditTagsDialogPresenter(resourceId, storage, index, onTagsChangedListener).apply {
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
        etNewTags.placeCursorToEnd()
        etNewTags.doAfterTextChanged { editable ->
            presenter.onInputChanged(editable.toString())
        }
        etNewTags.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                presenter.onInputDone(etNewTags.text.toString())
            }
            true
        }

        binding.layoutOutside.setOnClickListener {
            dismiss()
        }
    }

    override fun setResourceTags(tags: Tags) {
        binding.cgResource.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag

            chip.setOnClickListener {
                presenter.onResourceTagClick(tag)
            }

            binding.cgResource.addView(chip)
        }
    }

    override fun setRootTags(tags: List<Tag>) {
        binding.cgPopular.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(context)
            chip.text = tag

            chip.setOnClickListener {
                presenter.onRootTagClick(tag)
            }
            binding.cgPopular.addView(chip)
        }
    }

    override fun clearInput() {
        binding.etNewTags.setText("")
    }

    override fun getTheme() = R.style.FullScreenDialog
}