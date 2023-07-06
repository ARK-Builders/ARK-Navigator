package space.taran.arknavigator.mvp.view.item

import android.animation.ValueAnimator
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.meta.Metadata
import space.taran.arklib.domain.preview.PreviewLocator
import space.taran.arklib.domain.preview.PreviewStatus
import space.taran.arklib.utils.ImageUtils
import space.taran.arklib.utils.extension
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemFileGridBinding
import space.taran.arknavigator.ui.extra.ExtraLoader
import space.taran.arknavigator.utils.Score
import space.taran.arknavigator.utils.dpToPx
import timber.log.Timber
import java.nio.file.Path

class FileItemViewHolder(
    val binding: ItemFileGridBinding
) : RecyclerView.ViewHolder(binding.root), FileItemView {

    private var joinThumbnailJob: Job? = null

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
        iv.isVisible = true
        progressIv.isVisible = false
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

    override fun setThumbnail(
        path: Path,
        id: ResourceId,
        meta: Metadata,
        locator: PreviewLocator,
        presenterScope: CoroutineScope
    ) = with(binding) {
        joinThumbnailJob?.cancel()

        ExtraLoader.load(
            meta, listOf(primaryExtra, secondaryExtra),
            verbose = false
        )

        if (!locator.isGenerated()) {
            progressIv.isVisible = true
            Timber.d("join preview generation for $id")
            joinThumbnailJob = presenterScope.launch {
                locator.join()

                if (!isActive) return@launch

                withContext(Dispatchers.Main) {
                    progressIv.isVisible = false
                    onThumbnailReady(path, id, locator)
                }
            }
            return
        }

        onThumbnailReady(path, id, locator)
    }

    private fun onThumbnailReady(
        path: Path,
        id: ResourceId,
        locator: PreviewLocator,
    ) = with(binding) {
        val thumbnail = if (locator.check() != PreviewStatus.ABSENT) {
            locator.thumbnail()
        } else {
            null
        }

        val placeholder = ImageUtils.iconForExtension(extension(path))

        ImageUtils.loadThumbnailWithPlaceholder(
            id,
            thumbnail,
            placeholder,
            iv
        )
    }

    override fun setText(title: String, shortName: Boolean) = with(binding) {
        tvTitle.text = title
        tvTitle.maxLines = if (shortName) SHORT_NAME_LINES else Integer.MAX_VALUE
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

    override fun setPinned(isPinned: Boolean) {
        binding.vPinned.isVisible = isPinned
    }

    override fun displayScore(sortByScoresEnabled: Boolean, score: Score) {
        with(binding) {
            if (!sortByScoresEnabled) {
                scoreValue.visibility = View.GONE
                vPriority.visibility = View.GONE
                return
            }
            if (score == 0) {
                scoreValue.visibility = View.GONE
                vPriority.visibility = View.GONE
                return
            }
            scoreValue.apply {
                text = score.toString()
                isVisible = true
            }
            vPriority.isVisible = true
        }
    }

    companion object {
        private const val SHORT_NAME_LINES = 1
        private const val DEFAULT_ELEVATION = 1f
        private const val SELECTED_ELEVATION = 8f
        private const val ANIM_DURATION = 200L
    }
}
