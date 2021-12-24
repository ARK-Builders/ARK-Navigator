package space.taran.arknavigator.ui.fragments.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import space.taran.arknavigator.databinding.DialogInfoBinding
import space.taran.arknavigator.utils.extensions.textOrGone

class ConfirmationDialogFragment: DialogFragment() {

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

        binding.apply {
            titleTV.text = args.getString(TITLE)
            infoTV.text = args.getString(DESCRIPTION)
            posBtn.text = args.getString(POSITIVE_KEY)
            negBtn.textOrGone(args.getString(NEGATIVE_KEY))

            posBtn.setOnClickListener {
                setFragmentResult(POSITIVE_KEY, bundleOf())
                dismiss()
            }

            negBtn.setOnClickListener {
                setFragmentResult(NEGATIVE_KEY, bundleOf())
                dismiss()
            }
        }

        return binding.root
    }

    companion object {
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val POSITIVE_KEY = "positive_key"
        const val NEGATIVE_KEY = "negative_key"
        const val CONFIRMATION_DIALOG_TAG = "confirmationDialogFragment"

        fun newInstance(
            title: String,
            description: String,
            posBtnText: String,
            negBtnText: String? = null
        ): ConfirmationDialogFragment = ConfirmationDialogFragment().also { f ->
            f.arguments = bundleOf(
                TITLE to title,
                DESCRIPTION to description,
                POSITIVE_KEY to posBtnText,
                NEGATIVE_KEY to negBtnText
            )
        }
    }

}