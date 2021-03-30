package space.taran.arkbrowser.ui.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import space.taran.arkbrowser.R
import space.taran.arkbrowser.mvp.model.entity.File
import space.taran.arkbrowser.mvp.model.entity.Root
import space.taran.arkbrowser.mvp.model.entity.common.TagState
import space.taran.arkbrowser.mvp.presenter.TagsPresenter
import space.taran.arkbrowser.mvp.view.TagsView
import space.taran.arkbrowser.ui.App
import space.taran.arkbrowser.ui.MainActivity
import space.taran.arkbrowser.ui.adapter.FileGridRVAdapter
import kotlinx.android.synthetic.main.fragment_tags.*
import moxy.MvpAppCompatFragment
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter


class TagsFragment(val root: Root? = null, val files: List<File>? = null, val state: TagsPresenter.State? = null) : MvpAppCompatFragment(), TagsView {

    @InjectPresenter
    lateinit var presenter: TagsPresenter

    @ProvidePresenter
    fun providePresenter() =
        TagsPresenter(root, files, state!!).apply {
            App.instance.appComponent.inject(this)
        }

    var adapter: FileGridRVAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tags, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        App.instance.appComponent.inject(this)
    }

    override fun init() {
        (activity as MainActivity).setToolbarVisibility(true)
        rv_tags.layoutManager = GridLayoutManager(context, 3)
        adapter = FileGridRVAdapter(presenter.fileGridPresenter)
        rv_tags.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun openFile(uri: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_EDIT)
        val fileUri: Uri = Uri.parse(uri)
        intent.setDataAndType(fileUri, mimeType)
        startActivity(intent)
    }

    override fun updateAdapter() {
        adapter?.notifyDataSetChanged()
    }

    override fun setToolbarTitle(title: String) {
        (activity as MainActivity).setTitle(title)
    }


    override fun setTags(tags: List<TagState>) {
        tags.forEach { tagState ->
            val chip = Chip(context)
            chip.isCheckable = true
            chip.text = tagState.tag
            chip.isChecked = tagState.isChecked
            if (tagState.isActual) {
                chip.chipBackgroundColor =
                    ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.colorPrimary))
                chip.setTextColor(ContextCompat.getColor(context!!, R.color.white))
            }
            chip.setOnClickListener {
                presenter.tagChecked(tagState.tag, chip.isChecked)
            }
            chipg_tags.addView(chip)
        }

        if (tags.size > 1) {
            val chip = Chip(context)
            chip.chipIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_close)
            chip.textStartPadding = 0f
            chip.textEndPadding = 0f
            chip.setOnClickListener {
                presenter.clearTagsChecked()
            }
            chipg_tags.addView(chip)
        }
    }



    override fun clearTags() {
        chipg_tags.removeAllViews()
    }
}