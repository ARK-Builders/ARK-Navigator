package com.taran.imagemanager.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import co.lujun.androidtagview.ColorFactory
import co.lujun.androidtagview.TagView
import com.taran.imagemanager.R
import com.taran.imagemanager.mvp.model.entity.Folder
import com.taran.imagemanager.mvp.model.entity.Image
import com.taran.imagemanager.mvp.presenter.DetailPresenter
import com.taran.imagemanager.mvp.view.DetailView
import com.taran.imagemanager.ui.App
import com.taran.imagemanager.ui.adapter.DetailVPAdapter
import kotlinx.android.synthetic.main.dialog_tags.view.*
import kotlinx.android.synthetic.main.fragment_detail_view.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter

class DetailFragment: MvpAppCompatFragment(), DetailView {
    companion object {
        const val IMAGES_KEY = "images"
        const val POS_KEY = "pos"
        const val FOLDER_KEY = "folder"

        fun newInstance(images: MutableList<Image>, pos: Int, folder: Folder) = DetailFragment().apply {
            arguments = Bundle().apply {
                putInt(POS_KEY, pos)
                putParcelableArray(IMAGES_KEY, images.toTypedArray())
                putParcelable(FOLDER_KEY, folder)
            }
        }
    }

    var dialogView: View? = null

    @InjectPresenter
    lateinit var presenter: DetailPresenter

    @ProvidePresenter
    fun providePresenter() = DetailPresenter(
        arguments!!.getParcelableArray(IMAGES_KEY)!!.map { it as Image },
        arguments!!.getInt(POS_KEY),
        arguments!![FOLDER_KEY] as Folder
    ).apply {
        App.instance.appComponent.inject(this)
    }

    var adapter: DetailVPAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_detail_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        adapter = DetailVPAdapter(presenter.detailListPresenter)
        view_pager.adapter = adapter
        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                presenter.imageChanged(position)
            }
        })
        tags_image_detail.theme = ColorFactory.NONE
        tags_image_detail.tagBackgroundColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
        tags_image_detail.tagBorderColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
        tags_image_detail.tagTextColor = Color.WHITE
        fab.setOnClickListener {
            presenter.fabClicked()
        }
    }

    override fun setCurrentItem(pos: Int) {
        view_pager.setCurrentItem(pos, false)
    }

    override fun updateAdapter() {
        view_pager.adapter?.notifyDataSetChanged()
    }

    override fun showTagsDialog(imageTags: List<String>, folderTags: List<String>) {
        dialogView = LayoutInflater.from(context!!).inflate(R.layout.dialog_tags, null)
        val alertDialogBuilder = AlertDialog.Builder(context!!).setView(dialogView)
        alertDialogBuilder.show()

        dialogView!!.tags_image.theme = ColorFactory.NONE
        dialogView!!.tags_image.tagBackgroundColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
        dialogView!!.tags_image.tagBorderColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
        dialogView!!.tags_image.tagTextColor = Color.WHITE
        dialogView!!.tags_folder.theme = ColorFactory.NONE
        dialogView!!.tags_folder.tagBackgroundColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
        dialogView!!.tags_folder.tagBorderColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
        dialogView!!.tags_folder.tagTextColor = Color.WHITE

        dialogView!!.tags_image.tags = imageTags
        dialogView!!.tags_folder.tags = folderTags

        dialogView!!.tags_folder.setOnTagClickListener(object : TagView.OnTagClickListener {
            override fun onTagClick(position: Int, text: String?) {
                presenter.tagAdded(dialogView!!.tags_folder.tags[position])
            }

            override fun onTagLongClick(position: Int, text: String?) {}

            override fun onSelectedTagDrag(position: Int, text: String?) {}

            override fun onTagCrossClick(position: Int) {}
        })

        dialogView!!.et_tags.setOnEditorActionListener { v, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tag = dialogView!!.et_tags.text.toString()
                presenter.tagAdded(tag)
            }

            false
        }
    }

    override fun setDialogTags(imageTags: List<String>, folderTags: List<String>) {
        dialogView!!.tags_image.tags = imageTags
        dialogView!!.tags_folder.tags = folderTags
    }

    override fun setImageTags(imageTags: List<String>) {
        tags_image_detail.tags = imageTags
    }
}