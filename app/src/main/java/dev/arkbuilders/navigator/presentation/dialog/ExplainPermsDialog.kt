package dev.arkbuilders.navigator.presentation.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.airbnb.lottie.LottieCompositionFactory
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.DialogExplainPermsBinding
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.data.PermissionsHelper
import javax.inject.Inject

class ExplainPermsDialog : DialogFragment(R.layout.dialog_explain_perms) {
    private val viewBinding by viewBinding(DialogExplainPermsBinding::bind)

    @Inject
    lateinit var permsHelper: PermissionsHelper

    override fun onAttach(context: Context) {
        App.instance.appComponent.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        viewBinding.btnAllow.setOnClickListener {
            permsHelper.askForWritePermissions(this@ExplainPermsDialog)
            dismiss()
        }
        viewBinding.btnExit.setOnClickListener {
            activity?.finish()
        }
    }

    companion object {
        fun newInstance(context: Context): ExplainPermsDialog {
            LottieCompositionFactory.fromRawResSync(context, R.raw.anim_file_access)
            return ExplainPermsDialog()
        }
    }
}
