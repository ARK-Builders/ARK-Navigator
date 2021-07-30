package space.taran.arkbrowser.mvp.view.item

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_image.view.*
import space.taran.arkbrowser.mvp.model.dao.common.PredefinedIcon
import space.taran.arkbrowser.utils.imageForPredefinedIcon
import space.taran.arkbrowser.utils.loadImage
import java.nio.file.Path

//todo join with FileItemViewHolder, it is basically the same, just different sizes
class PreviewItemViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView),
    LayoutContainer, PreviewItemView {

    override var pos = -1

    override fun setPredefined(resource: PredefinedIcon): Unit = with(containerView) {
        iv_image.setImageResource(imageForPredefinedIcon(resource))
    }

    override fun setImage(file: Path): Unit = with(containerView) {
        loadImage(file, iv_image)
    }

    fun setOnClickHandler(handler: () -> Unit) = with(containerView) {
        iv_image.setOnClickListener {
            handler()
        }
    }
}