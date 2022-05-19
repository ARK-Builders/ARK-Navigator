package space.taran.arknavigator.ui.fragments.dialog

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import moxy.MvpAppCompatDialogFragment
import moxy.ktx.moxyPresenter
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.DialogRootsNewBinding
import space.taran.arknavigator.mvp.presenter.dialog.FolderPickerDialogPresenter
import space.taran.arknavigator.mvp.view.dialog.FolderPickerDialogView
import space.taran.arknavigator.ui.App
import space.taran.arknavigator.ui.adapter.FoldersRVAdapter
import space.taran.arknavigator.ui.fragments.utils.toast
import java.nio.file.Path
import kotlin.io.path.Path

class FolderPickerDialogFragment :
    MvpAppCompatDialogFragment(),
    FolderPickerDialogView {

    private lateinit var binding: DialogRootsNewBinding
    private val presenter by moxyPresenter {
        FolderPickerDialogPresenter(
            (requireArguments().getStringArray(INIT_PATHS_KEY))!!.map { Path(it) }
        ).apply {
            App.instance.appComponent.inject(this)
        }
    }

    private var foldersAdapter: FoldersRVAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogRootsNewBinding.inflate(inflater)
        return binding.root
    }

    override fun init() {
        initBackButtonListener()
        binding.rvRootsDialog.layoutManager = GridLayoutManager(context, 2)
        foldersAdapter = FoldersRVAdapter(presenter.gridPresenter)
        binding.rvRootsDialog.adapter = foldersAdapter
        binding.btnRootsDialogPick.setOnClickListener {
            presenter.onPickBtnClick()
        }
        binding.btnRootsDialogCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun notifyPathPicked(path: Path, rootNotFavorite: Boolean) {
        parentFragmentManager.setFragmentResult(
            REQUEST_PATH_PICKED_KEY,
            bundleOf().apply {
                putString(RESULT_PATH_KEY, path.toString())
                putBoolean(RESULT_ROOT_NOT_FAVORITE_KEY, rootNotFavorite)
            }
        )
        dismiss()
    }

    override fun setPickBtnState(isEnabled: Boolean, isRootNotFavorite: Boolean) {
        binding.btnRootsDialogPick.text =
            requireContext().getString(
                if (isRootNotFavorite) R.string.folders_pick_root
                else R.string.folders_pick_favorite
            )
        binding.btnRootsDialogPick.isEnabled = isEnabled
    }

    override fun toastFileChosenAsRoot() =
        toast(R.string.folders_file_chosen_as_root)

    override fun toastDeviceChosenAsRoot() =
        toast(R.string.folders_device_chosen_as_root)
    override fun showToast(msg: String) {
        Toast.makeText(
            requireContext(),
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun setFolderName(folderName: String) {
        binding.tvRootsDialogPath.text = folderName
    }

    override fun updateFolders() {
        foldersAdapter?.notifyDataSetChanged()
    }

    private fun initBackButtonListener() {
        requireDialog().setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                keyEvent.action == KeyEvent.ACTION_UP
            ) {
                if (!presenter.onBackClick())
                    dismiss()
            }
            return@setOnKeyListener true
        }
    }

    companion object {
        private const val INIT_PATHS_KEY = "openPaths"
        const val REQUEST_PATH_PICKED_KEY = "pathPicked"
        const val RESULT_PATH_KEY = "path"
        const val RESULT_ROOT_NOT_FAVORITE_KEY = "rootNotFavorite"

        fun newInstance(paths: List<Path>) = FolderPickerDialogFragment().apply {
            arguments = Bundle().apply {
                putStringArray(
                    INIT_PATHS_KEY,
                    paths.map { it.toString() }.toTypedArray()
                )
            }
        }
    }
}
