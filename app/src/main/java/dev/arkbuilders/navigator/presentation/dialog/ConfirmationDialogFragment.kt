package dev.arkbuilders.navigator.presentation.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dev.arkbuilders.arklib.data.folders.FoldersRepo.Companion.DELETE_FOLDER_KEY
import dev.arkbuilders.arklib.data.folders.FoldersRepo.Companion.FORGET_FAVORITE_KEY
import dev.arkbuilders.arklib.data.folders.FoldersRepo.Companion.FORGET_ROOT_KEY
import dev.arkbuilders.navigator.databinding.DialogInfoBinding
import dev.arkbuilders.navigator.presentation.utils.textOrGone

class ConfirmationDialogFragment : DialogFragment() {

    private lateinit var binding: DialogInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DialogInfoBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val args = requireArguments()
        val bundle = args.getBundle(EXTRA_BUNDLE)

        binding.apply {
            titleTV.text = args.getString(TITLE)
            infoTV.text = args.getString(DESCRIPTION)
            posBtn.text = args.getString(POSITIVE_BTN_KEY)
            negBtn.textOrGone(args.getString(NEGATIVE_BTN_KEY))
            deleteChkBox.isVisible = args
                .getString(POSITIVE_REQUEST_KEY) == FORGET_ROOT_KEY ||
                args.getString(POSITIVE_REQUEST_KEY) == FORGET_FAVORITE_KEY
            posBtn.setOnClickListener {
                val requestKey = args
                    .getString(POSITIVE_REQUEST_KEY) ?: DEFAULT_POSITIVE_REQUEST_KEY
                bundle?.putBoolean(DELETE_FOLDER_KEY, deleteChkBox.isChecked)
                setFragmentResult(
                    requestKey,
                    bundle ?: bundleOf()
                )
                dismiss()
            }

            negBtn.setOnClickListener {
                dismiss()
            }
        }

        return binding.root
    }

    companion object {
        private const val TITLE = "title"
        private const val DESCRIPTION = "description"
        private const val POSITIVE_BTN_KEY = "positive_key"
        private const val NEGATIVE_BTN_KEY = "negative_key"
        private const val POSITIVE_REQUEST_KEY = "positiveRequestKey"
        const val DEFAULT_POSITIVE_REQUEST_KEY = "defaultPositiveRequestKey"
        const val CONFIRMATION_DIALOG_TAG = "confirmationDialogFragment"
        const val EXTRA_BUNDLE = "extraBundle"

        fun newInstance(
            title: String,
            description: String,
            posBtnText: String,
            negBtnText: String?,
            positiveRequestKey: String? = null,
            bundle: Bundle? = null
        ): ConfirmationDialogFragment = ConfirmationDialogFragment().also { f ->
            f.arguments = bundleOf(
                TITLE to title,
                DESCRIPTION to description,
                POSITIVE_BTN_KEY to posBtnText,
                NEGATIVE_BTN_KEY to negBtnText,
                POSITIVE_REQUEST_KEY to positiveRequestKey,
                EXTRA_BUNDLE to bundle
            )
        }
    }
}
