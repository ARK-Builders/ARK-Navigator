package space.taran.arknavigator.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogInfoBinding

@SuppressLint("ClickableViewAccessibility")
fun showInfoDialog(
    context: Context,
    @StringRes title: Int,
    @StringRes descrText: Int,
    @StringRes posButtonText: Int = R.string.ok,
    @StringRes negButtonText: Int? = null,
    posButtonCallback: (() -> Unit)? = null,
    negButtonCallback: (() -> Unit)? = null
) {
    val builder = AlertDialog.Builder(context, R.style.AppTheme_AlertDialogStyle)
    val view = View.inflate(context, R.layout.dialog_info, null)
    val binding = DialogInfoBinding.bind(view)

    builder.setView(binding.root)

    binding.apply {
        titleTV.setText(title)
        infoTV.setText(descrText)
        builder.setPositiveButton(
            context.getString(posButtonText)
        ) { _, _ -> posButtonCallback?.invoke() }

        if (negButtonText != null){
            builder.setNegativeButton(
                context.getString(negButtonText)
            ) { _, _ -> negButtonCallback?.invoke() }
        }

        val dialog = builder.create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.bg_rounded_16dp
            )
        )
    }
}