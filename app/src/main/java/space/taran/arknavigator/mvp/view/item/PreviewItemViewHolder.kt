package space.taran.arknavigator.mvp.view.item

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ortiz.touchview.OnTouchImageViewListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.ui.fragments.utils.PredefinedIcon
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsList
import space.taran.arknavigator.utils.*
import space.taran.arknavigator.utils.extensions.autoDisposeScope
import java.nio.file.Path

//todo join with FileItemViewHolder, it is basically the same, just different sizes
class PreviewItemViewHolder(val binding: ItemImageBinding, val presenter: PreviewsList) :
    RecyclerView.ViewHolder(binding.root),
    PreviewItemView {

    override var pos = -1

    override fun setPredefined(resource: PredefinedIcon): Unit = with(binding.root) {
        binding.ivImage.setImageResource(imageForPredefinedIcon(resource))
    }

    override fun setImage(file: Path?, playVisible: Boolean, extension: String?) {
        if (file != null && extension != null){
            loadZoomImagePlaceholder(file,
                imageForPredefinedExtension(extension),
                binding.ivImage
            )
        }
        else if (file == null)
            binding.ivImage.setImageResource(imageForPredefinedExtension(extension))
        binding.icPlay.isVisible = playVisible
    }

    override fun setPDFPreview(file: Path): Unit = with(binding.root) {
        //Temporary workaround for asynchronous loading.
        //To be changed later on, when indexing will be asynchronous
        binding.layoutProgress.root.isVisible = true
        itemView.autoDisposeScope.launch {
            withContext(Dispatchers.IO){
                if (isPDF(file)){
                    val bitmap = createPdfPreview(file, binding.root.context)
                    withContext(Dispatchers.Main){
                        binding.layoutProgress.root.isVisible = false
                        binding.ivImage.setImageBitmap(bitmap)
                        setZoomEnabled(false)
                    }
                }
                else withContext(Dispatchers.Main){
                    binding.layoutProgress.root.isVisible = false
                    loadZoomImage(file, binding.ivImage)
                    setZoomEnabled(true)
                }
            }
        }
    }

    override fun setZoomEnabled(enabled: Boolean): Unit =
        binding.ivImage.let { touchImageView ->
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

    override fun resetZoom() = binding.ivImage.resetZoom()
}