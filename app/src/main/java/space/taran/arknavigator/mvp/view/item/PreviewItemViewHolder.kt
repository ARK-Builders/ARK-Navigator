package space.taran.arknavigator.mvp.view.item

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.ortiz.touchview.OnTouchImageViewListener
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_image.view.*
import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.utils.imageForPredefinedIcon
import space.taran.arknavigator.utils.loadZoomImage
import java.nio.file.Path

//todo join with FileItemViewHolder, it is basically the same, just different sizes
class PreviewItemViewHolder(override val containerView: View, val presenter: PreviewsList) :
    RecyclerView.ViewHolder(containerView),
    LayoutContainer, PreviewItemView {

    override var pos = -1

    override fun setPredefined(resource: PredefinedIcon): Unit = with(containerView) {
        iv_image.setImageResource(imageForPredefinedIcon(resource))
    }

    override fun setImage(file: Path): Unit = with(containerView) {
        loadZoomImage(file, iv_image)
    }

    override fun setZoomEnabled(enabled: Boolean): Unit =
        containerView.iv_image.let { touchImageView ->
            touchImageView.isZoomEnabled = enabled
            if (enabled) {
                touchImageView.setOnTouchImageViewListener(object : OnTouchImageViewListener {
                    override fun onMove() {
                        presenter.onImageZoom(touchImageView.isZoomed)
                    }
                })
            } else {
                touchImageView.setOnTouchImageViewListener(object : OnTouchImageViewListener {
                    override fun onMove() {}
                })
            }
        }

    override fun resetZoom() = containerView.iv_image.resetZoom()
}