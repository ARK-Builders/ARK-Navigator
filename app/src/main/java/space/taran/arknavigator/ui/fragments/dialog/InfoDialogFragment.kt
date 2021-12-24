package space.taran.arknavigator.ui.fragments.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import space.taran.arknavigator.databinding.DialogInfoBinding

class InfoDialogFragment : DialogFragment() {

    private var titleText: String = ""
    private var descriptionText: String = ""

    private lateinit var binding: DialogInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DialogInfoBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        titleText = requireArguments()[TITLE_TAG] as String
        descriptionText = requireArguments()[DESCRIPTION_TAG] as String

        binding.titleTV.text = titleText
        binding.infoTV.text = descriptionText

        binding.posBtn.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    companion object {
        const val TITLE_TAG = "title"
        const val DESCRIPTION_TAG = "description"
        const val BASE_INFO_DIALOG_TAG = "baseInfoDialogFragment"

        fun newInstance(
            title: String,
            description: String
        ): InfoDialogFragment = InfoDialogFragment().also { f ->
            f.arguments = bundleOf(
                TITLE_TAG to title,
                DESCRIPTION_TAG to description
            )
        }
    }

}