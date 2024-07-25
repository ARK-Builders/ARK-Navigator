package dev.arkbuilders.navigator.presentation.dialog

import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.arkbuilders.navigator.presentation.App
import kotlinx.coroutines.launch
import dev.arkbuilders.arklib.data.folders.FoldersRepo
import dev.arkbuilders.components.filepicker.ArkFilePickerConfig
import dev.arkbuilders.components.filepicker.ArkFilePickerFragment
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.Path

class RootPickerDialogFragment : ArkFilePickerFragment() {
    @Inject
    lateinit var foldersRepo: FoldersRepo

    private var rootNotFavorite = false

    override fun onAttach(context: Context) {
        App.instance.appComponent.inject(this)
        super.onAttach(context)
    }

    override fun onFolderChanged(currentFolder: Path) {
        lifecycleScope.launch {
            val folders = foldersRepo.provideFolders()
            val roots = folders.keys
            val favorites = folders.values.flatten()
            val root = roots.find { root -> currentFolder.startsWith(root) }
            root?.let {
                if (root == currentFolder) {
                    rootNotFavorite = true
                    binding.btnPick.text = "Root"
                    binding.btnPick.isEnabled = false
                } else {
                    var foundAsFavorite = false
                    favorites.forEach {
                        if (currentFolder.endsWith(it)) {
                            foundAsFavorite = true
                            return@forEach
                        }
                    }
                    rootNotFavorite = false
                    binding.btnPick.text = "Favorite"
                    binding.btnPick.isEnabled = !foundAsFavorite
                }
            } ?: let {
                rootNotFavorite = true
                binding.btnPick.text = "Root"
                binding.btnPick.isEnabled = true
            }
        }
    }

    override fun onPick(pickedPath: Path) {
        setFragmentResult(
            ROOT_PICKED_REQUEST_KEY,
            bundleOf().apply {
                putString(PICKED_PATH_BUNDLE_KEY, pickedPath.toString())
                putBoolean(ROOT_NOT_FAV_BUNDLE_KEY, rootNotFavorite)
            }
        )
    }

    companion object {
        fun newInstance(initialPath: Path?) = RootPickerDialogFragment().apply {
            setConfig(
                ArkFilePickerConfig(
                    initialPath = initialPath,
                    pathPickedRequestKey = "notUsed"
                )
            )
        }
    }
}

private const val ROOT_PICKED_REQUEST_KEY = "rootPicked"
private const val PICKED_PATH_BUNDLE_KEY = "pickedPath"
private const val ROOT_NOT_FAV_BUNDLE_KEY = "rootNotFav"

fun FragmentManager.onRootOrFavPicked(
    lifecycleOwner: LifecycleOwner,
    listener: (path: Path, rootNotFavorite: Boolean) -> Unit
) {
    setFragmentResultListener(
        ROOT_PICKED_REQUEST_KEY,
        lifecycleOwner
    ) { _, bundle ->
        listener(
            Path(
                bundle.getString(PICKED_PATH_BUNDLE_KEY)!!
            ),
            bundle.getBoolean(ROOT_NOT_FAV_BUNDLE_KEY)
        )
    }
}
