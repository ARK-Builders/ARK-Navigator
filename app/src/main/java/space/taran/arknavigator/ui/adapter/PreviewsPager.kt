package space.taran.arknavigator.ui.adapter

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import space.taran.arknavigator.databinding.ItemImageBinding
import space.taran.arknavigator.mvp.presenter.adapter.PreviewsPagerPresenter
import space.taran.arknavigator.mvp.view.item.PreviewItemViewHolder
import space.taran.arknavigator.ui.App

class PreviewsPager(val presenter: PreviewsPagerPresenter) : RecyclerView.Adapter<PreviewItemViewHolder>() {

    override fun getItemCount() = presenter.getCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PreviewItemViewHolder(
            ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false), presenter
        ).also {
            App.instance.appComponent.inject(it)
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PreviewItemViewHolder, position: Int) {
        holder.pos = position
        presenter.bindView(holder)
        val gestureDetector = getGestureDetector(holder)
        holder.binding.ivImage.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP)
                view.performClick()
            gestureDetector.onTouchEvent(motionEvent)
            return@setOnTouchListener true
        }
    }

    fun removeItem(position: Int) {
        presenter.remove(position)
        super.notifyItemRemoved(position)
    }

    private fun getGestureDetector(holder: PreviewItemViewHolder): GestureDetectorCompat {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                presenter.onItemClick(holder)
                return true
            }

        }
        return GestureDetectorCompat(holder.itemView.context, listener)
    }
}