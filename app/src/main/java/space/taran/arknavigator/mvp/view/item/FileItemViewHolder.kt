package space.taran.arknavigator.mvp.view.item

import android.animation.ValueAnimator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta
import space.taran.arknavigator.mvp.model.repo.preview.PreviewAndThumbnail
import space.taran.arknavigator.ui.extra.ExtraLoader
import space.taran.arknavigator.utils.ImageUtils
import space.taran.arknavigator.utils.dpToPx
import space.taran.arknavigator.utils.extension
import java.nio.file.Path

class FileItemViewHolder(
    val binding: ItemFileGridBinding
) : RecyclerView.ViewHolder(binding.root), FileItemView {

    override fun position(): Int = this.layoutPosition

    override fun setFolderIcon() =
        binding.iv.setImageResource(R.drawable.ic_baseline_folder)

    override fun setGenericIcon(path: Path) {
        val placeholder = ImageUtils.iconForExtension(extension(path))
        binding.iv.setImageResource(placeholder)
    }

    override fun reset(
        isSelectingEnabled: Boolean,
        isItemSelected: Boolean
    ) = with(binding) {
        cbSelected.isVisible = isSelectingEnabled
        cbSelected.isChecked = isItemSelected
        val elevation = if (isSelectingEnabled && isItemSelected)
            SELECTED_ELEVATION else DEFAULT_ELEVATION
        root.elevation = this.root.context.dpToPx(elevation)
    }

    override fun setSelected(isItemSelected: Boolean) =
        with(binding) {
            cbSelected.isChecked = isItemSelected
            val startElevation =
                if (isItemSelected) DEFAULT_ELEVATION else SELECTED_ELEVATION
            val endElevation =
                if (isItemSelected) SELECTED_ELEVATION else DEFAULT_ELEVATION
            val animator = ValueAnimator.ofFloat(startElevation, endElevation)
            animator.duration = ANIM_DURATION
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                root.elevation = root.context.dpToPx(value)
            }
            animator.start()
            return@with
        }

    override fun setIconOrPreview(path: Path, resource: ResourceMeta): Unit =
        with(binding.root) {
            val placeholder = ImageUtils.iconForExtension(extension(path))
            val thumbnail = PreviewAndThumbnail.locate(path, resource)?.thumbnail

            ImageUtils.loadThumbnailWithPlaceholder(
                resource.id,
                thumbnail,
                placeholder,
                binding.iv
            )
            ExtraLoader.load(
                resource, listOf(binding.primaryExtra, binding.secondaryExtra),
                verbose = false
            )
        }

    override fun setText(title: String) = with(binding.root) {
        binding.tvTitle.text = title
    }

    fun onSelectingChanged(enabled: Boolean) {
        if (!enabled && binding.cbSelected.isChecked) {
            val animator =
                ValueAnimator.ofFloat(SELECTED_ELEVATION, DEFAULT_ELEVATION)
            animator.duration = ANIM_DURATION
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                binding.root.elevation = binding.root.context.dpToPx(value)
            }
            animator.start()
        }
        animateCheckboxVisibility(enabled)
    }

    private fun animateCheckboxVisibility(isVisible: Boolean) = with(binding) {
        if (isVisible) {
            cbSelected.isVisible = true
            cbSelected.alpha = 0f
            cbSelected.animate().apply {
                duration = ANIM_DURATION
                alpha(1f)
            }
        } else {
            cbSelected.alpha = 1f
            cbSelected.isChecked = false
            cbSelected.animate().apply {
                duration = ANIM_DURATION
                alpha(0f)
                withEndAction {
                    cbSelected.isVisible = false
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_ELEVATION = 1f
        private const val SELECTED_ELEVATION = 8f
        private const val ANIM_DURATION = 200L
    }
}
