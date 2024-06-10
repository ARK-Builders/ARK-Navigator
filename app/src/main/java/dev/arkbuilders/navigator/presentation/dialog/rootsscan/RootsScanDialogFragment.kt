package dev.arkbuilders.navigator.presentation.dialog.rootsscan

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import moxy.MvpBottomSheetDialogFragment
import moxy.ktx.moxyPresenter
import dev.arkbuilders.navigator.R
import dev.arkbuilders.navigator.databinding.DialogRootsScanBinding
import dev.arkbuilders.navigator.presentation.App
import dev.arkbuilders.navigator.presentation.utils.toast
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class RootsScanDialogFragment : MvpBottomSheetDialogFragment(), RootsScanView {
    private lateinit var binding: DialogRootsScanBinding
    private val presenter by moxyPresenter {
        RootsScanDialogPresenter().apply {
            App.instance.appComponent.inject(this)
        }
    }
    private val defaultRootsFolders =
        listOf("DCIM/Camera", "Documents", "Pictures", "Downloads")

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
        initDefaultRootsInfo(defaultRootsFolders)
    }

    private fun initDefaultRootsInfo(rootsList: List<String>) {
        rootsList.forEach { root ->
            val view = CheckBox(context)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = params
            view.text = root
            view.isChecked = true
            val selectColor =
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            val normalColor = ContextCompat.getColor(requireContext(), R.color.gray)
            view.setTextColor(selectColor)
            view.buttonTintList = ColorStateList.valueOf(Color.BLUE)
            view.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    view.setTextColor(selectColor)
                } else {
                    view.setTextColor(normalColor)
                }
            }
            binding.layoutDefaultRoot.addView(view)
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
        val finalRoots: MutableList<Path> = mutableListOf()
        val checkedFolders: MutableList<String> = mutableListOf()
        val count = binding.layoutDefaultRoot.childCount
        for (i in 0 until count) {
            val child = binding.layoutDefaultRoot.getChildAt(i) as CheckBox
            if (child.isChecked) {
                checkedFolders.add(defaultRootsFolders[i])
            }
        }
        roots.forEach { root ->
            var foundDefaultFolder = false
            checkedFolders.forEach {
                val defaultRoot = Paths.get(root.toString() + '/' + it)
                if (defaultRoot.exists()) {
                    finalRoots.add(defaultRoot)
                    foundDefaultFolder = true
                }
            }
            if (!foundDefaultFolder) {
                finalRoots.add(root)
            }
        }

        val bundle = Bundle().apply {
            putStringArray(
                RESULT_KEY_ROOTS,
                finalRoots.map(Path::toString).toTypedArray()
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
