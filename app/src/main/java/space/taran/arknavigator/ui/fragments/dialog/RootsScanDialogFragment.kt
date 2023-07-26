package dev.arkbuilders.navigator.ui.fragments.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import moxy.MvpBottomSheetDialogFragment
import moxy.ktx.moxyPresenter
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.DialogRootsScanBinding
import dev.arkbuilders.navigator.mvp.presenter.dialog.RootsScanDialogPresenter
import dev.arkbuilders.navigator.mvp.view.dialog.RootsScanView
import dev.arkbuilders.navigator.ui.fragments.utils.toast
import java.nio.file.Path

class RootsScanDialogFragment : MvpBottomSheetDialogFragment(), RootsScanView {
    private lateinit var binding: DialogRootsScanBinding
    private val presenter by moxyPresenter {
        RootsScanDialogPresenter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogRootsScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun init() = with(binding) {
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        layoutConfirm.visibility = View.VISIBLE
        layoutScanCompleted.visibility = View.INVISIBLE
        layoutScanning.visibility = View.INVISIBLE
        btnOk.setOnClickListener {
            dismiss()
        }
        btnEnough.setOnClickListener {
            presenter.onEnoughBtnClick()
        }
        btnSkip.setOnClickListener {
            dismiss()
        }
        btnScan.setOnClickListener {
            presenter.onScanBtnClick()
        }
    }

    override fun startScan() = with(binding) {
        layoutConfirm.visibility = View.INVISIBLE
        layoutScanCompleted.visibility = View.INVISIBLE
        layoutScanning.visibility = View.VISIBLE
        tvScanning.setVisibilityAndLoadingStatus(View.VISIBLE)
        tvScanning.loadingText = getString(R.string.scanning)
    }

    override fun scanCompleted(foundRoots: Int) = with(binding) {
        tvScanning.setVisibilityAndLoadingStatus(View.GONE)
        layoutConfirm.visibility = View.INVISIBLE
        layoutScanCompleted.visibility = View.VISIBLE
        layoutScanning.visibility = View.INVISIBLE
        tvFoundRootsCompleted.text =
            getString(R.string.root_scanning_found, foundRoots)
    }

    override fun setProgress(foundRoots: Int) = with(binding) {
        tvFoundRoots.text = getString(R.string.root_scanning_found, foundRoots)
    }

    override fun toastFolderSkip(folder: Path) =
        toast(R.string.skipping_folder, folder.toString())

    override fun closeDialog() = dismiss()

    override fun notifyRootsFound(roots: List<Path>) {
        val bundle = Bundle().apply {
            putStringArray(
                RESULT_KEY_ROOTS,
                roots.map(Path::toString).toTypedArray()
            )
        }
        setFragmentResult(REQUEST_KEY_ROOTS_FOUND, bundle)
    }

    override fun getTheme() = R.style.RoundBottomSheetDialogTheme

    companion object {
        const val REQUEST_KEY_ROOTS_FOUND = "rootFound"
        const val RESULT_KEY_ROOTS = "roots"

        fun newInstance() = RootsScanDialogFragment()
    }
}
