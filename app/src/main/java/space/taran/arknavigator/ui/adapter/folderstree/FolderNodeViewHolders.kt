package space.taran.arknavigator.ui.adapter.folderstree

import android.animation.ValueAnimator
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemViewFolderTreeDeviceBinding
import space.taran.arknavigator.databinding.ItemViewFolderTreeFavoriteBinding
import space.taran.arknavigator.databinding.ItemViewFolderTreeRootBinding
import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FoldersTreePresenter

abstract class FolderNodeView(root: View, val presenter: FoldersTreePresenter) :
    RecyclerView.ViewHolder(root) {
    protected var chevron: View? = null
    protected var hasExpdanded: Boolean = false

    protected var animator = ValueAnimator().apply {
        duration = 500L
        addUpdateListener {
            chevron?.rotation = animatedValue as Float
        }
    }

    fun position(): Int = layoutPosition

    fun setExpanded(expanded: Boolean) {
        if (hasExpdanded == expanded)
            return

        if (expanded) {
            animator.setFloatValues(0F, 90F)
            animator.start()
        } else {
            animator.setFloatValues(90F, 00F)
            animator.start()
        }

        hasExpdanded = expanded
    }

    abstract fun setName(name: String)
}

class DeviceNodeViewHolder(
    private val binding: ItemViewFolderTreeDeviceBinding,
    presenter: FoldersTreePresenter
) : FolderNodeView(binding.root, presenter) {
    init {
        chevron = binding.chevron
        binding.root.setOnClickListener {
            presenter.onItemClick(this)
        }
    }

    override fun setName(name: String) {
        binding.nameTxt.text = name
    }
}

class RootNodeViewHolder(
    private val binding: ItemViewFolderTreeRootBinding,
    presenter: FoldersTreePresenter
) : FolderNodeView(binding.root, presenter) {
    init {
        chevron = binding.chevron
        binding.root.setOnClickListener {
            presenter.onItemClick(this)
        }
        binding.addBtn.setOnClickListener {
            presenter.onAddFolderBtnClick(position())
        }
        binding.navigateBtn.setOnClickListener {
            presenter.onNavigateBtnClick(this)
        }
    }

    override fun setName(name: String) {
        binding.nameTxt.text = name
    }
}

class FavoriteViewHolder(
    private val binding: ItemViewFolderTreeFavoriteBinding,
    presenter: FoldersTreePresenter
) : FolderNodeView(binding.root, presenter) {
    init {
        binding.navigateBtn.setOnClickListener {
            presenter.onNavigateBtnClick(this)
        }
    }

    override fun setName(name: String) {
        binding.nameTxt.text = name
    }
}