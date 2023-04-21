package space.taran.arknavigator.ui.fragments.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.WindowManager
import org.apache.commons.io.FileUtils
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogResourceInfoBinding
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.meta.Metadata
import space.taran.arknavigator.ui.extra.ExtraLoader
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class DetailsAlertDialog(
    val path: Path,
    val resource: Resource,
    val metadata: Metadata,
    context: Context
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dialogResourceInfoBinding = DialogResourceInfoBinding.inflate(
            layoutInflater
        )
        setContentView(dialogResourceInfoBinding.root)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val back = ColorDrawable(Color.TRANSPARENT)
        val inset = InsetDrawable(back, 30)
        window?.setBackgroundDrawable(inset)
        // common resources for all file type
        dialogResourceInfoBinding.resourceId.text = context.getString(
            R.string.resource_id_label,
            resource.id.crc32
        )
        dialogResourceInfoBinding.resourceName.text = context.getString(
            R.string.resource_name_label,
            resource.name
        )
        dialogResourceInfoBinding.resourcePath.text = context.getString(
            R.string.resource_path_label,
            path.absolutePathString()
        )
        dialogResourceInfoBinding.resourceSize.text = context.getString(
            R.string.resource_size_label,
            FileUtils.byteCountToDisplaySize(resource.size())
        )

        // load specific metadata from specific kind
        ExtraLoader.loadWithLabel(
            metadata,
            listOf(
                dialogResourceInfoBinding.resourceResolution,
                dialogResourceInfoBinding.resourceDuration,
                dialogResourceInfoBinding.resourceLink
            )
        )

        dialogResourceInfoBinding.layoutInfo.setOnClickListener {
            dismiss()
        }
    }

    override fun show() {
        super.show()
        (
            window?.decorView?.layoutParams as WindowManager.LayoutParams?
            )?.horizontalMargin = 50f
    }
}
