package dev.arkbuilders.navigator.presentation.screen.resources.adapter

import android.animation.ValueAnimator
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.arkbuilders.navigator.databinding.ItemFileGridBinding
import dev.arkbuilders.navigator.presentation.utils.extra.ExtraLoader
import dev.arkbuilders.arklib.domain.score.Score
import dev.arkbuilders.navigator.presentation.utils.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.domain.meta.Metadata
import dev.arkbuilders.arklib.domain.preview.PreviewLocator
import dev.arkbuilders.arklib.domain.preview.PreviewStatus
import dev.arkbuilders.arklib.utils.ImageUtils
import dev.arkbuilders.arklib.utils.extension
import timber.log.Timber
import java.nio.file.Path

class FileItemViewHolder(
    val binding: ItemFileGridBinding
) : RecyclerView.ViewHolder(binding.root) {

    private var joinThumbnailJob: Job? = null

    fun position(): Int = this.layoutPosition

    fun reset(
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

    fun setSelected(isItemSelected: Boolean) =
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

    fun setThumbnail(
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

    fun setText(title: String, shortName: Boolean) = with(binding) {
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

    fun setPinned(isPinned: Boolean) {
        binding.vPinned.isVisible = isPinned
    }

    fun displayScore(sortByScoresEnabled: Boolean, score: Score) {
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
