package dev.arkbuilders.navigator.ui.fragments.dialog

import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.DialogNotificationBinding

class StorageExceptionDialogFragment : DialogFragment() {

    private lateinit var binding: DialogNotificationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DialogNotificationBinding
            .inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(TRANSPARENT))

        val args = requireArguments()
        val storageType = args.getString(STORAGE_TYPE_KEY)
        val messageText = args.getString(MESSAGE_TEXT)

        binding.apply {
            titleTV.text = getString(R.string.corrupted_storage)
            infoTV.text = getString(
                R.string.storage_corrupt_info,
                storageType,
                messageText
            )
            closeBtn.setOnClickListener {
                setFragmentResult(
                    STORAGE_CORRUPTION_DETECTED,
                    bundleOf()
                )
                dismiss()
            }
        }

        return binding.root
    }

    companion object {
        const val TAG = "corrupt notification dialog"
        const val STORAGE_TYPE_KEY = "storage type key"
        const val MESSAGE_TEXT = "message text"
        const val STORAGE_CORRUPTION_DETECTED = "storage corruption detected"

        fun newInstance(
            storageType: String,
            messageText: String,
        ) = StorageExceptionDialogFragment().also {
            it.arguments = bundleOf().apply {
                putString(STORAGE_TYPE_KEY, storageType)
                putString(MESSAGE_TEXT, messageText)
            }
        }
    }
}
